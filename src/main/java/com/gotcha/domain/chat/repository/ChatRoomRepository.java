package com.gotcha.domain.chat.repository;

import com.gotcha.domain.chat.entity.ChatRoom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.user1.id = :userId OR cr.user2.id = :userId")
    List<ChatRoom> findAllByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatRoom cr WHERE cr.user1.id = :userId OR cr.user2.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
