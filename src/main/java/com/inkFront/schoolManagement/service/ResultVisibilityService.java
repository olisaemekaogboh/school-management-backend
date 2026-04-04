package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.ResultVisibilityUpdateDTO;
import com.inkFront.schoolManagement.model.Result;

import java.util.Map;

public interface ResultVisibilityService {

    Map<String, Object> updateTermVisibilityForStudent(
            Long studentId,
            String session,
            Result.Term term,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    );

    Map<String, Object> updateTermVisibilityForClass(
            Long classId,
            String session,
            Result.Term term,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    );

    Map<String, Object> updateSessionVisibilityForStudent(
            Long studentId,
            String session,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    );

    Map<String, Object> updateSessionVisibilityForClass(
            Long classId,
            String session,
            ResultVisibilityUpdateDTO request,
            String publishedByName
    );
}