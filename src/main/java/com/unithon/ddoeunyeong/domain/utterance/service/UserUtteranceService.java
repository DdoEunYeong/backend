package com.unithon.ddoeunyeong.domain.utterance.service;

import com.unithon.ddoeunyeong.domain.advice.entity.Emotion;
import com.unithon.ddoeunyeong.domain.utterance.dto.SaveUtteranceResponse;
import com.unithon.ddoeunyeong.domain.utterance.entity.UserUtterance;
import com.unithon.ddoeunyeong.domain.utterance.repository.UserUtteranceRepository;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUtteranceService {

    private final UserUtteranceRepository userUtteranceRepository;

    @Transactional
    public SaveUtteranceResponse saveLastUtterance(String userText, Emotion emotion, Long adviceId) {

        // 1) 유저 발화 및 감정 업데이트
        UserUtterance priorUserUtterance = userUtteranceRepository
                .findTopByAdviceIdOrderByCreatedAtDesc(adviceId)
                .orElseThrow(() -> {
                    return new CustomException(ErrorCode.NO_ADVICE);
                });

        priorUserUtterance.updateUtteranceAndEmotion(userText,emotion);

        return new SaveUtteranceResponse(adviceId);
    }
}
