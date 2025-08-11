package com.unithon.ddoeunyeong.domain.advice.service;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import com.unithon.ddoeunyeong.domain.advice.entity.AdviceStatus;
import com.unithon.ddoeunyeong.domain.advice.repository.AdviceRepository;
import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.child.repository.ChildRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AdviceService {
    private final AdviceRepository adviceRepository;
    private final ChildRepository childRepository;

    @Transactional
    public Advice createAdviceBeforeSurvey(Long childId) {
        Child child = childRepository.findById(childId).orElse(null);
        Advice advice = Advice.builder()
                .child(child)
                .status(AdviceStatus.PENDING)
                .build();
        return adviceRepository.save(advice);
    }

    @Transactional
    public Advice startAdviceSession(Long adviceId, String sessionId) {
        Advice advice = adviceRepository.findById(adviceId).orElse(null);
        advice.updateSessionId(sessionId);
        advice.updateStatus(AdviceStatus.IN_PROGRESS);
        return adviceRepository.save(advice);
    }

    @Transactional
    public void finishAdviceSession(Long adviceId, String s3Url, AdviceStatus status) {
        Advice advice = adviceRepository.findById(adviceId).orElseThrow();
        if (s3Url != null) advice.saveVideoUrl(s3Url);
        advice.updateStatus(status);
    }
}
