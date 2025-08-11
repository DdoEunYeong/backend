package com.unithon.ddoeunyeong.domain.survey.entity;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.global.time.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Survey extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String temp;

	@Builder
	private Survey(String temp, Advice advice) {
		this.temp = temp;
		this.advice = advice;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "advice_id", unique = true, nullable = false)
	private Advice advice;
}
