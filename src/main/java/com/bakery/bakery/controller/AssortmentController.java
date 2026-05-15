package com.bakery.bakery.controller;

import com.bakery.bakery.model.Product;
import com.bakery.bakery.model.Role;
import com.bakery.bakery.model.User;
import com.bakery.bakery.model.VerificationStatus;
import com.bakery.bakery.service.ProductService;
import com.bakery.bakery.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/assortment")
public class AssortmentController {

    private static final int PAGE_SIZE = 12;

    private final ProductService productService;
    private final UserService userService;

    public AssortmentController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    /**
     * GET /assortment
     *
     * Параметри:
     *   catalog   FLAVOR|DESIGN|LINE          (вкладка)
     *   q         рядок пошуку               (глобальний по всіх каталогах)
     *   flavors   список FlavorBase           (чекбокси, multiple)
     *   dietary   список тегів               (чекбокси, multiple)
     *   events    список DesignEvent          (чекбокси, multiple)
     *   designFor список DesignFor            (чекбокси, multiple)
     *   lines     список ProductLine          (чекбокси, multiple)
     *   urgency   список Urgency              (чекбокси, multiple)
     *   minPrice  int                         (слайдер від)
     *   maxPrice  int                         (слайдер до)
     *   sort      price_asc|price_desc|name_asc|name_desc
     *   page      int (1-based)
     */
    @GetMapping
    public String catalog(
            @RequestParam(defaultValue = "FLAVOR") String catalog,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> flavors,
            @RequestParam(required = false) List<String> dietary,
            @RequestParam(required = false) List<String> events,
            @RequestParam(required = false) List<String> designFor,
            @RequestParam(required = false) List<String> lines,
            @RequestParam(required = false) List<String> urgency,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "name_asc") String sort,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        // ── Поточний каталог ──────────────────────────────────────────────────
        Product.CatalogType catalogType = parseCatalog(catalog);

        // ── Чи юзер верифікований (бачить пільгову ціну) ─────────────────────
        User currentUser = userService.getCurrentUser();
        boolean isVerified = currentUser != null
                && currentUser.getVerificationStatus() == VerificationStatus.APPROVED
                && currentUser.getRole() != Role.SUPER_ADMIN;
        model.addAttribute("isVerified", isVerified);

        // ── Базовий список ────────────────────────────────────────────────────
        List<Product> products;

        if (q != null && !q.isBlank()) {
            // Пошук глобальний по всіх каталогах
            products = productService.search(q.trim());
            model.addAttribute("searchQuery", q.trim());
        } else {
            products = productService.findByCatalog(catalogType);
        }

        // ── Фільтри ───────────────────────────────────────────────────────────
        products = applyFilters(products, catalogType, flavors, dietary,
                events, designFor, lines, urgency, minPrice, maxPrice);

        // ── Сортування ────────────────────────────────────────────────────────
        products = applySorting(products, sort);

        // ── Пагінація ─────────────────────────────────────────────────────────
        int totalItems = products.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        page = Math.max(1, Math.min(page, totalPages));

        int fromIdx = (page - 1) * PAGE_SIZE;
        int toIdx   = Math.min(fromIdx + PAGE_SIZE, totalItems);
        List<Product> pageProducts = products.subList(fromIdx, toIdx);

        // ── Модель ────────────────────────────────────────────────────────────
        model.addAttribute("products",       pageProducts);
        model.addAttribute("totalItems",     totalItems);
        model.addAttribute("totalPages",     totalPages);
        model.addAttribute("currentPage",    page);
        model.addAttribute("currentCatalog", catalogType);
        model.addAttribute("currentSort",    sort);
        model.addAttribute("catalog",        catalog);

        // Enum-и для фільтрів
        model.addAttribute("catalogTypes",  Product.CatalogType.values());
        model.addAttribute("flavorBases",   Product.FlavorBase.values());
        model.addAttribute("designEvents",  Product.DesignEvent.values());
        model.addAttribute("designFors",    Product.DesignFor.values());
        model.addAttribute("productLines",  Product.ProductLine.values());
        model.addAttribute("urgencies",     Product.Urgency.values());

