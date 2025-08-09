package com.unithon.ddoeunyeong.domain.child.entity;

import com.unithon.ddoeunyeong.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "child")
public class Child {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "child_id")
	private Long id;

	//아이의 이름
	private String name;

	//아이의 나이
	private Long age;

	//아이의 성별
	private Gender gender;

	//아이의 성격
	private String characterType;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@Builder
	private Child(String name, Long age, User user, Gender gender, String character){
		this.name = name;
		this.age = age;
		this.gender = gender;
		this.user = user;
		this.characterType = character;
	}
}
