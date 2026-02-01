package com.tapsioss.ripple.sample.springjava;

import com.tapsioss.ripple.core.DefaultRippleEvent;
import com.tapsioss.ripple.core.DefaultRippleMetadata;
import com.tapsioss.ripple.spring.SpringRippleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample REST controller demonstrating Ripple SDK usage in Java.
 * Shows untyped usage which is perfect for Java applications.
 */
@RestController
@RequestMapping("/api")
public class SampleController {

    private static final Logger logger = LoggerFactory.getLogger(SampleController.class);
    private final SpringRippleClient<DefaultRippleEvent, DefaultRippleMetadata> rippleClient;

    public SampleController(SpringRippleClient<DefaultRippleEvent, DefaultRippleMetadata> rippleClient) {
        this.rippleClient = rippleClient;
    }

    /**
     * Track a page view event.
     */
    @GetMapping("/page/{pageName}")
    public Map<String, Object> trackPageView(@PathVariable String pageName) {
        long startTime = System.currentTimeMillis();
        
        // Track page view event
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", pageName);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("user_agent", "java-client");
        
        rippleClient.track("page_view", payload);
        
        // Track API request performance
        Map<String, Object> perfPayload = new HashMap<>();
        perfPayload.put("endpoint", "/api/page/" + pageName);
        perfPayload.put("method", "GET");
        perfPayload.put("duration_ms", System.currentTimeMillis() - startTime);
        perfPayload.put("status_code", 200);
        
        rippleClient.track("api_request", perfPayload);
        
        logger.info("Tracked page view for: {}", pageName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "tracked");
        response.put("page", pageName);
        response.put("queueSize", rippleClient.getQueueSize());
        response.put("sessionId", rippleClient.getSessionId());
        return response;
    }

    /**
     * Track a user action with metadata.
     */
    @PostMapping("/action")
    public Map<String, Object> trackAction(@RequestBody ActionRequest request) {
        long startTime = System.currentTimeMillis();
        
        // Set user context metadata
        rippleClient.setMetadata("user_id", request.getUserId());
        rippleClient.setMetadata("session_id", request.getSessionId());
        
        // Track user action with payload and event-specific metadata
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", request.getAction());
        payload.put("target", request.getTarget());
        payload.put("timestamp", System.currentTimeMillis());
        
        Map<String, Object> eventMetadata = new HashMap<>();
        eventMetadata.put("source", "api");
        eventMetadata.put("client_type", "java");
        
        rippleClient.track("user_action", payload, eventMetadata);
        
        // Track the API request itself
        Map<String, Object> apiPayload = new HashMap<>();
        apiPayload.put("endpoint", "/api/action");
        apiPayload.put("method", "POST");
        apiPayload.put("duration_ms", System.currentTimeMillis() - startTime);
        apiPayload.put("status_code", 200);
        
        rippleClient.track("api_request", apiPayload);
        
        logger.info("Tracked user action: {} for user: {}", request.getAction(), request.getUserId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "tracked");
        response.put("action", request.getAction());
        response.put("userId", request.getUserId());
        return response;
    }

    /**
     * Manually flush events.
     */
    @PostMapping("/flush")
    public Map<String, Object> flushEvents() {
        int queueSizeBefore = rippleClient.getQueueSize();
        
        // Track flush event
        Map<String, Object> payload = new HashMap<>();
        payload.put("manual_flush", true);
        payload.put("events_in_queue", queueSizeBefore);
        
        rippleClient.track("flush_triggered", payload);
        
        // Synchronous flush - waits for completion
        rippleClient.flushSync();
        
        logger.info("Manually flushed {} events", queueSizeBefore);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "flushed");
        response.put("eventsFlushed", queueSizeBefore);
        response.put("currentQueueSize", rippleClient.getQueueSize());
        return response;
    }

    /**
     * Get current queue status and metadata.
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("queueSize", rippleClient.getQueueSize());
        response.put("sessionId", rippleClient.getSessionId());
        response.put("metadata", rippleClient.getMetadata());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Simulate error for testing retry logic.
     */
    @PostMapping("/simulate-error")
    public Map<String, Object> simulateError() {
        // Track error event
        Map<String, Object> payload = new HashMap<>();
        payload.put("error_type", "simulated");
        payload.put("severity", "warning");
        payload.put("message", "This is a test error for demonstration");
        
        rippleClient.track("error_occurred", payload);
        
        logger.warn("Simulated error event tracked");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error_tracked");
        response.put("message", "Simulated error event has been tracked");
        return response;
    }
}

/**
 * Request body for action tracking.
 */
class ActionRequest {
    private String userId;
    private String action;
    private String target;
    private String sessionId;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
