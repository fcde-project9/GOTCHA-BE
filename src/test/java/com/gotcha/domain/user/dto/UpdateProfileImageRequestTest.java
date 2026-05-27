package com.gotcha.domain.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateProfileImageRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("GCS URL은 유효하다")
    void gcsUrlIsValid() {
        // given
        UpdateProfileImageRequest request = new UpdateProfileImageRequest(
                "https://storage.googleapis.com/gotcha-dev-files/profiles/abc-123.webp"
        );

        // when
        Set<ConstraintViolation<UpdateProfileImageRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("S3 URL은 유효하다")
    void s3UrlIsValid() {
        // given
        UpdateProfileImageRequest request = new UpdateProfileImageRequest(
                "https://gotcha-prod-bucket.s3.ap-northeast-2.amazonaws.com/profiles/uuid.jpg"
        );

        // when
        Set<ConstraintViolation<UpdateProfileImageRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("다른 지역의 S3 URL도 유효하다")
    void s3UrlWithDifferentRegionIsValid() {
        // given
        UpdateProfileImageRequest request = new UpdateProfileImageRequest(
                "https://my-bucket.s3.us-east-1.amazonaws.com/profiles/test.png"
        );

        // when
        Set<ConstraintViolation<UpdateProfileImageRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("CloudFront URL도 유효하다")
    void cloudfrontUrlIsValid() {
        // given
        UpdateProfileImageRequest request = new UpdateProfileImageRequest(
                "https://d1234abcd.cloudfront.net/profiles/uuid.webp"
        );

        // when
        Set<ConstraintViolation<UpdateProfileImageRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("빈 URL은 거부된다")
    void emptyUrlIsRejected() {
        // given
        UpdateProfileImageRequest request = new UpdateProfileImageRequest("");

        // when
        Set<ConstraintViolation<UpdateProfileImageRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("프로필 이미지 URL은 필수입니다");
    }
}
