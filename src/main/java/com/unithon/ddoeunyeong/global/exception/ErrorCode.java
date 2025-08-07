package com.unithon.ddoeunyeong.global.exception;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
	ALREADY_SIGNUP(BAD_REQUEST,410,"이미 가입된 회원입니다."),

	INVALID_PASSWORD(BAD_REQUEST,411,"잘못된 비밀번호입니다."),

	LOGIN_FAIL(HttpStatus.BAD_REQUEST,400,"로그인에 오류가 발생하였습니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,500,"서버에 오류가 발생하였습니다."),
	JWT_KEY_GENERATION_FAILED(HttpStatus.BAD_REQUEST,400,"JWT 키 생성에 실패하였습니다."),
	NO_REFRESH_TOKEN(UNAUTHORIZED,400, "리프레시 토큰이 없습니다."),
	LOGOUT_ERROR(BAD_REQUEST,400,"로그아웃에 실패하였습니다."),
	SIGNUP_ERROR(BAD_REQUEST,400,"회원가입에러입니다."),
	EXPIRED_REFRESH_TOKEN(UNAUTHORIZED, 400,"만료된 토큰입니다."),
	JWT_PARSE_FAILED(BAD_REQUEST,404,"토큰 파싱이 잘못되었습니다."),

	KAKAO_USER_ERROR(BAD_REQUEST,404,"카카오 유저 정보를 가져오지 못하였습니다."),
	TOKEN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,500, "토큰을 제대로 생성하지 못하였습니다."),


	USER_NOT_FOUND(BAD_REQUEST,404,"존재하지 않는 유저입니다.");



	private final HttpStatus status;
	private final int code;
	private final String message;


	ErrorCode( HttpStatus status,int code, String message){
		this.code = code;
		this.status = status;
		this.message = message;
	}
}
