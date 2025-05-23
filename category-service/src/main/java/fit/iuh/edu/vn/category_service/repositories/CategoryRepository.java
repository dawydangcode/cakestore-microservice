package fit.iuh.edu.vn.category_service.repositories;

import fit.iuh.edu.vn.category_service.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
}
