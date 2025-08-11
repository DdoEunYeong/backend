package com.unithon.ddoeunyeong.domain.advice.controller;

import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import com.unithon.ddoeunyeong.infra.gptapi.dto.AdivceReportResponse;
import com.unithon.ddoeunyeong.infra.gptapi.service.GptService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdviceController {

    GptService gptService;

    @GetMapping("/advice/{adviceId}/report")
    @Operation(summary = "마지막 상담 결과를 생성하는 API입니다.", description = "상담이 끝나면 해당 API를 호출해서 마지막 결과를 출력해주세요.")
    public BaseResponse<AdivceReportResponse> makeFinalReport(@PathVariable Long adviceId){
        try {
            return BaseResponse.<AdivceReportResponse>builder()
                    .code(200)
                    .message("상담 결과가 생성되었습니다.")
                    .data(gptService.askGptMakeFinalReport(adviceId))
                    .isSuccess(true)
                    .build();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FINAL_ERROR);
        }
    }
}
