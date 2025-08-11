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

	@PostMapping("/advice/survey")
	@Operation(summary = "설문지 추가 및 첫질문 제공 API입니다."+"knowAboutChild는 오늘 대화를 통해 자녀에게 궁금한 점\n" + "knowInfo는 오늘 대화에서 참고할 정보\n"
	,description = "상담을 시작하기 전 설문지를 추가해야합니다. 또한 이를 통해서 나온 첫질문을 프론트에서 사용해야합니다."
	)
	public BaseResponse<SurveyResponse> createSurvey(@RequestBody SurveyRequest request){
		return surveyService.createSurvey(request);
	}

}
