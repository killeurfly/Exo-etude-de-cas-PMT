package com.exo_PMT.pmt_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Données d'inscription.")
public class RegisterRequest {
    @Schema(description = "Nom affiché de l'utilisateur.", example = "Zoé")
    private String name;
    @Schema(description = "Adresse e-mail (normalisée en minuscules).", example = "zoe@example.test")
    private String email;
    @Schema(description = "Mot de passe.", example = "zoe12345")
    private String password;

    public RegisterRequest() {}

    public RegisterRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
