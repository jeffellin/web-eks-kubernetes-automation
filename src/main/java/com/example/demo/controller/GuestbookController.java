package com.example.demo.controller;

import com.example.demo.entity.Guestbook;
import com.example.demo.repository.GuestbookRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/guestbook")
@CrossOrigin(origins = "*")
/**
 * Comment area
 */
public class GuestbookController {

    @Autowired
    private GuestbookRepository guestbookRepository;

    @GetMapping
    public ResponseEntity<List<Guestbook>> getAllEntries(
            @RequestParam(required = false) String sortBy) {
        
        List<Guestbook> entries;
        if ("date".equals(sortBy)) {
            entries = guestbookRepository.findAllByOrderByCreatedAtDesc();
        } else {
            entries = guestbookRepository.findAll();
        }
        
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Guestbook> getEntryById(@PathVariable Long id) {
        Optional<Guestbook> entry = guestbookRepository.findById(id);
        
        if (entry.isPresent()) {
            return ResponseEntity.ok(entry.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Guestbook> createEntry(@Valid @RequestBody Guestbook guestbook) {
        try {
            Guestbook savedEntry = guestbookRepository.save(guestbook);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEntry);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Guestbook> updateEntry(
            @PathVariable Long id, 
            @Valid @RequestBody Guestbook updatedGuestbook) {
        
        Optional<Guestbook> existingEntry = guestbookRepository.findById(id);
        
        if (existingEntry.isPresent()) {
            Guestbook entry = existingEntry.get();
            entry.setName(updatedGuestbook.getName());
            entry.setEmail(updatedGuestbook.getEmail());
            entry.setComment(updatedGuestbook.getComment());
            
            Guestbook savedEntry = guestbookRepository.save(entry);
            return ResponseEntity.ok(savedEntry);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Guestbook> partialUpdateEntry(
            @PathVariable Long id,
            @RequestBody Guestbook partialUpdate) {
        
        Optional<Guestbook> existingEntry = guestbookRepository.findById(id);
        
        if (existingEntry.isPresent()) {
            Guestbook entry = existingEntry.get();
            
            if (partialUpdate.getName() != null && !partialUpdate.getName().trim().isEmpty()) {
                entry.setName(partialUpdate.getName());
            }
            if (partialUpdate.getEmail() != null && !partialUpdate.getEmail().trim().isEmpty()) {
                entry.setEmail(partialUpdate.getEmail());
            }
            if (partialUpdate.getComment() != null && !partialUpdate.getComment().trim().isEmpty()) {
                entry.setComment(partialUpdate.getComment());
            }
            
            Guestbook savedEntry = guestbookRepository.save(entry);
            return ResponseEntity.ok(savedEntry);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        if (guestbookRepository.existsById(id)) {
            guestbookRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Guestbook>> searchEntries(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String comment) {
        
        List<Guestbook> results;
        
        if (name != null && !name.trim().isEmpty()) {
            results = guestbookRepository.findByNameContainingIgnoreCase(name);
        } else if (email != null && !email.trim().isEmpty()) {
            results = guestbookRepository.findByEmail(email);
        } else if (comment != null && !comment.trim().isEmpty()) {
            results = guestbookRepository.findByCommentContaining(comment);
        } else {
            results = guestbookRepository.findAll();
        }
        
        return ResponseEntity.ok(results);
    }
}