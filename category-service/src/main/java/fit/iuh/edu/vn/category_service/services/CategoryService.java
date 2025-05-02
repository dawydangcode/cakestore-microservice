package fit.iuh.edu.vn.category_service.services;

import fit.iuh.edu.vn.category_service.exception.ResourceNotFoundException;
import fit.iuh.edu.vn.category_service.models.Category;
import fit.iuh.edu.vn.category_service.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8080/products/update-category/";

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    public Category createCategory(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists");
        }
        category.setCreatedAt(LocalDate.now());
        category.setUpdatedAt(LocalDate.now());
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = getCategoryById(id);
        if (!category.getName().equals(categoryDetails.getName()) && categoryRepository.existsByName(categoryDetails.getName())) {
            throw new IllegalArgumentException("Category with name '" + categoryDetails.getName() + "' already exists");
        }
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setImage(categoryDetails.getImage()); // Cập nhật hình ảnh
        category.setUpdatedAt(LocalDate.now());
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);

        // Gọi Product Service để cập nhật categoryId của các sản phẩm liên quan
        String productServiceUrl = PRODUCT_SERVICE_URL + id;
        try {
            restTemplate.postForObject(productServiceUrl, null, Void.class);
        } catch (Exception e) {
            // Log lỗi nhưng không làm gián đoạn quá trình xóa danh mục
            System.err.println("Failed to update products for category " + id + ": " + e.getMessage());
        }

        categoryRepository.delete(category);
    }
}