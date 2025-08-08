package com.unithon.ddoeunyeong.domain.gpt.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unithon.ddoeunyeong.domain.gpt.dto.GptResponse;
import com.unithon.ddoeunyeong.domain.gpt.dto.SttRequest;
import com.unithon.ddoeunyeong.domain.gpt.service.GptService;

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
}