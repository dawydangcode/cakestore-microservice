package iuh.fit.se.userservice.services;

import iuh.fit.se.userservice.entities.User;

public interface UserService {
    User findByUserName(String userName);
    User findByEmail(String email);
    void saveUser(User user);
    boolean existsByEmail(String email);
    boolean existsByUserName(String userName);
}