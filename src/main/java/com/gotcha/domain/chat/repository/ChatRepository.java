package com.gotcha.domain.chat.repository;

import com.gotcha.domain.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Chat c WHERE c.sender.id = :userId")
    void deleteBySenderId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Chat c WHERE c.chatRoom.id IN :chatRoomIds")
    void deleteByChatRoomIdIn(@Param("chatRoomIds") java.util.List<Long> chatRoomIds);
}
