package com.pcare.live;

import com.pcare.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events stream for the Command Centre. Auth is via a token query parameter because
 * the browser EventSource API cannot set an Authorization header.
 */
@Tag(name = "Live", description = "Real-time server-sent event stream")
@RestController
@RequestMapping("/api/v1/live")
public class LiveController {

    private final LiveHub liveHub;
    private final JwtService jwtService;

    public LiveController(LiveHub liveHub, JwtService jwtService) {
        this.liveHub = liveHub;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Subscribe to the real-time event stream (pass ?token=<jwt>)")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String token) {
        // Validate the JWT; throws if invalid/expired, which yields a 500->client reconnect.
        jwtService.parse(token);
        return liveHub.subscribe();
    }
}
