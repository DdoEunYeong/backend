package com.unithon.ddoeunyeong.domain.utterance.service;

import com.unithon.ddoeunyeong.domain.utterance.dto.LastUtteranceResponse;
import com.unithon.ddoeunyeong.domain.utterance.entity.UserUtterance;
import com.unithon.ddoeunyeong.domain.utterance.repository.UserUtteranceRepository;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserUtteranceService {

    private final UserUtteranceRepository userUtteranceRepository;

    @Transactional
    public LastUtteranceResponse saveLastUtterance(String userText, Long adviceId) {
        // 현재 발화를 이전의 UserUtterance에 담아서 저장
        UserUtterance lastUserUtterance = userUtteranceRepository
                .findTopByAdviceIdOrderByCreatedAtDesc(adviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_ADVICE));

        lastUserUtterance.updateUtterance(userText);

        return new LastUtteranceResponse(adviceId);
    }
}
