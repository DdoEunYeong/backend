package com.unithon.ddoeunyeong.infra.fastapi.face_service;

import com.unithon.ddoeunyeong.domain.advice.entity.Emotion;
import com.unithon.ddoeunyeong.infra.fastapi.dto.FaceEmotionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FaceAnalysisService {

    private final WebClient webClient;

    @Value("${ai.url}")
    private String AI_URL;

    /** 동기: block()으로 동기화(타임아웃/에러 시 NEUTRAL 폴백) */
    public Emotion faceEmotionAnalyze(MultipartFile image) {
        try {
            return faceEmotionAnalyzeAsync(image)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorReturn(Emotion.NEUTRAL)
                    .block();
        } catch (RuntimeException e) {
            return Emotion.NEUTRAL;
        }
    }

    /** 비동기: JPEG 이미지를 보내고 dominant_emotion을 Emotion enum으로 반환 */
    public Mono<Emotion> faceEmotionAnalyzeAsync(MultipartFile image) {
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        ByteArrayResource filePart = toByteArrayResource(image);

        mb.part("file", filePart)
                .contentType(guessImageType(image.getContentType()));

        return webClient.post()
                .uri(AI_URL + "/face-analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(mb.build()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(msg -> Mono.error(new IllegalStateException(
                                        "AI server error %s: %s".formatted(resp.statusCode(), msg)))))
                .bodyToMono(FaceEmotionResponse.class)
                .mapNotNull(res -> res != null && res.getResult() != null
                        ? res.getResult().getDominantEmotion()
                        : null)
                .map(Emotion::fromString)
                .switchIfEmpty(Mono.just(Emotion.NEUTRAL))
                .onErrorResume(e -> Mono.just(Emotion.NEUTRAL));
    }

    private static MediaType guessImageType(String raw) {
        try {
            if (raw == null || raw.isBlank()) return MediaType.IMAGE_JPEG;
            return MediaType.parseMediaType(raw);
        } catch (Exception e) {
            return MediaType.IMAGE_JPEG;
        }
    }

    private static ByteArrayResource toByteArrayResource(MultipartFile image) {
        try {
            byte[] bytes = image.getBytes();
            return new ByteArrayResource(bytes) {
                @Override public String getFilename() {
                    String name = image.getOriginalFilename();
                    return (name != null && !name.isBlank()) ? name : "image.jpg";
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("이미지 읽기 실패", e);
        }
    }
}
