package com.unithon.ddoeunyeong.domain.advice.entity;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.global.time.BaseTimeEntity;
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
    private Long id;

    @ManyToOne
    @JoinColumn(name = "child_id")
    private Child child;

    private String sessionId;

    @Enumerated(EnumType.STRING)
    private AdviceStatus status;

    // video S3 url
    private String videoUrl;

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
}
