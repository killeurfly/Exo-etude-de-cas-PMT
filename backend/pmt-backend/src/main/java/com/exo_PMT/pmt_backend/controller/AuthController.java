package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.dto.AuthResponse;
import com.exo_PMT.pmt_backend.dto.LoginRequest;
import com.exo_PMT.pmt_backend.dto.RegisterRequest;
import com.exo_PMT.pmt_backend.entity.User;
import com.exo_PMT.pmt_backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = userService.register(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return toResponse(userService.login(request.getEmail(), request.getPassword()));
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getId(), user.getName(), user.getEmail());
    }
}
