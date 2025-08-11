package com.unithon.ddoeunyeong.infra.gemini.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.infra.gemini.dto.GeminiResponse;
import com.unithon.ddoeunyeong.infra.gemini.service.GeminiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/gemini")
@RequiredArgsConstructor
@Tag(description = "Gemini API",name = "Gemini API")
public class GeminiController {

	private final GeminiService geminiService;


	@PostMapping(value = "/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Image edit API",description = "아이의 인형을 등록해서 배경을 제거하고 이미지 URL을 제공하는 API입니다.")
	public BaseResponse<GeminiResponse> editImage(@RequestParam Long childId, @RequestParam("edit")MultipartFile file){
		return geminiService.editImage(childId,file);
	}

}
