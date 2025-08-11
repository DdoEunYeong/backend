package com.unithon.ddoeunyeong.domain.advice.entity;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.survey.entity.Survey;
import com.unithon.ddoeunyeong.global.time.BaseTimeEntity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

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


    // 대표 감정 값을 통해 감정
    @Enumerated(EnumType.STRING)
    private Emotion emotionAvg;

    public void saveVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void saveEmotionAvg(Emotion emotionAvg) {
        this.emotionAvg = emotionAvg;
    }

    public void updateStatus (AdviceStatus status) {
        this.status = status;
    }

    public void updateSessionId (String sessionId) {
        this.sessionId = sessionId;
    }


    public void updateAnalysisResult(Long socialScore, Long coopScore, String summary, String coreQ, String childAns){
        this.socialScore = socialScore;
        this.coopScore = coopScore;
        this.summary = summary;
        this.coreQ = coreQ;
        this.childAns = childAns;
    }
}
