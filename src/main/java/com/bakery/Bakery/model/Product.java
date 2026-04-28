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

    // ЗМІНЕНО: тепер String через кому, бо можна обрати кілька
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
    public CatalogType getCatalogType() { return catalogType; }
    public void setCatalogType(CatalogType catalogType) { this.catalogType = catalogType; }
    public FlavorBase getFlavorBase() { return flavorBase; }
    public void setFlavorBase(FlavorBase flavorBase) { this.flavorBase = flavorBase; }
    public String getDietaryTags() { return dietaryTags; }
    public void setDietaryTags(String dietaryTags) { this.dietaryTags = dietaryTags; }
    public DesignEvent getDesignEvent() { return designEvent; }
    public void setDesignEvent(DesignEvent designEvent) { this.designEvent = designEvent; }
    public String getDesignFor() { return designFor; }           // тепер String
    public void setDesignFor(String designFor) { this.designFor = designFor; }
    public ProductLine getProductLine() { return productLine; }
    public void setProductLine(ProductLine productLine) { this.productLine = productLine; }
    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }

    public boolean hasDietaryTag(String tag) {
        return dietaryTags != null && dietaryTags.contains(tag);
    }
    public boolean hasDesignFor(String tag) {
        return designFor != null && designFor.contains(tag);
    }
}
