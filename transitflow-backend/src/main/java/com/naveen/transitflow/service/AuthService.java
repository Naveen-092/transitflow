package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.AuthResponse;
import com.naveen.transitflow.dto.LoginRequest;
import com.naveen.transitflow.dto.RegisterRequest;
import com.naveen.transitflow.dto.UserResponse;

public interface AuthService {

    UserResponse registerPassenger(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}