package com.unithon.ddoeunyeong.domain.advice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Builder
@Getter
public class AdviceReportResponse {

    private String childName;              // 아이 이름
    private String imageUrl; // 아이 사진
    private int sessionNumber;             // 상담 차시
    private LocalDate consultationDate;    // 상담 날짜 (년월일)

    // 좌상단
    private String consultationTopic;      // v 상담 주제(summary)
    private String duration;               // v 소요 시간 (ex: "45분")
    private String coreQuestion;          // v 집중 질문

    // 우상단
    private String childAnswer;             // v 궁금해한 사항
    private String otherTalks;              // v 그 외의 얘기

    // 중앙
    private int totalScore;                 // 총점수

    // 좌하단
    private int socialReferenceScore;       // v 사회참조 점수
    private int cooperationKindnessScore;   // v 협력/배려 점수
    private int emotionDiversityScore;      // 감정 다양성 점수

    // 중앙하단
    private String mostFrequentExpression;  // 최다 표정

    // 우하단
    private List<QnA> qnaList;               // 상담 Q&A 목록 (최대 5개)

    @Getter
    @Builder
    public static class QnA {
        private String question;
        private String answer;
    }

    private List<String> freqeuntWordList;
}
