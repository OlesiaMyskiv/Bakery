package com.bakery.Bakery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Вимикаємо блокування форм, щоб ти могла спокійно завантажувати фото і зберігати профіль
                .csrf(csrf -> csrf.disable())
                // Дозволяємо доступ до всіх сторінок (бо ми самі перевіряємо сесії в Контролерах)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN") // Тільки для тебе
                        .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true) // Куди кидати після входу
                        .permitAll()
                );
        return http.build();
    }
}