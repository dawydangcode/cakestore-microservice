package fit.iuh.edu.vn.product_service.services;

import fit.iuh.edu.vn.product_service.dto.OrderDTO;
import fit.iuh.edu.vn.product_service.dto.OrderItemDTO;
import fit.iuh.edu.vn.product_service.dto.ReviewDTO;
import fit.iuh.edu.vn.product_service.models.Review;
import fit.iuh.edu.vn.product_service.repositories.ReviewRepository;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ReviewService {
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    public ReviewDTO addReview(ReviewDTO reviewDTO, String token) {
        logger.info("Adding review for productId: {} by user: {}", reviewDTO.getProductId(), reviewDTO.getUserId());

        // Kiểm tra rating hợp lệ
        if (reviewDTO.getRating() < 1 || reviewDTO.getRating() > 5) {
            logger.warn("Invalid rating: {}", reviewDTO.getRating());
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
        }

        // Kiểm tra người dùng đã đánh giá chưa
        if (reviewRepository.existsByUserIdAndProductId(reviewDTO.getUserId(), reviewDTO.getProductId())) {
            logger.warn("User {} already reviewed product {}", reviewDTO.getUserId(), reviewDTO.getProductId());
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này rồi");
        }

        // Kiểm tra người dùng đã mua sản phẩm
        boolean hasPurchased = checkUserPurchase(reviewDTO.getUserId(), reviewDTO.getProductId(), token);
        if (!hasPurchased) {
            logger.warn("User {} has not purchased product {}", reviewDTO.getUserId(), reviewDTO.getProductId());
            throw new IllegalStateException("Chỉ người đã mua sản phẩm mới được đánh giá");
        }

        // Lưu đánh giá
        Review review = new Review(
                reviewDTO.getUserId(),
                reviewDTO.getProductId(),
                reviewDTO.getRating(),
                reviewDTO.getComment(),
                LocalDateTime.now()
        );
        Review savedReview = reviewRepository.save(review);
        logger.info("Review saved for productId: {} by user: {}", savedReview.getProductId(), savedReview.getUserId());

        return new ReviewDTO(
                savedReview.getId(),
                savedReview.getUserId(),
                savedReview.getProductId(),
                savedReview.getRating(),
                savedReview.getComment(),
                savedReview.getCreatedAt()
        );
    }

    public List<ReviewDTO> getReviewsByProductId(Long productId) {
        logger.info("Fetching reviews for productId: {}", productId);
        List<Review> reviews = reviewRepository.findByProductId(productId);
        return reviews.stream()
                .map(review -> new ReviewDTO(
                        review.getId(),
                        review.getUserId(),
                        review.getProductId(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt()
                ))
                .toList();
    }

    private boolean checkUserPurchase(String userId, Long productId, String token) {
        try {
            String url = orderServiceUrl + "/by-user?userName=" + userId;
            logger.info("Checking purchase history for user: {} at URL: {}", userId, url);

            // Gửi yêu cầu với token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<OrderDTO[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    OrderDTO[].class
            );

            OrderDTO[] orders = response.getBody();
            if (orders == null) {
                logger.warn("No orders found for user: {}", userId);
                return false;
            }

            // Kiểm tra xem có đơn hàng nào chứa productId
            return Arrays.stream(orders)
                    .flatMap(order -> order.getOrderItems().stream())
                    .anyMatch(item -> item.getProductId().equals(productId));
        } catch (Exception e) {
            logger.error("Failed to check purchase history for user: {}. Error: {}", userId, e.getMessage());
            return false;
        }
    }
}