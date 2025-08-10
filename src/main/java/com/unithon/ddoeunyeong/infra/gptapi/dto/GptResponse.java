package com.unithon.ddoeunyeong.infra.gptapi.dto;

public record GptResponse( String emotion,
						   String summary,
						   String followUpQuestion) {
}
