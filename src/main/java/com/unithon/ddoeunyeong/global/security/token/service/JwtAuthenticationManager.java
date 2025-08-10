package com.unithon.ddoeunyeong.global.security.token.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationManager implements AuthenticationManager {
	private final JwtTokenProvider jwtTokenProvider;


	public JwtAuthenticationManager(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	public Authentication authenticate(Authentication authentication) {
		String token = authentication.getCredentials().toString();
		return jwtTokenProvider.getAuthentication(token);
	}
}


