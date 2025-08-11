package com.unithon.ddoeunyeong.domain.utterance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unithon.ddoeunyeong.domain.utterance.entity.UserUtterance;
import org.springframework.stereotype.Repository;

@Repository
public interface UserUtteranceRepository extends JpaRepository<UserUtterance, Long> {

	Optional<UserUtterance> findTopByAdviceIdOrderByCreatedAtDesc(Long adviceId);
	List<UserUtterance> findTop5ByAdviceIdOrderByCreatedAtDesc(Long adviceId);
}
