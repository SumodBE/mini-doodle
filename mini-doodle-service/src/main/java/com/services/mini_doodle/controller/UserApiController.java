package com.services.mini_doodle.controller;

import com.services.mini_doodle.api.UserApi;
import com.services.mini_doodle.model.RegisterUserRequest;
import com.services.mini_doodle.model.RegisterUserResponse;
import com.services.mini_doodle.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class UserApiController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<RegisterUserResponse> registerUser(RegisterUserRequest registerUserRequest) {
        return ResponseEntity.ok(userService.registerUser(registerUserRequest));
    }

}
