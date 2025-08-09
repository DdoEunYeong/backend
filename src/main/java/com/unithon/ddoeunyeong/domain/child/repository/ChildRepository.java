package com.unithon.ddoeunyeong.domain.child.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.unithon.ddoeunyeong.domain.child.entity.Child;
import com.unithon.ddoeunyeong.domain.user.entity.User;

@Repository
public interface ChildRepository extends JpaRepository<Child,Long> {
	Optional<Child>findByIdAndUser(long childId, User user);

	@Query("SELECT c FROM Child c WHERE c.user.id = :userId")
	List<Child> findAllByUserId(@Param("userId") Long userId);
}
