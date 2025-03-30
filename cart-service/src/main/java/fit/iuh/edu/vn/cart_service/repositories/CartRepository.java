package fit.iuh.edu.vn.cart_service.repositories;

import fit.iuh.edu.vn.cart_service.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findByUserName(String userName);
}
