package com.achmadrizkin.splitbill.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Externalized configuration (12-factor): fails fast at startup if the username is missing. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotBlank String githubUsername) {
}
