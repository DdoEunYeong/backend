package com.unithon.ddoeunyeong.infra.fastapi.stt_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class STTService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient webClient; // WebClient 빈을 주입 받아 사용

    @Value("${ai.url}")
    private String AI_URL;

    /**
     * 공통: 오디오를 FastAPI로 전송하여 STT 결과(merged text)를 얻는다. (동기, String 반환)
     * WebClient를 사용하되 block()으로 기존 시그니처 유지
     */
    public String sttTranscribeAndMerge(MultipartFile audio) throws IOException {
        try {
            return sttTranscribeAndMergeAsync(audio).block(); // 동기화
        } catch (RuntimeException e) {
            // WebClient 쪽 예외를 IOException으로 변환
            if (e.getCause() instanceof IOException io) throw io;
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 필요 시 사용할 수 있는 비동기 버전
     */
    public Mono<String> sttTranscribeAndMergeAsync(MultipartFile audio) throws IOException {
        // 멀티파트 파트 구성
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(audio.getBytes()) {
            @Override
            public String getFilename() {
                return audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.m4a";
            }
        };
        multipart.add("file", fileResource);

        return webClient.post()
                .uri(AI_URL + "/stt")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart)
                        .with("file", fileResource))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(msg -> Mono.error(new IllegalStateException(
                                        "AI server error %s: %s".formatted(resp.statusCode(), msg)))))
                .bodyToMono(String.class)
                .map(this::mergeTextsFromSttJson);
    }

    private String mergeTextsFromSttJson(String sttAnswer) {
        try {
            JsonNode root = mapper.readTree(sttAnswer);

            List<String> texts = new ArrayList<>();
            for (JsonNode u : root.path("utterances")) {
                String t = u.path("text").asText(null);
                if (t != null && !t.isBlank()) texts.add(t);
            }
            return String.join(" ", texts);
        } catch (Exception e) {
            throw new RuntimeException("STT 응답 파싱 실패", e);
        }
    }
}