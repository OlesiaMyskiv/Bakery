package com.bakery.Bakery.repository;

import com.bakery.Bakery.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Всі за каталогом
    List<Product> findByCatalogTypeAndAvailableTrueOrderByNameAsc(Product.CatalogType catalogType);

    // За каталогом і смаком
    List<Product> findByCatalogTypeAndFlavorBaseAndAvailableTrueOrderByNameAsc(
            Product.CatalogType catalogType, Product.FlavorBase flavorBase);

    // За каталогом і подією
    List<Product> findByCatalogTypeAndDesignEventAndAvailableTrueOrderByNameAsc(
            Product.CatalogType catalogType, Product.DesignEvent designEvent);

    // За каталогом і лінійкою
    List<Product> findByCatalogTypeAndProductLineAndAvailableTrueOrderByNameAsc(
            Product.CatalogType catalogType, Product.ProductLine productLine);

    // За терміновістю
    List<Product> findByUrgencyAndAvailableTrueOrderByNameAsc(Product.Urgency urgency);

    // Для адміна — всі
    List<Product> findAllByOrderByCatalogTypeAscNameAsc();

    // За каталогом для адміна (включно з прихованими)
    List<Product> findByCatalogTypeOrderByNameAsc(Product.CatalogType catalogType);

    // Пошук за ім'ям
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) AND p.available = true")
    List<Product> searchByName(@Param("q") String query);
}