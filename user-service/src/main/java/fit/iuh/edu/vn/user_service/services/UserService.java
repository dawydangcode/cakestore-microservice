package fit.iuh.edu.vn.user_service.services;

import fit.iuh.edu.vn.user_service.models.User;
import fit.iuh.edu.vn.user_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
