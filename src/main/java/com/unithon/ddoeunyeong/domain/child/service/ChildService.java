package com.unithon.ddoeunyeong.domain.child.service;

import java.time.LocalDate;
import java.util.List;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.repository.AdviceRepository;
import com.unithon.ddoeunyeong.domain.child.dto.ChildInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
import com.unithon.ddoeunyeong.global.security.config.CustomUserDetails;
import com.unithon.ddoeunyeong.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChildService {

	private final ChildRepository childRepository;
	private final UserRepository userRepository;
	private final S3Service s3Service;

	private final AdviceRepository adviceRepository;

	public BaseResponse<Void> makeChild(CustomUserDetails customUserDetails,ChildRequest request){

		User user = userRepository.findById(customUserDetails.getUser().getId())
			.orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));

		Child child = Child.builder()
			.age(request.age())
			.birthDate(request.birthDate())
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


	public BaseResponse<Void> deleteChild(Long childId) {
		Child child = childRepository.findById(childId)
				.orElseThrow(()-> new CustomException(ErrorCode.NO_CHILD));

		childRepository.delete(child);

		return BaseResponse.<Void>builder()
				.isSuccess(true)
				.data(null)
				.message("아이를 뺐습니다.")
				.code(200)
				.build();
	}

	public BaseResponse<ChildInfo> getChildInfo(Long childId) {
		Child child = childRepository.findById(childId).orElseThrow(()->new CustomException(ErrorCode.NO_CHILD));

		Gender gender = child.getGender();
		String strGender = gender.equals(Gender.BOY) ? "남자" : "여자";

		int adviceCount = (int) adviceRepository.countByChildId(childId);

		Advice lastAdvice = adviceRepository.findTopByChildIdOrderByCreatedAtDesc(childId).orElse(null);

		LocalDate lastAdviceDate;

		if (lastAdvice != null){
			lastAdviceDate = lastAdvice.getCreatedAt().toLocalDate();
		} else {
			lastAdviceDate = null;
		}

		ChildInfo childInfo	= new ChildInfo(
				child.getId(),
				child.getName(),
				strGender,
				child.getAge(),
				child.getCharacterType(),
				adviceCount,
				lastAdviceDate,
				child.getBirthDate()
		);

		return BaseResponse.<ChildInfo>builder()
				.isSuccess(true)
				.data(childInfo)
				.message("아이의 정보입니다.")
				.code(200)
				.build();
	}

	public BaseResponse<List<ChildLists>> getAllChild(CustomUserDetails customUserDetails){

		List<ChildLists> lists = childRepository.findAllByUserId(customUserDetails.getUser().getId())
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
