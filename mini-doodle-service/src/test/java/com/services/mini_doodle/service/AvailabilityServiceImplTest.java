package com.services.mini_doodle.service;


import com.services.mini_doodle.exception.BadRequestException;
import com.services.mini_doodle.exception.InternalServerErrorException;
import com.services.mini_doodle.model.AvailabilityEntity;
import com.services.mini_doodle.model.AvailabilityRequest;
import com.services.mini_doodle.model.AvailabilityResponse;
import com.services.mini_doodle.model.BaseApiResponse;
import com.services.mini_doodle.model.User;
import com.services.mini_doodle.repo.AvailabilityAccessor;
import com.services.mini_doodle.repo.UserAccessor;
import com.services.mini_doodle.service.impl.AvailabilityServiceImpl;
import com.services.mini_doodle.util.DbResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

    @Mock
    private UserAccessor userAccessor;

    @Mock
    private AvailabilityAccessor availabilityAccessor;

    @InjectMocks
    private AvailabilityServiceImpl service;

    // --- Helpers ---
    private final String EMAIL = "test@example.com";
    private final UUID USER_ID = UUID.randomUUID();
    private final User SAMPLE_USER = User.builder().id(USER_ID).email(EMAIL).build();

    // Test Time: 2026-02-01 10:00 UTC
    private final OffsetDateTime START_TIME = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private final AvailabilityRequest REQUEST = new AvailabilityRequest(EMAIL, START_TIME, 60); // 60 mins duration

    // ========================================================================
    // TEST: addAvailability
    // ========================================================================

    @Test
    void addAvailability_Success() {
        // 1. Mock User Found
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(SAMPLE_USER));

        // 2. Mock Accessor Success
        UUID newSlotId = UUID.randomUUID();
        AvailabilityEntity savedEntity = AvailabilityEntity.builder()
                .id(newSlotId)
                .userId(USER_ID)
                .build();

        when(availabilityAccessor.addAvailability(eq(USER_ID), any(), any()))
                .thenReturn(DbResult.success(savedEntity));

        // 3. Execute
        AvailabilityResponse response = service.addAvailability(REQUEST);

        // 4. Assert
        assertNotNull(response);
        assertEquals(0, response.getStatusCode());
        assertEquals(newSlotId, response.getId());
        assertEquals(USER_ID, response.getUserId());

        // Verify time calculation logic (Start + 60 mins)
        verify(availabilityAccessor).addAvailability(
                eq(USER_ID),
                eq(START_TIME),
                eq(START_TIME.plusMinutes(60))
        );
    }

    @Test
    void addAvailability_UserNotFound_ThrowsBadRequest() {
        // 1. Mock User NOT Found (DbResult success but empty payload)
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(null));

        // 2. Execute & Assert
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.addAvailability(REQUEST));

        assertEquals(2001, ex.getCode());
        verify(availabilityAccessor, never()).addAvailability(any(), any(), any());
    }

    @Test
    void addAvailability_DbFailure_ThrowsInternalServerError() {
        // 1. Mock User Found
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(SAMPLE_USER));

        // 2. Mock Accessor Failure (e.g. Constraint Violation)
        when(availabilityAccessor.addAvailability(any(), any(), any()))
                .thenReturn(DbResult.error("DB Error"));

        // 3. Execute & Assert
        InternalServerErrorException ex = assertThrows(InternalServerErrorException.class,
                () -> service.addAvailability(REQUEST));

        assertEquals(2002, ex.getCode());
    }

    // ========================================================================
    // TEST: removeAvailability
    // ========================================================================

    @Test
    void removeAvailability_Success() {
        // 1. Mock User Found
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(SAMPLE_USER));

        // 2. Mock Accessor Success (Reduction logic worked)
        when(availabilityAccessor.reduceAvailability(eq(USER_ID), any(), any()))
                .thenReturn(DbResult.success("Success"));

        // 3. Execute
        BaseApiResponse response = service.removeAvailability(REQUEST);

        // 4. Assert
        assertEquals(0, response.getStatusCode());
        assertEquals("Success", response.getMessage());

        verify(availabilityAccessor).reduceAvailability(
                eq(USER_ID),
                eq(START_TIME),
                eq(START_TIME.plusMinutes(60))
        );
    }

    @Test
    void removeAvailability_UserNotFound_ThrowsBadRequest() {
        // 1. Mock User Accessor Error (e.g. DB connection failed during lookup)
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.error("DB Connection Fail"));

        // 2. Execute & Assert
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.removeAvailability(REQUEST));

        assertEquals(2001, ex.getCode());
        verify(availabilityAccessor, never()).reduceAvailability(any(), any(), any());
    }

    @Test
    void removeAvailability_AccessorFailure_ThrowsInternalServerError() {
        // 1. Mock User Found
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(SAMPLE_USER));

        // 2. Mock Accessor Failure (e.g. DB error during delete/save)
        when(availabilityAccessor.reduceAvailability(any(), any(), any()))
                .thenReturn(DbResult.error("DB Connection Failed"));

        // 3. Execute & Assert
        InternalServerErrorException ex = assertThrows(InternalServerErrorException.class,
                () -> service.removeAvailability(REQUEST));

        assertEquals(2003, ex.getCode());
    }
}
