package fit.iuh.edu.vn.product_service.repositories;

import fit.iuh.edu.vn.product_service.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
}
