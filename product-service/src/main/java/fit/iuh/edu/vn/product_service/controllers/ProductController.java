package fit.iuh.edu.vn.product_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fit.iuh.edu.vn.product_service.dto.ReviewDTO;
import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.services.ProductService;
import fit.iuh.edu.vn.product_service.services.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/list")
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("Received request to get all products at {}", LocalDateTime.now());
        List<Product> products = productService.getAllProducts();
        logger.info("Returning {} products", products.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/bestsellers")
    public ResponseEntity<List<Product>> getBestSellers() {
        logger.info("Received request to get best sellers at {}", LocalDateTime.now());
        List<Product> bestSellers = productService.getBestSellers();
        logger.info("Returning {} best sellers", bestSellers.size());
        return ResponseEntity.ok(bestSellers);
    }

    @PutMapping("/bestseller/{id}")
    public ResponseEntity<String> setBestSeller(@PathVariable Long id, @RequestParam boolean isBestSeller) {
        logger.info("Received request to set product {} as best seller: {}", id, isBestSeller);
        boolean success = productService.setBestSeller(id, isBestSeller);
        if (success) {
            return ResponseEntity.ok(isBestSeller ? "Đã đặt sản phẩm làm Best Seller" : "Đã xóa sản phẩm khỏi Best Seller");
        }
        return ResponseEntity.badRequest().body("Không thể cập nhật trạng thái Best Seller. Có thể đã đạt giới hạn 8 sản phẩm hoặc sản phẩm không tồn tại.");
    }

    @GetMapping("/count/bestsellers")
    public ResponseEntity<Long> countBestSellers() {
        logger.info("Received request to count best sellers at {}", LocalDateTime.now());
        long count = productService.countBestSellers();
        logger.info("Returning best seller count: {}", count);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam(required = false) String keyword) {
        logger.info("Received search request with keyword: {} at {}", keyword, LocalDateTime.now());
        try {
            List<Product> products;
            if (keyword != null && !keyword.trim().isEmpty()) {
                products = productService.searchProducts(keyword.trim().toLowerCase());
            } else {
                products = productService.getAllProducts();
            }
            logger.info("Returning {} search results", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error searching products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> addProduct(
            @RequestPart(value = "product", required = true) String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        logger.info("Received POST /products/add at {}", LocalDateTime.now());
        try {
            Product product;
            try {
                product = objectMapper.readValue(productJson, Product.class);
                logger.info("Parsed product: {}", product);
            } catch (IOException e) {
                logger.error("Failed to parse product JSON: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            logger.info("Received image: {}", (imageFile != null ? imageFile.getOriginalFilename() : "No image"));

            Product savedProduct = productService.addProduct(product, imageFile);
            logger.info("Product added successfully: {}", savedProduct.getName());
            return ResponseEntity.ok(savedProduct);
        } catch (IOException e) {
            logger.error("Error adding product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping(value = "/add-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> addProductJson(@RequestBody Product product) {
        logger.info("Received POST /products/add-json with product: {} at {}", product, LocalDateTime.now());
        try {
            Product savedProduct = productService.addProduct(product, null);
            logger.info("Product added successfully: {}", savedProduct.getName());
            return ResponseEntity.ok(savedProduct);
        } catch (IOException e) {
            logger.error("Error adding product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestPart(value = "product", required = true) String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        logger.info("Received PUT /products/update/{} at {}", id, LocalDateTime.now());
        try {
            Product product;
            try {
                product = objectMapper.readValue(productJson, Product.class);
                logger.info("Parsed product for update: {}", product);
            } catch (IOException e) {
                logger.error("Failed to parse product JSON: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            logger.info("Received image for update: {}", (imageFile != null ? imageFile.getOriginalFilename() : "No image"));

            Product updatedProduct = productService.updateProduct(id, product, imageFile);
            if (updatedProduct != null) {
                logger.info("Product updated successfully: {}", updatedProduct.getName());
                return ResponseEntity.ok(updatedProduct);
            }
            logger.warn("Product with id {} not found", id);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            logger.error("Error updating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PutMapping("/hide/{id}")
    public ResponseEntity<String> hideProduct(@PathVariable Long id) {
        logger.info("Received PUT /products/hide/{} at {}", id, LocalDateTime.now());
        boolean success = productService.hideProduct(id);
        if (success) {
            logger.info("Product with id {} hidden successfully", id);
            return ResponseEntity.ok("Đã ẩn sản phẩm thành công");
        }
        logger.warn("Product with id {} not found", id);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<Product>> getInactiveProducts() {
        logger.info("Received GET /products/inactive at {}", LocalDateTime.now());
        List<Product> products = productService.getInactiveProducts();
        logger.info("Returning {} inactive products", products.size());
        return ResponseEntity.ok(products);
    }

    @PutMapping("/restore/{id}")
    public ResponseEntity<String> restoreProduct(@PathVariable Long id) {
        logger.info("Received PUT /products/restore/{} at {}", id, LocalDateTime.now());
        try {
            boolean success = productService.restoreProduct(id);
            if (success) {
                logger.info("Product with id {} restored successfully", id);
                return ResponseEntity.ok("Đã khôi phục sản phẩm thành công");
            }
            logger.warn("Product with id {} not found", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to restore product {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi khi khôi phục sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        logger.info("Received GET /products/{} at {}", id, LocalDateTime.now());
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            logger.info("Returning product: {}", product.get().getName());
            return ResponseEntity.ok(product.get());
        }
        logger.warn("Product with id {} not found", id);
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/update-category/{categoryId}")
    public ResponseEntity<Void> updateProductsCategory(@PathVariable Long categoryId) {
        logger.info("Received POST /products/update-category/{} at {}", categoryId, LocalDateTime.now());
        productService.updateProductsCategory(categoryId);
        logger.info("Products category updated for categoryId: {}", categoryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> addReview(@RequestBody ReviewDTO reviewDTO, HttpServletRequest request) {
        logger.info("Received POST /products/reviews for productId: {} at {}", reviewDTO.getProductId(), LocalDateTime.now());
        String userName = (String) request.getAttribute("userName");
        String token = request.getHeader("Authorization") != null ? request.getHeader("Authorization").replace("Bearer ", "") : null;

        if (userName == null || token == null) {
            logger.warn("Unauthorized: Invalid token or userName");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token or userName");
        }

        try {
            reviewDTO.setUserId(userName);
            ReviewDTO savedReview = reviewService.addReview(reviewDTO, token);
            logger.info("Review added successfully for productId: {} by user: {}", reviewDTO.getProductId(), userName);
            return ResponseEntity.ok(savedReview);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to add review: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error adding review: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server");
        }
    }

    @PutMapping("/update-stock/{id}")
    public ResponseEntity<Product> updateProductStock(@PathVariable Long id, @RequestBody Map<String, Integer> stockUpdate) {
        logger.info("Received PUT /products/update-stock/{} at {}", id, LocalDateTime.now());
        try {
            if (!stockUpdate.containsKey("stock") || stockUpdate.get("stock") == null) {
                logger.error("Invalid stock value provided for product {}", id);
                return ResponseEntity.badRequest().body(null);
            }
            Optional<Product> existingProduct = productService.getProductById(id);
            if (existingProduct.isPresent()) {
                Product p = existingProduct.get();
                p.setStock(stockUpdate.get("stock"));
                p.setUpdateAt(LocalDate.now());
                if (p.getStock() <= 0) {
                    p.setStatus("INACTIVE");
                    logger.info("Product {} set to INACTIVE due to stock <= 0", p.getName());
                } else if (p.getStock() > 0 && p.getStatus().equals("INACTIVE")) {
                    p.setStatus("ACTIVE");
                    logger.info("Product {} restored to ACTIVE due to stock > 0", p.getName());
                }
                Product updatedProduct = productService.updateProduct(id, p, null);
                logger.info("Product stock updated successfully: {} with stock {}", updatedProduct.getName(), updatedProduct.getStock());
                return ResponseEntity.ok(updatedProduct);
            }
            logger.warn("Product with id {} not found", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating product stock: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/reviews/product/{productId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByProductId(@PathVariable Long productId) {
        logger.info("Received GET /products/reviews/product/{} at {}", productId, LocalDateTime.now());
        try {
            List<ReviewDTO> reviews = reviewService.getReviewsByProductId(productId);
            logger.info("Returning {} reviews for productId: {}", reviews.size(), productId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            logger.error("Error fetching reviews: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        logger.error("Missing required part: {}", ex.getRequestPartName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Missing required part: " + ex.getRequestPartName() + ". Please ensure all required parts are included in the request.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        logger.error("Unsupported Content-Type: {}. Expected 'multipart/form-data'", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body("Unsupported Content-Type: " + ex.getContentType() + ". Expected 'multipart/form-data'.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartException(MultipartException ex) {
        logger.error("Failed to parse multipart request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to parse multipart request: " + ex.getMessage() + ". Ensure the request is a valid multipart/form-data with a proper boundary.");
    }
}