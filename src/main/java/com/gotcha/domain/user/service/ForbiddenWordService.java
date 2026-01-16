package com.gotcha.domain.user.service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 금칙어 필터링 서비스
 * 닉네임에 욕설, 비속어, 차별/혐오 표현이 포함되어 있는지 검사합니다.
 */
@Slf4j
@Service
public class ForbiddenWordService {

    private final Set<String> koreanForbiddenWords = new HashSet<>();
    private final Set<String> englishForbiddenWords = new HashSet<>();
    private final Set<String> chosungForbiddenWords = new HashSet<>();

    // 한글 초성 매핑
    private static final char[] CHOSUNG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    // 한글 중성(모음) 매핑
    private static final char[] JUNGSUNG = {
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };

    // 한글 종성 매핑 (첫 번째는 종성 없음)
    private static final char[] JONGSUNG = {
            '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
            'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    @PostConstruct
    public void init() {
        loadForbiddenWords("forbidden-words/ko.txt", koreanForbiddenWords, true);
        loadForbiddenWords("forbidden-words/en.txt", englishForbiddenWords, false);
        log.info("Forbidden words loaded - Korean: {}, English: {}, Chosung: {}",
                koreanForbiddenWords.size(), englishForbiddenWords.size(), chosungForbiddenWords.size());
    }

    private void loadForbiddenWords(String resourcePath, Set<String> targetSet, boolean isKorean) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // 빈 줄이나 주석 무시
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    if (isKorean) {
                        // 초성만으로 이루어진 단어는 별도 관리
                        if (isChosungOnly(line)) {
                            chosungForbiddenWords.add(line);
                        } else {
                            targetSet.add(line);
                        }
                    } else {
                        targetSet.add(line.toLowerCase());
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load forbidden words from {}: {}", resourcePath, e.getMessage());
        }
    }

    /**
     * 닉네임에 금칙어가 포함되어 있는지 검사
     *
     * @param nickname 검사할 닉네임
     * @return 금칙어 포함 여부
     */
    public boolean containsForbiddenWord(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        // 1. 한국어 금칙어 검사
        if (containsKoreanForbiddenWord(nickname)) {
            return true;
        }

        // 2. 영어 금칙어 검사
        if (containsEnglishForbiddenWord(nickname)) {
            return true;
        }

        // 3. 초성 금칙어 검사
        if (containsChosungForbiddenWord(nickname)) {
            return true;
        }

        return false;
    }

