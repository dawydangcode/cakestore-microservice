package fit.iuh.edu.vn.product_service.controller;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    public ProductController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/list")
    public ModelAndView getUserList() {
        ServiceInstance serviceInstance = discoveryClient.getInstances("user-service").get(0);
        String userListHtml = restTemplate.getForObject(serviceInstance.getUri() + "/users/list", String.class);
        ModelAndView modelAndView = new ModelAndView("users/list");
        modelAndView.addObject("userListHtml", userListHtml);
        return modelAndView;
    }
}