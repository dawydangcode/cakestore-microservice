package fit.iuh.edu.vn.product_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.services.ProductService;
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
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/list")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> addProduct(
            @RequestPart(value = "product", required = true) String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            // Parse chuỗi JSON thành Product
            Product product;
            try {
                product = objectMapper.readValue(productJson, Product.class);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }
            System.out.println("Parsed product: " + product);
            System.out.println("Received image: " + (imageFile != null ? imageFile.getOriginalFilename() : "No image"));

            // Lưu sản phẩm
            Product savedProduct = productService.addProduct(product, imageFile);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping(value = "/add-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> addProductJson(@RequestBody Product product) throws IOException {
        System.out.println("Received product (JSON): " + product);
        Product savedProduct = productService.addProduct(product, null);
        return ResponseEntity.ok(savedProduct);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestPart(value = "product", required = true) String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            // Parse chuỗi JSON thành Product
            Product product;
            try {
                product = objectMapper.readValue(productJson, Product.class);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }
            System.out.println("Parsed product for update: " + product);
            System.out.println("Received image for update: " + (imageFile != null ? imageFile.getOriginalFilename() : "No image"));

            // Cập nhật sản phẩm
            Product updatedProduct = productService.updateProduct(id, product, imageFile);
            if (updatedProduct != null) {
                return ResponseEntity.ok(updatedProduct);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
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

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam(required = false) String keyword) {
        try {
            List<Product> products;
            if (keyword != null && !keyword.trim().isEmpty()) {
                products = productService.searchProducts(keyword.trim().toLowerCase());
            } else {
                products = productService.getAllProducts();
            }
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
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

    // Tiện ích để tạo ResponseEntity với body null và message
    private <T> ResponseEntity<T> body(T body, String message) {
        System.out.println(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}