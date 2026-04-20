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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Вимикаємо блокування форм, щоб ти могла спокійно завантажувати фото і зберігати профіль
                .csrf(csrf -> csrf.disable())
                // Дозволяємо доступ до всіх сторінок (бо ми самі перевіряємо сесії в Контролерах)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}