package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email); // Щоб ми могли перевірити, чи існує вже такий email
}