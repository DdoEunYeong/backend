package com.unithon.ddoeunyeong.infra.gptapi.dto;

public record GPTAdviceReportResponse(Long socialReferenceScore,
                                      Long cooperationKindnessScore,
                                      String summary,
                                      String coreQuestion,
                                      String childAnswer,
									  String otherTalks
) {
}
