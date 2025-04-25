package fit.iuh.edu.vn.product_service.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Category {

    @JsonProperty("categoryId")
    private Long categoryId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("createdAt")
    private LocalDate createdAt;

    @JsonProperty("updatedAt")
    private LocalDate updatedAt;
}