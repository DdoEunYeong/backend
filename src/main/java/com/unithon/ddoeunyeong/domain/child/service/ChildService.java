package com.unithon.ddoeunyeong.domain.child.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.unithon.ddoeunyeong.domain.child.dto.ChildDeleteRequest;
import com.unithon.ddoeunyeong.domain.child.dto.ChildLists;
import com.unithon.ddoeunyeong.domain.child.dto.ChildRequest;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.entity.Gender;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import com.unithon.ddoeunyeong.domain.user.entity.User;
import com.unithon.ddoeunyeong.domain.user.repository.UserRepository;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import com.unithon.ddoeunyeong.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChildService {

	private final ChildRepository childRepository;
	private final UserRepository userRepository;
	private final S3Service s3Service;

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

	public BaseResponse<List<ChildLists>> getAllChild(Long userId){

		List<ChildLists> lists = childRepository.findAllByUserId(userId)
			.stream().map(m ->new ChildLists(m.getId(),m.getName()))
			.toList();

		return BaseResponse.<List<ChildLists>>builder()
			.code(200)
			.message("모든 아이들을 조회하였습니다.")
			.data(lists)
			.isSuccess(true)
			.build();
	}



	public BaseResponse<String> postChildImage(MultipartFile file, Long childId){
		Child child = childRepository.findById(childId)
			.orElseThrow(()->new CustomException(ErrorCode.NO_CHILD));

		String url = s3Service.uploadFile(file);

		child.setImageUrl(url);
		childRepository.save(child);

		return BaseResponse.<String>builder()
			.code(201)
			.message("아이의 사진이 저장되었습니다.")
			.data(url)
			.isSuccess(true)
			.build();

	}



}
