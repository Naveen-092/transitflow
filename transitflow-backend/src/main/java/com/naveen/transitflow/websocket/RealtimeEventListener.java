package com.naveen.transitflow.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeEventListener {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Runs only after a successful commit. fallbackExecution covers events published outside a transaction. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEvent(RealtimeEvent event) {
        try {
            messagingTemplate.convertAndSend(event.destination(), event.payload());
        } catch (RuntimeException ex) {
            // The data is already committed. A failed push must never break the request.
            log.warn("Could not push to {}: {}", event.destination(), ex.getMessage());
        }
    }
}