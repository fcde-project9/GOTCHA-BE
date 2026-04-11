package com.gotcha.domain.post.repository;

import com.gotcha.domain.post.entity.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTypeRepository extends JpaRepository<PostType, Long> {

    boolean existsByTypeName(String typeName);
}
