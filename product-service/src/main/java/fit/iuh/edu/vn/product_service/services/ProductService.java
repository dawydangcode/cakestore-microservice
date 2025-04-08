package fit.iuh.edu.vn.product_service.services;

import com.netflix.discovery.converters.Auto;
import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product addProduct(Product product) {
        // Thiết lập ngày tạo và cập nhật
        product.setCreateAt(LocalDate.now());
        product.setUpdateAt(LocalDate.now());

        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isPresent()) {
            Product p = existingProduct.get();
            p.setCategoryId(product.getCategoryId());
            p.setName(product.getName());
            p.setDescription(product.getDescription());
            p.setPrice(product.getPrice());
            p.setStock(product.getStock());
            p.setUpdateAt(LocalDate.now());
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
}
