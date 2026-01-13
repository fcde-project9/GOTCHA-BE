package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@DisplayName("OAuth2AuthorizationRequestSerializer 테스트")
class OAuth2AuthorizationRequestSerializerTest {

    private static final String ENCRYPTION_KEY = "test-encryption-key-must-be-32-chars!!";

    private OAuth2AuthorizationRequestSerializer serializer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        serializer = new OAuth2AuthorizationRequestSerializer(objectMapper, ENCRYPTION_KEY);
    }

    @Nested
    @DisplayName("직렬화/역직렬화")
    class SerializeAndDeserialize {

        @Test
        @DisplayName("기본 OAuth2AuthorizationRequest 직렬화/역직렬화 성공")
        void basicRequest_serializeAndDeserialize() {
            // given
            OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                    .clientId("test-client-id")
                    .redirectUri("http://localhost:8080/callback")
                    .state("test-state-12345")
                    .build();

            // when
            String serialized = serializer.serialize(original);
            OAuth2AuthorizationRequest deserialized = serializer.deserialize(serialized);

            // then
            assertThat(serialized).isNotNull();
            assertThat(deserialized).isNotNull();
            assertThat(deserialized.getAuthorizationUri()).isEqualTo(original.getAuthorizationUri());
            assertThat(deserialized.getClientId()).isEqualTo(original.getClientId());
            assertThat(deserialized.getRedirectUri()).isEqualTo(original.getRedirectUri());
            assertThat(deserialized.getState()).isEqualTo(original.getState());
        }

        @Test
        @DisplayName("scope가 포함된 요청 직렬화/역직렬화 성공")
        void requestWithScopes_serializeAndDeserialize() {
            // given
            OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                    .clientId("test-client-id")
                    .redirectUri("http://localhost:8080/callback")
                    .scopes(Set.of("profile", "email"))
                    .state("test-state-12345")
                    .build();

            // when
            String serialized = serializer.serialize(original);
            OAuth2AuthorizationRequest deserialized = serializer.deserialize(serialized);

            // then
            assertThat(deserialized.getScopes()).containsExactlyInAnyOrder("profile", "email");
        }

        @Test
        @DisplayName("null 요청 직렬화 시 null 반환")
        void nullRequest_returnsNull() {
            // when
            String result = serializer.serialize(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null 문자열 역직렬화 시 null 반환")
        void nullString_returnsNull() {
            // when
            OAuth2AuthorizationRequest result = serializer.deserialize(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("잘못된 암호화 문자열 역직렬화 시 null 반환")
        void invalidEncryptedString_returnsNull() {
            // when
            OAuth2AuthorizationRequest result = serializer.deserialize("invalid-encrypted-data");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("암호화 키 처리")
    class EncryptionKeyHandling {

        @Test
        @DisplayName("짧은 키로도 동작 (패딩 적용)")
        void shortKey_workWithPadding() {
            // given
            OAuth2AuthorizationRequestSerializer shortKeySerializer =
                    new OAuth2AuthorizationRequestSerializer(new ObjectMapper(), "short-key");

            OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("https://example.com/authorize")
                    .clientId("client-id")
                    .redirectUri("http://localhost/callback")
                    .state("state")
                    .build();

            // when
            String serialized = shortKeySerializer.serialize(original);
            OAuth2AuthorizationRequest deserialized = shortKeySerializer.deserialize(serialized);

            // then
            assertThat(deserialized).isNotNull();
            assertThat(deserialized.getState()).isEqualTo("state");
        }

        @Test
        @DisplayName("긴 키로도 동작 (자르기 적용)")
        void longKey_workWithTruncation() {
            // given
            String longKey = "this-is-a-very-long-encryption-key-that-exceeds-32-characters-limit";
            OAuth2AuthorizationRequestSerializer longKeySerializer =
                    new OAuth2AuthorizationRequestSerializer(new ObjectMapper(), longKey);

            OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("https://example.com/authorize")
                    .clientId("client-id")
                    .redirectUri("http://localhost/callback")
                    .state("state")
                    .build();

            // when
            String serialized = longKeySerializer.serialize(original);
            OAuth2AuthorizationRequest deserialized = longKeySerializer.deserialize(serialized);

            // then
            assertThat(deserialized).isNotNull();
            assertThat(deserialized.getState()).isEqualTo("state");
        }

        @Test
        @DisplayName("다른 키로 역직렬화 시 실패")
        void differentKey_deserializationFails() {
            // given
            OAuth2AuthorizationRequestSerializer serializer1 =
                    new OAuth2AuthorizationRequestSerializer(new ObjectMapper(), "encryption-key-1-32-characters!!");
            OAuth2AuthorizationRequestSerializer serializer2 =
                    new OAuth2AuthorizationRequestSerializer(new ObjectMapper(), "encryption-key-2-32-characters!!");

            OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("https://example.com/authorize")
                    .clientId("client-id")
                    .redirectUri("http://localhost/callback")
                    .state("state")
                    .build();

            // when
            String serialized = serializer1.serialize(original);
            OAuth2AuthorizationRequest deserialized = serializer2.deserialize(serialized);

            // then
            assertThat(deserialized).isNull();
        }
    }

    @Nested
    @DisplayName("암호화 보안")
    class EncryptionSecurity {

        @Test
        @DisplayName("직렬화된 값은 원본 데이터를 포함하지 않음 (암호화됨)")
        void serializedValue_doesNotContainOriginalData() {
            // given
            String sensitiveClientId = "sensitive-client-id-12345";
            OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("https://example.com/authorize")
                    .clientId(sensitiveClientId)
                    .redirectUri("http://localhost/callback")
                    .state("state")
                    .build();

            // when
            String serialized = serializer.serialize(original);

            // then
            assertThat(serialized).doesNotContain(sensitiveClientId);
            assertThat(serialized).doesNotContain("example.com");
        }

        @Test
        @DisplayName("같은 요청을 직렬화해도 매번 다른 결과 (IV가 다름)")
        void sameRequest_differentSerializedResults() {
            // given
            OAuth2AuthorizationRequest original = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("https://example.com/authorize")
                    .clientId("client-id")
                    .redirectUri("http://localhost/callback")
                    .state("state")
                    .build();

            // when
            String serialized1 = serializer.serialize(original);
            String serialized2 = serializer.serialize(original);

            // then
            assertThat(serialized1).isNotEqualTo(serialized2);

            // 둘 다 역직렬화 가능
            assertThat(serializer.deserialize(serialized1)).isNotNull();
            assertThat(serializer.deserialize(serialized2)).isNotNull();
        }
    }
}
