package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Role;
import com.bakery.Bakery.model.User;
import com.bakery.Bakery.model.VerificationStatus;
import com.bakery.Bakery.repository.UserRepository;
import com.bakery.Bakery.service.FileStorageService;
import com.bakery.Bakery.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Controller
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserService userService;
    @Autowired private FileStorageService fileStorageService;

    // ==========================================
    // 1. РЕЄСТРАЦІЯ
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

        if (role == Role.ZSU || role == Role.DSNS) {
            user.setVerificationStatus(VerificationStatus.PENDING);
        } else {
            user.setVerificationStatus(VerificationStatus.NONE);
        }

        if ((role == Role.ZSU || role == Role.DSNS) && document != null && !document.isEmpty()) {
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
            e.printStackTrace();
            return "redirect:/login";
        }

        return "redirect:/profile";
    }

    // ==========================================
    // 2. РЕДАГУВАННЯ ПРОФІЛЮ
    // ==========================================
    @PostMapping("/edit-profile")
    public String editProfile(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "birthDate", required = false) String birthDateStr,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture) {

        User dbUser = userService.getCurrentUser();
        if (dbUser == null) return "redirect:/login";

        dbUser.setUsername(username);
        dbUser.setEmail(email);
        dbUser.setPhone(phone);

        if (password != null && !password.isEmpty()) {
            dbUser.setPassword(passwordEncoder.encode(password));
        }

        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            dbUser.setBirthDate(LocalDate.parse(birthDateStr));
        }

        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                String savedPath = fileStorageService.saveFile(profilePicture, "uploads/profiles");
                dbUser.setProfilePicturePath(savedPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        userRepository.save(dbUser);
        return "redirect:/profile?updated";
    }

    // ==========================================
    // 3. ВИДАЛЕННЯ АВАТАРКИ
    // ==========================================
    @GetMapping("/delete-avatar")
    public String deleteAvatar() {
        User dbUser = userService.getCurrentUser();
        if (dbUser != null) {
            dbUser.setProfilePicturePath(null);
            userRepository.save(dbUser);
        }
        return "redirect:/profile";
    }

    // ==========================================
    // 4. ВИДАЛЕННЯ АКАУНТУ
    // ==========================================
    @GetMapping("/delete-account")
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