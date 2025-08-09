package com.unithon.ddoeunyeong.domain.gpt.dto;

import java.util.List;

import com.unithon.ddoeunyeong.domain.survey.dto.SurveyDto;
import com.unithon.ddoeunyeong.domain.child.dto.ChildProfile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GptRequest {
	private ChildProfile childProfile;
	private List<String> history;
	private SurveyDto survey;
	private String latestInput;
}
