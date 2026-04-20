package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Role;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.model.VerificationStatus;
import com.bakery.Bakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

    // ==========================================
    // 1. МЕТОД РЕЄСТРАЦІЇ
    // ==========================================
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
            jakarta.servlet.http.HttpSession session,
            Model model) {

        // Перевірка паролів
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Паролі не співпадають!");
            return "register";
        }

        // Перевірка, чи існує вже такий email
        if (userRepository.findByEmail(email) != null) {
            model.addAttribute("error", "Користувач з такою поштою вже існує!");
            return "register";
        }

        // Створюємо нового користувача
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setConsent(consent);

        // Встановлюємо правильний статус перевірки при реєстрації
        if (role == Role.ZSU || role == Role.DSNS) {
            user.setVerificationStatus(VerificationStatus.PENDING); // Очікує на перевірку
        } else {
            user.setVerificationStatus(VerificationStatus.NONE); // Звичайний клієнт
        }

        // Логіка збереження фотографії
        if ((role == Role.ZSU || role == Role.DSNS) && document != null && !document.isEmpty()) {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String originalFileName = document.getOriginalFilename();
                String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
                Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, uniqueFileName);
                Files.write(fileNameAndPath, document.getBytes());
                user.setDocumentPath("/uploads/documents/" + uniqueFileName);
            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("error", "Помилка при завантаженні фото!");
                return "register";
            }
        }

        // Зберігаємо користувача в базу даних
        userRepository.save(user);

        // АВТОВХІД: Зберігаємо створеного користувача в сесію
        session.setAttribute("loggedInUser", user);

        // Перекидаємо у профіль
        return "redirect:/profile";
    }

    // ==========================================
    // 2. МЕТОД ВХОДУ (ЛОГІН)
    // ==========================================
    @PostMapping("/login")
    public String loginUser(
            @RequestParam("username") String email, // У твоїй формі поле email має name="username"
            @RequestParam("password") String password,
            jakarta.servlet.http.HttpSession session,
            Model model) {

        // Шукаємо користувача в базі за поштою
        User user = userRepository.findByEmail(email);

        // Перевірка, чи знайшовся користувач і чи збігається пароль
        if (user != null && user.getPassword().equals(password)) {
            // УСПІХ! Зберігаємо користувача в пам'ять
            session.setAttribute("loggedInUser", user);

            // Перекидаємо у профіль
            return "redirect:/profile";
        } else {
            // ПОМИЛКА! Неправильний логін або пароль
            model.addAttribute("error", "Неправильна електронна пошта або пароль!");
            return "login";
        }
    }

    // ==========================================
    // 3. МЕТОД ВИХОДУ (ЛОГАУТ)
    // ==========================================
    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpSession session) {
        session.invalidate(); // Очищаємо пам'ять
        return "redirect:/"; // Перекидаємо на головну сторінку
    }
    // Шлях до папки з аватарками
    public static String PROFILE_PICS_DIRECTORY = System.getProperty("user.dir") + "/uploads/profiles";

    // ==========================================
    // 4. МЕТОД РЕДАГУВАННЯ ПРОФІЛЮ
    // ==========================================
    @PostMapping("/edit-profile")
    public String editProfile(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "birthDate", required = false) String birthDateStr,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
            jakarta.servlet.http.HttpSession session) {

        // Отримуємо поточного користувача з сесії
        User sessionUser = (User) session.getAttribute("loggedInUser");
        if (sessionUser == null) return "redirect:/login"; // Якщо не увійшов

        // Знаходимо його в базі даних, щоб оновити
        User dbUser = userRepository.findById(sessionUser.getId()).orElse(null);
        if (dbUser == null) return "redirect:/login";

        // Оновлюємо текст
        dbUser.setUsername(username);
        dbUser.setEmail(email);

        // Оновлюємо пароль (тільки якщо ввели новий)
        if (password != null && !password.isEmpty()) {
            dbUser.setPassword(password);
        }

        // Оновлюємо дату народження
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            dbUser.setBirthDate(java.time.LocalDate.parse(birthDateStr));
        }

        // Зберігаємо нову аватарку (якщо завантажили)
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                Path uploadPath = Paths.get(PROFILE_PICS_DIRECTORY);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String uniqueFileName = UUID.randomUUID().toString() + "_" + profilePicture.getOriginalFilename();
                Path fileNameAndPath = Paths.get(PROFILE_PICS_DIRECTORY, uniqueFileName);
                Files.write(fileNameAndPath, profilePicture.getBytes());

                // Зберігаємо шлях у базу
                dbUser.setProfilePicturePath("/uploads/profiles/" + uniqueFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Зберігаємо оновленого користувача в базу і оновлюємо сесію
        userRepository.save(dbUser);
        session.setAttribute("loggedInUser", dbUser);

        return "redirect:/profile";
    }

    // ==========================================
    // 5. МЕТОД ВИДАЛЕННЯ АВАТАРКИ
    // ==========================================
    @GetMapping("/delete-avatar")
    public String deleteAvatar(jakarta.servlet.http.HttpSession session) {
        User sessionUser = (User) session.getAttribute("loggedInUser");
        if (sessionUser != null) {
            User dbUser = userRepository.findById(sessionUser.getId()).orElse(null);
            if (dbUser != null) {
                dbUser.setProfilePicturePath(null); // Видаляємо фото
                userRepository.save(dbUser);
                session.setAttribute("loggedInUser", dbUser); // Оновлюємо пам'ять
            }
        }
        return "redirect:/profile";
    }
}