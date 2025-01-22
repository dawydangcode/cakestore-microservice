package fit.iuh.edu.vn.cart_service.services;

import fit.iuh.edu.vn.cart_service.models.Cart;
import fit.iuh.edu.vn.cart_service.models.CartItem;
import fit.iuh.edu.vn.cart_service.repositories.CartItemRepository;
import fit.iuh.edu.vn.cart_service.repositories.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    public void addItemToCart(Long userId, Long productId, int quantity, float price) {
        // Tìm giỏ hàng của người dùng hoặc tạo giỏ hàng mặc định
        Optional<Cart> optionalCart = userId != null ? Optional.ofNullable(cartRepository.findByUserId(userId)) : Optional.empty();
        Cart cart;
        if (optionalCart.isPresent()) {
            cart = optionalCart.get();
        } else {
            // Tạo giỏ hàng mới nếu chưa có
            cart = new Cart();
            if (userId != null) {
                cart.setUserId(userId);
            }
            cart.setCreatedAt(LocalDate.now());
            cart.setUpdatedAt(LocalDate.now());
            cart = cartRepository.save(cart);
        }

        // Tìm sản phẩm trong giỏ hàng
        Optional<CartItem> optionalCartItem = Optional.ofNullable(cartItemRepository.findByCartIdAndProductId(cart.getId(), productId));
        CartItem cartItem;
        if (optionalCartItem.isPresent()) {
            // Cập nhật số lượng nếu sản phẩm đã có trong giỏ hàng
            cartItem = optionalCartItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        } else {
            // Thêm sản phẩm mới vào giỏ hàng
            cartItem = new CartItem();
            cartItem.setCartId(cart.getId());
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setPrice(price);
        }
        cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItems(Long userId) {
        Optional<Cart> optionalCart = userId != null ? Optional.ofNullable(cartRepository.findByUserId(userId)) : Optional.empty();
        if (optionalCart.isPresent()) {
            return cartItemRepository.findByCartId(optionalCart.get().getId());
        }
        return List.of();
    }
}