    private boolean containsKoreanForbiddenWord(String text) {
        String normalized = normalizeKorean(text);

        for (String forbidden : koreanForbiddenWords) {
            if (normalized.contains(forbidden)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsEnglishForbiddenWord(String text) {
        String normalized = normalizeEnglish(text);

        for (String forbidden : englishForbiddenWords) {
            // 금칙어도 동일하게 정규화하여 비교 (ass → as로 비교)
            String normalizedForbidden = removeRepeatedCharacters(forbidden);

            // 짧은 금칙어(3글자 미만)는 정확히 일치할 때만 검사 (과탐 방지)
            if (normalizedForbidden.length() < 3) {
                if (normalized.equals(normalizedForbidden)) {
                    return true;
                }
            } else if (normalized.contains(normalizedForbidden)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsChosungForbiddenWord(String text) {
        // 입력에 실제 초성 문자(ㄱ-ㅎ)가 포함된 경우만 검사
        // 일반 한글에서 초성을 추출하면 오탐이 발생함 (예: 시바견 → ㅅㅂㄱ → ㅅㅂ 감지)
        String chosungOnly = text.replaceAll("[^ㄱ-ㅎ]", "");

        // 초성이 전혀 없으면 검사 불필요
        if (chosungOnly.isEmpty()) {
            return false;
        }

        for (String forbidden : chosungForbiddenWords) {
            if (chosungOnly.contains(forbidden)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 한국어 텍스트 정규화
     * - 특수문자/공백/영문/숫자 제거
     * - 자모 분해 후 모음 반복 제거
     * - 유사 발음 치환
     */
    private String normalizeKorean(String text) {
        String result = text;

        // 1. 특수문자, 공백, 영문, 숫자 제거 (한글, 자모만 남김)
        // 영문/숫자 삽입 우회 방지: 시1발, 씨b발 → 시발, 씨발
        result = result.replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣ]", "");

        // 2. 자모 입력을 완성형으로 조합 시도 (ㅅㅣㅂㅏㄹ → 시발)
        result = combineJamo(result);

        // 3. 자모 분해 후 모음 반복 제거 (씨이이이발 → 씨발)
        result = removeVowelRepetition(result);

        // 4. 유사 발음 치환
        result = result.replace("시", "씨")
                .replace("팔", "발")
                .replace("색", "새")
                .replace("삭", "새")
                .replace("쉬", "씨")
                .replace("쒸", "씨")
                .replace("벌", "발")
                .replace("십", "씹")
                .replace("섭", "썹");

        // 5. 반복 음절 제거 (씨씨발 → 씨발)
        result = removeRepeatedCharacters(result);

        return result;
    }

    /**
     * 자모 분해 후 모음(중성) 반복을 제거하여 재조합
     * 예: "씨이이이발" → 자모 분해 → 모음 반복 제거 → "씨발"
     */
    private String removeVowelRepetition(String text) {
        // 자모로 분해
        StringBuilder jamo = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= '가' && c <= '힣') {
                int code = c - '가';
                int cho = code / (21 * 28);
                int jung = (code % (21 * 28)) / 28;
                int jong = code % 28;

                jamo.append(CHOSUNG[cho]);
                jamo.append(JUNGSUNG[jung]);
                if (jong != 0) {
                    jamo.append(JONGSUNG[jong]);
                }
            } else if ((c >= 'ㄱ' && c <= 'ㅎ') || (c >= 'ㅏ' && c <= 'ㅣ')) {
                jamo.append(c);
            }
        }

        // 자모 시퀀스에서 연속 모음 제거 (ㅅㅣㅇㅣㅇㅣㅂㅏㄹ → ㅅㅣㅂㅏㄹ)
        String jamoStr = jamo.toString();
        // 모음 뒤에 같은 모음이 반복되면 제거 (ㅣㅇㅣ 패턴 → ㅣ)
        // 'ㅇ' + 모음 패턴이 반복되면 제거 (이이이 → 이)
        jamoStr = jamoStr.replaceAll("([ㅏ-ㅣ])(ㅇ\\1)+", "$1");
        // 단순 모음 반복 제거
        jamoStr = jamoStr.replaceAll("([ㅏ-ㅣ])\\1+", "$1");

        // 다시 완성형으로 조합
        return combineJamo(jamoStr);
    }

    /**
     * 자모 시퀀스를 완성형 한글로 조합
     * 예: "ㅅㅣㅂㅏㄹ" → "시발"
     */
    private String combineJamo(String text) {
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        int i = 0;

        while (i < chars.length) {
            char c = chars[i];

            // 완성형 한글이면 그대로
            if (c >= '가' && c <= '힣') {
                result.append(c);
                i++;
                continue;
            }

            // 초성인 경우 조합 시도
            int choIndex = getChosungIndex(c);
            if (choIndex >= 0 && i + 1 < chars.length) {
                int jungIndex = getJungsungIndex(chars[i + 1]);
                if (jungIndex >= 0) {
                    // 초성 + 중성 발견
                    int jongIndex = 0;
                    if (i + 2 < chars.length) {
                        int possibleJong = getJongsungIndex(chars[i + 2]);
                        // 다음 문자가 종성이 될 수 있고, 그 다음이 중성이 아닌 경우만 종성으로 처리
                        if (possibleJong > 0) {
                            if (i + 3 >= chars.length || getJungsungIndex(chars[i + 3]) < 0) {
                                jongIndex = possibleJong;
                                i++;
                            }
                        }
                    }
                    char combined = (char) ('가' + (choIndex * 21 * 28) + (jungIndex * 28) + jongIndex);
                    result.append(combined);
                    i += 2;
                    continue;
                }
            }

            // 조합 불가능하면 그대로 추가
            result.append(c);
            i++;
        }

        return result.toString();
    }

    private int getChosungIndex(char c) {
        for (int i = 0; i < CHOSUNG.length; i++) {
            if (CHOSUNG[i] == c) return i;
        }
        return -1;
    }

    private int getJungsungIndex(char c) {
        for (int i = 0; i < JUNGSUNG.length; i++) {
            if (JUNGSUNG[i] == c) return i;
        }
        return -1;
    }

    private int getJongsungIndex(char c) {
        for (int i = 0; i < JONGSUNG.length; i++) {
            if (JONGSUNG[i] == c) return i;
        }
        return -1;
    }

    /**
     * 영어 텍스트 정규화
     * - 소문자 변환
     * - Leet speak 치환 (숫자/특수문자 → 알파벳)
     * - 특수문자/공백 제거
     * - 반복 문자 제거
     */
    private String normalizeEnglish(String text) {
        String result = text.toLowerCase();

        // 1. Leet speak 치환 먼저 (특수문자 제거 전에 처리)
        result = result.replace("@", "a")
                .replace("$", "s")
                .replace("!", "i")
                .replace("0", "o")
                .replace("1", "i")
                .replace("2", "z")
                .replace("3", "e")
                .replace("4", "a")
                .replace("5", "s")
                .replace("6", "g")
                .replace("7", "t")
                .replace("8", "b")
                .replace("9", "g");

        // 2. 특수문자, 공백 제거 (알파벳만 남김)
        result = result.replaceAll("[^a-z]", "");

        // 3. 반복 문자 제거 (fuuuck → fuck)
        result = removeRepeatedCharacters(result);

        return result;
    }

    /**
     * 반복 문자 제거 (연속 동일 문자를 1개로 축소)
     * 예: "씨씨발" → "씨발", "fuuuck" → "fuck"
     */
    private String removeRepeatedCharacters(String text) {
        if (text == null || text.length() < 2) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(text.charAt(0));

        for (int i = 1; i < text.length(); i++) {
            char current = text.charAt(i);
            char previous = text.charAt(i - 1);

            // 같은 문자가 연속되면 스킵 (1개만 유지)
            if (current != previous) {
                sb.append(current);
            }
        }

        return sb.toString();
    }

    /**
     * 한글 텍스트에서 초성 추출
     * 예: "시발" → "ㅅㅂ"
     */
    private String extractChosung(String text) {
        StringBuilder sb = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (c >= '가' && c <= '힣') {
                int index = (c - '가') / 588;
                sb.append(CHOSUNG[index]);
            } else if (c >= 'ㄱ' && c <= 'ㅎ') {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 초성만으로 이루어진 문자열인지 확인
     */
    private boolean isChosungOnly(String text) {
        for (char c : text.toCharArray()) {
            if (c < 'ㄱ' || c > 'ㅎ') {
                return false;
            }
        }
        return true;
    }
}
