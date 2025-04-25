package fit.iuh.edu.vn.product_service.controller;

import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/list")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping(value = "/add", consumes = {"multipart/form-data"})
    public ResponseEntity<Product> addProduct(
            @RequestPart("product") Product product,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {
        System.out.println("Received product: " + product);
        System.out.println("Received image: " + (imageFile != null ? imageFile.getOriginalFilename() : "No image"));
        Product savedProduct = productService.addProduct(product, imageFile);
        return ResponseEntity.ok(savedProduct);
    }

    // Thêm endpoint mới để test chỉ với JSON
    @PostMapping(value = "/add-json", consumes = {"application/json"})
    public ResponseEntity<Product> addProductJson(@RequestBody Product product) throws IOException {
        System.out.println("Received product (JSON): " + product);
        Product savedProduct = productService.addProduct(product, null); // Không có image
        return ResponseEntity.ok(savedProduct);
    }

    @PutMapping(value = "/update/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") Product product,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {
        System.out.println("Received product for update: " + product);
        System.out.println("Received image for update: " + (imageFile != null ? imageFile.getOriginalFilename() : "No image"));
        Product updatedProduct = productService.updateProduct(id, product, imageFile);
        if (updatedProduct != null) {
            return ResponseEntity.ok(updatedProduct);
        }
        return ResponseEntity.notFound().build();
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
}