package com.bakery.Bakery.config;

import com.bakery.Bakery.model.User;
import com.bakery.Bakery.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;

@Configuration
public class SecurityConfig {

    // ЦЕЙ МЕТОД ВИПРАВИТЬ ПОМИЛКУ
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // За потреби вимкніть для розробки
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/login", "/assortment", "/constructor", "/css/**", "/img/**", "/js/**").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("SUPER_ADMIN") // Тільки для адміна
                        .requestMatchers("/profile/**").authenticated() // Для всіх авторизованих
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // РОЗУМНИЙ РЕДИРЕКТ ПІСЛЯ ЛОГІНУ
                        .successHandler((request, response, authentication) -> {
                            // Перевіряємо роль користувача
                            String role = authentication.getAuthorities().iterator().next().getAuthority();

                            if (role.equals("SUPER_ADMIN") || role.equals("ROLE_SUPER_ADMIN")) {
                                response.sendRedirect("/admin/admin"); // Шлях до вашої адмінки
                            } else {
                                response.sendRedirect("/profile"); // Шлях для клієнтів (CLIENT, ZSU, DSNS)
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
            );
        };
    }
}