package com.bakery.bakery.config;

import com.bakery.bakery.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            com.bakery.bakery.model.User dbUser = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Не знайдено"));
            return User.builder()
                    .username(dbUser.getEmail())
                    .password(dbUser.getPassword())
                    .authorities(dbUser.getRole().name())
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/register", "/login",
                                "/forgot-password",
                                "/reset-password",
                                "/assortment", "/product/**", "/constructor", "/ai-design",
                                "/css/**", "/img/**", "/js/**", "/uploads/**"
                        ).permitAll()
                        .requestMatchers("/api/chat/**").permitAll()
                        .requestMatchers("/reviews/**", "/api/my-reviews").authenticated()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/api/admin/chat/**").hasAuthority("SUPER_ADMIN")
                        .requestMatchers("/admin/**").hasAuthority("SUPER_ADMIN")
                        // ── ШІ генерація — тільки залогінені ────────────────
                        .requestMatchers("/ai-design/generate").authenticated()
                        // ── КОШИК і ЗАМОВЛЕННЯ — тільки залогінені ──────────
                        .requestMatchers("/cart", "/cart/**").authenticated()
                        .requestMatchers("/api/my-orders", "/orders/**", "/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            String role = auth.getAuthorities().iterator().next().getAuthority();
                            res.sendRedirect(role.equals("SUPER_ADMIN") ? "/admin/orders" : "/profile");
                        })
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // AJAX/fetch запити отримують 401, звичайні — редірект на /login
                            String requestedWith = request.getHeader("X-Requested-With");
                            String accept = request.getHeader("Accept");
                            if ("XMLHttpRequest".equals(requestedWith)
                                    || (accept != null && accept.contains("application/json"))) {
                                response.sendError(401, "Unauthorized");
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
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
}