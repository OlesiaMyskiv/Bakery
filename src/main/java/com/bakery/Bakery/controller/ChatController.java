package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.*;
import com.bakery.Bakery.repository.*;
import com.bakery.Bakery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
public class ChatController {

    @Autowired private ChatSessionRepository sessionRepo;
    @Autowired private ChatMessageRepository messageRepo;
    @Autowired private UserService userService;

    // ─────────────────────────────────────────────────────────────────────
    // КЛІЄНТ / ГІСТЬ — отримати або створити сесію
    // ─────────────────────────────────────────────────────────────────────
    @PostMapping("/api/chat/session")
    public ResponseEntity<?> getOrCreateSession(
            @RequestBody(required = false) Map<String, String> body) {

        User currentUser = userService.getCurrentUser();
        ChatSession session;

        if (currentUser != null && currentUser.getRole() != Role.SUPER_ADMIN) {
            session = sessionRepo.findByUserId(currentUser.getId())
                    .orElseGet(() -> {
                        ChatSession s = new ChatSession();
                        s.setUser(currentUser);
                        return sessionRepo.save(s);
                    });
        } else {
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
        return ResponseEntity.ok(result);
    }

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

    @GetMapping("/api/admin/chat/sessions")
    public ResponseEntity<?> getAllSessions() {
        if (!isAdmin()) return ResponseEntity.status(403).build();

        List<ChatSession> sessions = sessionRepo.findAllByOrderByLastMessageAtDesc();
        List<Map<String, Object>> result = sessions.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("displayName", s.getDisplayName());
            m.put("unread", s.isUnreadForAdmin());
            m.put("lastMessageAt", s.getLastMessageAt() != null ? s.getLastMessageAt().toString() : "");
            return m;
        }).toList();

        return ResponseEntity.ok(result);
    }

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

    // ─── Допоміжні ───────────────────────────────────────────────────────

    private boolean isAdmin() {
        User u = userService.getCurrentUser();
        return u != null && u.getRole() == Role.SUPER_ADMIN;
    }

    private boolean hasAccess(Long sessionId, String guestToken) {
        User user = userService.getCurrentUser();
        ChatSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return false;
        if (isAdmin()) return true;
        if (user != null && session.getUser() != null && session.getUser().getId().equals(user.getId())) return true;
        if (guestToken != null && guestToken.equals(session.getGuestToken())) return true;
        return false;
    }

    private List<Map<String, Object>> toJsonList(List<ChatMessage> msgs) {
        return msgs.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("text", m.getText());
            map.put("fromAdmin", m.isFromAdmin());
            map.put("sentAt", m.getSentAt() != null ? m.getSentAt().toString() : "");
            return map;
        }).toList();
    }
}