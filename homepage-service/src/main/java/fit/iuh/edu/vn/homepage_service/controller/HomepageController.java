package fit.iuh.edu.vn.homepage_service.controller;

import fit.iuh.edu.vn.homepage_service.dto.ProductDTO;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class HomepageController {
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    public HomepageController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/")
    public String showHomepage(Model model) {
        ServiceInstance serviceInstance = discoveryClient.getInstances("product-service").get(0);
        String url = serviceInstance.getUri() + "/products/list";
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        List<Map<String, Object>> products = response.getBody();

        // Chuyển đổi Map thành đối tượng ProductDTO
        List<ProductDTO> productDTOs = products.stream().map(productMap -> {
            ProductDTO dto = new ProductDTO();
            dto.setId(((Number) productMap.get("id")).longValue());
            dto.setCategoryId(((Number) productMap.get("categoryId")).longValue());
            dto.setName((String) productMap.get("name"));
            dto.setDescription((String) productMap.get("description"));
            dto.setPrice(((Number) productMap.get("price")).floatValue());
            dto.setStock((Integer) productMap.get("stock"));
            dto.setCreateAt(LocalDate.parse((String) productMap.get("createAt")));
            dto.setUpdateAt(LocalDate.parse((String) productMap.get("updateAt")));
            return dto;
        }).collect(Collectors.toList());


        model.addAttribute("products", productDTOs);
        return "home/homepage";
    }
}