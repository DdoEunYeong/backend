package com.unithon.ddoeunyeong.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unithon.ddoeunyeong.domain.user.dto.LoginRequest;
import com.unithon.ddoeunyeong.domain.user.dto.SignUpRequest;
import com.unithon.ddoeunyeong.domain.user.entity.User;
import com.unithon.ddoeunyeong.domain.user.entity.UserRole;
import com.unithon.ddoeunyeong.domain.user.repository.UserRepository;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import com.unithon.ddoeunyeong.global.security.token.dto.TokenResponse;
import com.unithon.ddoeunyeong.global.security.token.repository.RefreshTokenRepository;
import com.unithon.ddoeunyeong.global.security.token.service.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public BaseResponse<Void> signUp(SignUpRequest request){

		if(userRepository.existsByUserId(request.userId())){
			throw new CustomException(ErrorCode.ALREADY_SIGNUP);
		}

		User user = User.builder()
			.userId(request.userId())
			.password(passwordEncoder.encode(request.password()))
			.userRole(UserRole.USER)
			.name(request.name())
			.build();

		userRepository.save(user);

		return BaseResponse.<Void>builder()
			.code(201)
			.isSuccess(true)
			.message("회원가입에 성공하였습니다.")
			.build();
	}

	@Transactional
	public BaseResponse<TokenResponse> login(LoginRequest request) {

		User user = userRepository.findByUserId(request.userId())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}

		TokenResponse token = jwtTokenProvider.createToken(user.getId().toString());

		return BaseResponse.<TokenResponse>builder()
			.code(200)
			.isSuccess(true)
			.message("로그인 성공")
			.data(token)
			.build();
	}

	@Transactional
	public BaseResponse<Void> logout(String accessToken) {
		String userId = jwtTokenProvider.getUserIdFromToken(accessToken);

		refreshTokenRepository.deleteByUserId(Long.parseLong(userId));

		return BaseResponse.<Void>builder()
			.code(200)
			.isSuccess(true)
			.message("로그아웃 되었습니다.")
			.build();
	}


	@Transactional
	public BaseResponse<Void> signout(String accessToken) {
		String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
		Long id = Long.parseLong(userId);

		User user = userRepository.findById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		refreshTokenRepository.deleteByUserId(id);
		userRepository.delete(user);

		return BaseResponse.<Void>builder()
			.code(200)
			.isSuccess(true)
			.message("회원 탈퇴가 완료되었습니다.")
			.build();
	}


}
