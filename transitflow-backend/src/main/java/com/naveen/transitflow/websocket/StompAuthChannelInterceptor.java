package com.naveen.transitflow.websocket;

import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.entity.Role;
import com.naveen.transitflow.repository.BookingRepository;
import com.naveen.transitflow.repository.TripRepository;
import com.naveen.transitflow.security.CustomUserDetails;
import com.naveen.transitflow.security.CustomUserDetailsService;
import com.naveen.transitflow.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Security for the WebSocket channel:
 *   CONNECT   -> must carry "Authorization: Bearer <jwt>"
 *   SUBSCRIBE -> only the topics, and only for the people allowed to see them
 *   SEND      -> never allowed (all data goes through REST)
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern TRIP_TOPIC = Pattern.compile("^/topic/trips/(\\d{1,18})/(seats|location|status)$");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    public StompAuthChannelInterceptor(JwtService jwtService,
                                       CustomUserDetailsService userDetailsService,
                                       TripRepository tripRepository,
                                       BookingRepository bookingRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message; // heartbeat or internal message
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            throw new MessageDeliveryException("Clients are not allowed to send messages");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessageDeliveryException("Missing Authorization header");
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            String email = jwtService.extractUsername(token);
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (!user.isEnabled() || !jwtService.isTokenValid(token, user)) {
                throw new MessageDeliveryException("Invalid token");
            }
            // Spring keeps this user for the rest of the WebSocket session.
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            throw new MessageDeliveryException("Invalid or expired token");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new MessageDeliveryException("Not authenticated");
        }

        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : TRIP_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessageDeliveryException("Subscription not allowed: " + destination);
        }

        Long tripId = Long.valueOf(matcher.group(1));
        if (!canSubscribe(user, tripId, matcher.group(2))) {
            throw new MessageDeliveryException("You are not allowed to subscribe to " + destination);
        }
    }

    private boolean canSubscribe(CustomUserDetails user, Long tripId, String kind) {
        Role role = user.getRole();
        boolean assignedDriver = role == Role.DRIVER
                && tripRepository.existsByIdAndDriverId(tripId, user.getId());

        return switch (kind) {
            case "seats" -> role == Role.PASSENGER || role == Role.ADMIN;
            case "status" -> role == Role.PASSENGER || role == Role.ADMIN || assignedDriver;
            case "location" -> role == Role.ADMIN || assignedDriver
                    || (role == Role.PASSENGER && bookingRepository.existsByTripIdAndUserIdAndStatus(
                            tripId, user.getId(), BookingStatus.CONFIRMED));
            default -> false;
        };
    }
}