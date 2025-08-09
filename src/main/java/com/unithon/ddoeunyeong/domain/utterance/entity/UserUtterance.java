package com.unithon.ddoeunyeong.domain.utterance.entity;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.global.time.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class UserUtterance extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "child_id")
	private Child child;


	private String utterance;


	@Builder
	private UserUtterance(String utterance,Child child){
		this.utterance = utterance;
		this.child = child;
	}



}
