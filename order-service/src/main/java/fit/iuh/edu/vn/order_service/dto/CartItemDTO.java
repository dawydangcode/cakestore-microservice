package fit.iuh.edu.vn.order_service.dto;

public class CartItemDTO {
    private Long cartId;
    private Long productId;
    private Integer quantity;
    private Float price;

    // Constructors
    public CartItemDTO() {}

    public CartItemDTO(Long cartId, Long productId, Integer quantity, Float price) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters và Setters
    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Float getPrice() { return price; }
    public void setPrice(Float price) { this.price = price; }
}