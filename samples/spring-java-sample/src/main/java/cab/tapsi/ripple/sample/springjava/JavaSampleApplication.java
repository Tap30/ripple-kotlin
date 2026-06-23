package cab.tapsi.ripple.sample.springjava;

import cab.tapsi.ripple.core.AdapterConfig;
import cab.tapsi.ripple.core.AppState;
import cab.tapsi.ripple.core.AppStateChangedPayload;
import cab.tapsi.ripple.core.DefaultRippleEvent;
import cab.tapsi.ripple.core.DefaultRippleMetadata;
import cab.tapsi.ripple.core.RippleConfig;
import cab.tapsi.ripple.core.ScreenPayload;
import cab.tapsi.ripple.core.adapters.LogLevel;
import cab.tapsi.ripple.spring.SpringRippleClient;
import cab.tapsi.ripple.spring.adapters.storage.FileStorageAdapter;
import cab.tapsi.ripple.spring.adapters.logging.Slf4jLoggerAdapter;
import cab.tapsi.ripple.spring.adapters.webflux.WebClientAdapter;
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
        client.screen(new ScreenPayload(
            "Java sample startup",
            null,
            "/java-sample/startup",
            null,
            null,
            null,
            null,
            null
        ));
        client.appOpened();
        client.getEvents().appStateChanged(new AppStateChangedPayload(AppState.FOREGROUND, AppState.OPENED));
        
        return client;
    }
}
