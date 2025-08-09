package com.unithon.ddoeunyeong.domain.child.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChildProfile {
	private String name;
	private Long age;
	private String characterType;

	@Builder
	private ChildProfile(String name, Long age, String characterType){
		this.name = name;
		this.age = age;
		this.characterType = characterType;
	}

}
