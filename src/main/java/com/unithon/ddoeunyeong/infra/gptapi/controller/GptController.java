package com.unithon.ddoeunyeong.infra.gptapi.controller;

import java.io.IOException;

import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GptFinalResponse;
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

	@PostMapping("/stt/test")
	@Operation(summary = "stt 테스트용")
	public GptTestResponse handleStt(@RequestBody SttRequest request) {
		return gptService.askGptTest(request.text());
	}

	@PostMapping(value = "/doll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "GPT에게 배경 제거를 맡기는 API입니다.\n" +"일단은 사용 X")
	public BaseResponse<String> makeDoll(@RequestParam("doll") MultipartFile file, @RequestParam Long childId){
		return gptService.makeDoll(childId,file);
	}

	@GetMapping("/final")
	@Operation(summary = "마지막 상담 결과를 생성하는 API입니다.", description = "상담이 끝나면 해당 API를 호출해서 마지막 결과를 출력해주세요.")
	public BaseResponse<GptFinalResponse> makeFinalAnswer(@RequestParam Long adviceId){
		try {
			return BaseResponse.<GptFinalResponse>builder()
				.code(200)
				.message("상담 결과가 생성되었습니다.")
				.data(gptService.askGptMakeFinalReport(adviceId))
				.isSuccess(true)
				.build();
		} catch (IOException e) {
			throw new CustomException(ErrorCode.FINAL_ERROR);
		}
	}

}