package com.bakery.Bakery.config;

import com.bakery.Bakery.model.Role;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "Myskiv12385@gmail.com";

            // Перевіряємо, чи такий адмін вже існує
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User superAdmin = new User();
                superAdmin.setUsername("Олеся Миськів"); // Твоє ім'я для профілю
                superAdmin.setEmail(adminEmail);
                superAdmin.setPhone("+380960000000"); // Можеш змінити на свій

                // Хешуємо пароль (це обов'язково для безпеки)
                superAdmin.setPassword(passwordEncoder.encode("19102005"));

                // Призначаємо роль
                superAdmin.setRole(Role.SUPER_ADMIN);

                userRepository.save(superAdmin);
                System.out.println("✅ SUPER_ADMIN успішно створений: " + adminEmail);
            }
        };
    }
}