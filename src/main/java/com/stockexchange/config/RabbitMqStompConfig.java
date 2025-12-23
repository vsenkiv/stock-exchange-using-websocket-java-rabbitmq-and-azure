package com.stockexchange.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket Configuration with RabbitMQ as External Broker
 * <p>
 * This configuration enables true multi-instance STOMP messaging.
 * <p>
 * Architecture: - Multiple Spring Boot instances connect to RabbitMQ as STOMP clients - RabbitMQ
 * maintains centralized subscription registry - Messages are distributed across all instances -
 * Scales horizontally to handle millions of connections
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class RabbitMqStompConfig implements WebSocketMessageBrokerConfigurer {

  @Value("${rabbitmq.stomp.enabled:false}")
  private boolean useRabbitMqBroker;

  @Value("${rabbitmq.stomp.host:localhost}")
  private String rabbitMqHost;

  @Value("${rabbitmq.stomp.port:61613}")
  private int rabbitMqPort;

  @Value("${rabbitmq.stomp.username:guest}")
  private String rabbitMqUsername;

  @Value("${rabbitmq.stomp.password:guest}")
  private String rabbitMqPassword;

  @Value("${rabbitmq.stomp.virtual-host:/}")
  private String virtualHost;

  @Value("${rabbitmq.stomp.heartbeat-interval:20000}")
  private long heartbeatInterval;

  @Override
  public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String destination = accessor.getDestination();

        // Log EVERY outgoing message
        log.debug("🔍 OUTGOING MESSAGE - Destination: {} | Command: {}",
            destination, accessor.getCommand());

        // Validate destination format for RabbitMQ
        if (destination != null) {
          // Allow /exchange/, /topic/, /queue/ prefixes
          if (!destination.startsWith("/exchange/")
              && !destination.startsWith("/topic/")
              && !destination.startsWith("/queue/")
              && !destination.startsWith("/app/")
              && !destination.startsWith("/user/")) {
            log.error("❌ INVALID DESTINATION FORMAT: {}", destination);
            throw new IllegalArgumentException("Invalid destination: " + destination);
          }
        }

        return message;
      }
    });
  }

  /**
   * Configure Message Broker
   * <p>
   * Chooses between: 1. RabbitMQ external broker (production, multi-instance) 2. Simple in-memory
   * broker (development, single instance)
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {

    if (useRabbitMqBroker) {
      // ================================================================
      // PRODUCTION: RabbitMQ STOMP Broker Relay
      // ================================================================

      log.info("========================================");
      log.info("Configuring STOMP with RabbitMQ Broker");
      log.info("========================================");
      log.info("RabbitMQ Host: {}", rabbitMqHost);
      log.info("RabbitMQ STOMP Port: {}", rabbitMqPort);
      log.info("RabbitMQ User: {}", rabbitMqUsername);
      log.info("RabbitMQ Virtual Host: {}", virtualHost);
      log.info("========================================");

      registry.enableStompBrokerRelay("/topic", "/queue", "/exchange")
          // RabbitMQ STOMP endpoint
          .setRelayHost(rabbitMqHost)
          .setRelayPort(rabbitMqPort)

          // Client connections (Spring Boot → RabbitMQ)
          .setClientLogin(rabbitMqUsername)
          .setClientPasscode(rabbitMqPassword)

          // System connections (internal Spring framework → RabbitMQ)
          .setSystemLogin(rabbitMqUsername)
          .setSystemPasscode(rabbitMqPassword)

          // Virtual host
          .setVirtualHost(virtualHost)

          // Heartbeat settings (keep connection alive)
          .setSystemHeartbeatSendInterval(heartbeatInterval)
          .setSystemHeartbeatReceiveInterval(heartbeatInterval)

          // Auto-start the relay on application startup
          .setAutoStartup(true);

      log.info("✅ STOMP Broker Relay to RabbitMQ configured successfully");

      /*
       * HOW IT WORKS:
       *
       * 1. Spring Boot connects to RabbitMQ on port 61613 using STOMP protocol
       * 2. When client subscribes to /topic/stock/AAPL:
       *    Client → Spring Boot → RabbitMQ (subscription registered)
       * 3. When Spring Boot broadcasts message:
       *    Spring Boot → RabbitMQ → ALL Spring Boot instances → Their clients
       * 4. RabbitMQ acts as central message distribution hub
       * 5. Subscriptions are shared across all instances
       */

    } else {
      // ================================================================
      // DEVELOPMENT: Simple In-Memory Broker
      // ================================================================

      log.info("========================================");
      log.info("Configuring STOMP with Simple Broker");
      log.info("⚠️  Single instance only - no scaling!");
      log.info("========================================");

      registry.enableSimpleBroker("/topic", "/queue")
          .setHeartbeatValue(new long[]{10000, 10000});

      log.info("✅ Simple STOMP Broker configured");
    }

    // Application destination prefix
    // Messages sent to /app/* are routed to @MessageMapping methods
    registry.setApplicationDestinationPrefixes("/app");

    // User destination prefix for private messages
    registry.setUserDestinationPrefix("/user");
  }

  /**
   * Register STOMP Endpoints
   * <p>
   * Creates WebSocket endpoint at /ws-stomp for client connections
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Endpoint with SockJS fallback
    registry.addEndpoint("/ws-stomp")
        .setAllowedOriginPatterns("*")
        .withSockJS();

    // Endpoint for native WebSocket (no SockJS)
    registry.addEndpoint("/ws-stomp")
        .setAllowedOriginPatterns("*");

    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();

    log.info("✅ STOMP endpoint registered at /ws-stomp");
  }
}
