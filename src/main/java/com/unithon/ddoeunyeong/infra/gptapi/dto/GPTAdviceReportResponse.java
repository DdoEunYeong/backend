package com.unithon.ddoeunyeong.infra.gptapi.dto;

import java.util.List;

public record GPTAdviceReportResponse(Long socialReferenceScore,
									  Long cooperationKindnessScore,
									  String summary,
									  String coreQuestion,
									  String childAnswer,
									  String otherTalks,
									  List<String> frequentWordList
									  ) {
}
