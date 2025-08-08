package com.unithon.ddoeunyeong.domain.gpt.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.unithon.ddoeunyeong.domain.gpt.dto.GptResponse;
import com.unithon.ddoeunyeong.domain.gpt.dto.SttRequest;
import com.unithon.ddoeunyeong.domain.gpt.service.GptService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GptController {

	private final GptService gptService;

	@PostMapping("/stt")
	public GptResponse handleStt(@RequestBody SttRequest request) {
		return gptService.askGpt(request.text());
	}


	@PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public BaseResponse<GptResponse> handleAudio(@RequestParam("audio") MultipartFile audioFile) throws IOException {
		return BaseResponse.<GptResponse>builder().isSuccess(true).code(200).message("꼬리질문이 생성되었습니다.").data(gptService.sendToFastApi(audioFile)).build();
	}
}