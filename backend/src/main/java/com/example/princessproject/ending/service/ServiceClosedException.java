package com.example.princessproject.ending.service;

/** 10/21 00:00(KST) 이후 운영 종료 - 로그인 및 기존 세션 요청을 막는다. */
public class ServiceClosedException extends RuntimeException {
    public static final String CODE = "SERVICE_CLOSED";

    public ServiceClosedException() {
        super("프린세스 프로젝트 1기 운영이 종료되었어요.");
    }
}
