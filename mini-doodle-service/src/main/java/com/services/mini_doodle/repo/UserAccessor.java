package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.User;
import com.services.mini_doodle.util.DbResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAccessor {

    private final UserRepository userRepository;

    public DbResult<User> findByEmail(String email) {
        try {
            return DbResult.success(userRepository.findByEmail(email).orElse(null));
        } catch (DataAccessException e) {
            return DbResult.error("Database connection failed: " + e.getMessage());
        }
    }

    public DbResult<User> registerUser(User user) {
        try {
            User savedUser = userRepository.save(user);
            return DbResult.success(savedUser);
        } catch (DataAccessException e) {
            return DbResult.error("Failed to register: " + e.getMessage());
        }
    }
}
