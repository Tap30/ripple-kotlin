package com.tapsioss.ripple.sample.springjava;

import com.tapsioss.ripple.core.AdapterConfig;
import com.tapsioss.ripple.core.DefaultRippleEvent;
import com.tapsioss.ripple.core.DefaultRippleMetadata;
import com.tapsioss.ripple.core.RippleConfig;
import com.tapsioss.ripple.core.adapters.LogLevel;
import com.tapsioss.ripple.spring.SpringRippleClient;
import com.tapsioss.ripple.spring.adapters.storage.FileStorageAdapter;
import com.tapsioss.ripple.spring.adapters.logging.Slf4jLoggerAdapter;
import com.tapsioss.ripple.spring.adapters.webflux.WebClientAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample Spring Boot application demonstrating Ripple SDK usage in Java.
 * 
 * This example shows untyped usage of the SDK, which is perfect for Java
 * applications that don't need compile-time type safety.
 */
@SpringBootApplication
public class JavaSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaSampleApplication.class, args);
    }

    /**
     * Configure Ripple client as a Spring bean using the factory method.
     * 
     * The factory method creates an untyped client that works seamlessly with Java.
     */
    @Bean
    public SpringRippleClient<DefaultRippleEvent, DefaultRippleMetadata> rippleClient() {
        RippleConfig config = new RippleConfig(
            "java-demo-key",
            "http://localhost:8080/events",
            "X-API-Key",
            5000L,  // flushInterval
            10,     // maxBatchSize
            3,      // maxRetries
            new AdapterConfig(
                new WebClientAdapter(),
                new FileStorageAdapter(),
                new Slf4jLoggerAdapter(LogLevel.INFO)
            )
        );

        // Use constructor with explicit generic types for Java compatibility
        SpringRippleClient<DefaultRippleEvent, DefaultRippleMetadata> client = 
            new SpringRippleClient<>(config);
        
        client.init();
        
        // Set application-wide metadata using simple key-value pairs
        client.setMetadata("service", "java-sample");
        client.setMetadata("environment", "development");
        client.setMetadata("language", "java");
        client.setMetadata("framework", "spring-boot");
        
        // Track application startup event
        Map<String, Object> startupPayload = new HashMap<>();
        startupPayload.put("event_type", "application_startup");
        startupPayload.put("timestamp", System.currentTimeMillis());
        startupPayload.put("jvm_version", System.getProperty("java.version"));
        
        client.track("application_startup", startupPayload);
        
        return client;
    }
}
