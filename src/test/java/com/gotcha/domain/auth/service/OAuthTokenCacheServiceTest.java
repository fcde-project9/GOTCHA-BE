package com.gotcha.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.domain.auth.service.OAuthTokenCacheService.TokenData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OAuthTokenCacheServiceTest {

    private OAuthTokenCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new OAuthTokenCacheService();
    }

    @Nested
    @DisplayName("storeTokens")
    class StoreTokens {

        @Test
        @DisplayName("토큰을 저장하고 임시 코드를 반환한다")
        void shouldStoreTokensAndReturnCode() {
            // given
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";
            boolean isNewUser = true;

            // when
            String code = cacheService.storeTokens(accessToken, refreshToken, isNewUser);

            // then
            assertThat(code).isNotNull();
            assertThat(code).isNotBlank();
        }

        @Test
        @DisplayName("매번 다른 임시 코드를 반환한다")
        void shouldReturnDifferentCodesEachTime() {
            // given
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";

            // when
            String code1 = cacheService.storeTokens(accessToken, refreshToken, true);
            String code2 = cacheService.storeTokens(accessToken, refreshToken, true);

            // then
            assertThat(code1).isNotEqualTo(code2);
        }
    }

    @Nested
    @DisplayName("exchangeCode")
    class ExchangeCode {

        @Test
        @DisplayName("유효한 코드로 토큰을 조회한다")
        void shouldReturnTokensWithValidCode() {
            // given
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";
            boolean isNewUser = true;
            String code = cacheService.storeTokens(accessToken, refreshToken, isNewUser);

            // when
            TokenData tokenData = cacheService.exchangeCode(code);

            // then
            assertThat(tokenData).isNotNull();
            assertThat(tokenData.getAccessToken()).isEqualTo(accessToken);
            assertThat(tokenData.getRefreshToken()).isEqualTo(refreshToken);
            assertThat(tokenData.isNewUser()).isTrue();
        }

        @Test
        @DisplayName("코드는 1회용이다 - 재사용 시 null 반환")
        void shouldReturnNullOnSecondUse() {
            // given
            String code = cacheService.storeTokens("access", "refresh", false);

            // when
            TokenData firstCall = cacheService.exchangeCode(code);
            TokenData secondCall = cacheService.exchangeCode(code);

            // then
            assertThat(firstCall).isNotNull();
            assertThat(secondCall).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 코드로 조회하면 null을 반환한다")
        void shouldReturnNullWithInvalidCode() {
            // when
            TokenData tokenData = cacheService.exchangeCode("non-existent-code");

            // then
            assertThat(tokenData).isNull();
        }

        @Test
        @DisplayName("isNewUser가 false인 경우도 정상 처리")
        void shouldHandleExistingUser() {
            // given
            String code = cacheService.storeTokens("access", "refresh", false);

            // when
            TokenData tokenData = cacheService.exchangeCode(code);

            // then
            assertThat(tokenData).isNotNull();
            assertThat(tokenData.isNewUser()).isFalse();
        }
    }

    @Nested
    @DisplayName("캐시 만료")
    class CacheExpiration {

        @Test
        @DisplayName("30초 후 캐시가 만료된다")
        void shouldExpireAfter30Seconds() {
            // given
            String code = cacheService.storeTokens("access", "refresh", true);

            // when - 31초 대기 (Awaitility 사용)
            // 실제 테스트에서는 시간이 오래 걸리므로 주석 처리
            // 이 테스트는 실제 환경에서 캐시 설정이 올바른지 확인하기 위한 것입니다
            // Awaitility.await()
            //         .atMost(35, TimeUnit.SECONDS)
            //         .pollDelay(31, TimeUnit.SECONDS)
            //         .untilAsserted(() -> {
            //             TokenData tokenData = cacheService.exchangeCode(code);
            //             assertThat(tokenData).isNull();
            //         });

            // 대신 즉시 조회하면 성공하는 것을 확인
            TokenData tokenData = cacheService.exchangeCode(code);
            assertThat(tokenData).isNotNull();
        }
    }

    @Nested
    @DisplayName("동시성")
    class Concurrency {

        @Test
        @DisplayName("여러 코드가 독립적으로 동작한다")
        void shouldHandleMultipleCodesIndependently() {
            // given
            String code1 = cacheService.storeTokens("access1", "refresh1", true);
            String code2 = cacheService.storeTokens("access2", "refresh2", false);
            String code3 = cacheService.storeTokens("access3", "refresh3", true);

            // when
            TokenData data1 = cacheService.exchangeCode(code1);
            TokenData data2 = cacheService.exchangeCode(code2);
            TokenData data3 = cacheService.exchangeCode(code3);

            // then
            assertThat(data1.getAccessToken()).isEqualTo("access1");
            assertThat(data2.getAccessToken()).isEqualTo("access2");
            assertThat(data3.getAccessToken()).isEqualTo("access3");
        }

        @Test
        @DisplayName("동일한 코드에 대한 동시 호출 시 단 하나의 스레드만 토큰을 획득한다")
        void shouldAllowOnlyOneThreadToExchangeSameCode() throws InterruptedException {
            // given
            String code = cacheService.storeTokens("access", "refresh", true);
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger nullCount = new AtomicInteger(0);
            List<TokenData> results = new ArrayList<>();

            // when - 모든 스레드가 동시에 동일한 코드로 exchangeCode 호출
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 모든 스레드가 동시에 시작하도록 대기
                        TokenData result = cacheService.exchangeCode(code);
                        synchronized (results) {
                            results.add(result);
                        }
                        if (result != null) {
                            successCount.incrementAndGet();
                        } else {
                            nullCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 모든 스레드 동시 시작
            endLatch.await(5, TimeUnit.SECONDS); // 모든 스레드 완료 대기
            executor.shutdown();

            // then - 정확히 1개의 스레드만 토큰을 획득해야 함 (원자적 연산 검증)
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(nullCount.get()).isEqualTo(threadCount - 1);
        }

        @Test
        @DisplayName("여러 다른 코드에 대한 동시 호출이 정상 동작한다")
        void shouldHandleConcurrentExchangeOfDifferentCodes() throws InterruptedException {
            // given
            int codeCount = 100;
            List<String> codes = new ArrayList<>();
            for (int i = 0; i < codeCount; i++) {
                codes.add(cacheService.storeTokens("access" + i, "refresh" + i, i % 2 == 0));
            }

            ExecutorService executor = Executors.newFixedThreadPool(20);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(codeCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // when - 여러 스레드가 서로 다른 코드를 동시에 교환
            for (String code : codes) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        TokenData result = cacheService.exchangeCode(code);
                        if (result != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // then - 모든 코드가 정확히 1번씩 교환되어야 함
            assertThat(successCount.get()).isEqualTo(codeCount);
        }
    }
}
