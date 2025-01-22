package fit.iuh.edu.vn.cart_service.controller;

import fit.iuh.edu.vn.cart_service.models.CartItem;
import fit.iuh.edu.vn.cart_service.services.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/addItemToCart")
    public ResponseEntity<String> addToCart(@RequestBody Map<String, Object> requestBody) {
        Long userId = requestBody.containsKey("userId") ? ((Number) requestBody.get("userId")).longValue() : null;
        Long productId = ((Number) requestBody.get("productId")).longValue();
        int quantity = ((Number) requestBody.get("quantity")).intValue();
        float price = ((Number) requestBody.get("price")).floatValue();

        cartService.addItemToCart(userId, productId, quantity, price);
        return ResponseEntity.ok("Product added to cart successfully");
    }

    @GetMapping("/getCartItems")
    public ResponseEntity<List<CartItem>> getCartItems(@RequestParam(required = false) Long userId) {
        List<CartItem> cartItems = cartService.getCartItems(userId);
        return ResponseEntity.ok(cartItems);
    }
}