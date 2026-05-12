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

    // ── Рівні ────────────────────────────────────────────────────────────────

    public record BonusLevel(String name, int minSpent, int maxSpent, int cashbackPercent) {}

    private static final List<BonusLevel> LEVELS = List.of(
            new BonusLevel("Ласун",         0,    300,  3),
            new BonusLevel("Гурман",        300,  800,  5),
            new BonusLevel("Майстер тіста", 800,  1500, 7),
            new BonusLevel("Шеф-пекар",     1500, Integer.MAX_VALUE, 10)
    );

    // ── Баланс ────────────────────────────────────────────────────────────────

    /**
     * Поточний баланс — сума всіх активних нарахувань мінус всі списання.
     */
    @Transactional(readOnly = true)
    public int getBalance(Long userId) {
        List<BonusTransaction> all = bonusRepo.findByUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();

        int balance = 0;
        for (BonusTransaction t : all) {
            if (t.getAmount() < 0) {
                // Списання завжди враховуємо
                balance += t.getAmount();
            } else {
                // Нарахування — тільки якщо не прострочено
                if (t.getExpiresAt() == null || t.getExpiresAt().isAfter(now)) {
                    balance += t.getAmount();
                }
            }
        }
        return Math.max(0, balance);
    }

    /**
     * Загальна сума витрат користувача (для визначення рівня).
     */
    @Transactional(readOnly = true)
    public int getTotalSpent(Long userId) {
        // Рахуємо суму унікальних замовлень (один раз кожне)
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

    /**
     * Поточний рівень користувача.
     */
    public BonusLevel getCurrentLevel(int totalSpent) {
        return LEVELS.stream()
                .filter(l -> totalSpent >= l.minSpent() && totalSpent < l.maxSpent())
                .findFirst()
                .orElse(LEVELS.get(LEVELS.size() - 1));
    }

    /**
     * Наступний рівень (null якщо вже максимальний).
     */
    public BonusLevel getNextLevel(int totalSpent) {
        for (int i = 0; i < LEVELS.size() - 1; i++) {
            if (totalSpent < LEVELS.get(i + 1).minSpent()) {
                return LEVELS.get(i + 1);
            }
        }
        return null; // вже Шеф-пекар
    }

    /**
     * Скільки гривень залишилось до наступного рівня.
     */
    public int getAmountToNextLevel(int totalSpent) {
        BonusLevel next = getNextLevel(totalSpent);
        if (next == null) return 0;
        return next.minSpent() - totalSpent;
    }

    /**
     * Прогрес у відсотках до наступного рівня.
     */
    public int getProgressPercent(int totalSpent) {
        BonusLevel current = getCurrentLevel(totalSpent);
        BonusLevel next = getNextLevel(totalSpent);
        if (next == null) return 100;
        int range = next.minSpent() - current.minSpent();
        int done  = totalSpent - current.minSpent();
        return Math.min(100, (int) ((double) done / range * 100));
    }

    // ── Нарахування ───────────────────────────────────────────────────────────

    /**
     * Нараховуємо бонуси за виконане замовлення.
     * Відсоток кешбеку залежить від рівня клієнта.
     */
    @Transactional
    public BonusTransaction earnForOrder(User user, Order order) {
        // Захист від подвійного нарахування за одне замовлення
        if (bonusRepo.existsByOrderIdAndType(order.getId(), BonusTransaction.TransactionType.EARNED)) {
            return null;
        }

        int totalSpent = getTotalSpent(user.getId());
        BonusLevel level = getCurrentLevel(totalSpent);

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

    /**
     * Вітальний бонус при реєстрації (50 балів, один раз).
     */
    @Transactional
    public BonusTransaction giveWelcomeBonus(User user) {
        if (bonusRepo.existsByUserIdAndType(user.getId(),
                BonusTransaction.TransactionType.WELCOME)) {
            return null; // вже нараховано
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

    /**
     * Бонус на день народження (подвійний кешбек за наступне замовлення).
     * Нараховуємо 100 балів один раз на рік.
     */
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
        t.setExpiresAt(LocalDateTime.now().plusMonths(1)); // діє 1 місяць
        return bonusRepo.save(t);
    }

    // ── Списання ──────────────────────────────────────────────────────────────

    /**
     * Списуємо бонуси при оплаті замовлення.
     * Максимум 30% від суми замовлення.
     * Повертає скільки фактично списано.
     */
    @Transactional
    public int spend(User user, Order order, int requestedAmount) {
        int balance = getBalance(user.getId());
        int maxAllowed = (int) Math.floor(order.getPrice() * 0.30); // 30% від замовлення
        int toSpend = Math.min(requestedAmount, Math.min(balance, maxAllowed));

        if (toSpend <= 0) return 0;

        BonusTransaction t = new BonusTransaction();
        t.setUser(user);
        t.setOrder(order);
        t.setType(BonusTransaction.TransactionType.SPENT);
        t.setAmount(-toSpend); // від'ємне!
        t.setDescription("Списано за замовлення №" + order.getId());
        t.setCreatedAt(LocalDateTime.now());
        bonusRepo.save(t);

        return toSpend;
    }

    // ── Дані для сторінки профілю ─────────────────────────────────────────────

    /**
     * Повний дашборд бонусів для відображення в профілі.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(Long userId) {
        int balance     = getBalance(userId);
        int totalSpent  = getTotalSpent(userId);
        BonusLevel curr = getCurrentLevel(totalSpent);
        BonusLevel next = getNextLevel(totalSpent);
        int progress    = getProgressPercent(totalSpent);
        int toNext      = getAmountToNextLevel(totalSpent);

        List<BonusTransaction> history =
                bonusRepo.findByUserIdOrderByCreatedAtDesc(userId);

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
        result.put("totalSpent",      totalSpent);
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