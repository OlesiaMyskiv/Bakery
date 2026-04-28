package com.bakery.Bakery.controller;

import com.bakery.Bakery.model.Product;
import com.bakery.Bakery.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Публічна сторінка асортименту (/assortment).
 * Доступна без авторизації (вже в SecurityConfig.permitAll).
 */
@Controller
@RequestMapping("/assortment")
public class AssortmentController {

    private final ProductService productService;

    public AssortmentController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /assortment — головна сторінка каталогу.
     * ?catalog=FLAVOR|DESIGN|LINE   (за замовчуванням FLAVOR)
     * ?flavor=CHOCOLATE|...         (фільтр по смаку)
     * ?event=BIRTHDAY|...           (фільтр по події)
     * ?line=BENTO|...               (фільтр по лінійці)
     * ?q=...                        (пошук по назві)
     */
    @GetMapping
    public String catalog(
            @RequestParam(defaultValue = "FLAVOR") String catalog,
            @RequestParam(required = false) String flavor,
            @RequestParam(required = false) String event,
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String q,
            Model model) {

        Product.CatalogType catalogType = parseCatalog(catalog);
        List<Product> products;

        if (q != null && !q.isBlank()) {
            products = productService.search(q.trim());
            model.addAttribute("searchQuery", q.trim());
        } else if (flavor != null && !flavor.isBlank() && catalogType == Product.CatalogType.FLAVOR) {
            products = filterByFlavor(catalogType, flavor);
        } else if (event != null && !event.isBlank() && catalogType == Product.CatalogType.DESIGN) {
            products = filterByEvent(catalogType, event);
        } else if (line != null && !line.isBlank() && catalogType == Product.CatalogType.LINE) {
            products = filterByLine(catalogType, line);
        } else {
            products = productService.findByCatalog(catalogType);
        }

        model.addAttribute("products", products);
        model.addAttribute("currentCatalog", catalogType);
        model.addAttribute("catalogTypes", Product.CatalogType.values());
        model.addAttribute("flavorBases",  Product.FlavorBase.values());
        model.addAttribute("designEvents", Product.DesignEvent.values());
        model.addAttribute("productLines", Product.ProductLine.values());

        return "assortment"; // templates/assortment.html
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product.CatalogType parseCatalog(String raw) {
        try { return Product.CatalogType.valueOf(raw); }
        catch (Exception e) { return Product.CatalogType.FLAVOR; }
    }

    private List<Product> filterByFlavor(Product.CatalogType catalog, String raw) {
        try {
            return productService.findByCatalog(catalog).stream()
                    .filter(p -> p.getFlavorBase() != null
                            && p.getFlavorBase().name().equals(raw))
                    .toList();
        } catch (Exception e) { return productService.findByCatalog(catalog); }
    }

    private List<Product> filterByEvent(Product.CatalogType catalog, String raw) {
        try {
            return productService.findByCatalog(catalog).stream()
                    .filter(p -> p.getDesignEvent() != null
                            && p.getDesignEvent().name().equals(raw))
                    .toList();
        } catch (Exception e) { return productService.findByCatalog(catalog); }
    }

    private List<Product> filterByLine(Product.CatalogType catalog, String raw) {
        try {
            return productService.findByCatalog(catalog).stream()
                    .filter(p -> p.getProductLine() != null
                            && p.getProductLine().name().equals(raw))
                    .toList();
        } catch (Exception e) { return productService.findByCatalog(catalog); }
    }
}
