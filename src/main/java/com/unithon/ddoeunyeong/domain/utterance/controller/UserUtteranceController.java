package com.unithon.ddoeunyeong.domain.utterance.controller;

import com.unithon.ddoeunyeong.domain.advice.entity.Emotion;
import com.unithon.ddoeunyeong.domain.utterance.dto.SaveUtteranceResponse;
import com.unithon.ddoeunyeong.domain.utterance.service.UserUtteranceService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.infra.fastapi.face_service.FaceAnalysisService;
import com.unithon.ddoeunyeong.infra.fastapi.stt_service.STTService;
import com.unithon.ddoeunyeong.domain.utterance.dto.ProcessUtteranceResponse;
import com.unithon.ddoeunyeong.infra.gptapi.service.GptService;
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

    private final GptService gptService;
    private final STTService sttService;
    private final FaceAnalysisService faceAnalysisService;
    private final UserUtteranceService userUtteranceService;

    @PostMapping(value = "/advice/{adviceId}/answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "답변 음성을 stt로 변경하고, 스크린샷으로 감정 분석 후, GPT로 다음 질문 반환")
    public BaseResponse<ProcessUtteranceResponse> answerQuestion(
            @PathVariable Long adviceId,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("image") MultipartFile image
    ) throws IOException {
        if (audio == null || audio.isEmpty()) {
            return BaseResponse.<ProcessUtteranceResponse>builder()
                    .isSuccess(false).code(400).message("audio 파일이 비어있습니다.").data(null).build();
        }
        if (image != null && !image.isEmpty()) {
            String ctype = image.getContentType();
            if (ctype == null || !ctype.startsWith("image/")) {
                return BaseResponse.<ProcessUtteranceResponse>builder()
                        .isSuccess(false).code(400).message("image는 이미지 파일이어야 합니다.").data(null).build();
            }
        }

        String userText = sttService.sttTranscribeAndMerge(audio);
        Emotion userEmotion = faceAnalysisService.faceEmotionAnalyze(image); // 동기 버전 사용
        userUtteranceService.saveLastUtterance(userText, userEmotion, adviceId);

        return BaseResponse.<ProcessUtteranceResponse>builder()
                .isSuccess(true).code(200).message("꼬리질문이 생성되었습니다.")
                .data(gptService.askGptAfterSurvey(userText, adviceId))
                .build();
    }

    @PostMapping(value = "/advice/{adviceId}/last-answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "답변 음성을 stt로 변경하고, 스크린샷으로 감정 분석 후 저장")
    public BaseResponse<SaveUtteranceResponse> answerLastQuestion(
            @PathVariable Long adviceId,
            @RequestParam("audio") MultipartFile audio,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws IOException {
        if (audio == null || audio.isEmpty()) {
            return BaseResponse.<SaveUtteranceResponse>builder()
                    .isSuccess(false).code(400).message("audio 파일이 비어있습니다.").data(null).build();
        }
        if (image != null && !image.isEmpty()) {
            String ctype = image.getContentType();
            if (ctype == null || !ctype.startsWith("image/")) {
                return BaseResponse.<SaveUtteranceResponse>builder()
                        .isSuccess(false).code(400).message("image는 이미지 파일이어야 합니다.").data(null).build();
            }
        }

        String userText = sttService.sttTranscribeAndMerge(audio);
        Emotion userEmotion = faceAnalysisService.faceEmotionAnalyze(image);

        return BaseResponse.<SaveUtteranceResponse>builder()
                .isSuccess(true).code(200).message("상담이 종료되었습니다.")
                .data(userUtteranceService.saveLastUtterance(userText, userEmotion, adviceId))
                .build();
    }
}

