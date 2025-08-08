package com.unithon.ddoeunyeong.infra.websocket;

import com.unithon.ddoeunyeong.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamWebSocketHandler extends BinaryWebSocketHandler {

    private final S3Service s3Service; // S3 업로더 주입

    // 사용자별 frame 저장소
    private final Map<String, List<byte[]>> sessionChunks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionChunks.put(session.getId(), new ArrayList<>());
        log.info("웹소켓 연결됨: {}", session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] payload = message.getPayload().array();
        sessionChunks.get(session.getId()).add(payload);  // 사용자별로 누적
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String msg = message.getPayload();
        if ("end".equals(msg)) {
            String url = saveAndUpload(session.getId());
            log.info("영상 업로드 완료: {}", url);
        }
    }

    private String saveAndUpload(String sessionId) {
        List<byte[]> chunks = sessionChunks.get(sessionId);
        if (chunks == null || chunks.isEmpty()) return null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (byte[] chunk : chunks) {
                baos.write(chunk);
            }

            MultipartFile multipartFile = new MockMultipartFile(
                    "recorded",
                    "recorded_" + sessionId + ".webm",
                    "video/webm",
                    baos.toByteArray()
            );

            // 3. S3 업로드
            String s3Url = s3Service.uploadFile(multipartFile);

            sessionChunks.remove(sessionId);
            return s3Url;

        } catch (IOException e) {
            log.error("영상 저장/업로드 실패", e);
            return null;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionChunks.remove(session.getId());
    }

    public byte[] getLatestFrame(String sessionId) {
        List<byte[]> chunks = sessionChunks.get(sessionId);
        if (chunks == null || chunks.isEmpty()) return null;
        return chunks.get(chunks.size() - 1); // 마지막 프레임만 리턴
    }
}

