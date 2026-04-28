package com.bakery.Bakery.service;

import com.bakery.Bakery.model.Product;
import com.bakery.Bakery.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public ProductService(ProductRepository productRepository,
                          FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }

    // ── Читання ───────────────────────────────────────────────────────────────

    public List<Product> findByCatalog(Product.CatalogType catalog) {
        return productRepository.findByCatalogTypeAndAvailableTrueOrderByNameAsc(catalog);
    }

    public List<Product> findByCatalogAdmin(Product.CatalogType catalog) {
        return productRepository.findByCatalogTypeOrderByNameAsc(catalog);
    }

    public List<Product> findAll() {
        return productRepository.findAllByOrderByCatalogTypeAscNameAsc();
    }

    public Product findByIdOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Продукт #" + id + " не знайдено"));
    }

    public List<Product> search(String query) {
        return productRepository.searchByName(query);
    }

    // ── Запис ─────────────────────────────────────────────────────────────────

    @Transactional
    public Product save(Product product, MultipartFile image) throws IOException {
        if (image != null && !image.isEmpty()) {
            String subFolder = switch (product.getCatalogType()) {
                case FLAVOR -> "flavor";
                case DESIGN -> "design";
                case LINE   -> "line";
            };
            String path = fileStorageService.saveFile(image, "uploads/assortment/" + subFolder);
            product.setImagePath(path);
        }
        return productRepository.save(product);
    }

    @Transactional
    public void toggleAvailability(Long id) {
        Product p = findByIdOrThrow(id);
        p.setAvailable(!p.isAvailable());
        productRepository.save(p);
    }

    @Transactional
    public void delete(Long id) {
        Product p = findByIdOrThrow(id);
        if (p.getImagePath() != null) {
            try {
                Path filePath = Paths.get(System.getProperty("user.dir"))
                        .resolve(p.getImagePath().replaceFirst("^/", ""));
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
                // файл вже видалено або недоступний — не критично
            }
        }
        productRepository.deleteById(id);
    }
}
