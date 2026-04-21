package com.gotcha.domain.post.entity;

import com.gotcha._global.entity.BaseTimeEntity;
import com.gotcha.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_comment_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_comment_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCommentLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_comment_id", nullable = false)
    private PostComment postComment;

    @Builder
    public PostCommentLike(User user, PostComment postComment) {
        this.user = user;
        this.postComment = postComment;
    }
}