        // Поточні фільтри (щоб чекбокси залишались відміченими)
        model.addAttribute("selectedFlavors",   flavors   != null ? flavors   : List.of());
        model.addAttribute("selectedDietary",   dietary   != null ? dietary   : List.of());
        model.addAttribute("selectedEvents",    events    != null ? events    : List.of());
        model.addAttribute("selectedDesignFor", designFor != null ? designFor : List.of());
        model.addAttribute("selectedLines",     lines     != null ? lines     : List.of());
        model.addAttribute("selectedUrgency",   urgency   != null ? urgency   : List.of());
        model.addAttribute("minPrice",          minPrice);
        model.addAttribute("maxPrice",          maxPrice);

        return "assortment";
    }

    // ── Логіка фільтрації ─────────────────────────────────────────────────────

    private List<Product> applyFilters(List<Product> products,
                                       Product.CatalogType catalog,
                                       List<String> flavors,
                                       List<String> dietary,
                                       List<String> events,
                                       List<String> designFor,
                                       List<String> lines,
                                       List<String> urgency,
                                       Integer minPrice,
                                       Integer maxPrice) {
        var stream = products.stream();

        // Фільтр по смаку (тільки FLAVOR)
        if (catalog == Product.CatalogType.FLAVOR && notEmpty(flavors)) {
            stream = stream.filter(p -> p.getFlavorBase() != null
                    && flavors.contains(p.getFlavorBase().name()));
        }

        // Дієтичні теги (тільки FLAVOR)
        if (catalog == Product.CatalogType.FLAVOR && notEmpty(dietary)) {
            stream = stream.filter(p -> dietary.stream()
                    .allMatch(tag -> p.hasDietaryTag(tag)));
        }

        // Подія (тільки DESIGN)
        if (catalog == Product.CatalogType.DESIGN && notEmpty(events)) {
            stream = stream.filter(p -> p.getDesignEvent() != null
                    && events.contains(p.getDesignEvent().name()));
        }

        // Для кого (тільки DESIGN)
        if (catalog == Product.CatalogType.DESIGN && notEmpty(designFor)) {
            stream = stream.filter(p -> designFor.stream()
                    .anyMatch(tag -> p.hasDesignFor(tag)));
        }

        // Лінійка (тільки LINE)
        if (catalog == Product.CatalogType.LINE && notEmpty(lines)) {
            stream = stream.filter(p -> p.getProductLine() != null
                    && lines.contains(p.getProductLine().name()));
        }

        // Терміновість (будь-який каталог)
        if (notEmpty(urgency)) {
            stream = stream.filter(p -> p.getUrgency() != null
                    && urgency.contains(p.getUrgency().name()));
        }

        // Ціна від
        if (minPrice != null) {
            stream = stream.filter(p -> p.getPrice() >= minPrice);
        }

        // Ціна до
        if (maxPrice != null) {
            stream = stream.filter(p -> p.getPrice() <= maxPrice);
        }

        return stream.toList();
    }

    // ── Логіка сортування ─────────────────────────────────────────────────────

    private List<Product> applySorting(List<Product> products, String sort) {
        Comparator<Product> comparator = switch (sort) {
            case "price_asc"  -> Comparator.comparingInt(Product::getPrice);
            case "price_desc" -> Comparator.comparingInt(Product::getPrice).reversed();
            case "name_desc"  -> Comparator.comparing(Product::getName).reversed();
            default           -> Comparator.comparing(Product::getName); // name_asc
        };
        return products.stream().sorted(comparator).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product.CatalogType parseCatalog(String raw) {
        try { return Product.CatalogType.valueOf(raw); }
        catch (Exception e) { return Product.CatalogType.FLAVOR; }
    }

    private boolean notEmpty(List<String> list) {
        return list != null && !list.isEmpty();
    }
}