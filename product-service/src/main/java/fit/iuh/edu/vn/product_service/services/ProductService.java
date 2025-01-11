package fit.iuh.edu.vn.product_service.services;

import com.netflix.discovery.converters.Auto;
import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

}
