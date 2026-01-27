package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.User;
import com.services.mini_doodle.util.DbResult;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccessor extends JpaRepository<User, UUID> {

    DbResult<User> findByEmail(String email);

    default DbResult<User> registerUser(User user){
        try {
            return DbResult.success(save(user));
        } catch (DataAccessException e){
            return DbResult.error(e.getMessage());
        }
    }

}
