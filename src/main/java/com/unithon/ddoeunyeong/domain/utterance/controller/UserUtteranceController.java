package com.unithon.ddoeunyeong.domain.utterance.controller;

import com.unithon.ddoeunyeong.domain.utterance.dto.LastUtteranceResponse;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.infra.fastapi.stt.STTService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserUtteranceController {

    private final STTService sttService;

    @PostMapping(value = "/stt/last-answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "stt & 마지막 답변 저장")
    public BaseResponse<LastUtteranceResponse> answerLastQuestion(@RequestParam("audio") MultipartFile audioFile, @RequestParam Long adviceId) throws IOException {

        return BaseResponse.<LastUtteranceResponse>builder().isSuccess(true).code(200).message("상담이 종료되었습니다.").data(sttService.sendToFastApiAndSaveLastUtterance(audioFile, adviceId)).build();
    }
}

