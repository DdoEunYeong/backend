package com.unithon.ddoeunyeong.domain.utterance.dto;

public record ProcessUtteranceResponse(String emotion,
									   String summary,
									   String followUpQuestion) {
}
