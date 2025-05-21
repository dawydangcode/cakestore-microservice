package fit.iuh.edu.vn.order_service.repositories;

import fit.iuh.edu.vn.order_service.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserName(String userName);
    List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime createdAt);
    Order findByOrderCode(Long orderCode);
}