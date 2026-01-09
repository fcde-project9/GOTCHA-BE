package com.gotcha.domain.user.repository;

import com.gotcha.domain.user.entity.UserPermissionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * UserPermissionHistory Repository
 *
 * 용도:
 * - 권한 변경 이력을 DB에 저장 (법적 증빙용)
 * - 이력은 저장만 하고 별도 조회는 하지 않음
 * - 필요시 DB에서 직접 확인
 *
 * 주의사항:
 * - 이력 데이터는 절대 삭제하지 않음
 * - JpaRepository의 기본 save() 메서드만 사용
 */
public interface UserPermissionHistoryRepository extends JpaRepository<UserPermissionHistory, Long> {
    // 기본 CRUD 메서드만 사용 (save, findById, findAll 등)
    // 커스텀 메서드 불필요
}
