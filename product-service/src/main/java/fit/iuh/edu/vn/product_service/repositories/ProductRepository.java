package fit.iuh.edu.vn.product_service.repositories;

import fit.iuh.edu.vn.product_service.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
