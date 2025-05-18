package fit.iuh.edu.vn.product_service.services;

import fit.iuh.edu.vn.product_service.dto.OrderDTO;
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
        logger.info("Adding review for productId: {} by user: {} for orderId: {}",
                reviewDTO.getProductId(), reviewDTO.getUserId(), reviewDTO.getOrderId());

        // Kiểm tra điểm đánh giá hợp lệ
        if (reviewDTO.getRating() < 1 || reviewDTO.getRating() > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
        }

        // Kiểm tra xem người dùng đã đánh giá cho đơn hàng này chưa
        if (reviewDTO.getOrderId() != null &&
                reviewRepository.existsByUserIdAndProductIdAndOrderId(
                        reviewDTO.getUserId(), reviewDTO.getProductId(), reviewDTO.getOrderId())) {
            logger.warn("User {} already reviewed product {} for order {}",
                    reviewDTO.getUserId(), reviewDTO.getProductId(), reviewDTO.getOrderId());
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này cho đơn hàng này rồi");
        }

        // Kiểm tra xem người dùng đã mua sản phẩm trong đơn hàng chưa
        boolean hasPurchased = checkUserPurchase(reviewDTO.getUserId(), reviewDTO.getProductId(),
                reviewDTO.getOrderId(), token);
        if (!hasPurchased) {
            logger.warn("User {} has not purchased product {} in order {}",
                    reviewDTO.getUserId(), reviewDTO.getProductId(), reviewDTO.getOrderId());
            throw new IllegalStateException("Chỉ người đã mua sản phẩm mới được đánh giá");
        }

        // Lưu đánh giá
        Review review = new Review(
                reviewDTO.getUserId(),
                reviewDTO.getProductId(),
                reviewDTO.getOrderId(), // Lưu orderId
                reviewDTO.getRating(),
                reviewDTO.getComment(),
                LocalDateTime.now()
        );
        Review savedReview = reviewRepository.save(review);
        return new ReviewDTO(
                savedReview.getId(),
                savedReview.getUserId(),
                savedReview.getProductId(),
                savedReview.getOrderId(),
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
                        review.getOrderId(), // Thêm orderId
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt()
                ))
                .toList();
    }

    private boolean checkUserPurchase(String userId, Long productId, Long orderId, String token) {
        try {
            String url = orderServiceUrl + "/by-user";
            logger.info("Kiểm tra lịch sử mua hàng: user={}, productId={}, orderId={}",
                    userId, productId, orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<OrderDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, OrderDTO[].class
            );

            OrderDTO[] orders = response.getBody();
            if (orders == null || orders.length == 0) {
                logger.warn("Không tìm thấy đơn hàng cho user: {}", userId);
                return false;
            }

            return Arrays.stream(orders)
                    .filter(order -> orderId == null || order.getId().equals(orderId))
                    .flatMap(order -> order.getOrderItems().stream())
                    .anyMatch(item -> String.valueOf(item.getProductId()).equals(String.valueOf(productId)));
        } catch (Exception e) {
            logger.error("Lỗi kiểm tra lịch sử mua hàng: user={}, orderId={}, lỗi: {}",
                    userId, orderId, e.getMessage(), e);
            return false;
        }
    }
}