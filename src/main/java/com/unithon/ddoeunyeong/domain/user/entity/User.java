package com.unithon.ddoeunyeong.domain.user.entity;

import com.unithon.ddoeunyeong.global.security.time.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String userId;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole userRole;

	@Builder
	private User(String userId, String password, UserRole userRole){
		this.userId = userId;
		this.password = password;
		this.userRole = userRole;
	}
}