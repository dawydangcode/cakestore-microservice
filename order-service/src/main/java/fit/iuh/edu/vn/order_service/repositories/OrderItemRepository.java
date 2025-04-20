package fit.iuh.edu.vn.order_service.repositories;

import fit.iuh.edu.vn.order_service.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}