package fit.iuh.edu.vn.product_service.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    @JsonProperty("id")
    private Long id;

    @Column(name = "category_id")
    @JsonProperty("categoryId")
    private Long categoryId;

    @Size(max = 255)
    @Column(name = "name")
    @JsonProperty("name")
    private String name;

    @Size(max = 255)
    @Column(name = "description")
    @JsonProperty("description")
    private String description;

    @Column(name = "price")
    @JsonProperty("price")
    private Float price;

    @Column(name = "stock")
    @JsonProperty("stock")
    private Integer stock;

    @Column(name = "create_at")
    @JsonProperty("createAt")
    private LocalDate createAt;

    @Column(name = "update_at")
    @JsonProperty("updateAt")
    private LocalDate updateAt;

    @Column(name = "image")
    @JsonProperty("image")
    private String image;

    @Transient
    @JsonProperty("category")
    private Category category; // Sử dụng class Category từ package models


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public @Size(max = 255) String getName() {
        return name;
    }

    public void setName(@Size(max = 255) String name) {
        this.name = name;
    }

    public @Size(max = 255) String getDescription() {
        return description;
    }

    public void setDescription(@Size(max = 255) String description) {
        this.description = description;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDate getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDate createAt) {
        this.createAt = createAt;
    }

    public LocalDate getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDate updateAt) {
        this.updateAt = updateAt;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}