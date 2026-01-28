package com.services.mini_doodle.repo;

// NOTE: Import your DbResult and DataAccessException correctly
import com.services.mini_doodle.model.User;
import com.services.mini_doodle.util.DbResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccessorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAccessor userAccessor;

    // --- Helpers ---
    private final String EMAIL = "test@example.com";
    private final User SAMPLE_USER = new User(); // Assuming standard constructor

    // ========================================================================
    // TEST: findByEmail
    // ========================================================================

    @Test
    void findByEmail_Success_UserFound() {
        // 1. Mock Behavior
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(SAMPLE_USER));

        // 2. Execute
        DbResult<User> result = userAccessor.findByEmail(EMAIL);

        // 3. Assert
        assertTrue(result.isSuccess(), "Should be successful");
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void findByEmail_Success_UserNotFound() {
        // 1. Mock Empty (User doesn't exist yet)
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // 2. Execute
        DbResult<User> result = userAccessor.findByEmail(EMAIL);

        // 3. Assert
        assertTrue(result.isSuccess(), "Operation technically succeeded even if user is null");
        //assertNull(result.getPayload(), "Payload should be null when user not found");
    }

    @Test
    void findByEmail_Failure_DatabaseException() {
        // 1. Mock Exception (using a concrete subclass of DataAccessException)
        when(userRepository.findByEmail(EMAIL))
                .thenThrow(new QueryTimeoutException("DB Timeout"));

        // 2. Execute
        DbResult<User> result = userAccessor.findByEmail(EMAIL);

        // 3. Assert
        assertFalse(result.isSuccess(), "Should be an error result");
        //assertNull(result.getPayload());
        assertTrue(result.getError().contains("Database connection failed"),
                "Error message should contain context");
    }

    // ========================================================================
    // TEST: registerUser
    // ========================================================================

    @Test
    void registerUser_Success() {
        // 1. Mock Behavior
        when(userRepository.save(any(User.class))).thenReturn(SAMPLE_USER);

        // 2. Execute
        DbResult<User> result = userAccessor.registerUser(new User());

        // 3. Assert
        assertTrue(result.isSuccess());
        //assertEquals(SAMPLE_USER, result.getPayload());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_Failure_DatabaseException() {
        // 1. Mock Exception
        when(userRepository.save(any(User.class)))
                .thenThrow(new QueryTimeoutException("Constraint Violation"));

        // 2. Execute
        DbResult<User> result = userAccessor.registerUser(new User());

        // 3. Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Failed to register"));
    }
}