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

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            com.bakery.Bakery.model.User dbUser = userRepository.findByEmail(email)
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
                                "/forgot-password",   // ← форма введення email
                                "/reset-password",    // ← форма нового пароля (за токеном)
                                "/assortment", "/product/**", "/constructor", "/ai-design",
                                "/css/**", "/img/**", "/js/**", "/uploads/**"
                        ).permitAll()
                        .requestMatchers("/api/chat/**").permitAll()
                        .requestMatchers("/api/admin/chat/**").hasAuthority("SUPER_ADMIN")
                        .requestMatchers("/admin/**").hasAuthority("SUPER_ADMIN")
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