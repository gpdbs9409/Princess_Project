package com.example.princessproject.common;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * OpenAI(비전 인증 + 레오집사 피드백) 호출 전용 동시성 제한 + 재시도 헬퍼.
 *
 * <p>배경 (2026-09): "동시에 여러 명이 인증 버튼을 누르면 가끔 오류가 난다"는 제보의 실제 원인은
 * 스레드 부족이 아니라 (1) OpenAI로 나가는 동시 요청 수가 순간적으로 튀어 OpenAI 쪽 레이트리밋
 * (429)이나 일시적 5xx를 만나는 것, (2) 그 실패가 우리 쪽에서 재시도 없이 그대로 사용자에게
 * 에러로 노출되는 것 두 가지였다. 이 서비스 규모(챌린지 코호트 단위, 동시 접속 수십~수백 명)에서는
 * Kafka/RabbitMQ/Redis 같은 별도 메시지 큐 인프라를 둘 필요가 없다 - 애플리케이션 안에 작은 대기열
 * (bounded semaphore) 하나만 둬도 순간적으로 몰리는 요청을 흡수하기에 충분하다.
 *
 * <p>동작: 허용된 개수(MAX_CONCURRENT_CALLS)를 넘는 호출은 스레드를 추가로 만들지 않고 permit이
 * 빌 때까지 짧게 대기했다가 순서대로 OpenAI에 나간다 - 즉, 이 앱 내부에 있는 "API 대기열"이다.
 * 또한 429/5xx를 만나면 지수 백오프(+지터)로 자동 재시도하므로, 순간적인 레이트리밋 정도는 사용자
 * 화면에 에러로 보이지 않고 조용히 넘어간다.
 */
@Component
public class OpenAiCallLimiter {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCallLimiter.class);

    // 비전/피드백 호출 하나가 수 초씩 걸릴 수 있어 동시에 너무 많이 내보내면 OpenAI 레이트리밋에
    // 걸리기 쉽다. 이 앱 규모(코호트당 수십~백여 명, 실시간 동시 인증은 그보다 훨씬 적음)에서는
    // 6개면 마감 시간대의 순간적인 몰림도 흡수하면서 개별 요청 지연은 크지 않다.
    private static final int MAX_CONCURRENT_CALLS = 6;
    // 이 이상 기다려야 한다면 대기열이 아니라 진짜 과부하 상태이므로, 무한 대기 대신 명확한 에러로
    // 실패시켜 사용자에게 "잠시 후 다시" 안내를 보여주는 편이 낫다.
    private static final long MAX_WAIT_SECONDS = 25;
    private static final int MAX_ATTEMPTS = 3;

    // fair=true: 먼저 기다린 요청부터 순서대로 permit을 받는다 (한 사용자가 여러 번 눌러 계속
    // 새치기하는 상황 방지).
    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_CALLS, true);

    public <T> T call(Supplier<T> openAiCall) {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an OpenAI call slot", e);
        }
        if (!acquired) {
            throw new OpenAiOverloadedException();
        }
        try {
            return callWithRetry(openAiCall);
        } finally {
            semaphore.release();
        }
    }

    private <T> T callWithRetry(Supplier<T> openAiCall) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return openAiCall.get();
            } catch (HttpClientErrorException.TooManyRequests e) {
                lastError = e;
                log.warn("OpenAI rate-limited us (attempt {}/{})", attempt, MAX_ATTEMPTS);
            } catch (HttpServerErrorException e) {
                lastError = e;
                log.warn("OpenAI returned a server error (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getStatusCode());
            }
            if (attempt < MAX_ATTEMPTS) {
                sleepBackoff(attempt);
            }
        }
        throw lastError;
    }

    private void sleepBackoff(int attempt) {
        try {
            // 0.5s, 1s, 2s ... + 지터. 여러 요청이 동시에 429를 맞아도 재시도가 같은 타이밍에
            // 다시 몰리지 않도록 무작위성을 섞는다.
            long baseMillis = 500L * (1L << (attempt - 1));
            long jitterMillis = ThreadLocalRandom.current().nextLong(250);
            Thread.sleep(baseMillis + jitterMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 대기열이 25초 안에 빠지지 않을 만큼 과부하일 때 던진다. GlobalExceptionHandler가 처리한다. */
    public static class OpenAiOverloadedException extends RuntimeException {
        public OpenAiOverloadedException() {
            super("지금 인증 요청이 많이 몰려 있어요. 잠시 후 다시 시도해주세요.");
        }
    }
}
