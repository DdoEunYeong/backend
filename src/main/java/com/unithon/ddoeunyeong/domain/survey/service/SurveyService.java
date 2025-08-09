package com.unithon.ddoeunyeong.domain.survey.service;

import org.springframework.stereotype.Service;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.domain.gpt.dto.FirstGptResponse;
import com.unithon.ddoeunyeong.domain.gpt.service.GptService;
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

	public BaseResponse<FirstGptResponse> createSurvey(SurveyRequest request){

		Child child = childRepository.findById(request.childId())
			.orElseThrow(()-> new CustomException(ErrorCode.NO_CHILD));

		Survey survey = Survey.builder()
			.temp(request.temp())
			.child(child).build();

		surveyRepository.save(survey);

		String firstQuestion = gptService.makeFirstQuestion(survey);


		return BaseResponse.<FirstGptResponse>builder()
			.code(201)
			.message("설문지를 작성하였습니다.")
			.data(new FirstGptResponse(firstQuestion))
			.isSuccess(true)
			.build();
	}

}
