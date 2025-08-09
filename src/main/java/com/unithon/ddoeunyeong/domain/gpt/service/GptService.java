package com.unithon.ddoeunyeong.domain.gpt.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.domain.gpt.dto.FirstGPTRequest;
import com.unithon.ddoeunyeong.domain.gpt.dto.GptRequest;
import com.unithon.ddoeunyeong.domain.gpt.dto.GptResponse;
import com.unithon.ddoeunyeong.domain.gpt.dto.GptTestResponse;
import com.unithon.ddoeunyeong.domain.survey.dto.SurveyDto;
import com.unithon.ddoeunyeong.domain.survey.entity.Survey;
import com.unithon.ddoeunyeong.domain.survey.repository.SurveyRepository;
import com.unithon.ddoeunyeong.domain.child.dto.ChildProfile;
import com.unithon.ddoeunyeong.domain.user.repository.UserRepository;
import com.unithon.ddoeunyeong.domain.utterance.entity.UserUtterance;
import com.unithon.ddoeunyeong.domain.utterance.repository.UserUtteranceRepository;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GptService {

	private final RestTemplate restTemplate = new RestTemplate();

	private static final String API_URL = "https://api.openai.com/v1/chat/completions";
	private final UserRepository userRepository;
	private final ChildRepository childRepository;
	private final SurveyRepository surveyRepository;
	private final UserUtteranceRepository userUtteranceRepository;

	private final ObjectMapper mapper = new ObjectMapper();


	@Value("${gpt.api-key}")
	private String API_KEY;

	@Value("${ai.url}")
	private String AI_URL;


	public GptResponse askGpt(String userText, Long childId) {


		Child child = childRepository.findById(childId)
			.orElseThrow(()-> new CustomException(ErrorCode.NO_CHILD));

		ChildProfile childProfile = ChildProfile.builder()
			.age(child.getAge())
			.characterType(child.getCharacterType())
			.name(child.getName())
			.build();

		Survey survey = surveyRepository.findTopByChildIdOrderByCreatedAtDesc(childId)
			.orElseThrow(()->new CustomException(ErrorCode.NO_SURVEY));

		SurveyDto surveyDto = new SurveyDto(survey.getTemp());


		List<String> userUtterances = userUtteranceRepository.findTop5ByChildIdOrderByCreatedAtDesc(childId).stream()
			.map(u -> u.getUtterance())
			.toList();


		//새로 받은 것에 대한 입력
		UserUtterance newUserUtter = UserUtterance.builder()
			.utterance(userText)
			.child(child)
			.build();

		userUtteranceRepository.save(newUserUtter);

		GptRequest gptRequest = new GptRequest(childProfile, userUtterances, surveyDto, userText);

		// 4) 직렬화 예외 처리
		String userMessageJson;
		try {
			userMessageJson = mapper.writeValueAsString(gptRequest);
		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorCode.JSON_SERIALIZE_FAIL);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", API_KEY);

		// system 메시지 설정
		String systemPrompt = """
        당신은 감정 분석과 꼬리질문을 수행하는 감성 상담 AI입니다. 그리고 어린아이를 대상으로 말한다는 것을 고려해주세요.
        사용자 정보와 발화 이력이 주어지면, 다음과 같은 JSON 응답을 반환하세요:
        {
          "emotion": "...",
          "summary": "...",
          "followUpQuestion": "..."
        }
    """;

		Map<String, Object> body = new HashMap<>();
		body.put("model", "gpt-4o");
		body.put("messages", List.of(
			Map.of("role", "system", "content", systemPrompt),
			Map.of("role", "user", "content", userMessageJson)
		));

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		Map<String, Object> responseBody;
		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new CustomException(ErrorCode.OPENAI_HTTP_ERROR);
			}
			responseBody = response.getBody();
			if (responseBody == null) {
				throw new CustomException(ErrorCode.OPENAI_EMPTY_BODY);
			}
		} catch (RestClientException e) {
			throw new CustomException(ErrorCode.OPENAI_COMM_FAIL);
		}

		// 5) 안전 파싱 (choices[0].message.content) + JSON만 추출
		String content = extractContentSafely(responseBody);
		String jsonOnly = extractFirstJsonObject(content);

		try {
			return mapper.readValue(jsonOnly, GptResponse.class);
		} catch (Exception e) {
			throw new CustomException(ErrorCode.OPENAI_PARSE_FAIL);
		}
	}



	public GptResponse sendToFastApi(MultipartFile audioFile,Long childId) throws IOException {
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

		return askGpt(sttAnswer,childId);
	}

	@SuppressWarnings("unchecked")
	private String extractContentSafely(Map<String, Object> responseBody) {
		Object choicesObj = responseBody.get("choices");
		if (!(choicesObj instanceof List<?> choices) || choices.isEmpty())
			throw new CustomException(ErrorCode.OPENAI_NO_CONTENT);

		Object first = choices.get(0);
		if (!(first instanceof Map<?, ?> firstMap))
			throw new CustomException(ErrorCode.OPENAI_NO_CONTENT);

		Object messageObj = firstMap.get("message");
		if (messageObj instanceof Map<?, ?> messageMap) {
			Object contentObj = messageMap.get("content");
			if (contentObj instanceof String s) return s;
		}
		Object textObj = firstMap.get("text");
		if (textObj instanceof String s) return s;

		throw new CustomException(ErrorCode.OPENAI_NO_CONTENT);
	}

	/** ```json ... ``` 형태여도 첫 번째 JSON 오브젝트만 추출 */
	private String extractFirstJsonObject(String content) {
		if (content == null || content.isBlank())
			throw new CustomException(ErrorCode.OPENAI_NO_CONTENT);

		String cleaned = content
			.replaceAll("(?s)```json\\s*", "")
			.replaceAll("(?s)```\\s*", "")
			.trim();

		int start = cleaned.indexOf('{');
		int end = findMatchingBrace(cleaned, start);
		if (start >= 0 && end > start) return cleaned.substring(start, end + 1);
		return cleaned; // 순수 JSON일 수도 있음
	}

	private int findMatchingBrace(String s, int start) {
		if (start < 0) return -1;
		int depth = 0;
		for (int i = start; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '{') depth++;
			else if (c == '}') {
				depth--;
				if (depth == 0) return i;
			}
		}
		return -1;
	}

	public GptTestResponse askGptTest(String userText) {

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

		return new GptTestResponse(content.trim());
	}


	public String makeFirstQuestion(Survey survey){

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", API_KEY);

		// system 메시지 설정
		String systemPrompt = """
        당신은 심리 상담 AI입니다. 그리고 어린아이를 대상으로 말한다는 것을 고려해주세요.
        사용자에 대한 정보가 입력되면 이에 대한 첫번째 질문을 생성해주세요.
    """;

		SurveyDto surveyDto = new SurveyDto(survey.getTemp());
		ChildProfile childProfile = ChildProfile.builder()
			.name(survey.getChild().getName())
			.age(survey.getChild().getAge())
			.characterType(survey.getChild().getCharacterType())
			.build();

		FirstGPTRequest firstGPTRequest = new FirstGPTRequest(childProfile,surveyDto);

		String userMessageJson;
		try {
			userMessageJson = mapper.writeValueAsString(firstGPTRequest);
		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorCode.JSON_SERIALIZE_FAIL);
		}

		Map<String, Object> body = new HashMap<>();
		body.put("model", "gpt-4o");
		body.put("messages", List.of(
			Map.of("role", "system", "content", systemPrompt),
			Map.of("role", "user", "content", userMessageJson)
		));

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		Map<String, Object> responseBody;

		ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);

		List<Map<String, Object>> choices = (List<Map<String, Object>>)response.getBody().get("choices");
		Map<String, String> message = (Map<String, String>)choices.get(0).get("message");
		String content = message.get("content");

		return content.trim();
	}

	// public GptResponse sendToFastApiTest(MultipartFile audioFile) throws IOException {
	// 	File tempFile = File.createTempFile("temp-audio", ".m4a");
	// 	audioFile.transferTo(tempFile);
	//
	// 	HttpHeaders headers = new HttpHeaders();
	// 	headers.setContentType(MediaType.MULTIPART_FORM_DATA);
	//
	// 	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	// 	body.add("file", new FileSystemResource(tempFile)); // ✅ FastAPI의 파라미터 이름에 맞춤
	//
	// 	HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
	//
	// 	ResponseEntity<String> response = restTemplate.postForEntity(
	// 		AI_URL+"/stt",
	// 		requestEntity,
	// 		String.class
	// 	);
	//
	// 	tempFile.delete();
	//
	// 	String sttAnswer = response.getBody();
	//
	// 	return askGpt(sttAnswer);
	// }


}