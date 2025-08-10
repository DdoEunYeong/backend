package com.unithon.ddoeunyeong.infra.fastapi.stt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unithon.ddoeunyeong.domain.utterance.dto.LastUtteranceResponse;
import com.unithon.ddoeunyeong.domain.utterance.service.UserUtteranceService;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GptResponse;
import com.unithon.ddoeunyeong.infra.gptapi.service.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class STTService {
    // domain GPT에서 STT 서비스 분리
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ai.url}")
    private String AI_URL;

    private final GptService gptService;
    private final UserUtteranceService userUtteranceService;

    public LastUtteranceResponse sendToFastApiAndSaveLastUtterance(MultipartFile audioFile, Long adviceId) throws IOException {
        File tempFile = File.createTempFile("temp-audio", ".m4a");
        audioFile.transferTo(tempFile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(tempFile)); // ✅ FastAPI의 파라미터 이름에 맞춤

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                AI_URL+"/stt",
                requestEntity,
                String.class
        );

        tempFile.delete();

        String sttAnswer = response.getBody();

        // 텍스트 파싱
        JsonNode root = mapper.readTree(sttAnswer);

        // 발화자 구분 없이 합쳐서 텍스트만 merge
        List<String> texts = new ArrayList<>();
        for (JsonNode u : root.path("utterances")) {
            String t = u.path("text").asText(null);
            if (t != null && !t.isBlank()) texts.add(t);
        }
        String mergedText = String.join(" ", texts);

        return userUtteranceService.saveLastUtterance(mergedText, adviceId);
    }

    public GptResponse sendToFastApiAndAskGpt(MultipartFile audioFile, Long adviceId) throws IOException {
        File tempFile = File.createTempFile("temp-audio", ".m4a");
        audioFile.transferTo(tempFile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(tempFile)); // ✅ FastAPI의 파라미터 이름에 맞춤

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                AI_URL+"/stt",
                requestEntity,
                String.class
        );

        tempFile.delete();

        String sttAnswer = response.getBody();

        // 텍스트 파싱
        ObjectMapper om = new ObjectMapper();

        JsonNode root = om.readTree(sttAnswer);

        // 발화자 구분 없이 합쳐서 텍스트만 merge
        List<String> texts = new ArrayList<>();
        for (JsonNode u : root.path("utterances")) {
            String t = u.path("text").asText(null);
            if (t != null && !t.isBlank()) texts.add(t);
        }
        String mergedText = String.join(" ", texts);

        return gptService.askGptAfterSurvey(mergedText,adviceId);
    }
}
