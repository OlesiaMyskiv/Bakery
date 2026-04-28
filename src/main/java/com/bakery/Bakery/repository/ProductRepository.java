// ===================== ProductRepository.java =====================
package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTypeOrderByNameAsc(Product.ProductType type);
    List<Product> findAllByOrderByTypeAscNameAsc();
}
