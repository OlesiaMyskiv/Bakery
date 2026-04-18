package com.bakery.Bakery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Дозволяємо доступ до всіх сторінок без авторизації (тимчасово)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // Тимчасово вимикаємо CSRF-захист, щоб не блокувало форму кошика/реєстрації
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}