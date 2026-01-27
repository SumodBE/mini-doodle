package com.services.mini_doodle.controller;

import com.services.mini_doodle.api.AvailabilityApi;
import com.services.mini_doodle.model.AvailabilityRequest;
import com.services.mini_doodle.model.AvailabilityResponse;
import com.services.mini_doodle.model.BaseApiResponse;
import com.services.mini_doodle.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class AvailabilityApiController implements AvailabilityApi {

    private final AvailabilityService availabilityService;

    @Override
    public ResponseEntity<AvailabilityResponse> addAvailability(AvailabilityRequest availabilityRequest) {
        return ResponseEntity.ok(availabilityService.addAvailability(availabilityRequest));
    }

    @Override
    public ResponseEntity<BaseApiResponse> removeAvailability(AvailabilityRequest availabilityRequest) {
        return ResponseEntity.ok(availabilityService.removeAvailability(availabilityRequest));
    }


}
