import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { clearSession, getToken } from '../api/session';

/**
 * Connects to the STOMP endpoint with the JWT and subscribes to the topics
 * for which a handler is given: onSeats, onStatus, onLocation.
 * onConnect fires on every (re)connect, so callers can refetch to catch up.
 * Returns true while connected.
 */
export function useTripSocket(tripId, handlers, enabled = true) {
  const handlersRef = useRef(handlers);
  const [connected, setConnected] = useState(false);

  // Always call the latest handlers without reconnecting.
  useEffect(() => {
    handlersRef.current = handlers;
  });

  useEffect(() => {
    if (!enabled || !tripId || !getToken()) return undefined;

    const client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 0,
      // Read the token again on every (re)connect attempt.
            // Read the token again on every (re)connect attempt. An expired token ends the session.
      beforeConnect: () => {
        const token = getToken();
        if (!token) {
          clearSession();
          window.location.assign('/login');
          return;
        }
        client.connectHeaders = { Authorization: `Bearer ${token}` };
      },
      onConnect: () => {
        const h = handlersRef.current;
        const listen = (kind, key) => {
          if (h[key]) {
            client.subscribe(`/topic/trips/${tripId}/${kind}`, (msg) => {
              handlersRef.current[key]?.(JSON.parse(msg.body));
            });
          }
        };
        listen('seats', 'onSeats');
        listen('status', 'onStatus');
        listen('location', 'onLocation');
        setConnected(true);
        handlersRef.current.onConnect?.();
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => console.warn('STOMP error:', frame.headers.message),
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, [tripId, enabled]);

  return connected;
}