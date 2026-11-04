// src/main/java/com/exo_PMT/pmt_backend/repository/UserRepository.java
package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndPassword(String email, String password);
    Optional<User> findByEmail(String email);
}