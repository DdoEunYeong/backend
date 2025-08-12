package com.unithon.ddoeunyeong.domain.advice.entity;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.global.time.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "advice")
@Getter
@Builder
public class Advice extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="advice_id")
    private Long id;

//    @OneToOne
//    @JoinColumn(name = "survey_id", unique = true, nullable = false)
//    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    private String sessionId;

    @Enumerated(EnumType.STRING)
    private AdviceStatus status;

    // video S3 url
    private String videoUrl;

    @Setter
    private Long socialScore;

    @Setter
    private Long coopScore;

    @Setter
    private String summary;

    @Setter
    private String coreQ;

    @Setter
    private String childAns;

    @Setter
    private String otherTalks;

    @Setter
    private int totalScore;

    @Setter
    private int emotionDiversityScore;

    @Setter
    private Long duration;

    @ElementCollection
    @CollectionTable(
            name = "advice_frequent_words", // 별도 테이블 이름
            joinColumns = @JoinColumn(name = "advice_id") // FK
    )
    @Column(name = "word")
    private List<String> frequentWordList = new ArrayList<>();

    // 대표 감정 값을 통해 감정
    @Enumerated(EnumType.STRING)
    private Emotion dominantEmotion;

    public void saveVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void updateStatus (AdviceStatus status) {
        this.status = status;
    }

    public void updateSessionId (String sessionId) {
        this.sessionId = sessionId;
    }

    public void updateGPTReportResult(
            Long socialScore,
            Long coopScore,
            String summary,
            String coreQ,
            String childAns,
            String otherTalks,
            List<String> frequentWordList
    ) {
        this.socialScore = socialScore;
        this.coopScore = coopScore;
        this.summary = summary;
        this.coreQ = coreQ;
        this.childAns = childAns;
        this.otherTalks = otherTalks;
        this.frequentWordList = frequentWordList;
    }

    public void updateEmotion(int emotionDiversityScore, Emotion emotion){
        this.emotionDiversityScore = emotionDiversityScore;
        this.dominantEmotion = emotion;
    }

    public void updateDuration(Long duration){
        this.duration = duration;
    }

    public void updateTotalScore(int totalScore){
        this.totalScore = totalScore;
    }
}
