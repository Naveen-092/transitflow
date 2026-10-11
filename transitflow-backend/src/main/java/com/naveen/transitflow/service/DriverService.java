package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.CreateDriverRequest;
import com.naveen.transitflow.dto.UpdateDriverRequest;
import com.naveen.transitflow.dto.UserResponse;

import java.util.List;

public interface DriverService {

    List<UserResponse> getAll();

    UserResponse create(CreateDriverRequest request);

    UserResponse update(Long id, UpdateDriverRequest request);
}