package com.bakery.Bakery.controller;

import com.bakery.Bakery.dto.EditProfileDTO;
import com.bakery.Bakery.dto.RegisterDTO;
import com.bakery.Bakery.model.Role;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.model.VerificationStatus;
import com.bakery.Bakery.repository.UserRepository;
import com.bakery.Bakery.service.FileStorageService;
import com.bakery.Bakery.service.PasswordResetService;
import com.bakery.Bakery.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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

    // ── Реєстрація ────────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDto", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("registerDto") RegisterDTO dto,
            BindingResult bindingResult,
            @RequestParam(value = "document", required = false) MultipartFile document,
            HttpServletRequest request,
            Model model) {

        // ── Валідація ──────────────────────────────────────────────────────────
        if (bindingResult.hasErrors()) return "register";

        if (!dto.passwordsMatch()) {
            model.addAttribute("error", "Паролі не співпадають!");
            return "register";
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            model.addAttribute("error", "Користувач з такою поштою вже існує!");
            return "register";
        }

        // ── Створення юзера ───────────────────────────────────────────────────
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setConsent(dto.isConsent());

        boolean needsVerification = dto.getRole() == Role.ZSU
                || dto.getRole() == Role.DSNS
                || dto.getRole() == Role.DPSU;
        user.setVerificationStatus(needsVerification
                ? VerificationStatus.PENDING
                : VerificationStatus.NONE);

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

        // ── Автологін після реєстрації ────────────────────────────────────────
        try {
            request.login(dto.getEmail(), dto.getPassword());
        } catch (ServletException e) {
            return "redirect:/login";
        }
        return "redirect:/profile";
    }

    // ── Редагування профілю ───────────────────────────────────────────────────

    @PostMapping("/edit-profile")
    public String editProfile(
            @Valid @ModelAttribute EditProfileDTO dto,
            BindingResult bindingResult,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Перевірте введені дані.");
            return "redirect:/profile";
        }

        User dbUser = userService.getCurrentUser();
        if (dbUser == null) return "redirect:/login";

        dbUser.setUsername(dto.getUsername());
        dbUser.setEmail(dto.getEmail());
        dbUser.setPhone(dto.getPhone());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            dbUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) {
            dbUser.setBirthDate(LocalDate.parse(dto.getBirthDate()));
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

    // ── Видалення аватарки ────────────────────────────────────────────────────

    @PostMapping("/delete-avatar")
    public String deleteAvatar() {
        User dbUser = userService.getCurrentUser();
        if (dbUser != null) {
            dbUser.setProfilePicturePath(null);
            userRepository.save(dbUser);
        }
        return "redirect:/profile";
    }

    // ── Видалення акаунту ─────────────────────────────────────────────────────

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

    // ── Відновлення пароля ────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam("email") String email,
                                 RedirectAttributes redirectAttributes) {
        // Завжди показуємо успіх — щоб не розкривати наявність email у системі
        passwordResetService.resetPassword(email.trim().toLowerCase());
        redirectAttributes.addFlashAttribute("successMsg",
                "Якщо такий email зареєстрований — тимчасовий пароль надіслано.");
        return "redirect:/forgot-password";
    }
}
