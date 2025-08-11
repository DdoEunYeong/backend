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
	@Operation(summary = "음성을 stt로 변경",description = "아이가 첫 설문지를 생성하고 나서 상담을 진행할 때 사용하는 API입니다. 아이가 말을 한 것에 대한 음성 파일을 입력받아서 해당 음성파일을 STT로 변경하고, 꼬리질문과 감정등의 결과값을 생성하는 API입니다.")
	public BaseResponse<GptResponse> handleAudio(@RequestParam("audio") MultipartFile audioFile,@RequestParam Long adviceId) throws IOException {
		return BaseResponse.<GptResponse>builder().isSuccess(true).code(200).message("꼬리질문이 생성되었습니다.").data(sttService.sendToFastApiAndAskGpt(audioFile,adviceId)).build();
	}

	@PostMapping(value = "/doll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "GPT에게 배경 제거를 맡기는 API입니다.\n" +"일단은 사용 X")
	public BaseResponse<String> makeDoll(@RequestParam("doll") MultipartFile file, @RequestParam Long childId){
		return gptService.makeDoll(childId,file);
	}

}