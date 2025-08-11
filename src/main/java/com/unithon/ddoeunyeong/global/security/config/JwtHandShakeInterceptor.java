package com.unithon.ddoeunyeong.global.security.config;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.repository.AdviceRepository;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.global.security.token.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandShakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChildRepository childRepository;
    private final AdviceRepository adviceRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();

        String token      = params.getFirst("token");
        String adviceIdStr= params.getFirst("adviceId"); // ← 새로 추가

        if (token == null || adviceIdStr == null) {
            setStatus(response, 400); // 필수 파라미터 누락
            return false;
        }

        log.info("[HS] params token?{} adviceId?{}", token!=null, adviceIdStr!=null);

        if (!jwtTokenProvider.validateToken(token)) {
            setStatus(response, 401); // 토큰 불량
            return false;
        }

        Long userId;
        Long adviceId;
        try {
            userId   = Long.valueOf(jwtTokenProvider.getUserIdFromToken(token));
            adviceId = Long.valueOf(adviceIdStr);
        } catch (Exception e) {
            setStatus(response, 400);
            return false;
        }

        // 세션 속성 저장
        attributes.put("userId", userId);
        attributes.put("adviceId", adviceId);

        return true;
    }


    private void setStatus(ServerHttpResponse response, int status) {
        if (response instanceof org.springframework.http.server.ServletServerHttpResponse r) {
            r.getServletResponse().setStatus(status);
        }
    }

    @Override public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                         WebSocketHandler wsHandler, Exception ex) {}
}
