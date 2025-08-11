package com.unithon.ddoeunyeong.domain.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unithon.ddoeunyeong.domain.user.dto.LoginRequest;
import com.unithon.ddoeunyeong.domain.user.dto.SignUpRequest;
import com.unithon.ddoeunyeong.domain.user.dto.UserInfo;
import com.unithon.ddoeunyeong.domain.user.service.UserService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.security.config.CustomUserDetails;
import com.unithon.ddoeunyeong.global.security.token.dto.TokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name ="유저 API", description = "유저 관련 API에 대한 설명입니다.")
public class UserController {

	private final UserService userService;

	@Operation(summary = "회원가입 API")
	@PostMapping("/signup")
	public BaseResponse<Void> signup(@RequestBody SignUpRequest request){
		return userService.signUp(request);
	}

	@Operation(summary = "로그아웃 API")
	@PatchMapping("/logout")
	public BaseResponse<Void> logout(@RequestParam String accessToken){
		return userService.logout(accessToken);
	}


	@Operation(summary = "회원탈퇴 API")
	@DeleteMapping("/signout")
	public BaseResponse<Void> signout(@RequestParam String accessToken){
		return userService.signout(accessToken);
	}

	@Operation(summary = "로그인 API")
	@PostMapping("/login")
	public BaseResponse<TokenResponse> login(@RequestBody LoginRequest loginRequest){
		return userService.login(loginRequest);
	}

	@GetMapping("/userInfo")
	@Operation(summary = "회원정보 제공 API")
	public BaseResponse<UserInfo> getUserInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails){
		return userService.getUser(customUserDetails);
	}



}
