package fit.iuh.edu.vn.product_service.repositories;

import fit.iuh.edu.vn.product_service.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE %:keyword%")
    List<Product> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT p FROM Product p WHERE p.isBestSeller = true AND p.status = 'ACTIVE'")
    List<Product> findByIsBestSellerTrue();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isBestSeller = true AND p.status = 'ACTIVE'")
    long countByIsBestSellerTrue();
    List<Product> findByNameContainingIgnoreCaseAndStatus(String name, String status);

    List<Product> findByCategoryIdAndStatus(Long categoryId, String status);

    List<Product> findByStatus(String status);
}
