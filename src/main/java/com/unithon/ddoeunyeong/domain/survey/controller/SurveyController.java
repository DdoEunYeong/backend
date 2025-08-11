package com.unithon.ddoeunyeong.domain.survey.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unithon.ddoeunyeong.domain.survey.dto.SurveyResponse;
import com.unithon.ddoeunyeong.domain.survey.dto.SurveyRequest;
import com.unithon.ddoeunyeong.domain.survey.service.SurveyService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SurveyController {

	private final SurveyService surveyService;

	@Operation(summary = "설문지 추가 및 첫질문 제공 API입니다.")
	@PostMapping("/survey")
	public BaseResponse<SurveyResponse> createSurvey(@RequestBody SurveyRequest request){
		return surveyService.createSurvey(request);
	}

}
