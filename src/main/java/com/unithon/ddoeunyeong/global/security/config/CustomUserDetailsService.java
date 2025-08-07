package com.unithon.ddoeunyeong.global.security.config;


import com.unithon.ddoeunyeong.domain.user.entity.User;
import com.unithon.ddoeunyeong.domain.user.repository.UserRepository;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String userId) {
		User user = userRepository.findById(Long.parseLong(userId))
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		return new CustomUserDetails(user);
	}
}

