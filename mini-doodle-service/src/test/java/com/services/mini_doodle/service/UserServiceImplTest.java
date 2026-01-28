package com.services.mini_doodle.service;


import com.services.mini_doodle.model.RegisterUserRequest;
import com.services.mini_doodle.model.RegisterUserResponse;

import com.services.mini_doodle.model.User;
import com.services.mini_doodle.repo.UserAccessor;
import com.services.mini_doodle.service.impl.UserServiceImpl;
import com.services.mini_doodle.util.DbResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserAccessor userAccessor;

    @InjectMocks
    private UserServiceImpl userService;

    // --- Helpers ---
    private final String EMAIL = "new.user@example.com";
    private final RegisterUserRequest REQUEST = RegisterUserRequest.builder()
            .email(EMAIL)
            .firstName("John")
            .lastName("Doe")
            .timezone("UTC")
            .build();

    // ========================================================================
    // 1. HAPPY PATH
    // ========================================================================

    @Test
    void registerUser_Success() {
        // 1. Mock: User does NOT exist yet
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(null));

        // 2. Mock: Save returns the new user
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .build();
        when(userAccessor.registerUser(any(User.class))).thenReturn(DbResult.success(savedUser));

        // 3. Execute
        RegisterUserResponse response = userService.registerUser(REQUEST);

        // 4. Assert
        assertEquals(0, response.getStatusCode());
        assertEquals("Success", response.getMessage());
        assertEquals(EMAIL, response.getEmail());

        // Verify we actually tried to save correct data
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userAccessor).registerUser(captor.capture());
        assertEquals("John", captor.getValue().getFirstName());
    }

    // ========================================================================
    // 2. EDGE CASE: USER ALREADY EXISTS
    // ========================================================================

    @Test
    void registerUser_AlreadyExists_ShouldReturn208() {
        // 1. Mock: User FOUND in DB
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .build();
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(existingUser));

        // 2. Execute
        RegisterUserResponse response = userService.registerUser(REQUEST);

        // 3. Assert
        assertEquals(HttpStatus.ALREADY_REPORTED.value(), response.getStatusCode());
        assertTrue(response.getMessage().contains("User already exists"));
        assertEquals(existingUser.getId(), response.getId());

        // Verify we NEVER tried to save a new user
        verify(userAccessor, never()).registerUser(any());
    }

    // ========================================================================
    // 3. ERROR CASE: DB FAILURE ON CHECK
    // ========================================================================

    @Test
    void registerUser_CheckUserFails_ShouldReturn500() {
        // 1. Mock: DB Error when checking email
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.error("Connection timeout"));

        // 2. Execute
        RegisterUserResponse response = userService.registerUser(REQUEST);

        // 3. Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode());
        assertEquals("API Down. Try again in few mins.", response.getMessage());

        // Verify we stopped early
        verify(userAccessor, never()).registerUser(any());
    }

    // ========================================================================
    // 4. ERROR CASE: DB FAILURE ON SAVE
    // ========================================================================

    @Test
    void registerUser_SaveUserFails_ShouldReturn500() {
        // 1. Mock: User check succeeds (not found)
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(null));

        // 2. Mock: Save FAILS
        when(userAccessor.registerUser(any(User.class))).thenReturn(DbResult.error("Write failed"));

        // 3. Execute
        RegisterUserResponse response = userService.registerUser(REQUEST);

        // 4. Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode());
        assertEquals("API Down. Try again in few mins.", response.getMessage());
    }
}
