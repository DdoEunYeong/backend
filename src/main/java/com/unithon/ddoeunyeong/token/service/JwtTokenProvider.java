package com.unithon.ddoeunyeong.token.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.unithon.ddoeunyeong.domain.user.entity.User;
import com.unithon.ddoeunyeong.domain.user.repository.UserRepository;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import com.unithon.ddoeunyeong.global.security.config.CustomUserDetails;
import com.unithon.ddoeunyeong.token.dto.TokenResponse;
import com.unithon.ddoeunyeong.token.entity.RefreshToken;
import com.unithon.ddoeunyeong.token.repository.RefreshTokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	//이걸 Oauth 유저가 아니라 -> 일반 사용자로 봐야함

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	@Value("${spring.jwt.secret}")
	private String secret;

	@Value("${spring.jwt.access-token-duration}")
	private Duration accessTokenDuration;

	@Value("${spring.jwt.refresh-token-duration}")
	private Duration refreshTokenDuration;

	private final MacAlgorithm alg = Jwts.SIG.HS512;
	private SecretKey key;

	@PostConstruct
	public void init() {
		try {
			this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new CustomException(ErrorCode.JWT_KEY_GENERATION_FAILED);
		}
	}

	public String createAccessToken(String userId) {
		try {
			Date now = new Date();
			Date expiry = new Date(now.getTime() + accessTokenDuration.toMillis());

			return Jwts.builder()
				.claims(Map.of("sub", userId))
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key, alg)
				.compact();
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public String createRefreshToken(String userId) {
		try {
			Date now = new Date();
			Date expiry = new Date(now.getTime() + refreshTokenDuration.toMillis());

			return Jwts.builder()
				.claims(Map.of("sub", userId))
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key, alg)
				.compact();
		} catch (Exception e) {
			throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
		}
	}


	public TokenResponse createToken(String userId) {
		try {
			String accessToken = createAccessToken(userId);
			String refreshToken = createRefreshToken(userId);

			Instant expiryDate = Instant.now().plus(refreshTokenDuration);

			RefreshToken refreshTokenEntity = RefreshToken.builder()
				.userId(Long.parseLong(userId))
				.token(refreshToken)
				.expiryDate(expiryDate)
				.build();

			refreshTokenRepository.save(refreshTokenEntity);
			return new TokenResponse(accessToken,refreshToken);

		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}


	public String getUserIdFromToken(String token) {
		try {
			return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
		} catch (JwtException e) {
			throw new CustomException(ErrorCode.JWT_PARSE_FAILED);
		}
	}


	public Authentication getAuthentication(String token) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();

			String userId = claims.getSubject();
			Long realUserId = Long.parseLong(userId);

			User user = userRepository.findById(realUserId)
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

			CustomUserDetails principal = new CustomUserDetails(user);
			return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());

		} catch (Exception e) {
			throw new CustomException(ErrorCode.LOGIN_FAIL);
		}
	}


	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
