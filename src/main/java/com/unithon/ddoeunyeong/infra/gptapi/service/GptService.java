package com.unithon.ddoeunyeong.infra.gptapi.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.repository.AdviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.infra.gptapi.dto.FirstGPTRequest;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GptRequest;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GptResponse;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GptTestResponse;
import com.unithon.ddoeunyeong.domain.survey.dto.SurveyDto;
import com.unithon.ddoeunyeong.domain.survey.entity.Survey;
import com.unithon.ddoeunyeong.domain.survey.repository.SurveyRepository;
import com.unithon.ddoeunyeong.domain.child.dto.ChildProfile;
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
	private final AdviceRepository adviceRepository;
	private final SurveyRepository surveyRepository;
	private final UserUtteranceRepository userUtteranceRepository;

	private final ObjectMapper mapper = new ObjectMapper();


	@Value("${gpt.api-key}")
	private String API_KEY;

	@Value("${ai.url}")
	private String AI_URL;

	public String makeFirstQuestionWithSurvey(Survey survey){

		// 1) GPT에게 넘길 사전 정보
		// child 사전 정보
		Child child = survey.getAdvice().getChild();
		ChildProfile childProfile = ChildProfile.builder()
				.name(child.getName())
				.age(child.getAge())
				.characterType(child.getCharacterType())
				.build();

		// 사전 조사 정보
		SurveyDto surveyDto = new SurveyDto(survey.getTemp());

		// 사전 정보 하나에 담아줌
		FirstGPTRequest firstGPTRequest = new FirstGPTRequest(childProfile,surveyDto);

		// 2) 직렬화 및 예외처리
		String userMessageJson;
		try {
			userMessageJson = mapper.writeValueAsString(firstGPTRequest);
		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorCode.JSON_SERIALIZE_FAIL);
		}

		// 3) 헤더 세팅
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", API_KEY);

		// 4) system 메시지 설정
		String systemPrompt = """
        	당신은 심리 상담 AI입니다. 그리고 어린아이를 대상으로 말한다는 것을 고려해주세요.
        	사용자에 대한 정보가 입력되면 이에 대한 첫번째 질문을 생성해주세요.
    """;

		Map<String, Object> body = new HashMap<>();
		body.put("model", "gpt-4o");
		body.put("messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", userMessageJson)
		));

		// 5) GPT 통신
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		Map<String, Object> responseBody;

		ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);

		List<Map<String, Object>> choices = (List<Map<String, Object>>)response.getBody().get("choices");
		Map<String, String> message = (Map<String, String>)choices.get(0).get("message");
		String content = message.get("content");

		// 6) 생성된 질문 미리 UserUtterance에 저장해둠
		Advice advice = survey.getAdvice();
		UserUtterance newUserUtter = UserUtterance.builder()
				.advice(advice)
				.question(content.trim())
				.build();
		userUtteranceRepository.save(newUserUtter);

		return content.trim();
	}

	public GptResponse askGptAfterSurvey(String userText, Long adviceId) throws IOException {

		// *현재 발화를 이전의 UserUtterance에 담아서 저장*
		UserUtterance priorUserUtterance = userUtteranceRepository.findTopByAdviceIdOrderByCreatedAtDesc(adviceId)
				.orElseThrow(() -> new CustomException(ErrorCode.NO_ADVICE));
		priorUserUtterance.updateUtterance(userText);

		// 1) GPT에게 넘길 사전 정보
		Advice advice = adviceRepository.findById(adviceId)
			.orElseThrow(null);

		// Child의 정보
		Child child = advice.getChild();
		ChildProfile childProfile = ChildProfile.builder()
			.age(child.getAge())
			.characterType(child.getCharacterType())
			.name(child.getName())
			.build();

		// 발화 정보 String 처리 하여 사전 정보로 넣어줄 준비
		List<String> rawUserUtterances = userUtteranceRepository.findTop5ByAdviceIdOrderByCreatedAtDesc(adviceId).stream()
				.map(UserUtterance::getUtterance)
				.toList();

		// 사전 조사 정보
		Survey survey = surveyRepository.findByAdviceId(adviceId).orElseThrow(()->new CustomException(ErrorCode.NO_SURVEY));
		SurveyDto surveyDto = new SurveyDto(survey.getTemp());

		// 사전 정보들 하나의 DTO에 담아줌
		GptRequest gptRequest = new GptRequest(childProfile, rawUserUtterances, surveyDto, userText);

		// 2) 사전 정보 직렬화 및 예외 처리
		String userMessageJson;
		try {
			userMessageJson = mapper.writeValueAsString(gptRequest);
		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorCode.JSON_SERIALIZE_FAIL);
		}

		// 3) 헤더 세팅
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", API_KEY);

		// 4) system 메시지 설정
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

		// 5) GPT 통신
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

		// 6) 안전 파싱 (choices[0].message.content) + JSON만 추출
		String content = extractContentSafely(responseBody);
		String jsonOnly = extractFirstJsonObject(content);

		// 7) 생성된 질문 미리 UserUtterance에 저장해둠
		JsonNode root = mapper.readTree(jsonOnly);

		// 값이 없거나 타입이 다르면 빈 문자열 반환
		String followUpQuestion = root.path("followUpQuestion").asText("");
		UserUtterance newUserUtter = UserUtterance.builder()
				.advice(advice)
				.question(followUpQuestion)
				.build();
		userUtteranceRepository.save(newUserUtter);

		try {
			return mapper.readValue(jsonOnly, GptResponse.class);
		} catch (Exception e) {
			throw new CustomException(ErrorCode.OPENAI_PARSE_FAIL);
		}
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