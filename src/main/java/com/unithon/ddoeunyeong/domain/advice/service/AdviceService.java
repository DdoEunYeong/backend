package com.unithon.ddoeunyeong.domain.advice.service;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.entity.AdviceStatus;
import com.unithon.ddoeunyeong.domain.advice.entity.Emotion;
import com.unithon.ddoeunyeong.domain.advice.repository.AdviceRepository;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.domain.utterance.entity.UserUtterance;
import com.unithon.ddoeunyeong.domain.utterance.repository.UserUtteranceRepository;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class AdviceService {
    private final AdviceRepository adviceRepository;
    private final ChildRepository childRepository;

    @Transactional
    public Advice createAdviceBeforeSurvey(Long childId) {
        Child child = childRepository.findById(childId).orElse(null);
        Advice advice = Advice.builder()
                .child(child)
                .status(AdviceStatus.PENDING)
                .build();
        return adviceRepository.save(advice);
    }

    @Transactional
    public Advice startAdviceSession(Long adviceId, String sessionId) {
        Advice advice = adviceRepository.findById(adviceId).orElse(null);
        advice.updateSessionId(sessionId);
        advice.updateStatus(AdviceStatus.IN_PROGRESS);
        return adviceRepository.save(advice);
    }

    @Transactional
    public void finishAdviceSession(Long adviceId, String s3Url, AdviceStatus status) {
        Advice advice = adviceRepository.findById(adviceId).orElseThrow();
        if (s3Url != null) advice.saveVideoUrl(s3Url);
        advice.updateStatus(status);
    }

    public int calculateEmotionDiversityScore(List<UserUtterance> utterances) {
        if (utterances == null || utterances.isEmpty()) {
            return 0; // 데이터 없으면 0점
        }

        int totalCount = utterances.size();
        int neutralCount = 0;

        // 등장한 감정 종류 Set
        Set<Emotion> uniqueEmotions = EnumSet.noneOf(Emotion.class);

        for (UserUtterance u : utterances) {
            Emotion e = u.getDominantEmotion();
            if (e != null) {
                uniqueEmotions.add(e);
                if (e == Emotion.NEUTRAL) {
                    neutralCount++;
                }
            }
        }

        int totalEmotionTypes = Emotion.values().length;
        double diversityRatio = (double) uniqueEmotions.size() / totalEmotionTypes;

        // NEUTRAL 비율
        double neutralRatio = (double) neutralCount / totalCount;

        // 기본 점수 × (1 - neutral 비율)
        double adjustedScore = diversityRatio * 100 * (1 - neutralRatio);

        return (int) Math.round(adjustedScore);
    }

}
