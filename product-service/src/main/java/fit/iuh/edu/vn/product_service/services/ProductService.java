package fit.iuh.edu.vn.product_service.services;

import fit.iuh.edu.vn.product_service.models.Category;
import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.repositories.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private S3Service s3Service;

    private static final String CATEGORY_SERVICE_URL = "http://localhost:8080/categories/";

    private final Map<Long, Category> categoryCache = new HashMap<>();

    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findByStatus("ACTIVE");
        for (Product product : products) {
            attachCategoryToProduct(product);
            if (product.isBestSeller() == null) {
                product.setBestSeller(false);
            }
        }
        return products;
    }

    public List<Product> getBestSellers() {
        List<Product> bestSellers = productRepository.findByIsBestSellerTrue();
        for (Product product : bestSellers) {
            attachCategoryToProduct(product);
            if (product.isBestSeller() == null) {
                product.setBestSeller(false);
            }
        }
        return bestSellers;
    }

    public boolean setBestSeller(Long id, boolean isBestSeller) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            if (isBestSeller) {
                long currentBestSellers = productRepository.countByIsBestSellerTrue();
                if (currentBestSellers >= 8) {
                    logger.warn("Cannot set product {} as Best Seller: Limit of 8 reached", id);
                    return false;
                }
            }
            Product product = productOpt.get();
            product.setBestSeller(isBestSeller);
            product.setUpdateAt(LocalDate.now());
            productRepository.save(product);
            return true;
        }
        return false;
    }

    public long countBestSellers() {
        return productRepository.countByIsBestSellerTrue();
    }

    public Product addProduct(Product product, MultipartFile imageFile) throws IOException {
        product.setCreateAt(LocalDate.now());
        product.setUpdateAt(LocalDate.now());
        if (product.isBestSeller() == null) {
            product.setBestSeller(false);
        }
        if (product.getStatus() == null) {
            product.setStatus("ACTIVE");
        }
        // Tự động ẩn nếu stock <= 0
        if (product.getStock() != null && product.getStock() <= 0) {
            product.setStatus("INACTIVE");
            logger.info("Product {} set to INACTIVE due to stock <= 0", product.getName());
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = s3Service.uploadFile(imageFile);
            product.setImage(imageUrl);
        }

        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product, MultipartFile imageFile) throws IOException {
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isPresent()) {
            Product p = existingProduct.get();
            p.setCategoryId(product.getCategoryId());
            p.setName(product.getName());
            p.setDescription(product.getDescription());
            p.setPrice(product.getPrice());
            p.setStock(product.getStock());
            p.setBestSeller(product.isBestSeller() != null ? product.isBestSeller() : false);
            p.setUpdateAt(LocalDate.now());
            // Tự động ẩn hoặc khôi phục dựa trên stock
            if (product.getStock() != null && product.getStock() <= 0) {
                p.setStatus("INACTIVE");
                logger.info("Product {} set to INACTIVE due to stock <= 0", p.getName());
            } else if (product.getStock() != null && product.getStock() > 0 && p.getStatus().equals("INACTIVE")) {
                p.setStatus("ACTIVE");
                logger.info("Product {} restored to ACTIVE due to stock > 0", p.getName());
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imageFile);
                p.setImage(imageUrl);
            }

            return productRepository.save(p);
        }
        return null;
    }

    public boolean hideProduct(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setStatus("INACTIVE");
            product.setUpdateAt(LocalDate.now());
            productRepository.save(product);
            return true;
        }
        return false;
    }

    public Optional<Product> getProductById(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        productOpt.ifPresent(product -> {
            attachCategoryToProduct(product);
            if (product.isBestSeller() == null) {
                product.setBestSeller(false);
            }
        });
        return productOpt;
    }

    private Product attachCategoryToProduct(Product product) {
        if (product.getCategoryId() != null) {
            try {
                Category category = categoryCache.get(product.getCategoryId());
                if (category == null) {
                    String url = CATEGORY_SERVICE_URL + product.getCategoryId();
                    logger.info("Calling Category Service: {}", url);
                    category = restTemplate.getForObject(url, Category.class);
                    if (category != null) {
                        categoryCache.put(product.getCategoryId(), category);
                    }
                }
                product.setCategory(category);
            } catch (RestClientException e) {
                logger.error("Failed to fetch category for product {}: {}", product.getId(), e.getMessage());
                product.setCategory(null);
            }
        }
        return product;
    }

    public void updateProductsCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndStatus(categoryId, "ACTIVE");
        for (Product product : products) {
            product.setCategoryId(null);
            productRepository.save(product);
        }
    }

    public List<Product> searchProducts(String keyword) {
        List<Product> products = productRepository.findByNameContainingIgnoreCaseAndStatus(keyword, "ACTIVE");
        return products.stream()
                .map(product -> {
                    attachCategoryToProduct(product);
                    if (product.isBestSeller() == null) {
                        product.setBestSeller(false);
                    }
                    return product;
                })
                .toList();
    }

    public List<Product> getInactiveProducts() {
        List<Product> products = productRepository.findByStatus("INACTIVE");
        for (Product product : products) {
            attachCategoryToProduct(product);
            if (product.isBestSeller() == null) {
                product.setBestSeller(false);
            }
        }
        return products;
    }

    public boolean restoreProduct(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            // Chỉ khôi phục nếu stock > 0
            if (product.getStock() != null && product.getStock() > 0) {
                product.setStatus("ACTIVE");
                product.setUpdateAt(LocalDate.now());
                productRepository.save(product);
                logger.info("Product {} restored to ACTIVE", product.getName());
                return true;
            } else {
                logger.warn("Cannot restore product {}: Stock is 0", id);
                throw new IllegalArgumentException("Cannot restore product: Stock is 0");
            }
        }
        return false;
    }
}