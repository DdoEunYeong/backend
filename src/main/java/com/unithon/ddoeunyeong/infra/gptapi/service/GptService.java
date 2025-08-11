package com.unithon.ddoeunyeong.infra.gptapi.service;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.repository.AdviceRepository;
import com.unithon.ddoeunyeong.domain.advice.service.AdviceService;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.domain.utterance.dto.QuestionAndAnswer;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GptFinalRequest;
import com.unithon.ddoeunyeong.infra.gptapi.dto.AdivceReportResponse;
import com.unithon.ddoeunyeong.infra.s3.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.infra.gptapi.dto.FirstGPTRequest;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GptRequest;
import com.unithon.ddoeunyeong.domain.utterance.dto.ProcessUtteranceResponse;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GptService {

	private final ChildRepository childRepository;
	private final AdviceRepository adviceRepository;
	private final SurveyRepository surveyRepository;
	private final UserUtteranceRepository userUtteranceRepository;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper mapper = new ObjectMapper();
	private final S3Service s3Service;

	private final AdviceService adviceService;
	private final WebClient openAiClient;

	private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
	private static final String OPENAI_IMAGE_EDIT_API_URL = "https://api.openai.com/v1/images/edits";
	private static final String CHAT_COMPLETIONS_URI = "/v1/chat/completions";

	@Value("${gpt.api-key}")
	private String OPENAI_API_KEY;

	public String makeFirstQuestionWithSurvey(Survey survey){
		Child child = survey.getAdvice().getChild();
		ChildProfile childProfile = ChildProfile.builder()
			.name(child.getName())
			.age(child.getAge())
			.characterType(child.getCharacterType())
			.build();

		SurveyDto surveyDto = new SurveyDto(survey.getKnowAboutChild(), survey.getKnowInfo());
		FirstGPTRequest firstGPTRequest = new FirstGPTRequest(childProfile, surveyDto);

		String userMessageJson;
		try {
			userMessageJson = mapper.writeValueAsString(firstGPTRequest);
		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorCode.JSON_SERIALIZE_FAIL);
		}

		// 3) 헤더 세팅
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", OPENAI_API_KEY);

		// 4) system 메시지 설정
		String systemPrompt = """
        당신은 심리 상담 AI입니다. 그리고 어린아이를 대상으로 말한다는 것을 고려해주세요.또한 민감한 주제에 대한 질문은 피해주세요.
		또한 제공하는 값중에서 knowAboutChild는 오늘 답변을 듣기 궁금한 질문입니다. 그리고 getKnowInfo는 오늘 대화에서 참조해줬으면 좋겠는 사항입니다.
		그리고 질문 형태는 하나로만 나오게끔 해주세요. 사용자에 대한 정보가 입력되면 이를 반영해서 첫번째 질문을 생성해주세요.
    """;

		Map<String, Object> body = new HashMap<>();
		body.put("model", "gpt-4o");
		body.put("messages", List.of(
			Map.of("role", "system", "content", systemPrompt),
			Map.of("role", "user", "content", userMessageJson)
		));

		Map<String, Object> responseBody;
		try {
			responseBody = openAiClient.post()
				.uri(CHAT_COMPLETIONS_URI)
				.bodyValue(body)
				.retrieve()
				.onStatus(s -> !s.is2xxSuccessful(),
					resp -> resp.bodyToMono(String.class)
						.defaultIfEmpty("")
						.map(e -> new CustomException(ErrorCode.OPENAI_HTTP_ERROR)))
				.bodyToMono(Map.class)
				.block();
		} catch (Exception e) {
			throw new CustomException(ErrorCode.OPENAI_COMM_FAIL);
		}

		String content = extractContentSafely(responseBody);

		Advice advice = survey.getAdvice();
		UserUtterance newUserUtter = UserUtterance.builder()
			.advice(advice)
			.question(content.trim())
			.build();
		userUtteranceRepository.save(newUserUtter);

		return content.trim();
	}

	public ProcessUtteranceResponse askGptAfterSurvey(String userText, Long adviceId) throws IOException {
		long t0 = System.currentTimeMillis();

		// 1) 사전 정보 조회
		// 상담 객체
		Advice advice = adviceRepository.findById(adviceId)
				.orElseThrow(() -> {
					log.error("[GPT][CTX] Advice 조회 실패: adviceId={}", adviceId);
					return new CustomException(ErrorCode.NO_ADVICE);
				});

		// 아이 객체
		Child child = advice.getChild();

		// 아이 프로필 정보
		ChildProfile childProfile = ChildProfile.builder()
				.age(child.getAge())
				.characterType(child.getCharacterType())
				.name(child.getName())
				.build();

		// 최근 질문 및 응답 5개 정보
		List<QuestionAndAnswer> rawUserUtterances = userUtteranceRepository
				.findTop5ByAdviceIdOrderByCreatedAtDesc(adviceId).stream()
				.map(u -> new QuestionAndAnswer(u.getQuestion(), u.getUtterance()))
				.toList();

		// 사전 조사 정보
		Survey survey = surveyRepository.findByAdviceId(adviceId)
				.orElseThrow(() -> {
					return new CustomException(ErrorCode.NO_SURVEY);
				});
		SurveyDto surveyDto = new SurveyDto(survey.getKnowAboutChild(), survey.getKnowInfo());

		GptRequest gptRequest = new GptRequest(childProfile, rawUserUtterances, surveyDto, userText);

		// 4) 직렬화
		String userMessageJson;
		try {
			userMessageJson = mapper.writeValueAsString(gptRequest);
		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorCode.JSON_SERIALIZE_FAIL);
		}

		// 5) system 메시지
		String systemPrompt = """
        당신은 감정 분석과 꼬리질문을 수행하는 감성 상담 AI입니다. 그리고 어린아이를 대상으로 말한다는 것을 고려해주세요.
        사용자 정보와 발화 이력, 이전 질문이 주어지면, 다음과 같은 JSON 응답을 반환하세요:
        이때 emotion은 angry, disgust, fear, happy, sad, surprise, neutral 중에 가장 연관될 것을 선택해주세요.
		또한 제공하는 값중에서 knowAboutChild는 오늘 답변을 듣기 궁금한 질문입니다. 그리고 getKnowInfo는 오늘 대화에서 참조해줬으면 좋겠는 사항입니다.
        또한 이전 질문을 고려하여 반복적인 내용이 반영되는 질문이 나오지 않도록 해주세요. 그리고 질문 형태는 하나로만 나오게끔 해주세요.
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

		// 7) OpenAI 호출
		Map<String, Object> responseBody;
		try {
			responseBody = openAiClient.post()
					.uri(CHAT_COMPLETIONS_URI)
					.bodyValue(body)
					.retrieve()
					.onStatus(s -> !s.is2xxSuccessful(),
							resp -> resp.bodyToMono(String.class)
									.defaultIfEmpty("")
									.map(e-> new CustomException(ErrorCode.OPENAI_HTTP_ERROR)))
					.bodyToMono(Map.class)
					.block();

			if (responseBody == null) throw new CustomException(ErrorCode.OPENAI_EMPTY_BODY);
		} catch (Exception e) {
			throw new CustomException(ErrorCode.OPENAI_COMM_FAIL);
		}

		// 8) JSON 추출
		String content = extractContentSafely(responseBody);
		String jsonOnly = extractFirstJsonObject(content);

		// 9) followUpQuestion 저장
		JsonNode root = mapper.readTree(jsonOnly);
		String followUpQuestion = root.path("followUpQuestion").asText("");

		UserUtterance newUserUtter = UserUtterance.builder()
			.advice(advice)
			.question(followUpQuestion)
			.build();
		userUtteranceRepository.save(newUserUtter);
		log.info("[GPT][FOLLOWUP] 질문 저장: adviceId={} questionLen={}", adviceId, (followUpQuestion == null ? 0 : followUpQuestion.length()));

		try {
			return mapper.readValue(jsonOnly, ProcessUtteranceResponse.class);
		} catch (Exception e) {
			throw new CustomException(ErrorCode.OPENAI_PARSE_FAIL);
		}
	}

	public AdivceReportResponse askGptMakeFinalReport(Long adviceId) throws IOException {

		Advice advice = adviceRepository.findById(adviceId).orElseThrow(() -> new CustomException(ErrorCode.NO_ADVICE));

		Child child = advice.getChild();

		ChildProfile childProfile = ChildProfile.builder()
				.age(child.getAge())
				.characterType(child.getCharacterType())
				.name(child.getName())
				.build();

		List<QuestionAndAnswer> rawUserUtterances = userUtteranceRepository
				.findTop5ByAdviceIdOrderByCreatedAtDesc(adviceId).stream()
				.map(u -> new QuestionAndAnswer(u.getQuestion(), u.getUtterance()))
				.toList();

		Survey survey = surveyRepository.findByAdviceId(adviceId)
				.orElseThrow(() -> new CustomException(ErrorCode.NO_SURVEY));

		SurveyDto surveyDto = new SurveyDto(survey.getKnowAboutChild(), survey.getKnowInfo());

		GptFinalRequest gptRequest = new GptFinalRequest(childProfile, rawUserUtterances, surveyDto);

		String userMessageJson;
		try {
			userMessageJson = mapper.writeValueAsString(gptRequest);
		} catch (JsonProcessingException e) {
			throw new CustomException(ErrorCode.JSON_SERIALIZE_FAIL);
		}
		String systemPrompt = """
			당신은 감정 분석을 수행하는 감성 상담 AI입니다.
			대상은 어린아이이며, 친절하고 이해하기 쉬운 말투를 사용해야 합니다.
			
			입력으로 다음 정보가 함께 제공됩니다:
			- knowAboutChild: 오늘 반드시 알고 싶은 핵심 질문(예: "학교에서 가장 재밌었던 일은?")
			- getKnowInfo: 오늘 대화에서 참고해야 할 맥락/주의사항(예: "새로운 반에 전학 옴")
			
			다음 두 가지 지표를 반드시 분석하여 0~100점으로 반환하세요.
			점수는 아래 규칙에 **엄격히** 따르되, 긍정적 신호가 있으면 짜지 않게 반영하세요.
			
			1) 사회참조 점수 (socialReferenceScore)
			- 의미: 대화 중 ‘타인’을 언급한 빈도를 100점 만점으로 환산
			- 카운트 규칙: '친구', '선생님', '반 친구', '엄마', '아빠', '형', '누나', '동생', '사촌', '이모', '삼촌' 등 타인 지칭이 등장할 때마다 +1
			- 점수화(캡 규칙):
				0회=20, 1회=40, 2회=60, 3회=80, 4회 이상=100
			  (동일 문장 내 여러 타인 단어가 있으면 각각 카운트)
			
			2) 협력·배려 점수 (cooperationKindnessScore)
			- 의미: 협력/배려 의도가 담긴 발화 빈도를 100점 만점으로 환산
			- 카운트 규칙 키워드 예: '같이', '함께', '도와줄게', '도와줘', '도와주다', '고마워', '미안해', '괜찮아', '잘했어', '수고했어'
			- 점수화(캡 규칙):
				0회=20, 1회=40, 2회=60, 3회=80, 4회 이상=100
			
			출력 필드 (반드시 모두 포함):
			- socialReferenceScore: 0~100 정수
			- cooperationKindnessScore: 0~100 정수
			- summary: 전체 대화 요지를 부모님에게 제공해주는 것으로 2문장으로 요약
			- coreQuestion: 지금까지 맥락에서 가장 중요한 집중 질문 1개 (가능하면 knowAboutChild를 반영)
			- childAnswer: knowAboutChild에 대한 아이의 실제 발화 기반 답변. 아이의 발화에서 근거가 명확할 때만 작성.
			  근거가 없으면 "해당 주제에 대해서 말하지 않았어요."로 둔다(추측 금지).
			
			중요 지침:
			- getKnowInfo는 summary와 coreQuestion의 어투/맥락 반영에만 사용한다(점수엔 직접 가중치 주지 말 것).
			- 점수 계산은 오직 카운트 기반 규칙으로 일관되게 수행한다.
			- 출력은 JSON 객체 **한 개만** 반환하고, JSON 외 텍스트/주석/설명은 절대 포함하지 말 것.
			- JSON 키 이름과 자료형을 정확히 지킬 것. 불필요한 필드 추가 금지.
			
			출력 예시:
			{
			  "socialReferenceScore": 80,
			  "cooperationKindnessScore": 100,
			  "summary": "오늘 너는 친구와 선생님 이야기를 들려줬어. 같이 해 보자는 마음과 고마운 마음도 잘 표현했구나!",
			  "coreQuestion": "오늘 가장 같이 하고 싶었던 놀이는 뭐였어?",
			  "childAnswer": "블록 놀이를 친구랑 같이 하고 싶다고 했어."
			}
			""";

		Map<String, Object> body = new HashMap<>();
		body.put("model", "gpt-4o");
		body.put("messages", List.of(
				Map.of("role", "system", "content", systemPrompt),
				Map.of("role", "user", "content", userMessageJson)
		));

		Map<String, Object> responseBody;
		try {
			responseBody = openAiClient.post()
					.uri(CHAT_COMPLETIONS_URI)
					.bodyValue(body)
					.retrieve()
					.onStatus(s -> !s.is2xxSuccessful(),
							resp -> resp.bodyToMono(String.class)
									.defaultIfEmpty("")
									.map(e-> new CustomException(ErrorCode.OPENAI_HTTP_ERROR)))
					.bodyToMono(Map.class)
					.block();

			if (responseBody == null) throw new CustomException(ErrorCode.OPENAI_EMPTY_BODY);
		} catch (Exception e) {
			throw new CustomException(ErrorCode.OPENAI_COMM_FAIL);
		}

		String content = extractContentSafely(responseBody);
		String jsonOnly = extractFirstJsonObject(content);
		if (jsonOnly == null || jsonOnly.isBlank()) {
			throw new CustomException(ErrorCode.OPENAI_PARSE_FAIL);
		}

		try {

			AdivceReportResponse gpt = mapper.readValue(jsonOnly, AdivceReportResponse.class);

			Long socialScore = gpt.socialReferenceScore() == null ? 20 : Math.max(0, Math.min(100, gpt.socialReferenceScore()));
			Long coopScore   = gpt.cooperationKindnessScore() == null ? 20 : Math.max(0, Math.min(100, gpt.cooperationKindnessScore()));
			String summary  = gpt.summary() == null ? "" : gpt.summary();
			String coreQ    = gpt.coreQuestion() == null ? "" : gpt.coreQuestion();
			String childAns = gpt.childAnswer() == null ? "" : gpt.childAnswer();

			List<UserUtterance> utteranceList = userUtteranceRepository.findAllByAdviceId(adviceId);
			int emotionScore = adviceService.calculateEmotionDiversityScore(utteranceList);
			
			// 4) Advice 엔티티 업데이트 후 저장
			advice.updateAnalysisResult(socialScore, coopScore, summary,coreQ,childAns, emotionScore);


			adviceRepository.save(advice);


			return gpt;

		} catch (IOException e) {
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

	public BaseResponse<String> makeDoll(Long childId, MultipartFile file) {

		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new CustomException(ErrorCode.NO_CHILD));

		// 1) 파일 파트
		ByteArrayResource filePart = toResource(file);

		HttpHeaders fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.parseMediaType(
			file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
		// Content-Disposition 명시(파일명 포함)
		fileHeaders.setContentDisposition(
			ContentDisposition.builder("form-data")
				.name("image")
				.filename(filePart.getFilename())
				.build()
		);
		HttpEntity<ByteArrayResource> imageEntity = new HttpEntity<>(filePart, fileHeaders);

		// 2) 멀티파트 본문
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("model", "gpt-image-1");
		body.add("image", imageEntity);
		body.add("prompt",
			"Replace the background with complete transparency. " +
				"Keep only the main subject (person/object) with precise edges. " +
				"Make the background fully transparent PNG format.");
		body.add("response_format", "b64_json");
		body.add("size", "1024x1024");

		// 3) 헤더
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.setBearerAuth(OPENAI_API_KEY == null ? "" : OPENAI_API_KEY);

		HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);

		// 4) 호출 + 에러 처리
		Map<String, Object> res;
		try {
			ResponseEntity<Map> resp =
				restTemplate.postForEntity(OPENAI_IMAGE_EDIT_API_URL, req, Map.class);

			res = resp.getBody();
			if (res == null) throw new CustomException(ErrorCode.OPENAI_EMPTY_BODY);

			// OpenAI 에러 바디 처리
			if (res.containsKey("error") && res.get("error") instanceof Map<?, ?> errMap) {
				Object m = errMap.get("message");
				String msg = (m instanceof String s) ? s : "OpenAI error";
				throw new CustomException(ErrorCode.OPENAI_HTTP_ERROR);
			}

		} catch (org.springframework.web.client.HttpStatusCodeException e) {
			String bodyStr = e.getResponseBodyAsString();
			int code = e.getRawStatusCode();
			throw new CustomException(ErrorCode.OPENAI_HTTP_ERROR);
		} catch (org.springframework.web.client.ResourceAccessException e) {
			throw new CustomException(ErrorCode.OPENAI_COMM_FAIL);
		}

		// 5) data[0].b64_json 파싱
		Object dataObj = res.get("data");
		if (!(dataObj instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> first)) {
			throw new CustomException(ErrorCode.OPENAI_PARSE_FAIL);
		}
		Object b64Obj = ((Map<?, ?>) first).get("b64_json");
		if (!(b64Obj instanceof String b64) || b64.isBlank()) {
			throw new CustomException(ErrorCode.OPENAI_PARSE_FAIL);
		}

		// 6) base64 → PNG 업로드
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(b64);
		} catch (IllegalArgumentException e) {
			throw new CustomException(ErrorCode.OPENAI_PARSE_FAIL);
		}
		String finalName = stripExt(file.getOriginalFilename()) + "-bg-removed.png";
		String url = s3Service.uploadBytes(bytes, "image/png", finalName);

		// 7) 저장
		child.setDollUrl(url);
		childRepository.save(child);

		return BaseResponse.<String>builder()
			.isSuccess(true)
			.code(200)
			.message("배경 제거 이미지가 생성되었습니다.")
			.data(url)
			.build();
	}

	private ByteArrayResource toResource(MultipartFile file) {
		try {
			return new ByteArrayResource(file.getBytes()) {
				@Override public String getFilename() {
					return file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.png";
				}
			};
		} catch (IOException e) {
			throw new RuntimeException("파일 읽기 실패", e);
		}
	}

	private String stripExt(String name) {
		if (name == null) return "image";
		int i = name.lastIndexOf('.');
		return (i > 0) ? name.substring(0, i) : name;
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
		headers.set("Authorization", OPENAI_API_KEY);

		Map<String, Object> body = new HashMap<>();
		body.put("model", "gpt-4o");
		body.put("messages", List.of(
			Map.of("role", "system", "content", "사용자의 발화에 적절한 꼬리 질문을 자연스럽게 만들어줘"),
			Map.of("role", "user", "content", userText)
		));

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_API_URL, request, Map.class);

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