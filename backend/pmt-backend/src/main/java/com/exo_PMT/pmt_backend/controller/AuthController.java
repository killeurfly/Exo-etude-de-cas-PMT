package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.model.User;
import com.exo_PMT.pmt_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Classe DTO pour la requête login
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> optionalUser = userRepository
                .findByEmailAndPassword(loginRequest.getEmail(), loginRequest.getPassword());

        if (optionalUser.isPresent()) {
            // Connexion OK
            return ResponseEntity.ok("Connexion réussie !");
        } else {
            // Mauvais email ou mot de passe
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Mauvais email ou mot de passe");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Email déjà utilisé");
        }

        userRepository.save(user);
        return ResponseEntity.ok("Inscription réussie !");
    }
}