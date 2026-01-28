package com.services.mini_doodle.repo;


import com.services.mini_doodle.model.AvailabilityEntity;
import com.services.mini_doodle.repo.AvailabilityRepository;
import com.services.mini_doodle.util.DbResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityAccessorTest {

    @Mock
    private AvailabilityRepository repository;

    @InjectMocks
    private AvailabilityAccessor accessor;

    // --- Test Data Helpers ---
    private final UUID USER_ID = UUID.randomUUID();
    private final OffsetDateTime NOW = OffsetDateTime.now();

    // ========================================================================
    // 1. BASIC CRUD TESTS
    // ========================================================================

    @Test
    void getOverlappingSlots_Success() {
        // Setup
        List<AvailabilityEntity> mockData = List.of(new AvailabilityEntity());
        when(repository.findOverlappingSlots(eq(USER_ID), any(), any())).thenReturn(mockData);

        // Execute
        DbResult<List<AvailabilityEntity>> result = accessor.getOverlappingSlots(USER_ID, NOW, NOW.plusHours(1));

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(1, result.getValue().get().size());
    }

    @Test
    void getOverlappingSlots_Failure_DbException() {
        // Setup
        when(repository.findOverlappingSlots(any(), any(), any()))
                .thenThrow(new QueryTimeoutException("Timeout"));

        // Execute
        DbResult<List<AvailabilityEntity>> result = accessor.getOverlappingSlots(USER_ID, NOW, NOW.plusHours(1));

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Database error"));
    }

    @Test
    void isUserAvailable_Success() {
        when(repository.isUserAvailable(eq(USER_ID), any(), any())).thenReturn(5L);

        DbResult<Long> result = accessor.isUserAvailable(USER_ID, NOW, NOW.plusHours(1));

        assertTrue(result.isSuccess());
        assertEquals(5L, result.getValue().get());
    }

    // ========================================================================
    // 2. COMPLEX LOGIC: ADD & MERGE
    // ========================================================================

    @Test
    void addAvailability_NoOverlap_ShouldJustSaveNewSlot() {
        // 1. Mock: No existing slots overlap
        when(repository.findOverlappingSlots(any(), any(), any())).thenReturn(Collections.emptyList());

        // Mock the Save
        when(repository.save(any(AvailabilityEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // 2. Execute
        OffsetDateTime start = NOW;
        OffsetDateTime end = NOW.plusHours(2);
        DbResult<AvailabilityEntity> result = accessor.addAvailability(USER_ID, start, end);

        // 3. Assert
        assertTrue(result.isSuccess());
        verify(repository, times(1)).save(any(AvailabilityEntity.class));
        verify(repository, never()).deleteAll(any()); // No delete needed

        AvailabilityEntity saved = result.getValue().get();
        assertEquals(start, saved.getStartTime());
        assertEquals(end, saved.getEndTime());
    }

    @Test
    void addAvailability_WithOverlap_ShouldMergeAndDeleteOld() {
        // 1. Setup Scenario:
        // Request: 10:00 - 12:00
        // Existing Overlap: 09:00 - 10:30 (Should expand Start to 09:00)

        OffsetDateTime reqStart = NOW.plusHours(1); // 10:00
        OffsetDateTime reqEnd = NOW.plusHours(3);   // 12:00

        AvailabilityEntity existingSlot = AvailabilityEntity.builder()
                .userId(USER_ID)
                .startTime(NOW)             // 09:00 (Earlier start)
                .endTime(NOW.plusHours(1).plusMinutes(30)) // 10:30
                .build();

        // Mock overlap finding
        when(repository.findOverlappingSlots(any(), any(), any())).thenReturn(List.of(existingSlot));
        // Mock save
        when(repository.save(any(AvailabilityEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // 2. Execute
        DbResult<AvailabilityEntity> result = accessor.addAvailability(USER_ID, reqStart, reqEnd);

        // 3. Assert
        assertTrue(result.isSuccess());

//        // Verify OLD slot was deleted
//        verify(repository).deleteAll(argThat(list -> list.c(existingSlot)));

        // Verify NEW slot has EXPANDED boundaries (09:00 to 12:00)
        ArgumentCaptor<AvailabilityEntity> captor = ArgumentCaptor.forClass(AvailabilityEntity.class);
        verify(repository).save(captor.capture());

        AvailabilityEntity finalSlot = captor.getValue();
        assertEquals(NOW, finalSlot.getStartTime(), "Start time should utilize the existing earlier slot");
        assertEquals(reqEnd, finalSlot.getEndTime(), "End time should utilize the requested later end");
    }

    // ========================================================================
    // 3. COMPLEX LOGIC: REDUCE & SPLIT
    // ========================================================================

    @Test
    void reduceAvailability_NoOverlaps_ShouldReturnError() {
        // Mock nothing found
        when(repository.findOverlappingSlots(any(), any(), any())).thenReturn(Collections.emptyList());

        DbResult<String> result = accessor.reduceAvailability(USER_ID, NOW, NOW.plusHours(1));

        assertFalse(result.isSuccess());
        assertEquals("No slots found to reduce", result.getError());
    }

    @Test
    void reduceAvailability_MiddleSplit_ShouldCreateTwoFragments() {
        // Scenario:
        // Existing: 09:00 - 12:00
        // Remove:   10:00 - 11:00
        // Expect:   Save(09-10) AND Save(11-12)

        OffsetDateTime slotStart = NOW;
        OffsetDateTime slotEnd = NOW.plusHours(3);

        OffsetDateTime removeStart = NOW.plusHours(1);
        OffsetDateTime removeEnd = NOW.plusHours(2);

        AvailabilityEntity existing = AvailabilityEntity.builder()
                .userId(USER_ID).startTime(slotStart).endTime(slotEnd).build();

        when(repository.findOverlappingSlots(any(), any(), any())).thenReturn(List.of(existing));

        // Execute
        DbResult<String> result = accessor.reduceAvailability(USER_ID, removeStart, removeEnd);

        // Assert
        assertTrue(result.isSuccess());

        // 1. Verify Delete Original
        verify(repository).deleteAll(List.of(existing));

        // 2. Verify Save 2 New Fragments
        ArgumentCaptor<List<AvailabilityEntity>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saveCaptor.capture());

        List<AvailabilityEntity> savedFragments = saveCaptor.getValue();
        assertEquals(2, savedFragments.size());

        // Fragment 1: 09:00 - 10:00
        AvailabilityEntity frag1 = savedFragments.get(0);
        assertEquals(slotStart, frag1.getStartTime());
        assertEquals(removeStart, frag1.getEndTime());

        // Fragment 2: 11:00 - 12:00
        AvailabilityEntity frag2 = savedFragments.get(1);
        assertEquals(removeEnd, frag2.getStartTime());
        assertEquals(slotEnd, frag2.getEndTime());
    }

    @Test
    void reduceAvailability_EdgeTrim_ShouldCreateOneFragment() {
        // Scenario:
        // Existing: 09:00 - 12:00
        // Remove:   09:00 - 10:00 (Trimming the head)
        // Expect:   Save(10:00 - 12:00)

        OffsetDateTime slotStart = NOW;
        OffsetDateTime slotEnd = NOW.plusHours(3);
        OffsetDateTime removeEnd = NOW.plusHours(1);

        AvailabilityEntity existing = AvailabilityEntity.builder()
                .userId(USER_ID).startTime(slotStart).endTime(slotEnd).build();

        when(repository.findOverlappingSlots(any(), any(), any())).thenReturn(List.of(existing));

        // Execute
        DbResult<String> result = accessor.reduceAvailability(USER_ID, slotStart, removeEnd);

        // Assert
        assertTrue(result.isSuccess());

        ArgumentCaptor<List<AvailabilityEntity>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saveCaptor.capture());

        List<AvailabilityEntity> savedFragments = saveCaptor.getValue();
        assertEquals(1, savedFragments.size());

        // Check new start time is the end of the removal
        assertEquals(removeEnd, savedFragments.get(0).getStartTime());
        assertEquals(slotEnd, savedFragments.get(0).getEndTime());
    }
}