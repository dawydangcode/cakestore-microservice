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
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Optional<Cart> optionalCart = Optional.ofNullable(cartRepository.findByUserId(userId));
        Cart cart;
        if (optionalCart.isPresent()) {
            cart = optionalCart.get();
        } else {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setCreatedAt(LocalDate.now());
            cart.setUpdatedAt(LocalDate.now());
            cart = cartRepository.save(cart);
        }

        Optional<CartItem> optionalCartItem = Optional.ofNullable(cartItemRepository.findByCartIdAndProductId(cart.getId(), productId));
        CartItem cartItem;
        if (optionalCartItem.isPresent()) {
            cartItem = optionalCartItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        } else {
            cartItem = new CartItem();
            cartItem.setCartId(cart.getId());
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setPrice(price);
        }
        cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItems(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Optional<Cart> optionalCart = Optional.ofNullable(cartRepository.findByUserId(userId));
        if (optionalCart.isPresent()) {
            return cartItemRepository.findByCartId(optionalCart.get().getId());
        }
        return List.of();
    }

}