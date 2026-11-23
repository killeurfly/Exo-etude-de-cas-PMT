package com.exo_PMT.pmt_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Informations générales de la documentation OpenAPI générée par springdoc :
 * titre, version et explication du paramètre {@code userId}. Interface : {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pmtOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PMT API")
                .version("1.0")
                .description("""
                        API REST de PMT (Project Management Tool) : comptes, projets, membres, tâches, \
                        historique et notifications.

                        **Identification** : l'application n'utilise pas d'authentification sécurisée \
                        (non requise par l'énoncé). Chaque appel indique l'utilisateur qui agit avec le \
                        paramètre `userId`, obtenu à la connexion (`POST /api/auth/login`).

                        **Rôles dans un projet** : ADMIN (tout), MEMBER (créer, assigner et modifier des tâches), \
                        OBSERVER (lecture seule).
                        """));
    }
}
