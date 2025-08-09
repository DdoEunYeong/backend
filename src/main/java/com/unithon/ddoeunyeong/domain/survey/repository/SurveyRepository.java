package com.unithon.ddoeunyeong.domain.survey.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.survey.entity.Survey;

public interface SurveyRepository extends JpaRepository<Survey,Long> {

	Optional<Survey> findByChild(Child child);

	Optional<Survey> findTopByChildIdOrderByCreatedAtDesc(Long childId);
}
