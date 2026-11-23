package com.exo_PMT.pmt_backend.controller;

import com.exo_PMT.pmt_backend.dto.AuthResponse;
import com.exo_PMT.pmt_backend.dto.LoginRequest;
import com.exo_PMT.pmt_backend.dto.RegisterRequest;
import com.exo_PMT.pmt_backend.entity.User;
import com.exo_PMT.pmt_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification", description = "Inscription et connexion. Renvoie l'identifiant à utiliser ensuite comme `userId`.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Créer un compte",
            description = "Crée un utilisateur. L'e-mail est normalisé (espaces retirés, minuscules). "
                    + "Les invitations en attente pour cet e-mail deviennent actives : la personne rejoint "
                    + "directement les projets où elle avait été invitée."
    )
    @ApiResponse(responseCode = "201", description = "Compte créé.")
    @ApiResponse(responseCode = "400", description = "Nom manquant, e-mail invalide ou mot de passe manquant.")
    @ApiResponse(responseCode = "409", description = "Cette adresse e-mail est déjà utilisée.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = userService.register(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @Operation(
            summary = "Se connecter",
            description = "Vérifie l'e-mail et le mot de passe et renvoie l'utilisateur. "
                    + "Son `id` sert de paramètre `userId` pour tous les autres appels."
    )
    @ApiResponse(responseCode = "200", description = "Connexion réussie.")
    @ApiResponse(responseCode = "400", description = "E-mail invalide ou mot de passe manquant.")
    @ApiResponse(responseCode = "401", description = "Adresse e-mail ou mot de passe incorrect.")
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return toResponse(userService.login(request.getEmail(), request.getPassword()));
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getId(), user.getName(), user.getEmail());
    }
}
