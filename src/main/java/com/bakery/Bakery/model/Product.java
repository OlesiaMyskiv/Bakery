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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "price_unit")
    private String priceUnit = "грн/шт";

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "available")
    private boolean available = true;

    // ── НОВІ ПОЛЯ ─────────────────────────────────────────────────────────────

    /** Знижка у відсотках для ЗСУ/ДСНС/ДПСУ. 0 = без знижки. Адмін задає. */
    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    /** "Про смак" — окремий текстовий блок на сторінці товару */
    @Column(name = "flavor_description", columnDefinition = "TEXT")
    private String flavorDescription;

    /** "Склад" — інгредієнти */
    @Column(name = "ingredients", columnDefinition = "TEXT")
    private String ingredients;

    /** Калорійність, наприклад "320 ккал/100 г" */
    @Column(name = "calories")
    private String calories;

    /** Алергени, наприклад "містить лактозу, глютен" */
    @Column(name = "allergens")
    private String allergens;

    /** Мінімальна вага в кг (для лічильника на сторінці товару) */
    @Column(name = "min_weight_kg")
    private Double minWeightKg = 1.0;

    // ── ІСНУЮЧІ ПОЛЯ ──────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_type", nullable = false)
    private CatalogType catalogType;

    public enum CatalogType {
        FLAVOR("Каталог Смаків"), DESIGN("Каталог Дизайнів"), LINE("Лінійка виробів");
        private final String label;
        CatalogType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "flavor_base")
    private FlavorBase flavorBase;

    public enum FlavorBase {
        CHOCOLATE("Шоколадні"), BERRY_FRUIT("Ягідні та фруктові"),
        NUT_CARAMEL("Горіхові / Карамельні"), CREAM_MOUSSE("Вершкові та мусові"),
        CHEESECAKE("Чізкейки");
        private final String label;
        FlavorBase(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    @Column(name = "dietary_tags")
    private String dietaryTags;

    @Enumerated(EnumType.STRING)
    @Column(name = "design_event")
    private DesignEvent designEvent;

    public enum DesignEvent {
        BIRTHDAY("День народження"), WEDDING("Весілля / Дівич-вечір"),
        CHRISTENING("Хрестини / Baby Shower"), COMMUNION("Перше причастя"),
        CORPORATE("Корпоративні свята"), ROMANTIC("Романтика / Річниця"),
        SEASONAL("Сезонні свята");
        private final String label;
        DesignEvent(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    @Column(name = "design_for")
    private String designFor;

    public enum DesignFor {
        FOR_GIRL("Для дівчинки"), FOR_BOY("Для хлопчика"),
        FOR_WOMAN("Для жінки"), FOR_MAN("Для чоловіка"),
        FOR_COUPLE("Для пари"), UNISEX("Універсальний");
        private final String label;
        DesignFor(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "product_line")
    private ProductLine productLine;

    public enum ProductLine {
        BENTO("Бенто-торти"), PORTION("Порційні десерти"), CUPCAKE("Капкейки"),
        MACARONS("Макаронс"), CAKE_POPS("Кейк-попси"), SWEET_BOX("Sweet-бокси"),
        CANDY_BAR("Кенді-бар"), DECOR("Аксесуари та декор"), CANDLES("Свічки"),
        TOPPERS("Топери"), CARDS("Листівки");
        private final String label;
        ProductLine(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency")
    private Urgency urgency;

    public enum Urgency {
        TODAY("Готові на сьогодні"), HOURS_24("Виготовлення 24 год"), HOURS_48("Виготовлення 48 год+");
        private final String label;
        Urgency(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // ── РОЗРАХУНКОВІ МЕТОДИ ───────────────────────────────────────────────────

    /**
     * Розраховує пільгову ціну для ЗСУ/ДСНС/ДПСУ.
     * Якщо знижка 0 — повертає звичайну ціну.
     */
    public Integer getDiscountedPrice() {
        if (discountPercent == null || discountPercent == 0) return price;
        return (int) Math.round(price * (1.0 - discountPercent / 100.0));
    }

    /** Чи є знижка для захисників? */
    public boolean hasDiscount() {
        return discountPercent != null && discountPercent > 0;
    }

    public boolean hasDietaryTag(String tag) {
        return dietaryTags != null && dietaryTags.contains(tag);
    }

    public boolean hasDesignFor(String tag) {
        return designFor != null && designFor.contains(tag);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public String getFlavorDescription() { return flavorDescription; }
    public void setFlavorDescription(String flavorDescription) { this.flavorDescription = flavorDescription; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getCalories() { return calories; }
    public void setCalories(String calories) { this.calories = calories; }

    public String getAllergens() { return allergens; }
    public void setAllergens(String allergens) { this.allergens = allergens; }

    public Double getMinWeightKg() { return minWeightKg != null ? minWeightKg : 1.0; }
    public void setMinWeightKg(Double minWeightKg) { this.minWeightKg = minWeightKg; }

    public CatalogType getCatalogType() { return catalogType; }
    public void setCatalogType(CatalogType catalogType) { this.catalogType = catalogType; }

    public FlavorBase getFlavorBase() { return flavorBase; }
    public void setFlavorBase(FlavorBase flavorBase) { this.flavorBase = flavorBase; }

    public String getDietaryTags() { return dietaryTags; }
    public void setDietaryTags(String dietaryTags) { this.dietaryTags = dietaryTags; }

    public DesignEvent getDesignEvent() { return designEvent; }
    public void setDesignEvent(DesignEvent designEvent) { this.designEvent = designEvent; }

    public String getDesignFor() { return designFor; }
    public void setDesignFor(String designFor) { this.designFor = designFor; }

    public ProductLine getProductLine() { return productLine; }
    public void setProductLine(ProductLine productLine) { this.productLine = productLine; }

    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
}