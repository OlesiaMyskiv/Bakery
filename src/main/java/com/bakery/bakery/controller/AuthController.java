package com.bakery.bakery.controller;

import com.bakery.bakery.model.Role;
import com.bakery.bakery.model.User;
import com.bakery.bakery.service.BonusService;
import com.bakery.bakery.model.VerificationStatus;
import com.bakery.bakery.repository.UserRepository;
import com.bakery.bakery.service.FileStorageService;
import com.bakery.bakery.service.PasswordResetService;
import com.bakery.bakery.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private BonusService bonusService;
    private final FileStorageService fileStorageService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          UserService userService,
                          FileStorageService fileStorageService,
                          PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.passwordResetService = passwordResetService;
    }

    // ── GET сторінки ──────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    // ── Забули пароль ─────────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        String tempPassword = passwordResetService.generateTempPassword(email);

        if (tempPassword != null) {
            redirectAttributes.addFlashAttribute("tempPassword", tempPassword);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Тимчасовий пароль згенеровано. Увійдіть і змініть його в профілі.");
        } else {
            redirectAttributes.addFlashAttribute("successMsg",
                    "Якщо цей email зареєстрований — тимчасовий пароль згенеровано.");
        }
        return "redirect:/forgot-password";
    }

    // ── Реєстрація ────────────────────────────────────────────────────────────

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

        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Email не може бути порожнім!");
            return "register";
        }
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

        // Нараховуємо вітальний бонус 50 балів
        if (bonusService != null) {
            bonusService.giveWelcomeBonus(user);
        }

        try {
            request.login(email, password);
        } catch (ServletException e) {
            return "redirect:/login";
        }
        return "redirect:/profile";
    }

    // ── Редагування профілю ───────────────────────────────────────────────────

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

    // ── Видалення аватарки / акаунту ──────────────────────────────────────────

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