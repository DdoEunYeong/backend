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
import com.unithon.ddoeunyeong.token.dto.TokenResponse;
import com.unithon.ddoeunyeong.token.repository.RefreshTokenRepository;
import com.unithon.ddoeunyeong.token.service.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public BaseResponse<Object> signUp(SignUpRequest request){

		if(userRepository.existsByUserId(request.userId())){
			throw new CustomException(ErrorCode.ALREADY_SIGNUP);
		}

		User user = User.builder()
			.userId(request.userId())
			.password(passwordEncoder.encode(request.password()))
			.userRole(UserRole.USER)
			.build();

		userRepository.save(user);

		return BaseResponse.builder()
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

		TokenResponse token = jwtTokenProvider.createToken(user.getUserId().toString());

		return BaseResponse.<TokenResponse>builder()
			.code(200)
			.isSuccess(true)
			.message("로그인 성공")
			.data(token)
			.build();
	}

	@Transactional
	public BaseResponse<Object> logout(String accessToken) {
		String userId = jwtTokenProvider.getUserIdFromToken(accessToken);

		refreshTokenRepository.deleteByUserId(Long.parseLong(userId));

		return BaseResponse.builder()
			.code(200)
			.isSuccess(true)
			.message("로그아웃 되었습니다.")
			.build();
	}


	@Transactional
	public BaseResponse<Object> signout(String accessToken) {
		String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
		Long id = Long.parseLong(userId);

		User user = userRepository.findById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		refreshTokenRepository.deleteByUserId(id);
		userRepository.delete(user);

		return BaseResponse.builder()
			.code(200)
			.isSuccess(true)
			.message("회원 탈퇴가 완료되었습니다.")
			.build();
	}


}
