package com.unithon.ddoeunyeong.domain.survey.service;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.repository.AdviceRepository;
import com.unithon.ddoeunyeong.domain.advice.service.AdviceService;
import com.unithon.ddoeunyeong.domain.survey.dto.SurveyResponse;
import org.springframework.stereotype.Service;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.infra.gptapi.service.GptService;
import com.unithon.ddoeunyeong.domain.survey.dto.SurveyRequest;
import com.unithon.ddoeunyeong.domain.survey.entity.Survey;
import com.unithon.ddoeunyeong.domain.survey.repository.SurveyRepository;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SurveyService {

	private final SurveyRepository surveyRepository;
	private final ChildRepository childRepository;
	private final GptService gptService;
	private final AdviceService adviceService;

	public BaseResponse<SurveyResponse> createSurvey(SurveyRequest request){

		Child child = childRepository.findById(request.childId())
			.orElseThrow(()-> new CustomException(ErrorCode.NO_CHILD));

		// Advice 생성
		Advice advice = adviceService.createAdviceBeforeSurvey(child.getId());

		// Survey 생성, Advice 함께 저장
		Survey survey = Survey.builder()
			.knowInfo(request.knowInfo())
			.knowAboutChild(request.knowAboutChild())
			.advice(advice)
			.build();

		// Survey 저장
		surveyRepository.save(survey);

		// GPT에게 첫 질문 생성 요청
		String firstQuestion = gptService.makeFirstQuestionWithSurvey(survey);

		return BaseResponse.<SurveyResponse>builder()
			.code(201)
			.message("설문지를 작성하였습니다.")
			.data(new SurveyResponse(firstQuestion, advice.getId()))
			.isSuccess(true)
			.build();
	}

}
