package com.example.demo.config; // Adjust package name to match your project

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Component
@Order(1)
public class DebugLoggingFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        System.out.println("\n========== INCOMING REQUEST ==========");
        System.out.println("Method: " + httpRequest.getMethod());
        System.out.println("Request URI: " + httpRequest.getRequestURI());
        System.out.println("Query String: " + httpRequest.getQueryString());
        System.out.println("Remote Addr: " + httpRequest.getRemoteAddr());
        
        System.out.println("\n--- HEADERS ---");
        Collections.list(httpRequest.getHeaderNames()).forEach(headerName -> 
            System.out.println(headerName + ": " + httpRequest.getHeader(headerName))
        );
        
        chain.doFilter(request, response);
        
        System.out.println("\n--- RESPONSE ---");
        System.out.println("Status: " + httpResponse.getStatus());
        System.out.println("Location Header: " + httpResponse.getHeader("Location"));
        System.out.println("========== END REQUEST ==========\n");
    }
}