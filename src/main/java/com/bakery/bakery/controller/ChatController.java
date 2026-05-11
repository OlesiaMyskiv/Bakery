package com.bakery.bakery.controller;

import com.bakery.bakery.model.*;
import com.bakery.bakery.repository.*;
import com.bakery.bakery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
public class ChatController {

    @Autowired private ChatSessionRepository sessionRepo;
    @Autowired private ChatMessageRepository messageRepo;
    @Autowired private UserService userService;

    // ── Клієнт: отримати або створити сесію ──────────────────────────────────
    @Transactional
    @PostMapping("/api/chat/session")
    public ResponseEntity<?> getOrCreateSession(@RequestBody(required = false) Map<String, String> body) {
        User currentUser = userService.getCurrentUser();
        ChatSession session;

        if (currentUser != null && currentUser.getRole() != Role.SUPER_ADMIN) {
            // Залогінений клієнт — сесія прив'язана до юзера
            session = sessionRepo.findByUserId(currentUser.getId())
                    .orElseGet(() -> {
                        ChatSession s = new ChatSession();
                        s.setUser(currentUser);
                        return sessionRepo.save(s);
                    });
        } else {
            // Гість — сесія за токеном
            String token = body != null ? body.get("guestToken") : null;
            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest().body("guestToken required");
            }
            session = sessionRepo.findByGuestToken(token)
                    .orElseGet(() -> {
                        ChatSession s = new ChatSession();
                        s.setGuestToken(token);
                        String name = body.get("guestName");
                        s.setGuestName(name != null && !name.isBlank() ? name : "Гість");
                        return sessionRepo.save(s);
                    });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("displayName", session.getDisplayName());
        result.put("unread", session.isUnreadForClient()); // Додано цей рядок
        return ResponseEntity.ok(result);
    }

    // ── Клієнт: перевірити чи є непрочитані повідомлення (фоновий запит) ────
    @Transactional(readOnly = true)
    @GetMapping("/api/chat/unread")
    public ResponseEntity<?> checkUnreadStatus(@RequestParam(required = false) String guestToken) {
        User currentUser = userService.getCurrentUser();
        ChatSession session = null;

        if (currentUser != null && currentUser.getRole() != Role.SUPER_ADMIN) {
            session = sessionRepo.findByUserId(currentUser.getId()).orElse(null);
        } else if (guestToken != null && !guestToken.isBlank()) {
            session = sessionRepo.findByGuestToken(guestToken).orElse(null);
        }

        boolean hasUnread = session != null && session.isUnreadForClient();
        return ResponseEntity.ok(Map.of("unread", hasUnread));
    }

    // ── Клієнт: отримати повідомлення ─────────────────────────────────────────
    @Transactional
    @GetMapping("/api/chat/{sessionId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long sessionId,
                                         @RequestParam(required = false) String guestToken) {
        if (!hasAccess(sessionId, guestToken)) return ResponseEntity.status(403).build();

        sessionRepo.findById(sessionId).ifPresent(s -> {
            s.setUnreadForClient(false);
            sessionRepo.save(s);
        });

        List<ChatMessage> msgs = messageRepo.findBySessionIdOrderBySentAtAsc(sessionId);
        return ResponseEntity.ok(toJsonList(msgs));
    }

    // ── Клієнт: надіслати повідомлення ────────────────────────────────────────
    @Transactional
    @PostMapping("/api/chat/{sessionId}/send")
    public ResponseEntity<?> sendMessage(@PathVariable Long sessionId,
                                         @RequestBody Map<String, String> body) {
        String guestToken = body.get("guestToken");
        if (!hasAccess(sessionId, guestToken)) return ResponseEntity.status(403).build();

        String text = body.get("text");
        if (text == null || text.isBlank()) return ResponseEntity.badRequest().build();

        ChatSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();

        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setText(text.trim());
        msg.setFromAdmin(false);
        messageRepo.save(msg);

        session.setLastMessageAt(LocalDateTime.now());
        session.setUnreadForAdmin(true);
        sessionRepo.save(session);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ── Адмін: список сесій ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    @GetMapping("/api/admin/chat/sessions")
    public ResponseEntity<?> getAllSessions() {
        if (!isAdmin()) return ResponseEntity.status(403).build();

        List<ChatSession> sessions = sessionRepo.findAllByOrderByLastMessageAtDesc();

        List<Map<String, Object>> result = sessions.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",            s.getId());
            // Безпечне отримання імені — уникаємо LazyInitializationException
            m.put("displayName",   getDisplayName(s));
            m.put("unread",        s.isUnreadForAdmin());
            m.put("lastMessageAt", s.getLastMessageAt() != null
                    ? s.getLastMessageAt().toString() : "");
            return m;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ── Адмін: повідомлення сесії ─────────────────────────────────────────────
    @Transactional
    @GetMapping("/api/admin/chat/{sessionId}/messages")
    public ResponseEntity<?> getSessionMessages(@PathVariable Long sessionId) {
        if (!isAdmin()) return ResponseEntity.status(403).build();

        sessionRepo.findById(sessionId).ifPresent(s -> {
            s.setUnreadForAdmin(false);
            sessionRepo.save(s);
        });

        List<ChatMessage> msgs = messageRepo.findBySessionIdOrderBySentAtAsc(sessionId);
        return ResponseEntity.ok(toJsonList(msgs));
    }

    // ── Адмін: надіслати відповідь ────────────────────────────────────────────
    @Transactional
    @PostMapping("/api/admin/chat/{sessionId}/send")
    public ResponseEntity<?> adminSend(@PathVariable Long sessionId,
                                       @RequestBody Map<String, String> body) {
        if (!isAdmin()) return ResponseEntity.status(403).build();

        String text = body.get("text");
        if (text == null || text.isBlank()) return ResponseEntity.badRequest().build();

        ChatSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();

        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setText(text.trim());
        msg.setFromAdmin(true);
        messageRepo.save(msg);

        session.setLastMessageAt(LocalDateTime.now());
        session.setUnreadForClient(true);
        sessionRepo.save(session);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Безпечне отримання імені без LazyInitializationException */
    private String getDisplayName(ChatSession s) {
        try {
            if (s.getUser() != null) return s.getUser().getUsername();
        } catch (Exception ignored) {}
        if (s.getGuestName() != null && !s.getGuestName().isBlank()) return s.getGuestName();
        return "Гість";
    }

    private boolean isAdmin() {
        User u = userService.getCurrentUser();
        return u != null && u.getRole() == Role.SUPER_ADMIN;
    }

    private boolean hasAccess(Long sessionId, String guestToken) {
        User user = userService.getCurrentUser();
        ChatSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return false;
        if (isAdmin()) return true;
        try {
            if (user != null && session.getUser() != null
                    && session.getUser().getId().equals(user.getId())) return true;
        } catch (Exception ignored) {}
        if (guestToken != null && guestToken.equals(session.getGuestToken())) return true;
        return false;
    }

    private List<Map<String, Object>> toJsonList(List<ChatMessage> msgs) {
        return msgs.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",        m.getId());
            map.put("text",      m.getText());
            map.put("fromAdmin", m.isFromAdmin());
            map.put("sentAt",    m.getSentAt() != null ? m.getSentAt().toString() : "");
            return map;
        }).toList();
    }
}