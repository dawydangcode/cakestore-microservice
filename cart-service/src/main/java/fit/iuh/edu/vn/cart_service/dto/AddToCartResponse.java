package fit.iuh.edu.vn.cart_service.dto;

import fit.iuh.edu.vn.cart_service.models.CartItem;

import java.util.List;

public class AddToCartResponse {
    private String message;
    private String userName;
    private CartItem addedItem;      // Thông tin sản phẩm vừa thêm
    private List<CartItem> cartItems; // Danh sách toàn bộ giỏ hàng

    // Constructor
    public AddToCartResponse(String message, String userName, CartItem addedItem, List<CartItem> cartItems) {
        this.message = message;
        this.userName = userName;
        this.addedItem = addedItem;
        this.cartItems = cartItems;
    }

    // Getters và Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public CartItem getAddedItem() { return addedItem; }
    public void setAddedItem(CartItem addedItem) { this.addedItem = addedItem; }
    public List<CartItem> getCartItems() { return cartItems; }
    public void setCartItems(List<CartItem> cartItems) { this.cartItems = cartItems; }
}