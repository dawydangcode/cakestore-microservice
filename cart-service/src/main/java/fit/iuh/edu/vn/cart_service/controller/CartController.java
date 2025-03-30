package fit.iuh.edu.vn.cart_service.controller;

import fit.iuh.edu.vn.cart_service.models.CartItem;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
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
    public ResponseEntity<String> addToCart(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        logger.info("Received POST /carts/addItemToCart with body: " + requestBody);
        String userName = (String) request.getAttribute("userName");
        logger.info("UserName from request: " + userName);
        if (userName == null) {
            logger.warn("Unauthorized: Invalid token");
            return ResponseEntity.status(401).body("Unauthorized: Invalid token");
        }
        Long productId = ((Number) requestBody.get("productId")).longValue();
        int quantity = ((Number) requestBody.get("quantity")).intValue();
        float price = ((Number) requestBody.get("price")).floatValue();

        logger.info("Adding item to cart for userName: " + userName + ", productId: " + productId);
        cartService.addItemToCart(userName, productId, quantity, price);
        return ResponseEntity.ok("Product added to cart successfully");
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
}