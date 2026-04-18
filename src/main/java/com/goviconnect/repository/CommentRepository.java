package com.goviconnect.repository;

import com.goviconnect.entity.Blog;
import com.goviconnect.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByBlogOrderByCreatedDateDesc(Blog blog);
}
