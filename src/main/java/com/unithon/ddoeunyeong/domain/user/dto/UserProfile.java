package com.unithon.ddoeunyeong.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserProfile {
	private String name;
	private Long age;
	private String characterType;

	@Builder
	private UserProfile(String name, Long age, String characterType){
		this.name = name;
		this.age = age;
		this.characterType = characterType;
	}

}
