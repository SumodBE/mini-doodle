package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.MeetingEntity;
import com.services.mini_doodle.repo.MeetingRepository;
import com.services.mini_doodle.util.DbResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingsEntityAccessorTest {

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private MeetingsEntityAccessor accessor;

    // --- Helpers ---
    private final UUID MEETING_ID = UUID.randomUUID();
    private final UUID USER_ID = UUID.randomUUID();
    private final MeetingEntity SAMPLE_MEETING = new MeetingEntity(); // Assumes default constructor exists

    // ========================================================================
    // 1. Save Meeting
    // ========================================================================

    @Test
    void saveMeeting_Success() {
        // 1. Mock
        when(meetingRepository.save(any(MeetingEntity.class))).thenReturn(SAMPLE_MEETING);

        // 2. Execute
        DbResult<MeetingEntity> result = accessor.saveMeeting(new MeetingEntity());

        // 3. Assert
        assertTrue(result.isSuccess());
        verify(meetingRepository).save(any(MeetingEntity.class));
    }

    @Test
    void saveMeeting_Failure_DbError() {
        // 1. Mock Exception
        when(meetingRepository.save(any(MeetingEntity.class)))
                .thenThrow(new QueryTimeoutException("DB Timeout"));

        // 2. Execute
        DbResult<MeetingEntity> result = accessor.saveMeeting(new MeetingEntity());

        // 3. Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Failed to save meeting"));
    }

    // ========================================================================
    // 2. Find By ID
    // ========================================================================

    @Test
    void findById_Success_Found() {
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(SAMPLE_MEETING));

        DbResult<MeetingEntity> result = accessor.findById(MEETING_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getValue().isPresent());
        assertNotNull(result.getValue().get());
    }

    @Test
    void findById_Success_NotFound() {
        // Mock empty optional
        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.empty());

        DbResult<MeetingEntity> result = accessor.findById(MEETING_ID);

        // Accessor logic wraps ".orElse(null)" in success
        assertTrue(result.isSuccess());
        assertTrue(result.getValue().isEmpty());
    }

    @Test
    void findById_Failure_DbError() {
        when(meetingRepository.findById(MEETING_ID))
                .thenThrow(new QueryTimeoutException("Connection lost"));

        DbResult<MeetingEntity> result = accessor.findById(MEETING_ID);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Error finding meeting"));
    }

    // ========================================================================
    // 3. Delete By ID
    // ========================================================================

    @Test
    void deleteById_Success() {
        // deleteById returns void, so we just verify no exception is thrown
        doNothing().when(meetingRepository).deleteById(MEETING_ID);

        DbResult<String> result = accessor.deleteById(MEETING_ID);

        assertTrue(result.isSuccess());
    }

    @Test
    void deleteById_Failure() {
        doThrow(new QueryTimeoutException("Constraint violation"))
                .when(meetingRepository).deleteById(MEETING_ID);

        DbResult<String> result = accessor.deleteById(MEETING_ID);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Failed to delete meeting"));
    }

    // ========================================================================
    // 4. Count Conflicts
    // ========================================================================

    @Test
    void countConflicts_Success() {
        List<String> emails = List.of("a@b.com");
        OffsetDateTime now = OffsetDateTime.now();

        when(meetingRepository.countConflicts(eq(emails), any(), any())).thenReturn(2L);

        DbResult<Long> result = accessor.countConflicts(emails, now, now.plusHours(1));

        assertTrue(result.isSuccess());
        assertTrue(result.getValue().isPresent());
        assertNotNull(result.getValue().get());
        assertEquals(2L, result.getValue().get());
    }

    @Test
    void countConflicts_Failure() {
        when(meetingRepository.countConflicts(any(), any(), any()))
                .thenThrow(new QueryTimeoutException("Error"));

        DbResult<Long> result = accessor.countConflicts(Collections.emptyList(), OffsetDateTime.now(), OffsetDateTime.now());

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Failed to count conflicts"));
    }

    // ========================================================================
    // 5. Find Meetings By User
    // ========================================================================

    @Test
    void findMeetingsByUserId_Success() {
        when(meetingRepository.findAllByOrganizerId(USER_ID))
                .thenReturn(List.of(SAMPLE_MEETING));

        DbResult<List<MeetingEntity>> result = accessor.findMeetingsByUserId(USER_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getValue().isPresent());
        assertNotNull(result.getValue().get());
        assertEquals(1, result.getValue().get().size());
    }

    @Test
    void findMeetingsByUserId_Failure() {
        when(meetingRepository.findAllByOrganizerId(USER_ID))
                .thenThrow(new QueryTimeoutException("Timeout"));

        DbResult<List<MeetingEntity>> result = accessor.findMeetingsByUserId(USER_ID);

        assertFalse(result.isSuccess());
        // Verify logic inside the catch block logs/returns specific message
        assertTrue(result.getError().contains("Failed to fetch meetings for user"));
    }
}
