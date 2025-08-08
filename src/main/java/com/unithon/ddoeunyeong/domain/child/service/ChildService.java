package com.unithon.ddoeunyeong.domain.child.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.unithon.ddoeunyeong.domain.child.dto.ChildDeleteRequest;
import com.unithon.ddoeunyeong.domain.child.dto.ChildRequest;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.entity.Gender;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.domain.user.entity.User;
import com.unithon.ddoeunyeong.domain.user.repository.UserRepository;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChildService {

	private final ChildRepository childRepository;
	private final UserRepository userRepository;

	public BaseResponse<Void> makeChild(ChildRequest request){

		User user = userRepository.findById(request.userId())
			.orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));

		Child child = Child.builder()
			.age(request.age())
			.character(request.character())
			.gender(Gender.valueOf(request.gender()))
			.name(request.name())
			.user(user)
			.build();

		childRepository.save(child);

		return BaseResponse.<Void>builder()
			.isSuccess(true)
			.data(null)
			.message("아이가 추가되었습니다.")
			.code(201)
			.build();
	}


	public BaseResponse<Void> deleteChild(ChildDeleteRequest request){


		User user = userRepository.findById(request.userId()).orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));

		Child  child = childRepository.findByIdAndUser(request.childId(),user)
			.orElseThrow(()-> new CustomException(ErrorCode.NO_CHILD));

		childRepository.delete(child);

		return BaseResponse.<Void>builder()
			.isSuccess(true)
			.data(null)
			.message("아이를 뺐습니다.")
			.code(200)
			.build();
	}



}
