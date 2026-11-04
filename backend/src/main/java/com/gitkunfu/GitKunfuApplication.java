package com.gitkunfu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GitKunfuApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitKunfuApplication.class, args);
    }
}
