package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.BusRequest;
import com.naveen.transitflow.dto.BusResponse;
import com.naveen.transitflow.dto.SeatConfigRequest;
import com.naveen.transitflow.dto.SeatResponse;
import com.naveen.transitflow.entity.Bus;
import com.naveen.transitflow.entity.BusStatus;
import com.naveen.transitflow.entity.Seat;
import com.naveen.transitflow.entity.SeatType;
import com.naveen.transitflow.entity.TripStatus;
import com.naveen.transitflow.exception.BadRequestException;
import com.naveen.transitflow.exception.DuplicateResourceException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.BusRepository;
import com.naveen.transitflow.repository.SeatRepository;
import com.naveen.transitflow.repository.TripRepository;
import com.naveen.transitflow.service.BusService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;

    public BusServiceImpl(BusRepository busRepository, SeatRepository seatRepository,
                          TripRepository tripRepository) {
        this.busRepository = busRepository;
        this.seatRepository = seatRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusResponse> getAll() {
        return busRepository.findAll(Sort.by("id")).stream().map(BusResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BusResponse getById(Long id) {
        return BusResponse.from(findBus(id));
    }

    @Override
    @Transactional
    public BusResponse create(BusRequest request) {
        String busNumber = normalize(request.busNumber());
        String registration = normalize(request.registrationNumber());

        if (busRepository.existsByBusNumber(busNumber)) {
            throw new DuplicateResourceException("Bus number already exists");
        }
        if (busRepository.existsByRegistrationNumber(registration)) {
            throw new DuplicateResourceException("Registration number already exists");
        }

        Bus bus = busRepository.save(new Bus(busNumber, registration, request.capacity()));
        return BusResponse.from(bus);
    }

    @Override
    @Transactional
    public BusResponse update(Long id, BusRequest request) {
        Bus bus = findBus(id);
        String busNumber = normalize(request.busNumber());
        String registration = normalize(request.registrationNumber());

        if (busRepository.existsByBusNumberAndIdNot(busNumber, id)) {
            throw new DuplicateResourceException("Bus number already exists");
        }
        if (busRepository.existsByRegistrationNumberAndIdNot(registration, id)) {
            throw new DuplicateResourceException("Registration number already exists");
        }
        if (request.capacity() != bus.getCapacity() && seatRepository.countByBusId(id) > 0) {
            throw new BadRequestException(
                    "Capacity follows the seat layout. Reconfigure the seats to change it.");
        }
        if (request.status() == BusStatus.INACTIVE && bus.getStatus() != BusStatus.INACTIVE) {
            ensureNoOpenTrips(id);
        }

        bus.setBusNumber(busNumber);
        bus.setRegistrationNumber(registration);
        bus.setCapacity(request.capacity());
        if (request.status() != null) {
            bus.setStatus(request.status());
        }
        return BusResponse.from(bus);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Bus bus = findBus(id);
        ensureNoOpenTrips(id);
        bus.setStatus(BusStatus.INACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(Long busId) {
        findBus(busId);
        return seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(busId)
                .stream().map(SeatResponse::from).toList();
    }

    @Override
    @Transactional
    public List<SeatResponse> configureSeats(Long busId, SeatConfigRequest request) {
        Bus bus = findBus(busId);

        if (tripRepository.existsByBusId(busId)) {
            throw new BadRequestException("Seats cannot be reconfigured because this bus already has trips");
        }

        int left = request.leftColumns();
        int right = request.rightColumns();
        int totalColumns = left + right;

        List<Seat> seats = new ArrayList<>();
        int number = 1;
        for (int row = 1; row <= request.rows(); row++) {
            for (int col = 1; col <= totalColumns; col++) {
                seats.add(new Seat(String.format("%02d", number++), row, col,
                        resolveSeatType(col, left, totalColumns), bus));
            }
        }

        seatRepository.deleteAllByBusId(busId); // bulk delete runs immediately
        List<Seat> saved = seatRepository.saveAll(seats);
        bus.setCapacity(saved.size());

        return saved.stream().map(SeatResponse::from).toList();
    }

    /** The aisle sits between column "left" and column "left + 1". */
    private static SeatType resolveSeatType(int col, int left, int totalColumns) {
        if (col == 1 || col == totalColumns) {
            return SeatType.WINDOW;
        }
        boolean hasAisle = totalColumns > left;
        if (hasAisle && (col == left || col == left + 1)) {
            return SeatType.AISLE;
        }
        return SeatType.NORMAL;
    }

    private Bus findBus(Long id) {
        return busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id " + id));
    }

    private void ensureNoOpenTrips(Long busId) {
        if (tripRepository.existsByBusIdAndStatusIn(busId, TripStatus.OPEN)) {
            throw new BadRequestException("Bus has scheduled or running trips. Cancel or complete them first.");
        }
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase();
    }
}