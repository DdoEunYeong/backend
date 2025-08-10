package com.unithon.ddoeunyeong.domain.survey.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unithon.ddoeunyeong.domain.survey.entity.Survey;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey,Long> {
	Optional<Survey> findByAdviceId(Long adviceId);
}
