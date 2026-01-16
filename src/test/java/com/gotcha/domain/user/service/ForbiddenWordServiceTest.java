package com.gotcha.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ForbiddenWordService 테스트
 *
 * 주의: 닉네임 형식이 ^[가-힣a-zA-Z]+$ 이므로 특수문자, 숫자, 공백, 초성(ㄱ-ㅎ)은
 * DTO 유효성 검사에서 이미 걸러집니다. 이 테스트는 유효한 형식의 닉네임만 검사합니다.
 */
class ForbiddenWordServiceTest {

    private ForbiddenWordService forbiddenWordService;

    @BeforeEach
    void setUp() {
        forbiddenWordService = new ForbiddenWordService();
        forbiddenWordService.init();
    }

    @Nested
    @DisplayName("한국어 금칙어 검사")
    class KoreanForbiddenWords {

        @Test
        @DisplayName("기본 금칙어 검출")
        void detectBasicForbiddenWord() {
            assertThat(forbiddenWordService.containsForbiddenWord("씨발")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("병신")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("지랄")).isTrue();
        }

        @Test
        @DisplayName("유사 발음 변형 검출 - 시발")
        void detectSimilarPronunciation() {
            assertThat(forbiddenWordService.containsForbiddenWord("시발")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("씨팔")).isTrue();
        }

        @Test
        @DisplayName("금칙어가 포함된 닉네임 검출")
        void detectForbiddenWordInNickname() {
            assertThat(forbiddenWordService.containsForbiddenWord("나는씨발이다")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("병신아")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("지랄하네")).isTrue();
        }

        @Test
        @DisplayName("금칙어 앞뒤로 일반 텍스트가 있는 경우")
        void detectForbiddenWordWithSurroundingText() {
            assertThat(forbiddenWordService.containsForbiddenWord("아씨발진짜")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("개병신같은")).isTrue();
        }
    }

    @Nested
    @DisplayName("영어 금칙어 검사")
    class EnglishForbiddenWords {

        @Test
        @DisplayName("기본 금칙어 검출")
        void detectBasicForbiddenWord() {
            assertThat(forbiddenWordService.containsForbiddenWord("fuck")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("shit")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("bitch")).isTrue();
        }

        @Test
        @DisplayName("대소문자 무관 검출")
        void detectCaseInsensitive() {
            assertThat(forbiddenWordService.containsForbiddenWord("FUCK")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("Fuck")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("FuCk")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("SHIT")).isTrue();
        }

        @Test
        @DisplayName("금칙어가 포함된 닉네임 검출")
        void detectForbiddenWordInNickname() {
            assertThat(forbiddenWordService.containsForbiddenWord("fuckYou")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("HolyShit")).isTrue();
            assertThat(forbiddenWordService.containsForbiddenWord("SonOfBitch")).isTrue();
        }
    }

    @Nested
    @DisplayName("정상 닉네임 통과")
    class ValidNicknames {

        @Test
        @DisplayName("일반 한글 닉네임 통과")
        void allowKoreanNickname() {
            assertThat(forbiddenWordService.containsForbiddenWord("가챠왕")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("뽑기마스터")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("빨간캡슐")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("파란토끼")).isFalse();
        }

        @Test
        @DisplayName("일반 영어 닉네임 통과")
        void allowEnglishNickname() {
            assertThat(forbiddenWordService.containsForbiddenWord("GachaKing")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("CapsuleMaster")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("BlueCapsule")).isFalse();
        }

        @Test
        @DisplayName("한영 혼합 닉네임 통과")
        void allowMixedNickname() {
            assertThat(forbiddenWordService.containsForbiddenWord("가챠King")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("Capsule왕")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("뽑기Master")).isFalse();
        }

        @Test
        @DisplayName("금칙어와 유사하지만 다른 단어 통과")
        void allowSimilarButDifferentWords() {
            // 시바견: 초성 ㅅㅂ가 욕설 초성과 같지만, 실제 초성 문자가 아니므로 통과
            assertThat(forbiddenWordService.containsForbiddenWord("시바견")).isFalse();
            // 일반 단어들
            assertThat(forbiddenWordService.containsForbiddenWord("HelloWorld")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("GameMaster")).isFalse();
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("빈 문자열")
        void emptyString() {
            assertThat(forbiddenWordService.containsForbiddenWord("")).isFalse();
        }

        @Test
        @DisplayName("null 입력")
        void nullInput() {
            assertThat(forbiddenWordService.containsForbiddenWord(null)).isFalse();
        }

        @Test
        @DisplayName("공백만 있는 경우")
        void onlySpaces() {
            assertThat(forbiddenWordService.containsForbiddenWord("   ")).isFalse();
        }

        @Test
        @DisplayName("한 글자 닉네임")
        void singleCharacter() {
            assertThat(forbiddenWordService.containsForbiddenWord("가")).isFalse();
            assertThat(forbiddenWordService.containsForbiddenWord("A")).isFalse();
        }

        @Test
        @DisplayName("최대 길이 닉네임")
        void maxLengthNickname() {
            assertThat(forbiddenWordService.containsForbiddenWord("가나다라마바사아자차카타")).isFalse();
        }
    }
}
