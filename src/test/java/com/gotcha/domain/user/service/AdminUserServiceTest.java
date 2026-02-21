package com.gotcha.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.user.dto.AdminUserListResponse;
import com.gotcha.domain.user.dto.AdminUserResponse;
import com.gotcha.domain.user.dto.UpdateUserStatusRequest;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserStatus;
import com.gotcha.domain.user.entity.UserType;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User normalUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        normalUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("user123")
                .nickname("일반사용자")
                .email("user@test.com")
                .build();
        ReflectionTestUtils.setField(normalUser, "id", 1L);

        adminUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("admin123")
                .nickname("관리자")
                .email("admin@test.com")
                .userType(UserType.ADMIN)
                .build();
        ReflectionTestUtils.setField(adminUser, "id", 2L);
    }

    @Nested
    @DisplayName("사용자 목록 조회")
    class GetUsers {

        @Test
        @DisplayName("전체 사용자 목록 조회 성공")
        void getUsers_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<User> userPage = new PageImpl<>(List.of(normalUser), pageable, 1);

            when(userRepository.findAllWithStatusFilter(null, pageable)).thenReturn(userPage);

            // when
            AdminUserListResponse response = adminUserService.getUsers(null, pageable);

            // then
            assertThat(response.users()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.page()).isEqualTo(0);
        }

        @Test
        @DisplayName("상태 필터링 조회")
        void getUsers_FilterByStatus() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(normalUser), pageable, 1);

            when(userRepository.findAllWithStatusFilter(UserStatus.ACTIVE, pageable)).thenReturn(userPage);

            // when
            AdminUserListResponse response = adminUserService.getUsers(UserStatus.ACTIVE, pageable);

            // then
            assertThat(response.users()).hasSize(1);
            assertThat(response.users().get(0).status()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("빈 결과 반환")
        void getUsers_Empty() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> emptyPage = Page.empty(pageable);

            when(userRepository.findAllWithStatusFilter(null, pageable)).thenReturn(emptyPage);

            // when
            AdminUserListResponse response = adminUserService.getUsers(null, pageable);

            // then
            assertThat(response.users()).isEmpty();
            assertThat(response.totalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("사용자 상세 조회")
    class GetUser {

        @Test
        @DisplayName("사용자 상세 조회 성공")
        void getUser_Success() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when
            AdminUserResponse response = adminUserService.getUser(1L);

            // then
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.nickname()).isEqualTo("일반사용자");
            assertThat(response.email()).isEqualTo("user@test.com");
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 실패")
        void getUser_NotFound_Fail() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminUserService.getUser(999L))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("사용자 상태 변경")
    class UpdateUserStatus {

        @Test
        @DisplayName("사용자 정지 성공 (24시간)")
        void suspendUser_Success() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.SUSPENDED, 24);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when
            AdminUserResponse response = adminUserService.updateUserStatus(1L, request);

            // then
            assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(response.suspendedUntil()).isNotNull();
        }

        @Test
        @DisplayName("사용자 정지 성공 (1시간)")
        void suspendUser_1Hour_Success() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.SUSPENDED, 1);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when
            AdminUserResponse response = adminUserService.updateUserStatus(1L, request);

            // then
            assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(response.suspendedUntil()).isNotNull();
        }

        @Test
        @DisplayName("사용자 정지 성공 (30일 = 720시간)")
        void suspendUser_30Days_Success() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.SUSPENDED, 720);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when
            AdminUserResponse response = adminUserService.updateUserStatus(1L, request);

            // then
            assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(response.suspendedUntil()).isNotNull();
        }

        @Test
        @DisplayName("허용되지 않는 정지 기간으로 정지 시 실패")
        void suspendUser_InvalidHours_Fail() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.SUSPENDED, 48);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("허용되지 않는 정지 기간입니다");
        }

        @Test
        @DisplayName("정지 기간 null로 정지 시 실패")
        void suspendUser_NullHours_Fail() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.SUSPENDED, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("허용되지 않는 정지 기간입니다");
        }

        @Test
        @DisplayName("사용자 영구 차단 성공")
        void banUser_Success() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when
            AdminUserResponse response = adminUserService.updateUserStatus(1L, request);

            // then
            assertThat(response.status()).isEqualTo(UserStatus.BANNED);
            assertThat(response.suspendedUntil()).isNull();
        }

        @Test
        @DisplayName("제재 해제 성공")
        void activateUser_Success() {
            // given
            normalUser.ban();
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.ACTIVE, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when
            AdminUserResponse response = adminUserService.updateUserStatus(1L, request);

            // then
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(response.suspendedUntil()).isNull();
        }

        @Test
        @DisplayName("관리자 계정에 제재 시도 시 실패")
        void updateStatus_AdminUser_Fail() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, null);
            when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

            // when & then
            assertThatThrownBy(() -> adminUserService.updateUserStatus(2L, request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("권한이 없습니다");
        }

        @Test
        @DisplayName("탈퇴한 사용자에 제재 시도 시 실패")
        void updateStatus_DeletedUser_Fail() {
            // given
            normalUser.delete();
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("이미 탈퇴한 사용자입니다");
        }

        @Test
        @DisplayName("존재하지 않는 사용자에 제재 시도 시 실패")
        void updateStatus_UserNotFound_Fail() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BANNED, null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminUserService.updateUserStatus(999L, request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("DELETED 상태로 변경 시도 시 실패")
        void updateStatus_ToDeleted_Fail() {
            // given
            UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.DELETED, null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("권한이 없습니다");
        }
    }
}
