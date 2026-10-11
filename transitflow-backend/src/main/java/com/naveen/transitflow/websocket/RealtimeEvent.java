package com.naveen.transitflow.websocket;

/** "Send this payload to this topic once the surrounding transaction has committed." */
public record RealtimeEvent(String destination, Object payload) {
}