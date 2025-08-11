package com.unithon.ddoeunyeong.domain.advice.repository;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdviceRepository extends JpaRepository<Advice, Long> {
    // childId에 해당하는 Advice 개수
    long countByChildId(Long childId);

    // childId에 해당하는 마지막 Advice (createdAt 기준 내림차순)
    Optional<Advice> findTopByChildIdOrderByCreatedAtDesc(Long childId);
}
