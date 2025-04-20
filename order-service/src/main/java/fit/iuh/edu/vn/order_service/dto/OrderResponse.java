package fit.iuh.edu.vn.order_service.dto;

import fit.iuh.edu.vn.order_service.models.Order;
import fit.iuh.edu.vn.order_service.models.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {
    private Long orderId;
    private String userName;
    private String fullName;
    private String phoneNumber;
    private String district;
    private String address;
    private Float totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItem> orderItems;

    public OrderResponse(Order order) {
        this.orderId = order.getId();
        this.userName = order.getUserName();
        this.fullName = order.getFullName();
        this.phoneNumber = order.getPhoneNumber();
        this.district = order.getDistrict();
        this.address = order.getAddress();
        this.totalPrice = order.getTotalPrice();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.orderItems = order.getOrderItems();
    }

    // Getters
    public Long getOrderId() { return orderId; }
    public String getUserName() { return userName; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDistrict() { return district; }
    public String getAddress() { return address; }
    public Float getTotalPrice() { return totalPrice; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderItem> getOrderItems() { return orderItems; }
}