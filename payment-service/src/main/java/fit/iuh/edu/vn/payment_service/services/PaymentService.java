package fit.iuh.edu.vn.payment_service.services;

import fit.iuh.edu.vn.payment_service.dto.PaymentRequest;
import fit.iuh.edu.vn.payment_service.models.Payment;
import fit.iuh.edu.vn.payment_service.repositories.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    public Payment processPayment(String userName, PaymentRequest paymentRequest) {
        logger.info("Processing payment for user: {}, orderId: {}, paymentMethod: {}",
                userName, paymentRequest.getOrderId(), paymentRequest.getPaymentMethod());

        // Giả lập xử lý thanh toán
        boolean paymentSuccess = processPaymentLogic(paymentRequest);

        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setUserName(userName);
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setStatus(paymentSuccess ? "Thành công" : "Thất bại");
        payment.setCreatedAt(LocalDateTime.now());

        logger.info("Saving payment: {}", payment);
        return paymentRepository.save(payment);
    }

    private boolean processPaymentLogic(PaymentRequest paymentRequest) {
        // Hiện tại hỗ trợ COD, giả lập cho BANK
        if ("COD".equals(paymentRequest.getPaymentMethod())) {
            logger.info("COD payment processed successfully for orderId: {}", paymentRequest.getOrderId());
            return true;
        } else if ("BANK".equals(paymentRequest.getPaymentMethod())) {
            // Giả lập thanh toán ngân hàng (tích hợp sau)
            logger.info("BANK payment simulation for orderId: {}", paymentRequest.getOrderId());
            return true; // Thay bằng logic thật (Stripe, VNPay,...)
        } else {
            logger.error("Invalid payment method: {}", paymentRequest.getPaymentMethod());
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        }
    }
}