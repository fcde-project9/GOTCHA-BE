package com.gotcha.domain.inquiry.repository;

import com.gotcha.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Inquiry i WHERE i.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
