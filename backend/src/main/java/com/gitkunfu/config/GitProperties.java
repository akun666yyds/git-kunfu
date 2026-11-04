package com.gitkunfu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gitkunfu")
public record GitProperties(
        String repositoryRoot,
        String baseUrl,
        String jwtSecret,
        long jwtTtlHours,
        String appName
) {
}
