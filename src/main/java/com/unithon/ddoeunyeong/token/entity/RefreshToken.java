package com.unithon.ddoeunyeong.token.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long tokenId;
	private Long userId;
	private String token;
	private Instant expiryDate;

	@Builder
	private RefreshToken(Long userId, Long id, String token, Instant expiryDate) {
		this.userId = userId;
		this.tokenId = id;
		this.token = token;
		this.expiryDate = expiryDate;
	}
}
