package fit.iuh.edu.vn.order_service.services;

import fit.iuh.edu.vn.order_service.models.Order;
import fit.iuh.edu.vn.order_service.models.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Async("asyncExecutor")
    public void sendOrderConfirmationEmail(Order order, String userName) {
        long startEmail = System.currentTimeMillis();
        String toEmail = order.getEmail() != null ? order.getEmail() : userName + "@example.com";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Xác nhận đơn hàng #" + order.getId() + " - IUH Bake");

        StringBuilder body = new StringBuilder();
        body.append("IUH Bake - Tiệm Bánh & Cafe\n");
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
        body.append("40,000₫\n");
        body.append("Tổng cộng\n");
        body.append((order.getTotalPrice() + 40000)).append("đ\n");

        message.setText(body.toString());

        try {
            logger.info("Sending confirmation email to: {}", toEmail);
            mailSender.send(message);
            logger.info("Email sent successfully for orderId: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to send email for orderId: {}. Error: {}", order.getId(), e.getMessage());
        } finally {
            long durationEmail = System.currentTimeMillis() - startEmail;
            logger.debug("Email sending took {} ms", durationEmail);
        }
    }
}