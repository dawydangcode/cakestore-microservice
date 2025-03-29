package fit.iuh.edu.vn.cart_service.controller;

import fit.iuh.edu.vn.cart_service.models.CartItem;
import fit.iuh.edu.vn.cart_service.services.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/addItemToCart")
    public ResponseEntity<String> addToCart(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        String userName = (String) request.getAttribute("userName"); // Lấy từ token
        if (userName == null) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid token");
        }
        Long productId = ((Number) requestBody.get("productId")).longValue();
        int quantity = ((Number) requestBody.get("quantity")).intValue();
        float price = ((Number) requestBody.get("price")).floatValue();

        cartService.addItemToCart(userName, productId, quantity, price);
        return ResponseEntity.ok("Product added to cart successfully");
    }

    @GetMapping("/getCartItems")
    public ResponseEntity<List<CartItem>> getCartItems(HttpServletRequest request) {
        String userName = (String) request.getAttribute("userName");
        if (userName == null) {
            return ResponseEntity.status(401).body(null);
        }
        List<CartItem> cartItems = cartService.getCartItems(userName);
        return ResponseEntity.ok(cartItems);
    }

}