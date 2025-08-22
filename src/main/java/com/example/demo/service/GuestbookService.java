package com.example.demo.service;

import com.example.demo.entity.Guestbook;
import com.example.demo.repository.GuestbookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GuestbookService {

    @Autowired
    private GuestbookRepository repository;

    public List<Guestbook> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Guestbook> findById(Long id) {
        return repository.findById(id);
    }

    public Guestbook save(Guestbook guestbook) {
        return repository.save(guestbook);
    }

    public void delete(Guestbook guestbook) {
        repository.delete(guestbook);
    }

    public List<Guestbook> findByNameContaining(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    public List<Guestbook> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public List<Guestbook> findByCommentContaining(String comment) {
        return repository.findByCommentContaining(comment);
    }
}