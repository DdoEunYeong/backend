package com.unithon.ddoeunyeong.domain.advice.dto;

import java.util.List;

public record AdvicePreviewListResponse(
        List<AdvicePreview> previewList
) {
    public record AdvicePreview(
            Long id,
            String date,
            int totalScore
    ) {}
}
