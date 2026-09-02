package com.pcare.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory pub/sub hub for real-time server-sent events. The Command Centre subscribes once and
 * receives a live stream of case, dispatch, location and notification events as they happen —
 * powering the live dashboard, operations feed and tracking map (blueprint points 6, 30, 63).
 */
@Component
public class LiveHub {

    private static final Logger log = LoggerFactory.getLogger(LiveHub.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("hello")
                    .data(Map.of("message", "connected", "at", Instant.now().toString())));
        } catch (IOException ignored) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    /** Broadcast an event to all subscribers. Dead connections are pruned. */
    public void broadcast(String eventName, Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public int subscriberCount() {
        return emitters.size();
    }
}
