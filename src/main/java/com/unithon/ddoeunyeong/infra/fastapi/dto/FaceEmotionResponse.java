package com.unithon.ddoeunyeong.infra.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.Map;

@Getter
public class FaceEmotionResponse {
    private Result result;

    @Getter
    public static class Result {
        @JsonProperty("dominant_emotion")
        private String dominantEmotion;
        private Map<String, Double> scores;
    }
}

