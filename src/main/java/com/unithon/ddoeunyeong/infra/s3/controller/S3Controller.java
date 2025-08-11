package com.unithon.ddoeunyeong.infra.s3.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.unithon.ddoeunyeong.infra.s3.service.S3Service;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class S3Controller {

	private final S3Service s3Service;

	@PostMapping(value = "/s3", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "파일 업로드 테스트용 API 입니다.",description = "파일을 업로드하면 S3에 업로드하고 url을 리턴시켜주는 API입니다.")
	public BaseResponse<String> uploadFile(@RequestParam MultipartFile multipartFile){
		return BaseResponse.<String>builder()
			.code(201)
			.message("파일 업로드에 성공하였습니다.")
			.data(s3Service.uploadFile(multipartFile))
			.isSuccess(true)
			.build();
	}

}
