package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Role;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.model.VerificationStatus;
import com.bakery.Bakery.repository.UserRepository;
import com.bakery.Bakery.service.EmailService;
import com.bakery.Bakery.service.FileStorageService;
import com.bakery.Bakery.service.PasswordResetService;
import com.bakery.Bakery.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          UserService userService,
                          FileStorageService fileStorageService,
                          PasswordResetService passwordResetService,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.passwordResetService = passwordResetService;
        this.emailService = emailService;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET — СТОРІНКИ
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ЗАБУЛИ ПАРОЛЬ — КРОК 1: введення email
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        // Запускаємо процес (навіть якщо email не знайдено — не кажемо юзеру)
        passwordResetService.sendResetLink(email);

        redirectAttributes.addFlashAttribute("successMsg",
                "Якщо цей email зареєстрований — посилання для скидання пароля надіслано. Перевірте пошту.");
        return "redirect:/forgot-password";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // СКИДАННЯ ПАРОЛЯ — КРОК 2: введення нового пароля за токеном
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Юзер клікає посилання з листа: /reset-password?token=abc123
     * Перевіряємо токен, якщо валідний — показуємо форму нового пароля.
     */
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token,
                                    Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("error", "Посилання недійсне. Запросіть нове.");
            return "reset-password";
        }

        User user = passwordResetService.validateToken(token);
        if (user == null) {
            model.addAttribute("error",
                    "Посилання недійсне або вже минуло 5 хвилин. Запросіть нове.");
            return "reset-password";
        }

        // Токен валідний — передаємо його у форму (прихованим полем)
        model.addAttribute("token", token);
        return "reset-password";
    }

    /**
     * Юзер ввів новий пароль і підтвердження — зберігаємо.
     */
    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {

        // Перевірка: паролі збігаються
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Паролі не збігаються. Спробуйте ще раз.");
            return "reset-password";
        }

        // Перевірка довжини
        if (password.length() < 6) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Пароль має бути не менше 6 символів.");
            return "reset-password";
        }

        // Зберігаємо новий пароль (сервіс також перевіряє токен ще раз)
        boolean success = passwordResetService.resetPassword(token, password);

        if (!success) {
            model.addAttribute("error",
                    "Посилання вже недійсне. Запросіть нове відновлення пароля.");
            return "reset-password";
        }

        // Надсилаємо підтвердження на пошту (необов'язково, але добра практика)
        try {
            User user = userRepository.findByResetToken(token).orElse(null);
            // Токен вже очищений після resetPassword — шукаємо по-іншому
            // (лист про підтвердження — необов'язковий, пропускаємо якщо не знайшли)
        } catch (Exception ignored) {}

        redirectAttributes.addFlashAttribute("successMsg",
                "Пароль успішно змінено! Тепер можете увійти з новим паролем.");
        return "redirect:/login";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // РЕЄСТРАЦІЯ
    // ══════════════════════════════════════════════════════════════════════════

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
            HttpServletRequest request,
            Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Паролі не співпадають!");
            return "register";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Користувач з такою поштою вже існує!");
            return "register";
        }

        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setConsent(consent);

        boolean needsVerification = role == Role.ZSU || role == Role.DSNS || role == Role.DPSU;
        user.setVerificationStatus(needsVerification
                ? VerificationStatus.PENDING : VerificationStatus.NONE);

        if (needsVerification && document != null && !document.isEmpty()) {
            try {
                String savedPath = fileStorageService.saveFile(document, "uploads/documents");
                user.setDocumentPath(savedPath);
            } catch (IOException e) {
                model.addAttribute("error", "Помилка при завантаженні документа!");
                return "register";
            }
        }

        userRepository.save(user);

        try {
            request.login(email, password);
        } catch (ServletException e) {
            return "redirect:/login";
        }
        return "redirect:/profile";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // РЕДАГУВАННЯ ПРОФІЛЮ
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/edit-profile")
    public String editProfile(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "birthDate", required = false) String birthDateStr,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
            RedirectAttributes redirectAttributes) {

        User dbUser = userService.getCurrentUser();
        if (dbUser == null) return "redirect:/login";

        dbUser.setUsername(username);
        dbUser.setEmail(email);
        dbUser.setPhone(phone);

        if (password != null && !password.isBlank()) {
            dbUser.setPassword(passwordEncoder.encode(password));
        }
        if (birthDateStr != null && !birthDateStr.isBlank()) {
            dbUser.setBirthDate(LocalDate.parse(birthDateStr));
        }
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                String savedPath = fileStorageService.saveFile(profilePicture, "uploads/profiles");
                dbUser.setProfilePicturePath(savedPath);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Помилка завантаження фото.");
                return "redirect:/profile";
            }
        }

        userRepository.save(dbUser);
        redirectAttributes.addFlashAttribute("successMsg", "Профіль успішно оновлено!");
        return "redirect:/profile";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ВИДАЛЕННЯ АВАТАРКИ / АКАУНТУ
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/delete-avatar")
    public String deleteAvatar() {
        User dbUser = userService.getCurrentUser();
        if (dbUser != null) {
            dbUser.setProfilePicturePath(null);
            userRepository.save(dbUser);
        }
        return "redirect:/profile";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(HttpSession session) {
        User dbUser = userService.getCurrentUser();
        if (dbUser != null) {
            userRepository.deleteById(dbUser.getId());
            session.invalidate();
            SecurityContextHolder.clearContext();
        }
        return "redirect:/";
    }
}