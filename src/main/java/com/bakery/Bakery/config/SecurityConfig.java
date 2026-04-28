package com.bakery.Bakery.config;

import com.bakery.Bakery.repository.UserRepository;
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

    // ЦЕЙ БІН КРИТИЧНО ВАЖЛИВИЙ: Він пояснює Spring'у, як шукати користувача
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            com.bakery.Bakery.model.User dbUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Користувача не знайдено"));

            return User.builder()
                    .username(dbUser.getEmail())
                    .password(dbUser.getPassword()) // Тут вже має бути захешований пароль з БД
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
                                "/assortment", "/constructor", "/ai-design",
                                "/css/**", "/img/**", "/js/**", "/uploads/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasAuthority("SUPER_ADMIN")
                        // Чат і API доступні для авторизованих
                        .requestMatchers("/chat/**", "/api/**", "/orders/**").authenticated()
                        .requestMatchers("/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            String role = authentication.getAuthorities().iterator().next().getAuthority();
                            if (role.equals("SUPER_ADMIN") || role.equals("ROLE_SUPER_ADMIN")) {
                                response.sendRedirect("/admin/orders");
                            } else {
                                response.sendRedirect("/profile");
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
}