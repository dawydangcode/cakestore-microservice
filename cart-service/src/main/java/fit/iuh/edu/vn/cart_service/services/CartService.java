package fit.iuh.edu.vn.cart_service.services;

import fit.iuh.edu.vn.cart_service.models.Cart;
import fit.iuh.edu.vn.cart_service.models.CartItem;
import fit.iuh.edu.vn.cart_service.repositories.CartItemRepository;
import fit.iuh.edu.vn.cart_service.repositories.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8082/products/";

    public CartItem addItemToCart(String userName, Long productId, int quantity, float price) {
        if (userName == null) {
            throw new IllegalArgumentException("userName cannot be null");
        }
        Optional<Cart> optionalCart = Optional.ofNullable(cartRepository.findByUserName(userName));
        Cart cart;
        if (optionalCart.isPresent()) {
            cart = optionalCart.get();
        } else {
            cart = new Cart();
            cart.setUserName(userName);
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
        cartItem = cartItemRepository.save(cartItem);
        return cartItem;
    }

    public List<CartItem> getCartItems(String userName) {
        if (userName == null) {
            throw new IllegalArgumentException("userName cannot be null");
        }
        Optional<Cart> optionalCart = Optional.ofNullable(cartRepository.findByUserName(userName));
        if (optionalCart.isPresent()) {
            List<CartItem> cartItems = cartItemRepository.findByCartId(optionalCart.get().getId());
            // Fetch thông tin sản phẩm từ product-service
            for (CartItem item : cartItems) {
                try {
                    String productUrl = PRODUCT_SERVICE_URL + item.getProductId();
                    Map<String, Object> product = restTemplate.getForObject(productUrl, Map.class);
                    if (product != null) {
                        item.setName((String) product.get("name"));
                        item.setImage((String) product.get("image"));
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch product info for productId " + item.getProductId() + ": " + e.getMessage());
                }
            }
            return cartItems;
        }
        return List.of();
    }

    public void removeItem(Long cartId, Long productId) {
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cartId, productId);
        if (cartItem != null) {
            cartItemRepository.delete(cartItem);
        } else {
            throw new IllegalArgumentException("Item with productId " + productId + " not found in cart " + cartId);
        }
    }

    public void decreaseItemQuantity(Long cartId, Long productId, int amount) {
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cartId, productId);
        if (cartItem != null) {
            int newQuantity = cartItem.getQuantity() - amount;
            if (newQuantity <= 0) {
                cartItemRepository.delete(cartItem);
            } else {
                cartItem.setQuantity(newQuantity);
                cartItemRepository.save(cartItem);
            }
        } else {
            throw new IllegalArgumentException("Item not found");
        }
    }

    public void increaseItemQuantity(Long cartId, Long productId, int amount) {
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cartId, productId);
        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + amount);
            cartItemRepository.save(cartItem);
        } else {
            throw new IllegalArgumentException("Item not found");
        }
    }
}