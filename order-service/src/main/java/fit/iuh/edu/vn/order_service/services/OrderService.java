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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${spring.mail.from}")
    private String fromEmail;

    public Order createOrder(String userName, OrderRequest orderRequest, String token) {
        logger.info("Creating order for user: {}, paymentMethod: {}, status: {}",
                userName, orderRequest.getPaymentMethod(), "Chờ thanh toán");

        List<CartItemDTO> cartItems = fetchCartItems(userName, token);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể tạo đơn hàng");
        }

        // Kiểm tra stock trước khi tạo đơn hàng
        for (CartItemDTO item : cartItems) {
            try {
                String productUrl = productServiceUrl + "/" + item.getProductId();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + token);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> productResponse = restTemplate.exchange(
                        productUrl, HttpMethod.GET, entity, Map.class);
                Map<String, Object> product = productResponse.getBody();
                if (product == null || !product.containsKey("stock")) {
                    logger.error("Product {} not found or missing stock", item.getProductId());
                    throw new IllegalStateException("Sản phẩm " + item.getProductId() + " không tồn tại");
                }
                Integer stock = (Integer) product.get("stock");
                if (stock == null || stock < item.getQuantity()) {
                    logger.error("Insufficient stock for product {}: required {}, available {}",
                            item.getProductId(), item.getQuantity(), stock);
                    throw new IllegalStateException("Sản phẩm " + product.get("name") + " không đủ tồn kho");
                }
            } catch (RestClientException e) {
                logger.error("Failed to fetch product {}: {}", item.getProductId(), e.getMessage());
                throw new RuntimeException("Không thể kiểm tra tồn kho sản phẩm: " + e.getMessage());
            }
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
            logger.info("Payment successful, updating order status and processing stock for user: {}", userName);
            savedOrder.setStatus("Đã thanh toán");
            orderRepository.save(savedOrder);

            // Cập nhật stock sản phẩm
            for (OrderItem item : savedOrder.getOrderItems()) {
                try {
                    String productUrl = productServiceUrl + "/update-stock/" + item.getProductId();
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + token);
                    headers.set("Content-Type", "application/json");

                    // Lấy thông tin sản phẩm hiện tại
                    ResponseEntity<Map> productResponse = restTemplate.exchange(
                            productServiceUrl + "/" + item.getProductId(),
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            Map.class);
                    Map<String, Object> product = productResponse.getBody();
                    if (product == null) {
                        logger.error("Product {} not found during stock update", item.getProductId());
                        throw new RuntimeException("Sản phẩm " + item.getProductId() + " không tồn tại");
                    }

                    // Tạo payload với stock mới
                    int newStock = ((Integer) product.get("stock")) - item.getQuantity();
                    Map<String, Integer> stockUpdate = Map.of("stock", newStock);

                    // Gửi yêu cầu cập nhật stock
                    HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(stockUpdate, headers);
                    ResponseEntity<Map> updateResponse = restTemplate.exchange(
                            productUrl, HttpMethod.PUT, entity, Map.class);
                    logger.info("Updated stock for product {}: new stock = {}", item.getProductId(), newStock);
                } catch (RestClientException e) {
                    logger.error("Failed to update stock for product {}: {}", item.getProductId(), e.getMessage());
                    savedOrder.setStatus("Thanh toán thành công nhưng cập nhật tồn kho thất bại");
                    orderRepository.save(savedOrder);
                    throw new RuntimeException("Cập nhật tồn kho thất bại cho sản phẩm " + item.getProductId());
                }
            }

            clearCart(userName, token);
            sendOrderConfirmationEmail(savedOrder, userName);
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
        List<Order> orders = orderRepository.findByUserName(userName);
        return enrichOrdersWithProductNames(orders);
    }

    public Order getOrderById(Long orderId) {
        logger.info("Fetching order detail for orderId: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        return enrichOrdersWithProductNames(List.of(order)).get(0);
    }

    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        List<Order> orders = orderRepository.findAll();
        return enrichOrdersWithProductNames(orders);
    }

    public Order updateOrderStatus(Long orderId, String status) {
        logger.info("Updating status for orderId: {} to {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        return enrichOrdersWithProductNames(List.of(updatedOrder)).get(0);
    }

    private List<Order> enrichOrdersWithProductNames(List<Order> orders) {
        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                String productUrl = null;
                try {
                    logger.debug("Fetching product details for productId: {}", item.getProductId());
                    productUrl = productServiceUrl + "/" + item.getProductId();
                    logger.debug("Calling Product Service: {}", productUrl);
                    Map<String, Object> product = restTemplate.getForObject(productUrl, Map.class);
                    logger.debug("Product response for productId {}: {}", item.getProductId(), product);
                    if (product != null && product.containsKey("name") && product.containsKey("image")) {
                        item.setProductName((String) product.get("name"));
                        item.setImage((String) product.get("image"));
                        logger.debug("Successfully fetched product: {} for productId: {}", product.get("name"), item.getProductId());
                    } else {
                        logger.warn("Product response is null or missing name/image for productId: {}", item.getProductId());
                        item.setProductName("Không xác định");
                        item.setImage(null);
                    }
                } catch (RestClientException e) {
                    logger.error("Failed to fetch product details for productId: {}. URL: {}. Error: {}",
                            item.getProductId(), productUrl, e.getMessage());
                    item.setProductName("Không xác định");
                    item.setImage(null);
                }
            }
        }
        return orders;
    }

    private Boolean processPayment(String userName, Order order, String token) {
        try {
            PaymentRequest paymentRequest = new PaymentRequest(
                    order.getId(),
                    order.getTotalPrice(),
                    order.getPaymentMethod()
            );
            logger.info("Calling payment-service for orderId: {}", order.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<PaymentRequest> entity = new HttpEntity<>(paymentRequest, headers);

            PaymentResponse response = restTemplate.postForObject(
                    paymentServiceUrl + "/process",
                    entity,
                    PaymentResponse.class
            );
            logger.info("Payment response: {}", response);
            return response != null && "Thành công".equals(response.getStatus());
        } catch (RestClientException e) {
            logger.error("Payment processing failed: {}", e.getMessage());
            return false;
        }
    }

    private List<CartItemDTO> fetchCartItems(String userName, String token) {
        try {
            logger.info("Fetching cart items for user: {}", userName);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<CartItemDTO[]> response = restTemplate.exchange(
                    cartServiceUrl + "/getCartItems",
                    HttpMethod.GET,
                    entity,
                    CartItemDTO[].class
            );
            return List.of(response.getBody());
        } catch (RestClientException e) {
            logger.error("Failed to fetch cart items: {}", e.getMessage());
            throw new RuntimeException("Không thể lấy giỏ hàng: " + e.getMessage());
        }
    }

    private void clearCart(String userName, String token) {
        List<CartItemDTO> cartItems = fetchCartItems(userName, token);
        for (CartItemDTO item : cartItems) {
            logger.info("Deleting cart item: cartId={}, productId={}", item.getCartId(), item.getProductId());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            restTemplate.exchange(
                    cartServiceUrl + "/cart/" + item.getCartId() + "/item/" + item.getProductId(),
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
        }
    }

    private void sendOrderConfirmationEmail(Order order, String userName) {
        String toEmail = userName + "@example.com";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Xác nhận đơn hàng #" + order.getId() + " - BBang House");

        StringBuilder body = new StringBuilder();
        body.append("BBang House - Tiệm Bánh & Cafe\n");
        body.append("Đơn hàng #").append(order.getId()).append("\n");
        body.append("Cám ơn bạn đã mua hàng!\n");
        body.append("Xin chào ").append(userName).append(", Chúng tôi đã nhận được đặt hàng của bạn và đã sẵn sàng để vận chuyển. ");
        body.append("Chúng tôi sẽ thông báo cho bạn khi đơn hàng được gửi đi.\n\n");
        body.append("[Xem đơn hàng](#) hoặc [Đến cửa hàng của chúng tôi](#)\n\n");
        body.append("Thông tin đơn hàng\n");
        for (OrderItem item : order.getOrderItems()) {
            body.append("  ").append(item.getProductName()).append(" × ").append(item.getQuantity()).append("\n");
            body.append("  ").append(item.getPrice() * item.getQuantity()).append("₫\n");
        }
        body.append("Tổng giá trị sản phẩm\n");
        body.append(order.getTotalPrice()).append("₫\n");
        body.append("Khuyến mãi\n");
        body.append("0₫\n");
        body.append("Phí vận chuyển\n");
        body.append("80,000₫\n");
        body.append("Tổng cộng\n");
        body.append((order.getTotalPrice() + 80000)).append(" VND\n");

        message.setText(body.toString());

        try {
            logger.info("Sending confirmation email to: {}", toEmail);
            mailSender.send(message);
            logger.info("Email sent successfully for orderId: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to send email for orderId: {}. Error: {}", order.getId(), e.getMessage());
        }
    }
}
