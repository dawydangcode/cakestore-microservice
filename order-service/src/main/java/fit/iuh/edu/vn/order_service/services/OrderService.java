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
    private EmailService emailService;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    public Order createOrder(String userName, OrderRequest orderRequest, String token) {
        long startTotal = System.currentTimeMillis();
        logger.info("Starting order creation for user: {}, paymentMethod: {}, status: {}",
                userName, orderRequest.getPaymentMethod(), "Chờ thanh toán");

        // Step 1: Fetch Cart Items
        long startFetchCart = System.currentTimeMillis();
        List<CartItemDTO> cartItems = fetchCartItems(userName, token);
        long durationFetchCart = System.currentTimeMillis() - startFetchCart;
        logger.debug("Fetched {} cart items in {} ms", cartItems.size(), durationFetchCart);

        if (cartItems.isEmpty()) {
            logger.error("Cart is empty, cannot create order");
            throw new IllegalStateException("Giỏ hàng trống, không thể tạo đơn hàng");
        }

        // Step 2: Check Stock
        long startCheckStock = System.currentTimeMillis();
        for (CartItemDTO item : cartItems) {
            long startProductFetch = System.currentTimeMillis();
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
            } finally {
                long durationProductFetch = System.currentTimeMillis() - startProductFetch;
                logger.debug("Checked stock for product {} in {} ms", item.getProductId(), durationProductFetch);
            }
        }
        long durationCheckStock = System.currentTimeMillis() - startCheckStock;
        logger.debug("Completed stock check for {} items in {} ms", cartItems.size(), durationCheckStock);

        // Step 3: Calculate Total and Create Order
        long startCreateOrder = System.currentTimeMillis();
        float totalPrice = cartItems.stream()
                .map(item -> item.getPrice() * item.getQuantity())
                .reduce(0f, Float::sum);

        Order order = new Order();
        order.setUserName(userName);
        order.setFullName(orderRequest.getFullName());
        order.setPhoneNumber(orderRequest.getPhoneNumber());
        order.setEmail(orderRequest.getEmail()); // Set email từ OrderRequest
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
        long durationCreateOrder = System.currentTimeMillis() - startCreateOrder;
        logger.debug("Created and saved order in {} ms", durationCreateOrder);

        // Step 4: Process Payment
        long startPayment = System.currentTimeMillis();
        Boolean paymentSuccess = processPayment(userName, savedOrder, token);
        long durationPayment = System.currentTimeMillis() - startPayment;
        logger.debug("Processed payment in {} ms, success: {}", durationPayment, paymentSuccess);

        if (paymentSuccess != null && paymentSuccess) {
            logger.info("Payment successful, updating order status and processing stock for user: {}", userName);
            savedOrder.setStatus("Đã thanh toán");
            orderRepository.save(savedOrder);

            // Step 5: Update Stock
            long startUpdateStock = System.currentTimeMillis();
            for (OrderItem item : savedOrder.getOrderItems()) {
                long startStockUpdate = System.currentTimeMillis();
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
                } finally {
                    long durationStockUpdate = System.currentTimeMillis() - startStockUpdate;
                    logger.debug("Updated stock for product {} in {} ms", item.getProductId(), durationStockUpdate);
                }
            }
            long durationUpdateStock = System.currentTimeMillis() - startUpdateStock;
            logger.debug("Completed stock update for {} items in {} ms", savedOrder.getOrderItems().size(), durationUpdateStock);

            // Step 6: Clear Cart
            long startClearCart = System.currentTimeMillis();
            clearCart(userName, token);
            long durationClearCart = System.currentTimeMillis() - startClearCart;
            logger.debug("Cleared cart in {} ms", durationClearCart);

            // Trả về order ngay sau khi xóa giỏ hàng
            long durationUntilClearCart = System.currentTimeMillis() - startTotal;
            logger.info("Order creation completed up to cart clearing for orderId: {} in {} ms", savedOrder.getId(), durationUntilClearCart);

            // Step 7: Send Confirmation Email (Async)
            try {
                emailService.sendOrderConfirmationEmail(savedOrder, userName);
                logger.debug("Triggered async email sending for orderId: {}", savedOrder.getId());
            } catch (Exception e) {
                logger.error("Failed to trigger async email for orderId: {}. Error: {}", savedOrder.getId(), e.getMessage());
            }

            return savedOrder;
        } else {
            logger.error("Payment failed for orderId: {}", savedOrder.getId());
            savedOrder.setStatus("Thanh toán thất bại");
            orderRepository.save(savedOrder);
            throw new RuntimeException("Thanh toán thất bại");
        }
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
        long startPayment = System.currentTimeMillis();
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
            long durationPayment = System.currentTimeMillis() - startPayment;
            logger.debug("Payment service call took {} ms", durationPayment);
            return response != null && "Thành công".equals(response.getStatus());
        } catch (RestClientException e) {
            logger.error("Payment processing failed: {}", e.getMessage());
            long durationPayment = System.currentTimeMillis() - startPayment;
            logger.debug("Payment service call failed after {} ms", durationPayment);
            return false;
        }
    }

    private List<CartItemDTO> fetchCartItems(String userName, String token) {
        long startFetch = System.currentTimeMillis();
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
            List<CartItemDTO> cartItems = List.of(response.getBody());
            long durationFetch = System.currentTimeMillis() - startFetch;
            logger.debug("Cart service call took {} ms, returned {} items", durationFetch, cartItems.size());
            return cartItems;
        } catch (RestClientException e) {
            logger.error("Failed to fetch cart items: {}", e.getMessage());
            long durationFetch = System.currentTimeMillis() - startFetch;
            logger.debug("Cart service call failed after {} ms", durationFetch);
            throw new RuntimeException("Không thể lấy giỏ hàng: " + e.getMessage());
        }
    }

    private void clearCart(String userName, String token) {
        long startClear = System.currentTimeMillis();
        List<CartItemDTO> cartItems = fetchCartItems(userName, token);
        for (CartItemDTO item : cartItems) {
            long startDelete = System.currentTimeMillis();
            logger.info("Deleting cart item: cartId={}, productId={}", item.getCartId(), item.getProductId());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            try {
                restTemplate.exchange(
                        cartServiceUrl + "/cart/" + item.getCartId() + "/item/" + item.getProductId(),
                        HttpMethod.DELETE,
                        entity,
                        Void.class
                );
            } catch (RestClientException e) {
                logger.error("Failed to delete cart item cartId={}, productId={}: {}",
                        item.getCartId(), item.getProductId(), e.getMessage());
            } finally {
                long durationDelete = System.currentTimeMillis() - startDelete;
                logger.debug("Deleted cart item cartId={}, productId={} in {} ms",
                        item.getCartId(), item.getProductId(), durationDelete);
            }
        }
        long durationClear = System.currentTimeMillis() - startClear;
        logger.debug("Completed cart clearing for {} items in {} ms", cartItems.size(), durationClear);
    }
}