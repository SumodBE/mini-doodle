package com.services.mini_doodle.service.impl;

import com.services.mini_doodle.model.RegisterUserRequest;
import com.services.mini_doodle.model.RegisterUserResponse;
import com.services.mini_doodle.model.User;
import com.services.mini_doodle.repo.UserAccessor;
import com.services.mini_doodle.service.MiniDoodleService;
import com.services.mini_doodle.util.DbResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MiniDoodleServiceImpl implements MiniDoodleService {

    private final UserAccessor userAccessor;

    public MiniDoodleServiceImpl(UserAccessor userAccessor) {
        this.userAccessor = userAccessor;
    }

    @Override
    public RegisterUserResponse registerUser(RegisterUserRequest request) {
        DbResult<User> userExists = userAccessor.findByEmail(request.getEmail());

        if(!userExists.isSuccess()){
            return buildRegisterUserResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "API Down. Try again in few mins.", null, null);
        }

       else if(userExists.isSuccess() && userExists.getValue().isPresent()){
            return buildRegisterUserResponse(HttpStatus.ALREADY_REPORTED.value(), "User already exists with for given email"
            , userExists.getValue().get().getId(), userExists.getValue().get().getEmail());
       }

       User newUser = User.builder()
               .email(request.getEmail())
               .firstName(request.getFirstName())
               .lastName(request.getLastName())
               .timezone(request.getTimezone())
               .build();
       DbResult<User> persistNewUser = userAccessor.registerUser(newUser);

       if(!persistNewUser.isSuccess()){
           return buildRegisterUserResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                   "API Down. Try again in few mins.", null, null);
       }

       return buildRegisterUserResponse(0, "Success", newUser.getId(), newUser.getEmail());
    }

    private static RegisterUserResponse buildRegisterUserResponse(int code, String message, UUID id, String email){
        return RegisterUserResponse.builder()
                .email(email)
                .message(message)
                .id(id)
                .statusCode(code)
                .build();
    }

}
