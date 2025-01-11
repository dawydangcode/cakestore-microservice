package fit.iuh.edu.vn.product_service.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}