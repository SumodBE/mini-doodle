package com.services.mini_doodle.service;

import com.services.mini_doodle.model.RegisterUserRequest;
import com.services.mini_doodle.model.RegisterUserResponse;

public interface MiniDoodleService {

    RegisterUserResponse registerUser(RegisterUserRequest request);

}
