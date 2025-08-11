package com.unithon.ddoeunyeong.global.security.config;

import static org.springframework.security.config.Customizer.*;

import java.util.List;


import com.unithon.ddoeunyeong.global.security.token.service.JwtAuthenticationFilter;
import com.unithon.ddoeunyeong.global.security.token.service.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(withDefaults())
			.httpBasic(AbstractHttpConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/ws/**",
					"/api/auth/**",
					"/api/v1/**",
					"/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-ui.html",
					"/webjars/**",
					"/api/v1/user/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(e -> e
				.authenticationEntryPoint((request, response, authException) -> {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
				})
			)
			// ✅ 여기 추가: CSP
			.headers(h -> h
					.contentSecurityPolicy(csp -> csp.policyDirectives(String.join("; ",
						"default-src 'self'",
						// fetch/XHR/WebSocket 허용할 도메인들
						"connect-src 'self' https://api.v0.dev https://vitals.vercel-insights.com",
						"script-src 'self'",
						"img-src 'self' data: https:",
						"style-src 'self' 'unsafe-inline'",
						"object-src 'none'",
						"base-uri 'self'",
						"frame-ancestors 'self'"
					)))
				// 필요 시 Report-Only로 먼저 점검하고 싶다면 아래 한 줄 사용
				// .contentSecurityPolicy(csp -> csp.policyDirectives("...").reportOnly())
			)
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
				UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(false);
		config.setAllowedOrigins(List.of("http://localhost:5500")); // 명시 오리진
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
