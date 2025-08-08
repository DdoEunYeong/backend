package com.unithon.ddoeunyeong.domain.child.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.user.entity.User;

@Repository
public interface ChildRepository extends JpaRepository<Child,Long> {
	Optional<Child>findByIdAndUser(long childId, User user);
}
