package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.AdminBookingResponse;
import com.naveen.transitflow.entity.Booking;
import com.naveen.transitflow.entity.BookingSeat;
import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.BookingRepository;
import com.naveen.transitflow.repository.BookingSeatRepository;
import com.naveen.transitflow.service.AdminBookingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminBookingServiceImpl implements AdminBookingService {

    private static final int MAX_RESULTS = 200;

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public AdminBookingServiceImpl(BookingRepository bookingRepository,
                                   BookingSeatRepository bookingSeatRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBookingResponse> search(String q, BookingStatus status) {
        String term = q == null ? "" : q.trim();
        List<BookingStatus> statuses = status == null ? List.of(BookingStatus.values()) : List.of(status);

        List<Booking> bookings = bookingRepository.search(term, statuses, PageRequest.of(0, MAX_RESULTS));
        if (bookings.isEmpty()) {
            return List.of();
        }
        Map<Long, List<BookingSeat>> seatsByBooking = bookingSeatRepository
                .findByBookingIds(bookings.stream().map(Booking::getId).toList()).stream()
                .collect(Collectors.groupingBy(bs -> bs.getBooking().getId()));

        return bookings.stream()
                .map(b -> AdminBookingResponse.from(b, seatsByBooking.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBookingResponse getById(Long id) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
        return AdminBookingResponse.from(booking, bookingSeatRepository.findByBookingIds(List.of(id)));
    }
}