package com.tapsioss.ripple.sample.springjava;

import com.tapsioss.ripple.core.AdapterConfig;
import com.tapsioss.ripple.core.RippleConfig;
import com.tapsioss.ripple.core.adapters.LogLevel;
import com.tapsioss.ripple.spring.SpringRippleClient;
import com.tapsioss.ripple.spring.adapters.storage.FileStorageAdapter;
import com.tapsioss.ripple.spring.adapters.logging.Slf4jLoggerAdapter;
import com.tapsioss.ripple.spring.adapters.webflux.WebClientAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Sample Spring Boot application demonstrating Ripple SDK usage in Java.
 * 
 * This example shows that the SDK works seamlessly with Java code,
 * without requiring any Kotlin or coroutines knowledge.
 */
@SpringBootApplication
public class JavaSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaSampleApplication.class, args);
    }

    /**
     * Configure Ripple client as a Spring bean.
     * 
     * The client is thread-safe and can be injected into any service.
     */
    @Bean
    public SpringRippleClient rippleClient() {
        RippleConfig config = new RippleConfig(
            "your-api-key",
            "https://api.example.com/events",
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

        SpringRippleClient client = new SpringRippleClient(config);
        client.init();
        
        // Set application-wide metadata
        client.setMetadata("service", "java-sample");
        client.setMetadata("environment", "development");
        
        return client;
    }
}
