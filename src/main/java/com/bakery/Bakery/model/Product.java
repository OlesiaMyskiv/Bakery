package com.bakery.Bakery.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Назва виробу
    @Column(nullable = false)
    private String name;

    // Опис
    @Column(columnDefinition = "TEXT")
    private String description;

    // Ціна
    @Column(nullable = false)
    private Integer price;

    // Одиниця ціни: "грн/шт", "грн/кг", "грн/пачку", "грн/порцію"
    @Column(name = "price_unit")
    private String priceUnit = "грн/шт";

    // Фото
    @Column(name = "image_path")
    private String imagePath;

    // Доступний чи прихований
    @Column(name = "available")
    private boolean available = true;

    // ============================================================
    // КАТАЛОГ 1 — Каталог Смаків
    // ============================================================

    // Основа смаку
    @Enumerated(EnumType.STRING)
    @Column(name = "flavor_base")
    private FlavorBase flavorBase;

    public enum FlavorBase {
        CHOCOLATE("Шоколадні"),
        BERRY_FRUIT("Ягідні та фруктові"),
        NUT_CARAMEL("Горіхові / Карамельні"),
        CREAM_MOUSSE("Вершкові та мусові"),
        CHEESECAKE("Чізкейки");

        private final String label;
        FlavorBase(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // Дієтичні особливості (зберігаємо як рядок через кому: "NO_SUGAR,VEGAN")
    @Column(name = "dietary_tags")
    private String dietaryTags;

    public static final String DIET_NO_SUGAR   = "NO_SUGAR";
    public static final String DIET_NO_GLUTEN  = "NO_GLUTEN";
    public static final String DIET_NO_LACTOSE = "NO_LACTOSE";
    public static final String DIET_VEGAN      = "VEGAN";

    // ============================================================
    // КАТАЛОГ 2 — Каталог Дизайнів
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "design_event")
    private DesignEvent designEvent;

    public enum DesignEvent {
        BIRTHDAY("День народження"),
        FOR_GIRL("Для дівчинки"),
        FOR_BOY("Для хлопчика"),
        FOR_WOMAN("Для жінки"),
        FOR_MAN("Для чоловіка"),
        WEDDING("Весілля / Дівич-вечір"),
        CHRISTENING("Хрестини / Baby Shower"),
        COMMUNION("Перше причастя"),
        CORPORATE("Корпоративні свята"),
        ROMANTIC("Романтика / Річниця"),
        SEASONAL("Сезонні свята");

        private final String label;
        DesignEvent(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // ============================================================
    // КАТАЛОГ 3 — Лінійка виробів
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "product_line")
    private ProductLine productLine;

    public enum ProductLine {
        BENTO("Бенто-торти"),
        PORTION("Порційні десерти"),
        CUPCAKE("Капкейки"),
        MACARONS("Макаронс"),
        CAKE_POPS("Кейк-попси"),
        SWEET_BOX("Sweet-бокси"),
        CANDY_BAR("Кенді-бар"),
        DECOR("Аксесуари та декор"),
        CANDLES("Свічки"),
        TOPPERS("Топери"),
        CARDS("Листівки");

        private final String label;
        ProductLine(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // ============================================================
    // ТЕРМІНОВІСТЬ — спільна для всіх каталогів
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency")
    private Urgency urgency;

    public enum Urgency {
        TODAY("Готові на сьогодні"),
        HOURS_24("Виготовлення 24 год"),
        HOURS_48("Виготовлення 48 год+");

        private final String label;
        Urgency(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // ============================================================
    // Який каталог: FLAVOR / DESIGN / LINE
    // ============================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_type", nullable = false)
    private CatalogType catalogType;

    public enum CatalogType {
        FLAVOR("Каталог Смаків"),
        DESIGN("Каталог Дизайнів"),
        LINE("Лінійка виробів");

        private final String label;
        CatalogType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // ============================================================
    // Getters & Setters
    // ============================================================

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

    public FlavorBase getFlavorBase() { return flavorBase; }
    public void setFlavorBase(FlavorBase flavorBase) { this.flavorBase = flavorBase; }

    public String getDietaryTags() { return dietaryTags; }
    public void setDietaryTags(String dietaryTags) { this.dietaryTags = dietaryTags; }

    public DesignEvent getDesignEvent() { return designEvent; }
    public void setDesignEvent(DesignEvent designEvent) { this.designEvent = designEvent; }

    public ProductLine getProductLine() { return productLine; }
    public void setProductLine(ProductLine productLine) { this.productLine = productLine; }

    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }

    public CatalogType getCatalogType() { return catalogType; }
    public void setCatalogType(CatalogType catalogType) { this.catalogType = catalogType; }

    // Зручний метод для перевірки дієтичного тегу в Thymeleaf
    public boolean hasDietaryTag(String tag) {
        return dietaryTags != null && dietaryTags.contains(tag);
    }
}