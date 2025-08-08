package com.unithon.ddoeunyeong.domain.gpt.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.unithon.ddoeunyeong.domain.gpt.dto.GptResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GptService {

	private final RestTemplate restTemplate = new RestTemplate();

	private static final String API_URL = "https://api.openai.com/v1/chat/completions";

	@Value("${gpt.api-key}")
	private String API_KEY;

	public GptResponse askGpt(String userText) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", API_KEY);

		Map<String, Object> body = new HashMap<>();
		body.put("model", "gpt-4o");
		body.put("messages", List.of(
			Map.of("role", "system", "content", "사용자의 발화에 적절한 꼬리 질문을 자연스럽게 만들어줘"),
			Map.of("role", "user", "content", userText)
		));

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);

		List<Map<String, Object>> choices = (List<Map<String, Object>>)response.getBody().get("choices");
		Map<String, String> message = (Map<String, String>)choices.get(0).get("message");
		String content = message.get("content");

		return new GptResponse(content.trim());
	}
}