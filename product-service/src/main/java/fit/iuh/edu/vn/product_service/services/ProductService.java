package fit.iuh.edu.vn.product_service.services;

import fit.iuh.edu.vn.product_service.models.Category;
import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private S3Service s3Service; // Thêm S3Service

    private static final String CATEGORY_SERVICE_URL = "http://localhost:8080/categories/";

    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            attachCategoryToProduct(product);
        }
        return products;
    }

    public Product addProduct(Product product, MultipartFile imageFile) throws IOException {
        // Thiết lập ngày tạo và cập nhật
        product.setCreateAt(LocalDate.now());
        product.setUpdateAt(LocalDate.now());

        // Upload hình ảnh lên S3 nếu có
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
            p.setUpdateAt(LocalDate.now());

            // Upload hình ảnh mới lên S3 nếu có
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imageFile);
                p.setImage(imageUrl);
            }

            return productRepository.save(p);
        }
        return null;
    }

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<Product> getProductById(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        productOpt.ifPresent(this::attachCategoryToProduct);
        return productOpt;
    }

    private Product attachCategoryToProduct(Product product) {
        if (product.getCategoryId() != null) {
            try {
                String url = CATEGORY_SERVICE_URL + product.getCategoryId();
                System.out.println("Calling Category Service: " + url);
                Category category = restTemplate.getForObject(url, Category.class);
                product.setCategory(category);
            } catch (RestClientException e) {
                System.err.println("Failed to fetch category for product " + product.getId() + ": " + e.getMessage());
                product.setCategory(null);
            }
        }
        return product; // Trả về product đã được gắn category
    }

    public void updateProductsCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);
        for (Product product : products) {
            product.setCategoryId(null);
            productRepository.save(product);
        }
    }

    public List<Product> searchProducts(String keyword) {
        List<Product> products = productRepository.findByNameContainingIgnoreCase(keyword);
        return products.stream()
                .map(this::attachCategoryToProduct)
                .toList();
    }
}