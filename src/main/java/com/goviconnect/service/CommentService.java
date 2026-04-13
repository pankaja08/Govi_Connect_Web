package com.goviconnect.service;

import com.goviconnect.entity.Blog;
import com.goviconnect.entity.Comment;
import com.goviconnect.entity.CommentLike;
import com.goviconnect.entity.User;
import com.goviconnect.repository.CommentLikeRepository;
import com.goviconnect.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByBlog(Blog blog, User currentUser) {
        List<Comment> comments = commentRepository.findByBlogOrderByCreatedDateDesc(blog);

        if (comments.isEmpty()) {
            return comments;
        }

        // Batch fetch all like counts in ONE query (eliminates N+1)
        Map<Long, Long> likeCounts = new HashMap<>();
        for (Object[] row : commentLikeRepository.countByComments(comments)) {
            likeCounts.put((Long) row[0], (Long) row[1]);
        }

        // Batch fetch user's liked comment IDs in ONE query (eliminates N+1)
        Set<Long> likedIds = currentUser != null
                ? new HashSet<>(commentLikeRepository.findLikedCommentIds(comments, currentUser))
                : Collections.emptySet();

        for (Comment comment : comments) {
            comment.setLikeCount(likeCounts.getOrDefault(comment.getId(), 0L));
            comment.setLikedByCurrentUser(likedIds.contains(comment.getId()));
        }

        return comments;
    }

    @Transactional
    public Comment addComment(Blog blog, User author, String content) {
        Comment comment = Comment.builder()
                .blog(blog)
                .author(author)
                .content(content)
                .build();
        return commentRepository.save(comment);
    }

    @Transactional
    public long toggleLike(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentAndUser(comment, user);
        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
        } else {
            CommentLike newLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(newLike);
        }

        return commentLikeRepository.countByComment(comment);
    }

    @Transactional
    public void addExpertReply(Long commentId, String reply, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getBlog().getAuthor().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the author of the blog can reply to comments.");
        }

        comment.setExpertReply(reply);
        comment.setExpertReplyDate(java.time.LocalDateTime.now());
        commentRepository.save(comment);
    }
}
