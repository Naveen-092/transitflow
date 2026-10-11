package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.BookingResponse;
import com.naveen.transitflow.dto.SeatSelectionRequest;

import java.util.List;

public interface BookingService {

    BookingResponse confirm(Long userId, SeatSelectionRequest request);

    List<BookingResponse> getMyBookings(Long userId);

    BookingResponse getById(Long userId, Long bookingId);

    BookingResponse cancel(Long userId, Long bookingId);
}