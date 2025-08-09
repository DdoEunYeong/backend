package com.unithon.ddoeunyeong.domain.utterance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unithon.ddoeunyeong.domain.utterance.entity.UserUtterance;

public interface UserUtteranceRepository extends JpaRepository<UserUtterance, Long> {

	List<UserUtterance> findTop5ByChildIdOrderByCreatedAtDesc(Long childId);
}
