package fit.iuh.edu.vn.cart_service.services;

import fit.iuh.edu.vn.cart_service.models.Cart;
import fit.iuh.edu.vn.cart_service.models.CartItem;
import fit.iuh.edu.vn.cart_service.repositories.CartItemRepository;
import fit.iuh.edu.vn.cart_service.repositories.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
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
        // Kiểm tra trạng thái và tồn kho sản phẩm trước khi thêm
        try {
            String productUrl = PRODUCT_SERVICE_URL + productId;
            Map<String, Object> product = restTemplate.getForObject(productUrl, Map.class);
            if (product != null) {
                String status = (String) product.get("status");
                Integer stock = (Integer) product.get("stock");
                if (!"ACTIVE".equals(status)) {
                    throw new IllegalArgumentException("Sản phẩm đã bị ẩn và không thể thêm vào giỏ hàng.");
                }
                if (stock == null || stock <= 0) {
                    throw new IllegalArgumentException("Sản phẩm đã hết hàng.");
                }
                if (stock < quantity) {
                    throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho.");
                }
            } else {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi kiểm tra sản phẩm: " + e.getMessage());
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
            List<CartItem> validItems = new ArrayList<>();
            List<Long> itemsToRemove = new ArrayList<>();

            // Kiểm tra trạng thái sản phẩm
            for (CartItem item : cartItems) {
                try {
                    String productUrl = PRODUCT_SERVICE_URL + item.getProductId();
                    Map<String, Object> product = restTemplate.getForObject(productUrl, Map.class);
                    if (product != null) {
                        String status = (String) product.get("status");
                        if ("ACTIVE".equals(status)) {
                            item.setName((String) product.get("name"));
                            item.setImage((String) product.get("image"));
                            validItems.add(item);
                        } else {
                            itemsToRemove.add(item.getProductId());
                        }
                    } else {
                        itemsToRemove.add(item.getProductId());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch product info for productId " + item.getProductId() + ": " + e.getMessage());
                    itemsToRemove.add(item.getProductId());
                }
            }

            // Xóa các sản phẩm không hợp lệ
            for (Long productId : itemsToRemove) {
                CartItem item = cartItemRepository.findByCartIdAndProductId(optionalCart.get().getId(), productId);
                if (item != null) {
                    cartItemRepository.delete(item);
                }
            }

            return validItems;
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
            // Kiểm tra tồn kho trước khi tăng số lượng
            try {
                String productUrl = PRODUCT_SERVICE_URL + productId;
                Map<String, Object> product = restTemplate.getForObject(productUrl, Map.class);
                if (product != null) {
                    Integer stock = (Integer) product.get("stock");
                    if (stock == null || stock < cartItem.getQuantity() + amount) {
                        throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho.");
                    }
                } else {
                    throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Lỗi khi kiểm tra sản phẩm: " + e.getMessage());
            }
            cartItem.setQuantity(cartItem.getQuantity() + amount);
            cartItemRepository.save(cartItem);
        } else {
            throw new IllegalArgumentException("Item not found");
        }
    }
}