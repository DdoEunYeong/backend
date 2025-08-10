package com.unithon.ddoeunyeong.infra.websocket;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.repository.AdviceRepository;
import com.unithon.ddoeunyeong.domain.advice.service.AdviceService;
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

import static com.unithon.ddoeunyeong.domain.advice.entity.AdviceStatus.ABORTED;
import static com.unithon.ddoeunyeong.domain.advice.entity.AdviceStatus.COMPLETED;


@Slf4j
@Component
@RequiredArgsConstructor
public class StreamWebSocketHandler extends BinaryWebSocketHandler {

    private final S3Service s3Service; // S3 업로더 주입

    private final AdviceService adviceService;

    // 사용자별 frame 저장소
    private final Map<String, List<byte[]>> sessionChunks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId  = (Long) session.getAttributes().get("userId");
        Long childId = (Long) session.getAttributes().get("childId");

        // 재진입 방지
        if (session.getAttributes().get("adviceId") == null) {
            Advice advice = adviceService.startAdviceSession(userId, childId, session.getId());
            session.getAttributes().put("adviceId", advice.getId());
        }

        sessionChunks.put(session.getId(), new ArrayList<>());
        log.info("[WS][OPEN] id={} userId={} childId={}", session.getId(), userId, childId);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        List<byte[]> list = sessionChunks.computeIfAbsent(session.getId(), k -> new ArrayList<>());
        var buf = message.getPayload().slice();
        var bytes = new byte[buf.remaining()];
        buf.get(bytes);
        list.add(bytes);
        log.debug("[WS][BIN] 바이너리 수신: id={} chunkSize={}B totalChunks={}",
                session.getId(), bytes.length, list.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String msg = message.getPayload();
        log.info("[WS][TXT] 텍스트 수신: id={} msg='{}'", session.getId(), msg);
        if ("end".equals(msg)) {
            log.info("[WS][END] 업로드 시작: id={}", session.getId());
            String url = saveAndUpload(session.getId());
            Long adviceId = (Long) session.getAttributes().get("adviceId");
            adviceService.finishAdviceSession(adviceId, url, COMPLETED);
            if (url != null) {
                log.info("[WS][END] 업로드 완료: id={} url={}", session.getId(), url);
            } else {
                log.warn("[WS][END] 업로드 실패 또는 업로드할 청크 없음: id={}", session.getId());
            }
        }
    }

    private String saveAndUpload(String sessionId) {
        List<byte[]> chunks = sessionChunks.get(sessionId);
        int chunkCount = chunks != null ? chunks.size() : 0;
        if (chunks == null || chunks.isEmpty()) {
            log.warn("[WS][SAVE] 저장할 청크 없음: id={}", sessionId);
            return null;
        }

        try {
            log.info("[WS][SAVE] 파일 병합 시작: id={} chunks={}", sessionId, chunkCount);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            long totalBytes = 0;
            for (byte[] chunk : chunks) {
                baos.write(chunk);
                totalBytes += chunk.length;
            }

            log.info("[WS][SAVE] 병합 완료: id={} totalBytes={}B (~{} MB)",
                    sessionId, totalBytes, String.format("%.2f", totalBytes / 1024.0 / 1024.0));

            MultipartFile multipartFile = new MockMultipartFile(
                    "recorded",
                    "recorded_" + sessionId + ".webm",
                    "video/webm",
                    baos.toByteArray()
            );

            log.info("[WS][S3] 업로드 시작: id={} filename={} size={}B",
                    sessionId, multipartFile.getOriginalFilename(), multipartFile.getSize());

            String s3Url = s3Service.uploadFile(multipartFile);

            log.info("[WS][S3] 업로드 성공: id={} url={}", sessionId, s3Url);

            sessionChunks.remove(sessionId);
            log.debug("[WS][CLEANUP] 세션 청크 제거: id={}", sessionId);

            return s3Url;

        } catch (IOException e) {
            log.error("[WS][SAVE] 영상 저장/업로드 실패: id=" + sessionId, e);
            return null;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long adviceId = (Long) session.getAttributes().get("adviceId");
        if (adviceId != null) {
            adviceService.finishAdviceSession(adviceId, /*url=*/null, /*status=*/ABORTED);
        }
        sessionChunks.remove(session.getId());
        log.info("[WS][CLOSE] 세션 종료: id={} code={} reason={}",
                session.getId(), status.getCode(), status.getReason());
    }

    public byte[] getLatestFrame(String sessionId) {
        List<byte[]> chunks = sessionChunks.get(sessionId);
        if (chunks == null || chunks.isEmpty()) {
            log.debug("[WS][FRAME] 프레임 없음: id={}", sessionId);
            return null;
        }
        byte[] last = chunks.get(chunks.size() - 1);
        log.debug("[WS][FRAME] 마지막 프레임 조회: id={} size={}B index={}",
                sessionId, last.length, chunks.size() - 1);
        return last; // 마지막 프레임만 리턴
    }
}

