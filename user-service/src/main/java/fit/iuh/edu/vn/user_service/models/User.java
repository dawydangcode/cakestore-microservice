package fit.iuh.edu.vn.user_service.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @jakarta.validation.constraints.Size(max = 50)
    @Column(name = "username", length = 50)
    private String username;

    @jakarta.validation.constraints.Size(max = 50)
    @Column(name = "password", length = 50)
    private String password;

    @jakarta.validation.constraints.Size(max = 255)
    @Column(name = "full_name")
    private String fullName;

    @Lob
    @Column(name = "role")
    private String role;

    @jakarta.validation.constraints.Size(max = 255)
    @Column(name = "email")
    private String email;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "phone")
    private Integer phone;

    @Column(name = "create_at")
    private LocalDate createAt;

    @Column(name = "update_at")
    private LocalDate updateAt;

}