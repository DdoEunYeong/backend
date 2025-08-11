package com.unithon.ddoeunyeong.infra.gptapi.dto;

public record AdivceReportResponse(Long socialReferenceScore,
                                   Long cooperationKindnessScore,
                                   String summary,
                                   String coreQuestion,
                                   String childAnswer
							   ) {
}
