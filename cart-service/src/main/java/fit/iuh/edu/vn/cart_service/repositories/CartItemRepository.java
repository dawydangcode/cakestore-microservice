package fit.iuh.edu.vn.cart_service.repositories;

import fit.iuh.edu.vn.cart_service.models.Cart;
import fit.iuh.edu.vn.cart_service.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    //findByCartIdAndProductId
    public CartItem findByCartIdAndProductId(Long cartId, Long productId);
    //findByCartId
    public List<CartItem> findByCartId(Long cartId);
}
