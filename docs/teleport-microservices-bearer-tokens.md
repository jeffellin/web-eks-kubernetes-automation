# Teleport with Spring Microservices and Bearer Tokens

**Question:** Can the Teleport proxy be used for microservices that require Bearer tokens?

**Answer:** Yes! Teleport can absolutely be used for microservice authentication with Bearer tokens. Here are the different patterns:

## Pattern 1: User Context Propagation (Forwarding JWTs)

Your current setup can be extended to microservices by forwarding the JWT:

### Calling Service

```java
// In your Spring microservice that calls another service
@Service
public class OrderService {

    @Autowired
    private RestTemplate restTemplate;

    public Order createOrder(OrderRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            // Forward the JWT to another microservice as Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());

            HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);
            return restTemplate.postForObject(
                "http://inventory-service/api/orders",
                entity,
                Order.class
            );
        }
    }
}
```

### Receiving Microservice

**application.properties:**
```properties
# inventory-service application.properties
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://ellinj.teleport.sh/.well-known/jwks.json
```

**Security Configuration:**
```java
// Standard OAuth2 Resource Server config - looks for Authorization: Bearer
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

**Benefits:**
- ✅ User identity flows through all services
- ✅ Standard `Authorization: Bearer` header
- ✅ Each service validates JWT independently
- ✅ Works with Spring's default OAuth2 Resource Server

## Pattern 2: Teleport Machine ID (Service-to-Service Auth)

For **service accounts** (not user context), Teleport has **Machine ID**:

### Bot Configuration

```yaml
# Bot configuration for your microservice
kind: bot
version: v1
metadata:
  name: inventory-service-bot
spec:
  roles:
    - inventory-service-role
```

### Service Implementation

```java
@Service
public class TeleportMachineIdClient {

    private String getServiceToken() {
        // Use Teleport's tbot to get a service token
        // tbot runs as sidecar and refreshes tokens
        return Files.readString(Path.of("/var/run/tbot/token"));
    }

    public Order callInventoryService(OrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getServiceToken());

        HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(
            "http://inventory-service/api/orders",
            entity,
            Order.class
        );
    }
}
```

### Kubernetes Deployment

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: order-service
spec:
  containers:
  - name: order-service
    image: your-app:latest
  - name: tbot
    image: public.ecr.aws/gravitational/tbot
    args:
      - start
      - --destination-dir=/var/run/tbot
      - --join-method=kubernetes
    volumeMounts:
    - name: tbot-token
      mountPath: /var/run/tbot
  volumes:
  - name: tbot-token
    emptyDir: {}
```

## Pattern 3: Service Mesh Integration

Teleport can integrate with service meshes that handle microservice auth:

### With Istio

```yaml
# Teleport can issue SPIFFE certificates for Istio
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
spec:
  mtls:
    mode: STRICT
```

Services authenticate via mTLS, no Bearer tokens needed!

## Pattern 4: Custom Header → Bearer Header Adapter

You could create an adapter for internal services:

```java
@Component
public class TeleportToStandardJwtConverter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String teleportJwt = httpRequest.getHeader("Teleport-Jwt-Assertion");

        if (teleportJwt != null && httpRequest.getHeader("Authorization") == null) {
            // Wrap request to add standard Authorization header
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getHeader(String name) {
                    if ("Authorization".equals(name)) {
                        return "Bearer " + teleportJwt;
                    }
                    return super.getHeader(name);
                }
            };
            chain.doFilter(wrapper, response);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

## Recommended Architecture for Microservices

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ 1. User authenticates
       ▼
┌─────────────────┐
│ Teleport Proxy  │
│ (Entry Point)   │
└────────┬────────┘
         │ 2. Injects JWT in Teleport-Jwt-Assertion
         ▼
┌─────────────────┐
│  API Gateway    │ (Your Spring App)
│  (Edge Service) │ - Extracts JWT from Teleport header
└────────┬────────┘ - Validates signature
         │ 3. Forwards with Authorization: Bearer <jwt>
         │
    ┌────┴────────────┬──────────────┐
    ▼                 ▼              ▼
┌──────────┐   ┌──────────┐   ┌──────────┐
│ Service  │   │ Service  │   │ Service  │
│    A     │   │    B     │   │    C     │
└──────────┘   └──────────┘   └──────────┘
  - Validates JWT using same JWKS
  - Uses standard OAuth2 Resource Server config
  - Looks for Authorization: Bearer header
```

## Implementation Example

### Edge Service (Receives from Teleport)

```java
// SecurityConfig with custom header resolver (what you have)
@Bean
public TeleportJwtBearerTokenResolver bearerTokenResolver() {
    return new TeleportJwtBearerTokenResolver(); // Custom header
}
```

### Internal Services (Standard Bearer)

```java
// No custom configuration needed!
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwkSetUri("https://ellinj.teleport.sh/.well-known/jwks.json")
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            )
        );
        return http.build();
    }
}
```

### Forwarding the JWT

#### Using RestTemplate

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Interceptor to add Authorization header to all outgoing requests
        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                request.getHeaders().setBearerAuth(jwtAuth.getToken().getTokenValue());
            }

            return execution.execute(request, body);
        }));

        return restTemplate;
    }
}
```

#### Using WebClient (More Modern)

```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .filter((request, next) -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth instanceof JwtAuthenticationToken jwtAuth) {
                    request.headers().setBearerAuth(jwtAuth.getToken().getTokenValue());
                }

                return next.exchange(request);
            })
            .build();
    }
}
```

## Summary

| Pattern | Use Case | Bearer Token? |
|---------|----------|---------------|
| **User Context Propagation** | User identity flows through services | ✅ Yes - forward JWT as Bearer |
| **Machine ID** | Service-to-service (no user) | ✅ Yes - service gets its own JWT |
| **Service Mesh** | High-security microservices | ❌ No - uses mTLS instead |
| **Edge Translation** | One edge service, standard internals | ✅ Yes - convert at edge |

## Bottom Line

**Yes, Teleport works great with microservices and Bearer tokens!** You just need to:

1. **For user requests:** Forward the JWT between services using the standard `Authorization: Bearer` header
2. **For service accounts:** Use Teleport Machine ID to get service-specific JWTs
3. **For high security:** Consider service mesh integration with mTLS

The key insight is that your edge service (the one receiving requests from Teleport) uses the custom `Teleport-Jwt-Assertion` header, but internal microservices can use standard Bearer token authentication.
