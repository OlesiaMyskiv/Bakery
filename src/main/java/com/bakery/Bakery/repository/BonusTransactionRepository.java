package com.bakery.bakery.repository;

import com.bakery.bakery.model.BonusTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BonusTransactionRepository extends JpaRepository<BonusTransaction, Long> {

    // Всі транзакції користувача (від нових до старих)
    List<BonusTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Активні (не прострочені) нарахування
    @Query("SELECT t FROM BonusTransaction t WHERE t.user.id = :userId " +
            "AND t.amount > 0 " +
            "AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    List<BonusTransaction> findActiveEarned(@Param("userId") Long userId,
                                            @Param("now") LocalDateTime now);

    // Чи є вже вітальний бонус для цього юзера
    boolean existsByUserIdAndType(Long userId, BonusTransaction.TransactionType type);

    // Чи вже нараховано бонус за конкретне замовлення (захист від подвійного нарахування)
    boolean existsByOrderIdAndType(Long orderId, BonusTransaction.TransactionType type);

    // Чи є бонус на день народження цього року
    @Query("SELECT COUNT(t) > 0 FROM BonusTransaction t WHERE t.user.id = :userId " +
            "AND t.type = 'BIRTHDAY' " +
            "AND YEAR(t.createdAt) = :year")
    boolean hasBirthdayBonusThisYear(@Param("userId") Long userId,
                                     @Param("year") int year);

    // ТОП донаторів місяця — сума DONATED за поточний місяць по юзеру
    @Query("SELECT t.user.id, t.user.username, t.user.profilePicturePath, " +
            "SUM(ABS(t.amount)) as totalDonated " +
            "FROM BonusTransaction t " +
            "WHERE t.type = 'DONATED' " +
            "AND MONTH(t.createdAt) = MONTH(CURRENT_DATE) " +
            "AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) " +
            "GROUP BY t.user.id, t.user.username, t.user.profilePicturePath " +
            "ORDER BY totalDonated DESC")
    List<Object[]> findTopDonatorsByMonth();

}