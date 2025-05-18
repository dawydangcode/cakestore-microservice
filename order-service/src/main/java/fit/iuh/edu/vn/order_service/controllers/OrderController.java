package fit.iuh.edu.vn.order_service.controllers;

import fit.iuh.edu.vn.order_service.dto.OrderRequest;
import fit.iuh.edu.vn.order_service.dto.OrderResponse;
import fit.iuh.edu.vn.order_service.dto.StatusUpdateRequest;
import fit.iuh.edu.vn.order_service.models.Order;
import fit.iuh.edu.vn.order_service.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/user/{userName}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable String userName) {
        List<Order> orders = orderService.getOrdersByUser(userName); // Giả định phương thức này
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId) {
        Order order = orderService.getOrderById(orderId); // Giả định phương thức này
        return ResponseEntity.ok(order);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long orderId, @RequestBody StatusUpdateRequest statusUpdate) {
        Order updatedOrder = orderService.updateOrderStatus(orderId, statusUpdate.getStatus());
        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/by-user")
    public ResponseEntity<List<Order>> getOrdersByUser(HttpServletRequest request) {
        String userName = (String) request.getAttribute("userName");
        if (userName == null) {
            return ResponseEntity.status(401).body(null);
        }
        List<Order> orders = orderService.getOrdersByUser(userName);
        return ResponseEntity.ok(orders);
    }
}