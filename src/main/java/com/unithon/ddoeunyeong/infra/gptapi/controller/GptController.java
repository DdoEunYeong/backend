package com.unithon.ddoeunyeong.infra.gptapi.controller;

import java.io.IOException;

import com.unithon.ddoeunyeong.infra.fastapi.stt.STTService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.unithon.ddoeunyeong.infra.gptapi.dto.GptResponse;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GptTestResponse;
import com.unithon.ddoeunyeong.infra.gptapi.dto.SttRequest;
import com.unithon.ddoeunyeong.infra.gptapi.service.GptService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GptController {

	private final GptService gptService;

	private final STTService sttService;

	@PostMapping("/stt/test")
	@Operation(summary = "stt 테스트용")
	public GptTestResponse handleStt(@RequestBody SttRequest request) {
		return gptService.askGptTest(request.text());
	}

	@PostMapping(value = "/stt/answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "음성을 stt로 변경")
	public BaseResponse<GptResponse> handleAudio(@RequestParam("audio") MultipartFile audioFile,@RequestParam Long adviceId) throws IOException {
		return BaseResponse.<GptResponse>builder().isSuccess(true).code(200).message("꼬리질문이 생성되었습니다.").data(sttService.sendToFastApiAndAskGpt(audioFile,adviceId)).build();
	}

}