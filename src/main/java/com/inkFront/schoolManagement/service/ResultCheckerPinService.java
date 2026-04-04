package com.inkFront.schoolManagement.service;

import com.inkFront.schoolManagement.dto.GeneratedResultCheckerPinDTO;
import com.inkFront.schoolManagement.dto.ResultCheckerPinCreateDTO;
import com.inkFront.schoolManagement.dto.ResultCheckerPinResponseDTO;
import com.inkFront.schoolManagement.dto.ResultPinVerificationResponseDTO;
import com.inkFront.schoolManagement.model.Result;

import java.util.List;

public interface ResultCheckerPinService {

    List<GeneratedResultCheckerPinDTO> generatePins(
            ResultCheckerPinCreateDTO request,
            String createdByName
    );

    List<ResultCheckerPinResponseDTO> getAllPins();

    ResultCheckerPinResponseDTO deactivatePin(Long pinId);

    ResultPinVerificationResponseDTO verifyTermPin(
            Long studentId,
            String session,
            Result.Term term,
            String rawPin
    );

    ResultPinVerificationResponseDTO verifySessionPin(
            Long studentId,
            String session,
            String rawPin
    );

    void consumeTermPin(
            Long studentId,
            String session,
            Result.Term term,
            String rawPin,
            String usedByName
    );

    void consumeSessionPin(
            Long studentId,
            String session,
            String rawPin,
            String usedByName
    );
}