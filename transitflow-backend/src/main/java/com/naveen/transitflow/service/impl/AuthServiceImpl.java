package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.AuthResponse;
import com.naveen.transitflow.dto.LoginRequest;
import com.naveen.transitflow.dto.RegisterRequest;
import com.naveen.transitflow.dto.UserResponse;
import com.naveen.transitflow.entity.Role;
import com.naveen.transitflow.entity.User;
import com.naveen.transitflow.exception.DuplicateResourceException;
import com.naveen.transitflow.repository.UserRepository;
import com.naveen.transitflow.security.CustomUserDetails;
import com.naveen.transitflow.security.JwtService;
import com.naveen.transitflow.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse registerPassenger(RegisterRequest request) {
        String email = normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phone(),
                Role.PASSENGER            // always PASSENGER, never taken from the request
        );

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalize(request.email()), request.password()));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(),
                UserResponse.from(principal.getUser()));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}