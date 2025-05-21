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

        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setUserName(userName);
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setCreatedAt(LocalDateTime.now());

        if ("COD".equals(paymentRequest.getPaymentMethod())) {
            logger.info("COD payment processed successfully for orderId: {}", paymentRequest.getOrderId());
            payment.setStatus("Thành công");
        } else if ("BANK".equals(paymentRequest.getPaymentMethod())) {
            logger.info("BANK payment initiated for orderId: {}. Awaiting PayOS confirmation.", paymentRequest.getOrderId());
            payment.setStatus("Chờ xác nhận");
        } else {
            logger.error("Invalid payment method: {}", paymentRequest.getPaymentMethod());
            payment.setStatus("Thất bại");
            paymentRepository.save(payment);
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        }

        logger.info("Saving payment: {}", payment);
        return paymentRepository.save(payment);
    }
}