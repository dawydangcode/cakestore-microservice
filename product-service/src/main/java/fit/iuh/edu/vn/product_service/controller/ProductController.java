package fit.iuh.edu.vn.product_service.controller;

import fit.iuh.edu.vn.product_service.models.Product;
import fit.iuh.edu.vn.product_service.repositories.ProductRepository;
import fit.iuh.edu.vn.product_service.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;


    public ProductController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/list")
    public ResponseEntity<List<Product>> getProductList() {
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(products);
    }
}