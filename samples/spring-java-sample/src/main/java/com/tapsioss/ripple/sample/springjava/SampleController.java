package com.tapsioss.ripple.sample.springjava;

import com.tapsioss.ripple.spring.SpringRippleClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample REST controller demonstrating Ripple SDK usage in Java.
 */
@RestController
@RequestMapping("/api")
public class SampleController {

    private final SpringRippleClient rippleClient;

    public SampleController(SpringRippleClient rippleClient) {
        this.rippleClient = rippleClient;
    }

    /**
     * Track a page view event.
     */
    @GetMapping("/page/{pageName}")
    public Map<String, Object> trackPageView(@PathVariable String pageName) {
        // Simple event tracking - no coroutines needed!
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", pageName);
        payload.put("timestamp", System.currentTimeMillis());
        
        rippleClient.track("page_view", payload, null);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "tracked");
        response.put("page", pageName);
        response.put("queueSize", rippleClient.getQueueSize());
        return response;
    }

    /**
     * Track a user action with metadata.
     */
    @PostMapping("/action")
    public Map<String, Object> trackAction(@RequestBody ActionRequest request) {
        // Set user context
        rippleClient.setMetadata("user_id", request.getUserId());
        
        // Track with payload and event-specific metadata
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", request.getAction());
        payload.put("target", request.getTarget());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("session_id", request.getSessionId());
        
        rippleClient.track("user_action", payload, metadata);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "tracked");
        response.put("action", request.getAction());
        return response;
    }

    /**
     * Manually flush events.
     */
    @PostMapping("/flush")
    public Map<String, Object> flushEvents() {
        int queueSizeBefore = rippleClient.getQueueSize();
        
        // Synchronous flush - waits for completion
        rippleClient.flushSync();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "flushed");
        response.put("eventsFlushed", queueSizeBefore);
        return response;
    }

    /**
     * Get current queue status.
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("queueSize", rippleClient.getQueueSize());
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
