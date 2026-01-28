package com.services.mini_doodle.service;

import com.services.mini_doodle.exception.BadRequestException;
import com.services.mini_doodle.exception.InternalServerErrorException;
import com.services.mini_doodle.model.*;
import com.services.mini_doodle.repo.AvailabilityAccessor;
import com.services.mini_doodle.repo.MeetingsEntityAccessor;
import com.services.mini_doodle.repo.UserAccessor;
import com.services.mini_doodle.service.impl.MeetingServiceImpl;
import com.services.mini_doodle.util.DbResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingsServiceImplTest {

    @Mock
    private MeetingsEntityAccessor meetingsEntityAccessor;
    @Mock
    private AvailabilityAccessor availabilityAccessor;
    @Mock
    private UserAccessor userAccessor;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    // --- Test Data ---
    private final String ORGANIZER_EMAIL = "boss@example.com";
    private final String PARTICIPANT_EMAIL = "worker@example.com";
    private final UUID ORGANIZER_ID = UUID.randomUUID();
    private final UUID PARTICIPANT_ID = UUID.randomUUID();
    private final OffsetDateTime START = OffsetDateTime.now();
    private final MeetingRequest REQUEST = MeetingRequest.builder()
            .organizerEmail(ORGANIZER_EMAIL)
            .participantEmails(List.of(PARTICIPANT_EMAIL))
            .startTime(START)
            .durationMinutes(60)
            .title("Sync")
            .description("Work")
            .build();

    private final User ORGANIZER = User.builder().id(ORGANIZER_ID).email(ORGANIZER_EMAIL).build();
    private final User PARTICIPANT = User.builder().id(PARTICIPANT_ID).email(PARTICIPANT_EMAIL).build();

    // ========================================================================
    // SCHEDULE MEETING TESTS
    // ========================================================================

    @Test
    void scheduleMeeting_Success() {
        // 1. Mock Users Exist
        when(userAccessor.findByEmail(ORGANIZER_EMAIL)).thenReturn(DbResult.success(ORGANIZER));
        when(userAccessor.findByEmail(PARTICIPANT_EMAIL)).thenReturn(DbResult.success(PARTICIPANT));

        // 2. Mock Availability (Everyone is free)
        when(availabilityAccessor.isUserAvailable(any(), any(), any())).thenReturn(DbResult.success(1L));

        // 3. Mock Conflicts (No conflicts found)
        when(meetingsEntityAccessor.countConflicts(any(), any(), any())).thenReturn(DbResult.success(0L));

        // 4. Mock Saving Meeting
        MeetingEntity savedMeeting = MeetingEntity.builder()
                .meetingId(UUID.randomUUID())
                .participantEmails(List.of(PARTICIPANT_EMAIL, ORGANIZER_EMAIL))
                .build();
        when(meetingsEntityAccessor.saveMeeting(any())).thenReturn(DbResult.success(savedMeeting));

        // 5. Mock Reducing Availability
        when(availabilityAccessor.reduceAvailability(any(), any(), any())).thenReturn(DbResult.success("Success"));

        // EXECUTE
        MeetingResponse response = meetingService.scheduleMeeting(REQUEST);

        // ASSERT
        assertNotNull(response);
        assertEquals(0, response.getStatusCode());
        assertEquals(savedMeeting.getMeetingId(), response.getMeetingId());

        // Verify Availability was reduced for BOTH users
        verify(availabilityAccessor, times(2)).reduceAvailability(any(), eq(START), eq(START.plusMinutes(60)));
    }

    @Test
    void scheduleMeeting_OrganizerNotFound_ThrowsBadRequest() {
        // Mock Organizer Not Found
        when(userAccessor.findByEmail(ORGANIZER_EMAIL)).thenReturn(DbResult.success(null));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> meetingService.scheduleMeeting(REQUEST));
        assertEquals(3001, ex.getCode());
    }

    @Test
    void scheduleMeeting_ParticipantNotFound_ThrowsBadRequest() {
        // Organizer exists
        when(userAccessor.findByEmail(ORGANIZER_EMAIL)).thenReturn(DbResult.success(ORGANIZER));
        // Participant lookup fails/empty
        when(userAccessor.findByEmail(PARTICIPANT_EMAIL)).thenReturn(DbResult.success(null));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> meetingService.scheduleMeeting(REQUEST));
        assertEquals(3002, ex.getCode());
    }

    @Test
    void scheduleMeeting_UserNotAvailable_ThrowsBadRequest() {
        // Users exist
        when(userAccessor.findByEmail(ORGANIZER_EMAIL)).thenReturn(DbResult.success(ORGANIZER));
        when(userAccessor.findByEmail(PARTICIPANT_EMAIL)).thenReturn(DbResult.success(PARTICIPANT));

        // Mock Availability Check returns 0 (Not Available)
        //when(availabilityAccessor.isUserAvailable(eq(ORGANIZER_ID), any(), any())).thenReturn(DbResult.success(1L)); // Organizer ok
        when(availabilityAccessor.isUserAvailable(eq(PARTICIPANT_ID), any(), any())).thenReturn(DbResult.success(0L)); // Participant busy

        BadRequestException ex = assertThrows(BadRequestException.class, () -> meetingService.scheduleMeeting(REQUEST));
        assertEquals(3003, ex.getCode());
    }

    @Test
    void scheduleMeeting_ConflictDetected_ThrowsBadRequest() {
        // Users exist & "Technically" available in availability table
        when(userAccessor.findByEmail(any())).thenReturn(DbResult.success(ORGANIZER)); // Simplified for both calls
        when(availabilityAccessor.isUserAvailable(any(), any(), any())).thenReturn(DbResult.success(1L));

        // But Conflict Check finds a meeting overlap
        when(meetingsEntityAccessor.countConflicts(any(), any(), any())).thenReturn(DbResult.success(1L));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> meetingService.scheduleMeeting(REQUEST));
        assertEquals(3005, ex.getCode());
    }

    @Test
    void scheduleMeeting_ReduceAvailabilityFails_ThrowsInternalServerError() {
        // Happy path setup...
        when(userAccessor.findByEmail(ORGANIZER_EMAIL)).thenReturn(DbResult.success(ORGANIZER));
        when(userAccessor.findByEmail(PARTICIPANT_EMAIL)).thenReturn(DbResult.success(PARTICIPANT));
        when(availabilityAccessor.isUserAvailable(any(), any(), any())).thenReturn(DbResult.success(1L));
        when(meetingsEntityAccessor.countConflicts(any(), any(), any())).thenReturn(DbResult.success(0L));
        when(meetingsEntityAccessor.saveMeeting(any())).thenReturn(DbResult.success(new MeetingEntity()));

        // ...Except Reduce Availability Fails for one user
        //when(availabilityAccessor.reduceAvailability(eq(ORGANIZER_ID), any(), any())).thenReturn(DbResult.success("Ok"));
        when(availabilityAccessor.reduceAvailability(eq(PARTICIPANT_ID), any(), any())).thenReturn(DbResult.error("DB Fail"));

        InternalServerErrorException ex = assertThrows(InternalServerErrorException.class, () -> meetingService.scheduleMeeting(REQUEST));
        assertEquals(3006, ex.getCode());
    }

    // ========================================================================
    // CANCEL MEETING TESTS
    // ========================================================================

    @Test
    void cancelMeeting_Success() {
        UUID meetingId = UUID.randomUUID();

        MeetingEntity existingMeeting = MeetingEntity.builder()
                .meetingId(meetingId)
                .participantEmails(List.of(ORGANIZER_EMAIL, PARTICIPANT_EMAIL))
                .startTime(START)
                .endTime(START.plusHours(1))
                .build();

        // 1. Mock Find Meeting
        when(meetingsEntityAccessor.findById(meetingId)).thenReturn(DbResult.success(existingMeeting));

        // 2. Mock Find Users (to get IDs for restoring availability)
        when(userAccessor.findByEmail(ORGANIZER_EMAIL)).thenReturn(DbResult.success(ORGANIZER));
        when(userAccessor.findByEmail(PARTICIPANT_EMAIL)).thenReturn(DbResult.success(PARTICIPANT));

        // 3. Mock Add Availability (Restore) - Return value doesn't impact flow in current impl
        when(availabilityAccessor.addAvailability(any(), any(), any())).thenReturn(DbResult.success(null));

        // EXECUTE
        BaseApiResponse response = meetingService.cancelMeeting(meetingId);

        // ASSERT
        assertEquals(200, response.getStatusCode());
        verify(meetingsEntityAccessor).deleteById(meetingId);

        // Verify we restored availability for BOTH users
        verify(availabilityAccessor).addAvailability(eq(ORGANIZER_ID), eq(START), eq(START.plusHours(1)));
        verify(availabilityAccessor).addAvailability(eq(PARTICIPANT_ID), eq(START), eq(START.plusHours(1)));
    }

    @Test
    void cancelMeeting_MeetingNotFound_ThrowsBadRequest() {
        UUID meetingId = UUID.randomUUID();
        when(meetingsEntityAccessor.findById(meetingId)).thenReturn(DbResult.success(null));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> meetingService.cancelMeeting(meetingId));
        assertEquals(3007, ex.getCode());
        verify(meetingsEntityAccessor, never()).deleteById(any());
    }
}
