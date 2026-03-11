// src/main/java/com/exo_PMT/pmt_backend/repository/UserRepository.java
package com.exo_PMT.pmt_backend.repository;

import com.exo_PMT.pmt_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Méthode pour rechercher un utilisateur par email et mot de passe
    Optional<User> findByEmailAndPassword(String email, String password);

    // Optionnel : chercher seulement par email pour vérifier l'existence
    Optional<User> findByEmail(String email);
}