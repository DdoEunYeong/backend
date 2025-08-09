package com.unithon.ddoeunyeong.domain.gpt.dto;

public record GptResponse( String emotion,
						   String summary,
						   String followUpQuestion) {
}
