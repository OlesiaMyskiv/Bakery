package com.bakery.bakery.config;

import com.bakery.bakery.model.Role;
import com.bakery.bakery.model.User;
import com.bakery.bakery.model.VerificationStatus;
import com.bakery.bakery.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "myskiv12385@gmail.com";

            // Знаходимо існуючого адміна (незалежно від регістру) і ОНОВЛЮЄМО його
            userRepository.findByEmailIgnoreCase(adminEmail).ifPresentOrElse(existingAdmin -> {
                existingAdmin.setEmail(adminEmail); // нормалізуємо до нижнього регістру
                existingAdmin.setPassword(passwordEncoder.encode("19102005"));
                existingAdmin.setRole(Role.SUPER_ADMIN);
                existingAdmin.setVerificationStatus(VerificationStatus.NONE);
                userRepository.save(existingAdmin);
                System.out.println("✅ Адмін оновлено: " + adminEmail);
            }, () -> {
                User superAdmin = new User();
                superAdmin.setUsername("Олеся Миськів");
                superAdmin.setEmail(adminEmail);
                superAdmin.setPhone("+380960000000");
                superAdmin.setPassword(passwordEncoder.encode("19102005"));
                superAdmin.setRole(Role.SUPER_ADMIN);
                superAdmin.setVerificationStatus(VerificationStatus.NONE);
                userRepository.save(superAdmin);
                System.out.println("✅ SUPER_ADMIN створений: " + adminEmail);
            });
        };
    }
}