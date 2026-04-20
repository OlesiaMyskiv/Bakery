package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Role;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Шлях до папки, де будуть зберігатися фото посвідчень
    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads/documents";

    @PostMapping("/register")
    public String registerUser(
            @RequestParam("username") String username,
            @RequestParam("phone") String phone,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirm_password") String confirmPassword,
            @RequestParam("role") Role role,
            @RequestParam(value = "consent", defaultValue = "false") boolean consent,
            @RequestParam(value = "document", required = false) MultipartFile document,
            Model model) {

        // 1. Перевірка паролів
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Паролі не співпадають!");
            return "register";
        }

        // 2. Перевірка, чи існує вже такий email
        if (userRepository.findByEmail(email) != null) {
            model.addAttribute("error", "Користувач з такою поштою вже існує!");
            return "register";
        }

        // 3. Створюємо нового користувача
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(password); // В реальному проєкті пароль треба шифрувати!
        user.setRole(role);
        user.setConsent(consent);

        // 4. Логіка збереження фотографії (якщо це ЗСУ або ДСНС)
        if ((role == Role.ZSU || role == Role.DSNS) && document != null && !document.isEmpty()) {
            try {
                // Створюємо папку, якщо її ще немає
                Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Генеруємо унікальне ім'я для файлу, щоб вони не перезаписували один одного
                String originalFileName = document.getOriginalFilename();
                String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

                // Повний шлях до нового файлу
                Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, uniqueFileName);

                // Зберігаємо файл на комп'ютер
                Files.write(fileNameAndPath, document.getBytes());

                // Зберігаємо лише шлях у базу даних
                user.setDocumentPath("/uploads/documents/" + uniqueFileName);

            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("error", "Помилка при завантаженні фото!");
                return "register";
            }
        }

        // 5. Зберігаємо користувача в базу даних
        userRepository.save(user);

        // Після успішної реєстрації відправляємо на сторінку входу
        return "redirect:/login";
    }
}