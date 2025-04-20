package fit.iuh.edu.vn.order_service.services;

import fit.iuh.edu.vn.order_service.dto.CartItemDTO;
import fit.iuh.edu.vn.order_service.dto.OrderRequest;
import fit.iuh.edu.vn.order_service.models.Order;
import fit.iuh.edu.vn.order_service.models.OrderItem;
import fit.iuh.edu.vn.order_service.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    public Order createOrder(String userName, OrderRequest orderRequest, String token) {
        // Lấy giỏ hàng từ cart-service
        List<CartItemDTO> cartItems = fetchCartItems(userName, token);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể tạo đơn hàng");
        }

        // Tính tổng giá
        float totalPrice = cartItems.stream()
                .map(item -> item.getPrice() * item.getQuantity())
                .reduce(0f, Float::sum);

        // Tạo Order
        Order order = new Order();
        order.setUserName(userName);
        order.setFullName(orderRequest.getFullName());
        order.setPhoneNumber(orderRequest.getPhoneNumber());
        order.setDistrict(orderRequest.getDistrict());
        order.setAddress(orderRequest.getAddress());
        order.setTotalPrice(totalPrice);
        order.setStatus("Chờ xử lý");
        order.setCreatedAt(LocalDateTime.now());

        // Chuyển CartItemDTO thành OrderItem
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            return orderItem;
        }).toList();

        order.setOrderItems(orderItems);

        // Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);

        // Xóa giỏ hàng sau khi đặt hàng
        clearCart(userName, token);

        return savedOrder;
    }

    private List<CartItemDTO> fetchCartItems(String userName, String token) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(cartServiceUrl + "/getCartItems")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(CartItemDTO.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy giỏ hàng: " + e.getMessage());
        }
    }

    private void clearCart(String userName, String token) {
        List<CartItemDTO> cartItems = fetchCartItems(userName, token);
        for (CartItemDTO item : cartItems) {
            webClientBuilder.build()
                    .delete()
                    .uri(cartServiceUrl + "/cart/" + item.getCartId() + "/item/" + item.getProductId())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        }
    }
}