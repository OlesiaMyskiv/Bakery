package com.bakery.Bakery.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;

    @Column(name = "price_per_kg")
    private Integer pricePerKg; // ціна за кг або за штуку

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "available")
    private boolean available = true;

    public enum ProductType {
        CAKE("Готовий торт"),
        SPONGE("Бісквіт"),
        CREAM("Крем"),
        FILLING("Начинка"),
        CUPCAKE("Капкейк"),
        COOKIE("Печиво");

        private final String label;
        ProductType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProductType getType() { return type; }
    public void setType(ProductType type) { this.type = type; }

    public Integer getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(Integer pricePerKg) { this.pricePerKg = pricePerKg; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
