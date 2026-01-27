package com.services.mini_doodle.service;

import com.services.mini_doodle.model.RegisterUserRequest;
import com.services.mini_doodle.model.RegisterUserResponse;

public interface UserService {

    RegisterUserResponse registerUser(RegisterUserRequest request);

}
