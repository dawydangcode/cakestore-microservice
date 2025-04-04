package fit.iuh.edu.vn.cart_service.controller;

import fit.iuh.edu.vn.cart_service.dto.AddToCartResponse;
import fit.iuh.edu.vn.cart_service.models.CartItem;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import fit.iuh.edu.vn.cart_service.services.CartService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/carts")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @PostMapping("/addItemToCart")
    public ResponseEntity<AddToCartResponse> addToCart(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        logger.info("Received POST /carts/addItemToCart with body: " + requestBody);
        String userName = (String) request.getAttribute("userName");
        logger.info("UserName from request: " + userName);
        if (userName == null) {
            logger.warn("Unauthorized: Invalid token");
            return ResponseEntity.status(401).body(new AddToCartResponse("Unauthorized: Invalid token", null, null, null));
        }
        Long productId = ((Number) requestBody.get("productId")).longValue();
        int quantity = ((Number) requestBody.get("quantity")).intValue();
        float price = ((Number) requestBody.get("price")).floatValue();

        logger.info("Adding item to cart for userName: " + userName + ", productId: " + productId);
        CartItem addedItem = cartService.addItemToCart(userName, productId, quantity, price); // Lấy CartItem vừa thêm
        List<CartItem> cartItems = cartService.getCartItems(userName); // Lấy toàn bộ giỏ hàng

        AddToCartResponse response = new AddToCartResponse(
                "Product added to cart successfully", userName, addedItem, cartItems
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getCartItems")
    public ResponseEntity<List<CartItem>> getCartItems(HttpServletRequest request) {
        logger.info("Received GET /carts/getCartItems");
        String userName = (String) request.getAttribute("userName");
        logger.info("UserName from request: " + userName);
        if (userName == null) {
            logger.warn("Unauthorized: Invalid token");
            return ResponseEntity.status(401).body(null);
        }
        List<CartItem> cartItems = cartService.getCartItems(userName);
        return ResponseEntity.ok(cartItems);
    }
    @DeleteMapping("/cart/{cartId}/item/{productId}")
    public ResponseEntity<String> removeItemFromCart(@PathVariable Long cartId, @PathVariable Long productId) {
        try {
            cartService.removeItem(cartId, productId);
            return ResponseEntity.ok("Item removed from cart successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}