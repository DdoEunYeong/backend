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

	//오늘 대화를 통해 자녀에게 궁금한 점
	private String knowAboutChild;

	//오늘 대화에서 참고할 정보
	private String knowInfo;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "advice_id", unique = true, nullable = false)
	private Advice advice;


	@Builder
	private Survey(String knowAboutChild, String knowInfo, Advice advice) {
		this.knowAboutChild = knowAboutChild;
		this.knowInfo = knowInfo;
		this.advice = advice;
	}

}
