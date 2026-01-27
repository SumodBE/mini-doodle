package com.services.mini_doodle.service;

import com.services.mini_doodle.model.AvailabilityRequest;
import com.services.mini_doodle.model.AvailabilityResponse;
import com.services.mini_doodle.model.BaseApiResponse;

public interface AvailabilityService {

    AvailabilityResponse addAvailability(AvailabilityRequest request);

    BaseApiResponse removeAvailability(AvailabilityRequest request);

}
