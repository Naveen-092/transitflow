package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.CreateDriverRequest;
import com.naveen.transitflow.dto.UpdateDriverRequest;
import com.naveen.transitflow.dto.UserResponse;
import com.naveen.transitflow.entity.Role;
import com.naveen.transitflow.entity.TripStatus;
import com.naveen.transitflow.entity.User;
import com.naveen.transitflow.exception.BadRequestException;
import com.naveen.transitflow.exception.DuplicateResourceException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.TripRepository;
import com.naveen.transitflow.repository.UserRepository;
import com.naveen.transitflow.service.DriverService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverServiceImpl implements DriverService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PasswordEncoder passwordEncoder;

    public DriverServiceImpl(UserRepository userRepository, TripRepository tripRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findByRoleOrderByIdDesc(Role.DRIVER).stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional
    public UserResponse create(CreateDriverRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        User driver = new User(request.name().trim(), email,
                passwordEncoder.encode(request.password()), request.phone(), Role.DRIVER);
        return UserResponse.from(userRepository.save(driver));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateDriverRequest request) {
        User driver = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.DRIVER)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + id));

        if (Boolean.FALSE.equals(request.active()) && driver.isActive()
                && tripRepository.existsByDriverIdAndStatusIn(id, TripStatus.OPEN)) {
            throw new BadRequestException("Driver has scheduled or running trips. Reassign or cancel them first.");
        }

        driver.setName(request.name().trim());
        driver.setPhone(request.phone());
        if (request.active() != null) {
            driver.setActive(request.active());
        }
        return UserResponse.from(driver);
    }
}