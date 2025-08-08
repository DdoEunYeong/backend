package com.unithon.ddoeunyeong.domain.child.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unithon.ddoeunyeong.domain.child.dto.ChildDeleteRequest;
import com.unithon.ddoeunyeong.domain.child.dto.ChildRequest;
import com.unithon.ddoeunyeong.domain.child.service.ChildService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/child")
@RequiredArgsConstructor
@Tag(name ="자식 API", description = "자식 관련 API에 대한 설명입니다.")
public class ChildController {

	private final ChildService childService;

	@PostMapping("")
	@Operation(summary = "아이를 추가하는 API 입니다.")
	public BaseResponse<Void> makeChild(@RequestBody ChildRequest request){
		return childService.makeChild(request);
	}

	@DeleteMapping("")
	@Operation(summary = "자식을 삭제하는 API 입니다.")
	public BaseResponse<Void> deleteChild(@RequestParam ChildDeleteRequest request){
		return childService.deleteChild(request);
	}


}
