package fit.iuh.edu.vn.product_service.repositories;

import fit.iuh.edu.vn.product_service.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByUserIdAndProductIdAndOrderId(String userId, Long productId, Long orderId);
    List<Review> findByProductId(Long productId);
}