package com.example.demo.repository;

import com.example.demo.entity.Guestbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestbookRepository extends JpaRepository<Guestbook, Long> {
    
    List<Guestbook> findByNameContainingIgnoreCase(String name);
    
    List<Guestbook> findByEmail(String email);
    
    @Query("SELECT g FROM Guestbook g WHERE g.comment LIKE %:keyword%")
    List<Guestbook> findByCommentContaining(@Param("keyword") String keyword);
    
    List<Guestbook> findAllByOrderByCreatedAtDesc();
}