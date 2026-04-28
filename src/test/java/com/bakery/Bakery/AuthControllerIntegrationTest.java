package com.bakery.Bakery.controller;

import com.bakery.Bakery.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Інтеграційний тест реєстрації.
 * Використовує реальний Spring контекст, але @Transactional відкочує зміни після кожного тесту.
 *
 * Потрібна тестова БД. Налаштуй src/test/resources/application-test.properties:
 *   spring.datasource.url=jdbc:h2:mem:testdb
 *   spring.jpa.hibernate.ddl-auto=create-drop
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /register — повертає сторінку реєстрації")
    void getRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register — з невірними даними залишається на сторінці")
    void postRegister_withBlankEmail_staysOnPage() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "Тест")
                        .param("phone", "+380991234567")
                        .param("email", "")                   // порожній email
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @DisplayName("POST /register — паролі не збігаються, показує помилку")
    void postRegister_passwordMismatch_showsError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "Тест")
                        .param("phone", "+380991234567")
                        .param("email", "newuser@test.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "other456")
                        .param("role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }
}
