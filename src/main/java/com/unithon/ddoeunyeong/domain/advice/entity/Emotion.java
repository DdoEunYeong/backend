package com.unithon.ddoeunyeong.domain.advice.entity;

import java.util.Locale;

public enum Emotion {
    ANGRY,
    DISGUST,
    FEAR,
    HAPPY,
    SAD,
    SURPRISE,
    NEUTRAL;

    public static Emotion fromString(String s) {
        if (s == null) return null;
        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "angry":    return ANGRY;
            case "disgust":  return DISGUST;
            case "fear":     return FEAR;
            case "happy":    return HAPPY;
            case "sad":      return SAD;
            case "surprise": return SURPRISE;
            case "neutral":  return NEUTRAL;
            default:         return null; // 필요 시 return NEUTRAL;
        }
    }
}
