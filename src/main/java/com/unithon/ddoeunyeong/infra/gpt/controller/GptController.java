package com.unithon.ddoeunyeong.infra.gpt.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.unithon.ddoeunyeong.domain.gpt.dto.FirstGptResponse;
import com.unithon.ddoeunyeong.domain.gpt.dto.GptResponse;
import com.unithon.ddoeunyeong.domain.gpt.dto.GptTestResponse;
import com.unithon.ddoeunyeong.domain.gpt.dto.SttRequest;
import com.unithon.ddoeunyeong.domain.gpt.service.GptService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GptController {

	private final GptService gptService;

	@PostMapping("/stt")
	@Operation(summary = "stt 테스트용")
	public GptTestResponse handleStt(@RequestBody SttRequest request) {
		return gptService.askGptTest(request.text());
	}


	@PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "음성을 stt로 변경")
	public BaseResponse<GptResponse> handleAudio(@RequestParam("audio") MultipartFile audioFile,@RequestParam Long childId) throws IOException {
		return BaseResponse.<GptResponse>builder().isSuccess(true).code(200).message("꼬리질문이 생성되었습니다.").data(gptService.sendToFastApi(audioFile,childId)).build();
	}

	@PostMapping(value = "/doll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "GPT에게 배경 제거를 맡기는 API입니다.\n" +"일단은 사용 X")
	public BaseResponse<String> makeDoll(@RequestParam("doll") MultipartFile file, @RequestParam Long childId){
		return gptService.makeDoll(childId,file);
	}

}