package com.services.mini_doodle.controller;

import com.services.mini_doodle.api.FetchApi;
import com.services.mini_doodle.model.FetchScheduleRequest;
import com.services.mini_doodle.model.FetchScheduleResponse;
import com.services.mini_doodle.service.FetchScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class FetchScheduleController implements FetchApi {

    private final FetchScheduleService fetchScheduleService;

    @Override
    public ResponseEntity<FetchScheduleResponse> getSchedule(FetchScheduleRequest fetchScheduleRequest) {
        return ResponseEntity.ok(fetchScheduleService.fetchSchedule(fetchScheduleRequest));
    }
}
