package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.Result;
import com.inkFront.schoolManagement.model.ResultCheckerPin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultCheckerPinRepository extends JpaRepository<ResultCheckerPin, Long> {

    @EntityGraph(attributePaths = {"student", "schoolClass"})
    List<ResultCheckerPin> findByPinScopeAndSessionOrderByCreatedAtDesc(
            ResultCheckerPin.PinScope pinScope,
            String session
    );

    @EntityGraph(attributePaths = {"student", "schoolClass"})
    List<ResultCheckerPin> findByPinScopeAndSessionAndTermOrderByCreatedAtDesc(
            ResultCheckerPin.PinScope pinScope,
            String session,
            Result.Term term
    );
}