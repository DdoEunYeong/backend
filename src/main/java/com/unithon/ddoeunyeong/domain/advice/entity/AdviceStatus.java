package com.unithon.ddoeunyeong.domain.advice.entity;

public enum AdviceStatus {

    /**
     * 상담 세션이 생성되었으나 아직 시작 전
     * (필요 시 예약 상태에 사용)
     */
    PENDING,

    /**
     * 상담이 진행 중인 상태
     */
    IN_PROGRESS,

    /**
     * 상담 영상이 정상적으로 저장됨
     */
    UPLOAD_SUCCESSED,

    /**
     * 상담이 정상 종료됨
     * (예: 소켓 종료 및 세션 종료)
     */
    COMPLETED,

    /**
     * 상담이 서버/사용자에 의해 취소됨
     */
    CANCELLED
}
