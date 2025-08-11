package com.unithon.ddoeunyeong.infra.gptapi.dto;

public record GptFinalResponse(Long socialReferenceScore,
							   Long cooperationKindnessScore,
							   String summary,
							   String coreQuestion,
							   String childAnswer
							   ) {
}
