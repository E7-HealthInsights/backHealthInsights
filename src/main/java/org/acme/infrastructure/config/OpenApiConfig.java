package org.acme.infrastructure.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@OpenAPIDefinition(
    info = @Info(
        title       = "HealthInsights API",
        version     = "1.0.0",
        description = "API para análisis de diabetes en México"
    ),
    components = @Components(
        securitySchemes = @SecurityScheme(
            securitySchemeName = "bearerAuth",
            type               = SecuritySchemeType.HTTP,
            scheme             = "bearer",
            bearerFormat       = "JWT (Firebase ID Token)"
        )
    )
)
public class OpenApiConfig extends Application {
}
