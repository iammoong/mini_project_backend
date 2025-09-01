package project.moonki.config.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration(proxyBeanMethods = true)
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WsJwtHandshakeInterceptor jwtHandshakeInterceptor;

    // 스케줄러: 기존 충돌 회피용 이름 유지
    @Bean(name = "wsHeartbeatScheduler")
    public TaskScheduler wsHeartbeatScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(2);
        ts.setThreadNamePrefix("ws-heartbeat-");
        ts.initialize();
        return ts;
    }

    @Bean
    public WsUserHandshakeHandler wsUserHandshakeHandler() {
        return new WsUserHandshakeHandler();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(wsHeartbeatScheduler())
                .setHeartbeatValue(new long[]{10000, 10000});
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(wsUserHandshakeHandler())
                .withSockJS();
    }
}
