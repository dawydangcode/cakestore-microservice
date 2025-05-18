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
import java.time.LocalDateTime;
import java.util.List;
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
    public List<Product> getAllProducts() {
        logger.info("Received request to get all products");
        List<Product> products = productService.getAllProducts();
        logger.info("Returning {} products", products.size());
        return products;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam(required = false) String keyword) {
        logger.info("Received search request with keyword: {}", keyword);
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

    // Các phương thức khác giữ nguyên
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> addProduct(
            @RequestPart(value = "product", required = true) String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            Product product;
            try {
                product = objectMapper.readValue(productJson, Product.class);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            logger.info("Parsed product: {}", product);
            logger.info("Received image: {}", (imageFile != null ? imageFile.getOriginalFilename() : "No image"));

            Product savedProduct = productService.addProduct(product, imageFile);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            logger.error("Error adding product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping(value = "/add-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> addProductJson(@RequestBody Product product) throws IOException {
        logger.info("Received product (JSON): {}", product);
        Product savedProduct = productService.addProduct(product, null);
        return ResponseEntity.ok(savedProduct);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestPart(value = "product", required = true) String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            Product product;
            try {
                product = objectMapper.readValue(productJson, Product.class);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            logger.info("Parsed product for update: {}", product);
            logger.info("Received image for update: {}", (imageFile != null ? imageFile.getOriginalFilename() : "No image"));

            Product updatedProduct = productService.updateProduct(id, product, imageFile);
            if (updatedProduct != null) {
                return ResponseEntity.ok(updatedProduct);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/update-category/{categoryId}")
    public ResponseEntity<Void> updateProductsCategory(@PathVariable Long categoryId) {
        productService.updateProductsCategory(categoryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> addReview(@RequestBody ReviewDTO reviewDTO, HttpServletRequest request) {
        logger.info("Received POST /products/reviews for productId: {}", reviewDTO.getProductId());
        String userName = (String) request.getAttribute("userName");
        String token = request.getHeader("Authorization").replace("Bearer ", "");

        if (userName == null) {
            logger.warn("Unauthorized: Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
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

    @GetMapping("/reviews/product/{productId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByProductId(@PathVariable Long productId) {
        logger.info("Processing GET /products/reviews/product/{} at {}", productId, LocalDateTime.now());
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Missing required part: " + ex.getRequestPartName() + ". Please ensure all required parts are included in the request.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body("Unsupported Content-Type: " + ex.getContentType() + ". Expected 'multipart/form-data'.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartException(MultipartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to parse multipart request: " + ex.getMessage() + ". Ensure the request is a valid multipart/form-data with a proper boundary.");
    }

    private <T> ResponseEntity<T> body(T body, String message) {
        logger.info(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}