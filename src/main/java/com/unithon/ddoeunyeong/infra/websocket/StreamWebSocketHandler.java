package com.unithon.ddoeunyeong.infra.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StreamWebSocketHandler extends BinaryWebSocketHandler {

    // private final S3Uploader s3Uploader; // S3 업로더 주입

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

    /*
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String msg = message.getPayload();
        if ("end".equals(msg)) {
            String url = saveAndUpload(session.getId());
            log.info("영상 업로드 완료: {}", url);
        }
    }
    */

    /*
    private String saveAndUpload(String sessionId) {
        List<byte[]> chunks = sessionChunks.get(sessionId);
        if (chunks == null || chunks.isEmpty()) return null;

        try {
            // 1. 파일로 저장
            File tempFile = File.createTempFile("recorded_" + sessionId, ".webm");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                for (byte[] chunk : chunks) {
                    fos.write(chunk);
                }
            }

            // 2. S3 업로드
            String s3Url = s3Uploader.uploadFile(tempFile, "videos/" + tempFile.getName());

            // 3. 임시파일 삭제 및 메모리 정리
            tempFile.delete();
            sessionChunks.remove(sessionId);
            return s3Url;

        } catch (IOException e) {
            log.error("영상 저장/업로드 실패", e);
            return null;
        }
    }
    */

    public void afterConnectionClosed(String sessionId, CloseStatus status) {
        sessionChunks.remove(sessionId);
    }

    public byte[] getLatestFrame(String sessionId) {
        List<byte[]> chunks = sessionChunks.get(sessionId);
        if (chunks == null || chunks.isEmpty()) return null;
        return chunks.get(chunks.size() - 1); // 마지막 프레임만 리턴
    }
}

