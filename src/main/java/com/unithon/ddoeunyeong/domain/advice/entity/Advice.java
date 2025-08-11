package com.unithon.ddoeunyeong.domain.advice.entity;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.utterance.entity.UserUtterance;
import com.unithon.ddoeunyeong.global.time.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    private int emotionDiversityScore;

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

    public void updateAnalysisResult(Long socialScore, Long coopScore, String summary, String coreQ, String childAns, int emotionDiversityScore) {
        this.socialScore = socialScore;
        this.coopScore = coopScore;
        this.summary = summary;
        this.coreQ = coreQ;
        this.childAns = childAns;
        this.emotionDiversityScore = emotionDiversityScore;
    }
}
