// src/main/java/com/exo_PMT/pmt_backend/repository/UserRepository.java
package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Accès à la table {@code users}. */
public interface UserRepository extends JpaRepository<User, Long> {
    /** Recherche un compte par son e-mail (à normaliser avant l'appel). */
    Optional<User> findByEmail(String email);
}