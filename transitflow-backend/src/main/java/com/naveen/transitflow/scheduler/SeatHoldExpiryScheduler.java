package com.naveen.transitflow.scheduler;

import com.naveen.transitflow.service.SeatHoldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Housekeeping only. Correctness does not depend on it: every read and write
 * treats an expired hold as available, even if this job has not run yet.
 */
@Component
public class SeatHoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldExpiryScheduler.class);

    private final SeatHoldService seatHoldService;

    public SeatHoldExpiryScheduler(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.hold-expiry-delay-ms:30000}")
    public void releaseExpiredHolds() {
        int released = seatHoldService.releaseExpiredHolds();
        if (released > 0) {
            log.info("Released {} expired seat hold(s)", released);
        }
    }
}