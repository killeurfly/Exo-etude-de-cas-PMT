package com.exo_PMT.pmt_backend.service;

import com.exo_PMT.pmt_backend.model.User;
import com.exo_PMT.pmt_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository repo) {
        this.userRepository = repo;
    }

    public User register(User user) {
        // Ici tu peux ajouter hash du password si tu veux plus tard
        return userRepository.save(user);
    }

    public User login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password))
                .orElse(null);
    }
}