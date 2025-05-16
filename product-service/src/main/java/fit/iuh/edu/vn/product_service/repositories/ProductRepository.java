package fit.iuh.edu.vn.product_service.repositories;

import fit.iuh.edu.vn.product_service.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE %:keyword%")
    List<Product> findByNameContainingIgnoreCase(String keyword);
}
