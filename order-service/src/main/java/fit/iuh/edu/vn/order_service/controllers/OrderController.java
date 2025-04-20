package fit.iuh.edu.vn.order_service.controller;

import fit.iuh.edu.vn.order_service.dto.OrderRequest;
import fit.iuh.edu.vn.order_service.dto.OrderResponse;
import fit.iuh.edu.vn.order_service.models.Order;
import fit.iuh.edu.vn.order_service.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody OrderRequest orderRequest,
            HttpServletRequest request) {
        String userName = (String) request.getAttribute("userName");
        String token = request.getHeader("Authorization");
        if (userName == null || token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(null);
        }
        token = token.replace("Bearer ", "");

        Order order = orderService.createOrder(userName, orderRequest, token);
        OrderResponse response = new OrderResponse(order);
        return ResponseEntity.ok(response);
    }
}