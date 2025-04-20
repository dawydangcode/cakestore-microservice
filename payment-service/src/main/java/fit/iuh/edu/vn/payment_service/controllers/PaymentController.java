package fit.iuh.edu.vn.payment_service.controllers;

import fit.iuh.edu.vn.payment_service.dto.PaymentRequest;
import fit.iuh.edu.vn.payment_service.dto.PaymentResponse;
import fit.iuh.edu.vn.payment_service.models.Payment;
import fit.iuh.edu.vn.payment_service.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest paymentRequest,
            HttpServletRequest request) {
        String userName = (String) request.getAttribute("userName");
        String token = request.getHeader("Authorization");
        if (userName == null || token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(null);
        }

        Payment payment = paymentService.processPayment(userName, paymentRequest);
        PaymentResponse response = new PaymentResponse(payment);
        return ResponseEntity.ok(response);
    }
}