package com.services.mini_doodle.service;


import com.services.mini_doodle.exception.BadRequestException;
import com.services.mini_doodle.exception.InternalServerErrorException;
import com.services.mini_doodle.model.FetchScheduleRequest;
import com.services.mini_doodle.model.FetchScheduleResponse;
import com.services.mini_doodle.model.AvailabilityEntity;
import com.services.mini_doodle.model.MeetingEntity;
import com.services.mini_doodle.model.User;
import com.services.mini_doodle.repo.AvailabilityAccessor;
import com.services.mini_doodle.repo.MeetingsEntityAccessor;
import com.services.mini_doodle.repo.UserAccessor;
import com.services.mini_doodle.service.impl.FetchScheduleServiceImpl;
import com.services.mini_doodle.util.DbResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchScheduleServiceImplTest {

    @Mock
    private UserAccessor userAccessor;
    @Mock
    private AvailabilityAccessor availabilityAccessor;
    @Mock
    private MeetingsEntityAccessor meetingsEntityAccessor;

    @InjectMocks
    private FetchScheduleServiceImpl service;

    // --- Helpers ---
    private final String EMAIL = "user@example.com";
    private final UUID USER_ID = UUID.randomUUID();
    private final User SAMPLE_USER = User.builder().id(USER_ID).email(EMAIL).firstName("Test").build();
    private final FetchScheduleRequest REQUEST = new FetchScheduleRequest(EMAIL, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1));

    // ========================================================================
    // HAPPY PATH
    // ========================================================================

    @Test
    void fetchSchedule_Success() {
        // 1. Mock User Found
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(SAMPLE_USER));

        // 2. Mock Availability Found
        AvailabilityEntity avail = AvailabilityEntity.builder()
                .id(UUID.randomUUID())
                .startTime(OffsetDateTime.now())
                .endTime(OffsetDateTime.now().plusHours(1))
                .userId(USER_ID)
                .build();
        when(availabilityAccessor.findAvailabilityForUser(USER_ID))
                .thenReturn(DbResult.success(List.of(avail)));

        // 3. Mock Meetings Found
        MeetingEntity meeting = MeetingEntity.builder()
                .meetingId(UUID.randomUUID())
                .title("Sync")
                .participantEmails(List.of(EMAIL))
                .startTime(OffsetDateTime.now())
                .endTime(OffsetDateTime.now().plusHours(1))
                .description("Some Description")
                .organizer(SAMPLE_USER)
                .build();
        when(meetingsEntityAccessor.findMeetingsByUserId(USER_ID))
                .thenReturn(DbResult.success(List.of(meeting)));

        // EXECUTE
        FetchScheduleResponse response = service.fetchSchedule(REQUEST);

        // ASSERT
        assertNotNull(response);
        assertEquals(0, response.getStatusCode());
        assertEquals(EMAIL, response.getEmail());

        // Verify Mapping
        assertEquals(1, response.getAvailabilities().size());
        assertEquals(avail.getId(), response.getAvailabilities().get(0).getId());

        assertEquals(1, response.getMeetings().size());
        assertEquals(meeting.getMeetingId(), response.getMeetings().get(0).getId());
    }

    // ========================================================================
    // ERROR SCENARIOS: USER LOOKUP
    // ========================================================================

    @Test
    void fetchSchedule_UserDbFailure_ThrowsInternalServerError() {
        // Mock DB Error
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.error("DB Down"));

        InternalServerErrorException ex = assertThrows(InternalServerErrorException.class,
                () -> service.fetchSchedule(REQUEST));

        assertEquals(4001, ex.getCode());
    }

    @Test
    void fetchSchedule_UserNotFound_ThrowsBadRequest() {
        // Mock Success but Empty (User not found)
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(null));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.fetchSchedule(REQUEST));

        assertEquals(4002, ex.getCode());
    }

    // ========================================================================
    // ERROR SCENARIOS: AVAILABILITY LOOKUP
    // ========================================================================

    @Test
    void fetchSchedule_AvailabilityDbFailure_ThrowsInternalServerError() {
        // Mock User Found
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(SAMPLE_USER));

        // Mock Availability Error
        when(availabilityAccessor.findAvailabilityForUser(USER_ID))
                .thenReturn(DbResult.error("DB Error"));

        InternalServerErrorException ex = assertThrows(InternalServerErrorException.class,
                () -> service.fetchSchedule(REQUEST));

        assertEquals(4001, ex.getCode());
    }

    @Test
    void fetchSchedule_AvailabilityNotFound_ThrowsInternalServerError() {
        // NOTE: Your current logic treats "No Availability Found" (null/empty) as a 500 error.

        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(SAMPLE_USER));

        // Mock Empty Result (DbResult success, but payload is null/empty optional)
        when(availabilityAccessor.findAvailabilityForUser(USER_ID))
                .thenReturn(DbResult.success(null));

        InternalServerErrorException ex = assertThrows(InternalServerErrorException.class,
                () -> service.fetchSchedule(REQUEST));

        assertEquals(4001, ex.getCode());
    }

    // ========================================================================
    // ERROR SCENARIOS: MEETING LOOKUP
    // ========================================================================

    @Test
    void fetchSchedule_MeetingsDbFailure_ThrowsInternalServerError() {
        when(userAccessor.findByEmail(EMAIL)).thenReturn(DbResult.success(SAMPLE_USER));

        // Mock Availability Success
        when(availabilityAccessor.findAvailabilityForUser(USER_ID))
                .thenReturn(DbResult.success(List.of())); // Returning empty list object, not null

        // Mock Meeting Error
        when(meetingsEntityAccessor.findMeetingsByUserId(USER_ID))
                .thenReturn(DbResult.error("DB Error"));

        InternalServerErrorException ex = assertThrows(InternalServerErrorException.class,
                () -> service.fetchSchedule(REQUEST));

        assertEquals(4001, ex.getCode());
    }
}
