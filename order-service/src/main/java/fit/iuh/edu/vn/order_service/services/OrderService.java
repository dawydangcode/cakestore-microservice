package fit.iuh.edu.vn.order_service.services;

import fit.iuh.edu.vn.order_service.dto.CartItemDTO;
import fit.iuh.edu.vn.order_service.dto.OrderRequest;
import fit.iuh.edu.vn.order_service.dto.PaymentRequest;
import fit.iuh.edu.vn.order_service.dto.PaymentResponse;
import fit.iuh.edu.vn.order_service.models.Order;
import fit.iuh.edu.vn.order_service.models.OrderItem;
import fit.iuh.edu.vn.order_service.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    public Order createOrder(String userName, OrderRequest orderRequest, String token) {
        logger.info("Creating order for user: {}, paymentMethod: {}, status: {}",
                userName, orderRequest.getPaymentMethod(), "Chờ thanh toán");

        List<CartItemDTO> cartItems = fetchCartItems(userName, token);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể tạo đơn hàng");
        }

        float totalPrice = cartItems.stream()
                .map(item -> item.getPrice() * item.getQuantity())
                .reduce(0f, Float::sum);

        Order order = new Order();
        order.setUserName(userName);
        order.setFullName(orderRequest.getFullName());
        order.setPhoneNumber(orderRequest.getPhoneNumber());
        order.setDistrict(orderRequest.getDistrict());
        order.setAddress(orderRequest.getAddress());
        order.setTotalPrice(totalPrice);
        order.setStatus("Chờ thanh toán");
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            return orderItem;
        }).collect(Collectors.toCollection(ArrayList::new));

        order.setOrderItems(orderItems);

        logger.info("Saving order: {}", order);
        Order savedOrder = orderRepository.save(order);

        Boolean paymentSuccess = processPayment(userName, savedOrder, token);
        if (paymentSuccess != null && paymentSuccess) {
            logger.info("Payment successful, updating order status and clearing cart for user: {}", userName);
            savedOrder.setStatus("Đã thanh toán");
            orderRepository.save(savedOrder);
            clearCart(userName, token);
        } else {
            logger.error("Payment failed for orderId: {}", savedOrder.getId());
            savedOrder.setStatus("Thanh toán thất bại");
            orderRepository.save(savedOrder);
            throw new RuntimeException("Thanh toán thất bại");
        }

        return savedOrder;
    }

    public List<Order> getOrdersByUser(String userName) {
        logger.info("Fetching orders for user: {}", userName);
        return orderRepository.findByUserName(userName);
    }

    public Order getOrderById(Long orderId) {
        logger.info("Fetching order detail for orderId: {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
    }

    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    public Order updateOrderStatus(Long orderId, String status) {
        logger.info("Updating status for orderId: {} to {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    private Boolean processPayment(String userName, Order order, String token) {
        try {
            PaymentRequest paymentRequest = new PaymentRequest(
                    order.getId(),
                    order.getTotalPrice(),
                    order.getPaymentMethod()
            );
            logger.info("Calling payment-service for orderId: {}", order.getId());
            return webClientBuilder.build()
                    .post()
                    .uri(paymentServiceUrl + "/process")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(paymentRequest)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .map(response -> "Thành công".equals(response.getStatus()))
                    .onErrorResume(Throwable.class, e -> {
                        logger.error("Payment processing failed: {}", e.getMessage());
                        return Mono.just(false);
                    })
                    .block();
        } catch (Exception e) {
            logger.error("Payment processing failed: {}", e.getMessage());
            return false;
        }
    }

    private List<CartItemDTO> fetchCartItems(String userName, String token) {
        try {
            logger.info("Fetching cart items for user: {}", userName);
            return webClientBuilder.build()
                    .get()
                    .uri(cartServiceUrl + "/getCartItems")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(CartItemDTO.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            logger.error("Failed to fetch cart items: {}", e.getMessage());
            throw new RuntimeException("Không thể lấy giỏ hàng: " + e.getMessage());
        }
    }

    private void clearCart(String userName, String token) {
        List<CartItemDTO> cartItems = fetchCartItems(userName, token);
        for (CartItemDTO item : cartItems) {
            logger.info("Deleting cart item: cartId={}, productId={}", item.getCartId(), item.getProductId());
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