package com.unithon.ddoeunyeong.domain.utterance.entity;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.entity.Emotion;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.global.time.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
public class UserUtterance extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "advice_id")
	private Advice advice;

	@Setter
	private String question;

	@Setter
	private String utterance;

	@Enumerated(EnumType.STRING)
	private Emotion dominantEmotion;

	@Builder
	private UserUtterance(Advice advice, String question){
		this.advice = advice;
		this.question = question;
	}

	public void updateUtteranceAndEmotion (
			String utterance,
			Emotion emotion
	){
		this.utterance = utterance;
		this.dominantEmotion = emotion;
	}

}
