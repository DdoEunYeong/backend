package com.unithon.ddoeunyeong.infra.gptapi.dto;

import java.util.List;

import com.unithon.ddoeunyeong.domain.child.dto.ChildProfile;
import com.unithon.ddoeunyeong.domain.survey.dto.SurveyDto;
import com.unithon.ddoeunyeong.domain.utterance.dto.QuestionAndAnser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class GptFinalRequest {
	private ChildProfile childProfile;
	private List<QuestionAndAnser> history;
	private SurveyDto survey;
}
