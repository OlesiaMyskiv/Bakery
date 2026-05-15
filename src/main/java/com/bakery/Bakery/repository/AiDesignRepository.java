package com.bakery.bakery.repository;

import com.bakery.bakery.model.AiDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiDesignRepository extends JpaRepository<AiDesign, Long> {
    List<AiDesign> findByIsPublicTrueOrderByCreatedAtDesc();
}