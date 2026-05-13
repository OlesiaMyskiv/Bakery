package com.bakery.bakery.service;

import com.bakery.bakery.model.*;
import com.bakery.bakery.repository.BonusTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BonusService {

    @Autowired
    private BonusTransactionRepository bonusRepo;

    // ── Рівні (межі — кількість бонусів на балансі) ───────────────────────────
    public record BonusLevel(String name, int minBalance, int maxBalance, int cashbackPercent) {}

    private static final List<BonusLevel> LEVELS = List.of(
            new BonusLevel("Ласун",         0,    300,              3),
            new BonusLevel("Гурман",        300,  800,              5),
            new BonusLevel("Майстер тіста", 800,  1500,             7),
            new BonusLevel("Шеф-пекар",     1500, Integer.MAX_VALUE, 10)
    );

    // ── Баланс ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public int getBalance(Long userId) {
        List<BonusTransaction> all = bonusRepo.findByUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        int balance = 0;
        for (BonusTransaction t : all) {
            if (t.getAmount() < 0) {
                balance += t.getAmount();
            } else {
                if (t.getExpiresAt() == null || t.getExpiresAt().isAfter(now)) {
                    balance += t.getAmount();
                }
            }
        }
        return Math.max(0, balance);
    }

    @Transactional(readOnly = true)
    public int getTotalSpent(Long userId) {
        Set<Long> seenOrderIds = new HashSet<>();
        int total = 0;
        for (BonusTransaction t : bonusRepo.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (t.getType() == BonusTransaction.TransactionType.EARNED
                    && t.getOrder() != null
                    && seenOrderIds.add(t.getOrder().getId())) {
                total += t.getOrder().getPrice() != null ? t.getOrder().getPrice() : 0;
            }
        }
        return total;
    }

    // ── Рівень — ТІЛЬКИ від балансу ───────────────────────────────────────────
    public BonusLevel getCurrentLevel(int balance) {
        return LEVELS.stream()
                .filter(l -> balance >= l.minBalance() && balance < l.maxBalance())
                .findFirst()
                .orElse(LEVELS.get(LEVELS.size() - 1));
    }

    public BonusLevel getNextLevel(int balance) {
        for (int i = 0; i < LEVELS.size() - 1; i++) {
            if (balance < LEVELS.get(i + 1).minBalance()) {
                return LEVELS.get(i + 1);
            }
        }
        return null;
    }

    public int getAmountToNextLevel(int balance) {
        BonusLevel next = getNextLevel(balance);
        if (next == null) return 0;
        return next.minBalance() - balance;
    }

    public int getProgressPercent(int balance) {
        BonusLevel current = getCurrentLevel(balance);
        BonusLevel next    = getNextLevel(balance);
        if (next == null) return 100;
        int range = next.minBalance() - current.minBalance();
        int done  = balance           - current.minBalance();
        return Math.min(100, (int) ((double) done / range * 100));
    }

    // ── Нарахування ───────────────────────────────────────────────────────────
    @Transactional
    public BonusTransaction earnForOrder(User user, Order order) {
        if (bonusRepo.existsByOrderIdAndType(order.getId(), BonusTransaction.TransactionType.EARNED)) {
            return null;
        }
        int balance = getBalance(user.getId());
        BonusLevel level = getCurrentLevel(balance);

        int earned = (int) Math.round(order.getPrice() * level.cashbackPercent() / 100.0);
        if (earned <= 0) return null;

        BonusTransaction t = new BonusTransaction();
        t.setUser(user);
        t.setOrder(order);
        t.setType(BonusTransaction.TransactionType.EARNED);
        t.setAmount(earned);
        t.setDescription("Кешбек " + level.cashbackPercent() + "% за замовлення №" + order.getId());
        t.setCreatedAt(LocalDateTime.now());
        t.setExpiresAt(LocalDateTime.now().plusMonths(6));
        return bonusRepo.save(t);
    }

    @Transactional
    public BonusTransaction giveWelcomeBonus(User user) {
        if (bonusRepo.existsByUserIdAndType(user.getId(), BonusTransaction.TransactionType.WELCOME)) {
            return null;
        }
        BonusTransaction t = new BonusTransaction();
        t.setUser(user);
        t.setType(BonusTransaction.TransactionType.WELCOME);
        t.setAmount(50);
        t.setDescription("Вітальний бонус за реєстрацію");
        t.setCreatedAt(LocalDateTime.now());
        t.setExpiresAt(LocalDateTime.now().plusMonths(6));
        return bonusRepo.save(t);
    }

    @Transactional
    public BonusTransaction giveBirthdayBonus(User user) {
        int year = LocalDate.now().getYear();
        if (bonusRepo.hasBirthdayBonusThisYear(user.getId(), year)) {
            return null;
        }
        BonusTransaction t = new BonusTransaction();
        t.setUser(user);
        t.setType(BonusTransaction.TransactionType.BIRTHDAY);
        t.setAmount(100);
        t.setDescription("Бонус на день народження");
        t.setCreatedAt(LocalDateTime.now());
        t.setExpiresAt(LocalDateTime.now().plusMonths(1));
        return bonusRepo.save(t);
    }

    // ── Списання ──────────────────────────────────────────────────────────────
    @Transactional
    public int spend(User user, Order order, int requestedAmount) {
        int balance    = getBalance(user.getId());
        int maxAllowed = (int) Math.floor(order.getPrice() * 0.30);
        int toSpend    = Math.min(requestedAmount, Math.min(balance, maxAllowed));
        if (toSpend <= 0) return 0;

        BonusTransaction t = new BonusTransaction();
        t.setUser(user);
        t.setOrder(order);
        t.setType(BonusTransaction.TransactionType.SPENT);
        t.setAmount(-toSpend);
        t.setDescription("Списано за замовлення №" + order.getId());
        t.setCreatedAt(LocalDateTime.now());
        bonusRepo.save(t);
        return toSpend;
    }

    // ── Дашборд ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(Long userId) {
        int balance  = getBalance(userId);
        int spent    = getTotalSpent(userId);

        BonusLevel curr = getCurrentLevel(balance);
        BonusLevel next = getNextLevel(balance);
        int progress    = getProgressPercent(balance);
        int toNext      = getAmountToNextLevel(balance);

        List<BonusTransaction> history = bonusRepo.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> historyList = history.stream().limit(10)
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("amount",      t.getAmount());
                    m.put("description", t.getDescription());
                    m.put("type",        t.getType().name());
                    m.put("date",        t.getCreatedAt() != null
                            ? t.getCreatedAt().toLocalDate().toString() : "");
                    m.put("expires",     t.getExpiresAt() != null
                            ? t.getExpiresAt().toLocalDate().toString() : null);
                    m.put("orderId",     t.getOrder() != null ? t.getOrder().getId() : null);
                    return m;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("balance",         balance);
        result.put("totalSpent",      spent);
        result.put("currentLevel",    curr.name());
        result.put("currentCashback", curr.cashbackPercent());
        result.put("nextLevel",       next != null ? next.name() : null);
        result.put("nextCashback",    next != null ? next.cashbackPercent() : null);
        result.put("progress",        progress);
        result.put("toNextLevel",     toNext);
        result.put("history",         historyList);
        result.put("isMaxLevel",      next == null);
        return result;
    }

    public List<BonusLevel> getAllLevels() { return LEVELS; }
}