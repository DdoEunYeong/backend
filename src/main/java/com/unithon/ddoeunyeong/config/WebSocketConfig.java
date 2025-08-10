package com.unithon.ddoeunyeong.config;

import com.unithon.ddoeunyeong.global.security.config.JwtHandShakeInterceptor;
import com.unithon.ddoeunyeong.infra.websocket.StreamWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final StreamWebSocketHandler streamWebSocketHandler;
    private final JwtHandShakeInterceptor jwtHandShakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(streamWebSocketHandler, "/ws/stream")
                .addInterceptors(jwtHandShakeInterceptor)
                .setAllowedOriginPatterns("http://localhost:5500");
    }
}

