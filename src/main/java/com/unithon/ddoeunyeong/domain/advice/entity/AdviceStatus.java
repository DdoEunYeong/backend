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
     * 상담이 정상적으로 종료됨
     */
    COMPLETED,

    /**
     * 상담이 비정상 종료됨
     * (예: 네트워크 끊김, 클라이언트 강제 종료)
     */
    ABORTED,

    /**
     * 상담이 서버/사용자에 의해 취소됨
     */
    CANCELLED
}
