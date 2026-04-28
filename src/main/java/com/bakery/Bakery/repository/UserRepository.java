// ===================== UserRepository.java =====================
package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.User;
import com.bakery.Bakery.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByVerificationStatusOrderByIdDesc(VerificationStatus status);
    long countByVerificationStatus(VerificationStatus status);
}
