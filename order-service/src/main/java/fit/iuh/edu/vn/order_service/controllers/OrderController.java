package fit.iuh.edu.vn.order_service.controllers;

import fit.iuh.edu.vn.order_service.dto.OrderRequest;
import fit.iuh.edu.vn.order_service.dto.OrderResponse;
import fit.iuh.edu.vn.order_service.dto.StatusUpdateRequest;
import fit.iuh.edu.vn.order_service.models.Order;
import fit.iuh.edu.vn.order_service.models.OrderItem;
import fit.iuh.edu.vn.order_service.services.OrderService;
import fit.iuh.edu.vn.order_service.services.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import vn.payos.PayOS;
import vn.payos.type.PaymentLinkData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PayOS payOS;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${payos.checksum-key}")
    private String checksumKey;

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
        List<Order> orders = orderService.getOrdersByUser(userName);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId) {
        Order order = orderService.getOrderById(orderId);
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

    @GetMapping("/payos/{orderCode}")
    public ResponseEntity<ObjectNode> getPayOSOrderStatus(@PathVariable("orderCode") long orderCode) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode response = objectMapper.createObjectNode();
        try {
            PaymentLinkData order = payOS.getPaymentLinkInformation(orderCode);
            response.set("data", objectMapper.valueToTree(order));
            response.put("error", 0);
            response.put("message", "ok");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", -1);
            response.put("message", e.getMessage());
            response.set("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handlePayOSWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            logger.info("Received PayOS webhook: {}", webhookData);

            // Lấy signature và data
            String receivedSignature = (String) webhookData.get("signature");
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            if (data == null || receivedSignature == null) {
                logger.error("Missing data or signature in webhook payload");
                throw new IllegalArgumentException("Missing data or signature in webhook payload");
            }

            // Kiểm tra signature
            String computedSignature = computeSignature(data);
            if (!computedSignature.equals(receivedSignature)) {
                logger.error("Invalid signature: received={}, computed={}", receivedSignature, computedSignature);
                Map<String, Object> response = new HashMap<>();
                response.put("error", -1);
                response.put("message", "Invalid signature");
                return ResponseEntity.status(400).body(response);
            }
            logger.info("Signature verified successfully");

            // Lấy orderCode và code
            String orderCodeStr = Objects.toString(data.get("orderCode"), null);
            if (orderCodeStr == null) {
                logger.error("Missing orderCode in webhook data");
                throw new IllegalArgumentException("Missing orderCode in webhook data");
            }
            Long orderCode;
            try {
                orderCode = Long.parseLong(orderCodeStr);
            } catch (NumberFormatException e) {
                logger.error("Invalid orderCode format: {}", orderCodeStr);
                throw new IllegalArgumentException("Invalid orderCode format: " + orderCodeStr);
            }
            String paymentStatus = Objects.toString(data.get("code"), null);
            if (paymentStatus == null) {
                logger.error("Missing code in webhook data");
                throw new IllegalArgumentException("Missing code in webhook data");
            }
            logger.info("Webhook orderCode: {}, paymentStatus: {}", orderCode, paymentStatus);

            // Xử lý dữ liệu mẫu
            if (orderCode == 123) {
                logger.info("Received test webhook with orderCode=123, skipping order processing");
                Map<String, Object> response = new HashMap<>();
                response.put("error", 0);
                response.put("message", "Test webhook received successfully");
                return ResponseEntity.ok(response);
            }

            // Tìm đơn hàng bằng orderCode
            Order order = orderService.getOrderByOrderCode(orderCode);
            if (order == null) {
                logger.error("Order not found with orderCode: {}", orderCode);
                throw new RuntimeException("Order not found with orderCode: " + orderCode);
            }
            logger.info("Found order with id: {} for orderCode: {}", order.getId(), orderCode);

            if ("00".equals(paymentStatus)) {
                order.setStatus("Đã thanh toán");
                orderService.updateOrderStatus(order.getId(), "Đã thanh toán");
                logger.info("Updated order {} to Đã thanh toán", order.getId());

                // Cập nhật tồn kho
                for (OrderItem item : order.getOrderItems()) {
                    try {
                        String productUrl = productServiceUrl + "/update-stock/" + item.getProductId();
                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Content-Type", "application/json");

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

                        int newStock = ((Integer) product.get("stock")) - item.getQuantity();
                        Map<String, Integer> stockUpdate = Map.of("stock", newStock);

                        HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(stockUpdate, headers);
                        restTemplate.exchange(productUrl, HttpMethod.PUT, entity, Map.class);
                        logger.info("Updated stock for product {}: new stock = {}", item.getProductId(), newStock);
                    } catch (Exception e) {
                        logger.error("Failed to update stock for product {}: {}", item.getProductId(), e.getMessage());
                        order.setStatus("Thanh toán thành công nhưng cập nhật tồn kho thất bại");
                        orderService.updateOrderStatus(order.getId(), "Thanh toán thành công nhưng cập nhật tồn kho thất bại");
                        throw new RuntimeException("Cập nhật tồn kho thất bại: " + e.getMessage());
                    }
                }

                // Xóa giỏ hàng
                try {
                    orderService.clearCart(order.getUserName(), "");
                } catch (Exception e) {
                    logger.error("Failed to clear cart for user {}: {}", order.getUserName(), e.getMessage());
                }

                // Gửi email xác nhận
                try {
                    emailService.sendOrderConfirmationEmail(order, order.getUserName());
                } catch (Exception e) {
                    logger.error("Failed to send confirmation email for order {}: {}", order.getId(), e.getMessage());
                }
            } else {
                order.setStatus("Thanh toán thất bại");
                orderService.updateOrderStatus(order.getId(), "Thanh toán thất bại");
                logger.info("Updated order {} to Thanh toán thất bại", order.getId());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("error", 0);
            response.put("message", "Webhook processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to process PayOS webhook: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", -1);
            response.put("message", "Failed to process webhook: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private String computeSignature(Map<String, Object> data) throws Exception {
        List<String> sortedKeys = new ArrayList<>(data.keySet());
        Collections.sort(sortedKeys);
        StringBuilder dataStr = new StringBuilder();
        for (int i = 0; i < sortedKeys.size(); i++) {
            String key = sortedKeys.get(i);
            Object valueObj = data.get(key);
            String value = valueObj != null ? valueObj.toString() : "";
            dataStr.append(key).append("=").append(value);
            if (i < sortedKeys.size() - 1) {
                dataStr.append("&");
            }
        }
        logger.info("Signature data string: {}", dataStr);
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(checksumKey.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(dataStr.toString().getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        logger.info("Computed signature: {}", hexString);
        return hexString.toString();
    }

    @PostMapping("/generate-qr")
    public ResponseEntity<Map<String, String>> generatePaymentLink(@RequestBody Map<String, String> request) {
        try {
            String orderId = request.get("orderId");
            float amount = Float.parseFloat(request.get("amount"));
            String checkoutUrl = orderService.generatePaymentLink(orderId, amount);
            Map<String, String> response = new HashMap<>();
            response.put("checkoutUrl", checkoutUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to generate payment link: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/check-payment/{orderId}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable String orderId) {
        try {
            Order order = orderService.getOrderById(Long.parseLong(orderId));
            long orderCode = Long.parseLong(orderId + (order.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) % 1000));
            PaymentLinkData paymentData = payOS.getPaymentLinkInformation(orderCode);
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", paymentData.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to check payment status for orderId {}: {}", orderId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleInvalidWebhook() {
        logger.warn("Received GET request to /orders/webhook, only POST is supported");
        Map<String, Object> response = new HashMap<>();
        response.put("error", -1);
        response.put("message", "Webhook only supports POST method");
        return ResponseEntity.status(405).body(response); // Method Not Allowed
    }
}