package fit.iuh.edu.vn.order_service.repositories;

import fit.iuh.edu.vn.order_service.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}