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
            if (normalized.contains(forbidden)) {
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
     * - 특수문자/공백 제거
     * - 반복 문자 제거
     * - 유사 발음 치환
     */
    private String normalizeKorean(String text) {
        String result = text;

        // 1. 특수문자, 공백 제거 (한글, 영문, 숫자, 초성만 남김)
        result = result.replaceAll("[^가-힣a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ]", "");

        // 2. 반복 문자 제거 (씨이이이 → 씨이)
        result = removeRepeatedCharacters(result);

        // 3. 유사 발음 치환
        result = result.replace("시", "씨")
                .replace("팔", "발")
                .replace("색", "새")
                .replace("삭", "새")
                .replace("쉬", "씨")
                .replace("쒸", "씨");

        return result;
    }

    /**
     * 영어 텍스트 정규화
     * - 소문자 변환
     * - 특수문자/공백 제거
     * - 반복 문자 제거
     * - Leet speak 치환
     */
    private String normalizeEnglish(String text) {
        String result = text.toLowerCase();

        // 1. 특수문자, 공백 제거
        result = result.replaceAll("[^a-z0-9]", "");

        // 2. 반복 문자 제거 (fuuuck → fuck)
        result = removeRepeatedCharacters(result);

        // 3. Leet speak 치환
        result = result.replace("0", "o")
                .replace("1", "i")
                .replace("3", "e")
                .replace("4", "a")
                .replace("5", "s")
                .replace("7", "t")
                .replace("8", "b")
                .replace("@", "a")
                .replace("$", "s");

        return result;
    }

    /**
     * 반복 문자 제거
     * 예: "씨이이이발" → "씨이발", "fuuuck" → "fuck"
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

            // 같은 문자가 반복되면 스킵 (단, 2번까지는 허용 - 씨이 같은 패턴 감지용)
            if (current != previous || (sb.length() >= 2 && sb.charAt(sb.length() - 2) != current)) {
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
