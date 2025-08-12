package com.unithon.ddoeunyeong.domain.advice.controller;

import com.unithon.ddoeunyeong.domain.advice.dto.AdviceReportResponse;
import com.unithon.ddoeunyeong.domain.advice.service.AdviceService;
import com.unithon.ddoeunyeong.global.exception.BaseResponse;
import com.unithon.ddoeunyeong.global.exception.CustomException;
import com.unithon.ddoeunyeong.global.exception.ErrorCode;
import com.unithon.ddoeunyeong.infra.gptapi.dto.GPTAdviceReportResponse;
import com.unithon.ddoeunyeong.infra.gptapi.service.GptService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdviceController {

    private final GptService gptService;
    private final AdviceService adviceService;

    @GetMapping("/advice/{adviceId}/report")
    @Operation(summary = "마지막 상담 결과를 조회하는 API입니다.", description = "해당 API를 호출해서 상담 결과를 출력해주세요.")
    public BaseResponse<AdviceReportResponse> getFinalReport(@PathVariable Long adviceId) throws IOException {

        return BaseResponse.<AdviceReportResponse>builder()
                .code(200)
                .message("상담 결과가 생성되었습니다.")
                .data(adviceService.getAdviceReport(adviceId))
                .isSuccess(true)
                .build();
    }

    @PostMapping("/advice/{adviceId}/report")
    @Operation(summary = "마지막 상담 결과를 생성하는 API입니다.", description = "상담이 끝나면 해당 API를 호출해서 마지막 결과를 출력해주세요.")
    public BaseResponse<AdviceReportResponse> makeFinalReport(@PathVariable Long adviceId) throws IOException {
        GPTAdviceReportResponse gpt = gptService.askGptMakeFinalReport(adviceId);

        return BaseResponse.<AdviceReportResponse>builder()
                .code(200)
                .message("상담 결과가 생성되었습니다.")
                .data(adviceService.makeAdviceReport(adviceId, gpt))
                .isSuccess(true)
                .build();
    }
}
