package com.unithon.ddoeunyeong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class WebSocketContainerConfig {

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(512 * 1024);   // 512KB
        container.setMaxBinaryMessageBufferSize(2 * 1024 * 1024); // 2MB
        container.setMaxSessionIdleTimeout(60_000L);         // 선택: 60초
        container.setAsyncSendTimeout(10_000L);              // 선택: 10초
        return container;
    }
}

