package fit.iuh.edu.vn.payment_service.repositories;

import fit.iuh.edu.vn.payment_service.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}