package com.services.mini_doodle.controller;

import com.services.mini_doodle.api.UserApi;
import com.services.mini_doodle.model.RegisterUserRequest;
import com.services.mini_doodle.model.RegisterUserResponse;
import com.services.mini_doodle.service.MiniDoodleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserApiController implements UserApi {

    private final MiniDoodleService miniDoodleService;

    public UserApiController(MiniDoodleService miniDoodleService) {
        this.miniDoodleService = miniDoodleService;
    }

    @Override
    public ResponseEntity<RegisterUserResponse> registerUser(RegisterUserRequest registerUserRequest) {
        return ResponseEntity.ok(miniDoodleService.registerUser(registerUserRequest));
    }

}
