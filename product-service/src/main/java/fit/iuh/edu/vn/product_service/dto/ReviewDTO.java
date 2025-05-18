package fit.iuh.edu.vn.product_service.dto;

import java.time.LocalDateTime;

public class ReviewDTO {
    private Long id;
    private String userId;
    private Long productId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    // Constructors
    public ReviewDTO() {
    }

    public ReviewDTO(Long id, String userId, Long productId, int rating, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}