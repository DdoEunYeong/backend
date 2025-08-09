package com.unithon.ddoeunyeong.domain.gpt.dto;

import com.unithon.ddoeunyeong.domain.child.dto.ChildProfile;
import com.unithon.ddoeunyeong.domain.survey.dto.SurveyDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FirstGPTRequest {
	private ChildProfile childProfile;
	private SurveyDto survey;
}
