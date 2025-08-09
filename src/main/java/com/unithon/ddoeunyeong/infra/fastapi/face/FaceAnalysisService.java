package com.unithon.ddoeunyeong.infra.fastapi.face;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class FaceAnalysisService {

    private final WebClient webClient;

    @Value("${ai.url}")
    private String aiServerUrl;

    public void sendToFastAPIServer(byte[] imageBytes) {
        ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "frame.jpg";
            }
        };

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", imageResource);

        webClient.post()
                .uri(aiServerUrl + "/face-analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> System.out.println("❌ 감정 분석 실패: " + e.getMessage()))
                .subscribe(result -> System.out.println("🎯 감정 분석 결과: " + result));
    }
}
