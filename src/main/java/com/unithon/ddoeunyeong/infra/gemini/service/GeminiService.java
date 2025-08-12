package com.unithon.ddoeunyeong.infra.gemini.service;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import com.unithon.ddoeunyeong.infra.gemini.dto.GeminiResponse;
import com.unithon.ddoeunyeong.infra.gemini.dto.ImageEditResponse;
import com.unithon.ddoeunyeong.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeminiService {

	private final RestTemplate fastApiRestTemplate; // 위 Bean 주입
	private final S3Service s3Service;
	private final ChildRepository childRepository;

	@Value("${ai.url}")
	private  String fastApiUrl;

	private static final String instruction =
			"Remove 100% of the background, leaving only the main subject (person/object) with perfectly sharp and clean edges. " +
					"Instead of making the background transparent, fill the background with solid color #ECF4FF. " +
					"Ensure the fill is uniform with no white, grey, or semi-transparent artifacts. " +
					"Output must be a high-quality PNG image with the background fully replaced by #ECF4FF.";

	@Transactional
	public BaseResponse<GeminiResponse> editImage(Long childId,MultipartFile file){

		Child child = childRepository.findById(childId)
			.orElseThrow(()->new CustomException(ErrorCode.NO_CHILD));

		String imageUrl = editViaFastApiAndUploadToS3(file);

		child.setDollUrl(imageUrl);

		childRepository.save(child);

		return BaseResponse.<GeminiResponse>builder()
			.isSuccess(true)
			.data(new GeminiResponse(imageUrl))
			.message("아이의 인형 이미지 편집 후 저장이 성공하였습니다.")
			.code(201)
			.build();
	}


	private String editViaFastApiAndUploadToS3(MultipartFile file) {
		try {
			// 파일 파트 (파일명 반드시 제공)
			ByteArrayResource filePart = new ByteArrayResource(file.getBytes()) {
				@Override public String getFilename() {
					return (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
						? file.getOriginalFilename() : "upload.bin";
				}
			};

			// 멀티파트 바디 구성
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

			// file 파트
			HttpHeaders fileHeaders = new HttpHeaders();
			fileHeaders.setContentType(MediaType.parseMediaType(
				file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
			HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(filePart, fileHeaders);
			body.add("file", fileEntity);

			// instruction 파트 (Form 텍스트)
			HttpHeaders textHeaders = new HttpHeaders();
			textHeaders.setContentType(MediaType.TEXT_PLAIN);
			HttpEntity<String> instructionEntity = new HttpEntity<>(instruction, textHeaders);
			body.add("instruction", instructionEntity);

			// return_base64 파트 (bool 을 문자열로)
			HttpEntity<String> returnBase64Entity = new HttpEntity<>("true", textHeaders);
			body.add("return_base64", returnBase64Entity);

			// 최종 요청
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);

			ResponseEntity<ImageEditResponse> res = fastApiRestTemplate.postForEntity(
				fastApiUrl+"image-edit", new HttpEntity<>(body, headers), ImageEditResponse.class);

			ImageEditResponse resp = res.getBody();
			if (resp == null || resp.data() == null || resp.data().isBlank()) {
				throw new IllegalStateException("FastAPI 응답이 비어있습니다.");
			}

			// base64 정리 (data: 접두어는 현재 FastAPI 코드상 없음, 그래도 방어)
			String b64 = resp.data().trim();
			int comma = b64.indexOf(',');
			if (comma > 0) b64 = b64.substring(comma + 1);

			byte[] bytes = Base64.getMimeDecoder().decode(b64);

			String mime = (resp.mimeType() == null || resp.mimeType().isBlank())
				? MediaType.APPLICATION_OCTET_STREAM_VALUE : resp.mimeType();
			String ext = switch (mime) {
				case "image/png" -> "png";
				case "image/jpeg", "image/jpg" -> "jpg";
				case "image/webp" -> "webp";
				default -> "bin";
			};

			String baseName = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
				? stripExt(file.getOriginalFilename()) : "input";
			String finalName = baseName + "-edited-" + UUID.randomUUID() + "." + ext;

			// S3 업로드 (임시 저장 없이 바로)
			return s3Service.uploadBytes(bytes, mime, finalName);

		} catch (IOException e) {
			throw new RuntimeException("원본 파일 읽기 실패", e);
		} catch (HttpClientErrorException e) {
			// 404/422 등 서버 응답 본문을 에러에 포함해 디버깅 편하게
			throw new RuntimeException("FastAPI 호출 실패: status=" + e.getStatusCode()
				+ ", body=" + e.getResponseBodyAsString(), e);
		}
	}

	private String stripExt(String name) {
		int i = name.lastIndexOf('.');
		return (i > 0) ? name.substring(0, i) : name;
	}
}