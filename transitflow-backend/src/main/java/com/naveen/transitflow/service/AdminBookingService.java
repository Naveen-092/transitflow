package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.AdminBookingResponse;
import com.naveen.transitflow.entity.BookingStatus;

import java.util.List;

public interface AdminBookingService {

    /** Latest 200 matches. q matches reference, passenger name or email; blank q matches everything. */
    List<AdminBookingResponse> search(String q, BookingStatus status);

    AdminBookingResponse getById(Long id);
}