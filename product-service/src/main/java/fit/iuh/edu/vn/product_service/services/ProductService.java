package fit.iuh.edu.vn.product_service.services;

import com.netflix.discovery.converters.Auto;
import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
}
