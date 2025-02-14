package com.example.authentication.repositories;

import com.example.authentication.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity> findByEmail(@Param("email") String email);
    Boolean existsByEmail(String email);
}
