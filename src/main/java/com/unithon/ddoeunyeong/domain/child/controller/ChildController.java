package com.unithon.ddoeunyeong.domain.child.controller;

import java.util.List;

import com.unithon.ddoeunyeong.domain.child.dto.ChildInfo;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.unithon.ddoeunyeong.domain.child.dto.ChildLists;
import com.unithon.ddoeunyeong.domain.child.dto.ChildRequest;
import com.unithon.ddoeunyeong.domain.child.service.ChildService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.security.config.CustomUserDetails;

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
	@Operation(summary = "아이를 추가하는 API 입니다.\n"+"성별은 BOY, GIRL 로 입력하면 됩니다.",description = "user가 부모이기 때문에 부모에게 아이를 추가하기 위해서 사용하는 API입니다.")
	public BaseResponse<Void> makeChild(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestBody ChildRequest request){
		return childService.makeChild(customUserDetails,request);
	}

	@DeleteMapping("/{childId}")
	@Operation(summary = "자식을 삭제하는 API 입니다.",description = "부모가 만들어둔 자식을 삭제하는 API입니다.")
	public BaseResponse<Void> deleteChild(@PathVariable Long childId) {
		return childService.deleteChild(childId);
	}

	@GetMapping("")
	@Operation(summary = "자식 정보 리스트를 제공하는 API 입니다.", description = "부모가 등록한 자식들의 정보를 가져오는 API입니다.")
	public BaseResponse<List<ChildLists>> getAllChild(@AuthenticationPrincipal CustomUserDetails customUserDetails){
		return childService.getAllChild(customUserDetails);
	}

	@GetMapping("/{childId}")
	@Operation(summary = "자식 정보를 제공하는 API 입니다.", description = "부모가 등록한 자식의 정보를 가져오는 API입니다.")
	public BaseResponse<ChildInfo> getAllChild(@PathVariable Long childId){
		return childService.getChildInfo(childId);
	}

	@PostMapping(value = "/doll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "아이의 사진을 추가하는 API 입니다.",description = "부모가 등록한 자식에게 사진을 추가하는 API입니다.")
	public BaseResponse<String> postImageChild(@RequestParam("doll")MultipartFile file, Long childId){
		return childService.postChildImage(file,childId);
	}


}
