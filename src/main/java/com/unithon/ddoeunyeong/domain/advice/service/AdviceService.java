package com.unithon.ddoeunyeong.domain.advice.service;

import com.unithon.ddoeunyeong.domain.advice.dto.AdviceReportResponse;
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
import com.unithon.ddoeunyeong.infra.gptapi.dto.GPTAdviceReportResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AdviceService {
    private final AdviceRepository adviceRepository;
    private final ChildRepository childRepository;
    private final UserUtteranceRepository userUtteranceRepository;

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

    public AdviceReportResponse getAdviceReport(Long adviceId) {
        // 1) 기본 로드
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_ADVICE));

        List<UserUtterance> utteranceList = userUtteranceRepository.findAllByAdviceId(adviceId);

        // 2) 계산 항목
        int emotionDiversityScore = calculateEmotionDiversityScore(utteranceList);
        Emotion emotion = mostFrequentEmotionEnum(utteranceList);
        String mostFreqExpressionLabel = mapEmotionToLabel(emotion);

        String durationText = formatDuration(safeGetDurationSeconds(advice)); // "45분" 형태

        // sessionNumber = 해당 child의 전체 상담 개수
        int sessionNumber = Math.toIntExact(
                adviceRepository.countByChildId(advice.getChild().getId())
        );

        // 사회 점수 & 협력 점수
        int socialReferenceScore = safeInt(advice.getSocialScore());
        int cooperationKindness  = safeInt(advice.getCoopScore());

        // 총점: 정책에 따라 조정 가능
        int totalScore = (socialReferenceScore + cooperationKindness + emotionDiversityScore)/3;

        // 4) QnA: 발화 리스트에서 최대 5개 추출
        List<AdviceReportResponse.QnA> qnaList = utteranceList.stream()
                .filter(u -> u.getQuestion() != null || u.getUtterance() != null)
                .limit(5)
                .map(u -> AdviceReportResponse.QnA.builder()
                        .question(nullToDash(u.getQuestion()))
                        .answer(nullToDash(u.getUtterance()))
                        .build())
                .toList();

        // 5) 응답 빌드
        return AdviceReportResponse.builder()
                .childName(advice.getChild().getName())
                .sessionNumber(sessionNumber)
                .consultationDate(advice.getCreatedAt().toLocalDate())
                // 좌상단
                .consultationTopic(advice.getSummary())
                .duration(durationText)
                .coreQuestion(advice.getCoreQ())
                // 우상단
                .childAnswer(advice.getChildAns())
                .otherTalks(advice.getOtherTalks())
                // 중앙
                .totalScore(totalScore)
                // 좌하단
                .socialReferenceScore(socialReferenceScore)
                .cooperationKindnessScore(cooperationKindness)
                .emotionDiversityScore(emotionDiversityScore)
                // 중앙하단
                .mostFrequentExpression(mostFreqExpressionLabel)
                // 우하단
                .qnaList(qnaList)
                .build();
    }

    @Transactional
    public AdviceReportResponse makeAdviceReport(Long adviceId, GPTAdviceReportResponse gpt) {
        // 1) 기본 로드
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_ADVICE));

        List<UserUtterance> utteranceList = userUtteranceRepository.findAllByAdviceId(adviceId);

        // 2) 계산 항목
        // (저장 O) 감정 다양성 및 가장 많은 표정 계산
        int emotionDiversityScore = calculateEmotionDiversityScore(utteranceList);
        Emotion emotion = mostFrequentEmotionEnum(utteranceList);
        String mostFreqExpressionLabel = mapEmotionToLabel(emotion);
        advice.updateEmotion(emotionDiversityScore, emotion);

        // (저장 X) 초 기반의 duration 불러와서 계산
        String durationText = formatDuration(safeGetDurationSeconds(advice)); // "45분" 형태

        // (저장 X) sessionNumber = 해당 child의 전체 상담 개수
        int sessionNumber = Math.toIntExact(
                adviceRepository.countByChildId(advice.getChild().getId())
        );

        // 3) (저장 X; 이미 저장되어있음) GPT 필드 매핑 (record이므로 gpt.socialReferenceScore() 형태)
        int socialReferenceScore   = safeInt(gpt.socialReferenceScore());     // null 안전 변환
        int cooperationKindness    = safeInt(gpt.cooperationKindnessScore());

        // (저장 O) 총점: 정책에 따라 조정 가능
        int totalScore = (socialReferenceScore + cooperationKindness + emotionDiversityScore)/3;
        advice.updateTotalScore(totalScore);

        // 4) (저장 X) QnA: 발화 리스트에서 최대 5개 추출
        List<AdviceReportResponse.QnA> qnaList = utteranceList.stream()
                .filter(u -> u.getQuestion() != null || u.getUtterance() != null)
                .limit(5)
                .map(u -> AdviceReportResponse.QnA.builder()
                        .question(nullToDash(u.getQuestion()))
                        .answer(nullToDash(u.getUtterance()))
                        .build())
                .toList();

        // 5) 응답 빌드
        return AdviceReportResponse.builder()
                .childName(advice.getChild().getName())
                .sessionNumber(sessionNumber)
                .consultationDate(advice.getCreatedAt().toLocalDate())
                // 좌상단
                .consultationTopic(gpt.summary())
                .duration(durationText)
                .coreQuestion(gpt.coreQuestion())
                // 우상단
                .childAnswer(gpt.childAnswer())
                .otherTalks(gpt.otherTalks())
                // 중앙
                .totalScore(totalScore)
                // 좌하단
                .socialReferenceScore(socialReferenceScore)
                .cooperationKindnessScore(cooperationKindness)
                .emotionDiversityScore(emotionDiversityScore)
                // 중앙하단
                .mostFrequentExpression(mostFreqExpressionLabel)
                // 우하단
                .qnaList(qnaList)
                .build();
    }

    @Transactional
    public void writeDuration(Long adviceId, Long seconds){
        Advice advice = adviceRepository.findById(adviceId).orElseThrow(() -> new CustomException(ErrorCode.NO_ADVICE));

        advice.updateDuration(seconds);
    }

    private int calculateEmotionDiversityScore(List<UserUtterance> utterances) {
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

    private String formatDuration(Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) return "0분";
        long minutes = Math.round(durationSeconds / 60.0); // 반올림
        if (minutes <= 0) minutes = 1;                     // 1분 미만 보정
        return minutes + "분";
    }

    private Long safeGetDurationSeconds(Advice advice) {
        try {
            // 프로젝트에 따라 필드/메서드명이 다를 수 있음
            return advice.getDuration(); // 없으면 적절히 수정
        } catch (Exception e) {
            return 0L;
        }
    }

    private int safeInt(Long v) {
        return v == null ? 0 : Math.toIntExact(v);
    }

    private String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private Emotion mostFrequentEmotionEnum(List<UserUtterance> list) {
        if (list == null || list.isEmpty()) return null; // 필요 시 Emotion.NEUTRAL
        return list.stream()
                .map(UserUtterance::getDominantEmotion)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String mapEmotionToLabel(Emotion e) {
        if (e == null) return "-";
        return switch (e) {
            case HAPPY     -> "기쁨";
            case SAD       -> "슬픔";
            case ANGRY     -> "분노";
            case SURPRISE  -> "놀람";
            case DISGUST   -> "혐오";
            case FEAR      -> "두려움";
            case NEUTRAL   -> "중립";
        };
    }

}
