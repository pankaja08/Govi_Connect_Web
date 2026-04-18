package com.goviconnect.repository;

import com.goviconnect.entity.Comment;
import com.goviconnect.entity.CommentLike;
import com.goviconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);

    long countByComment(Comment comment);

    void deleteByUser(User user);

    /**
     * Batch count likes for multiple comments in ONE query instead of N queries.
     */
    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment IN :comments GROUP BY cl.comment.id")
    List<Object[]> countByComments(@Param("comments") List<Comment> comments);

    /**
     * Batch find all likes by a user for multiple comments in ONE query.
     */
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.comment IN :comments AND cl.user = :user")
    List<Long> findLikedCommentIds(@Param("comments") List<Comment> comments, @Param("user") User user);
}
