package com.unithon.ddoeunyeong.domain.advice.repository;

import com.unithon.ddoeunyeong.domain.advice.entity.Advice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdviceRepository extends JpaRepository<Advice, Long> {
}
