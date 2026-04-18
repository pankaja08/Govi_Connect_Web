package com.goviconnect.repository;

import com.goviconnect.entity.ForumQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumQuestionRepository extends JpaRepository<ForumQuestion, Long> {
    
    @EntityGraph(attributePaths = {"author", "answers"})
    List<ForumQuestion> findByActiveTrueOrderByCreatedDateDesc();

    @EntityGraph(attributePaths = {"author", "answers"})
    List<ForumQuestion> findByContentContainingIgnoreCaseAndActiveTrueOrderByCreatedDateDesc(String content);

    @EntityGraph(attributePaths = {"author", "answers"})
    List<ForumQuestion> findByAuthorAndActiveTrueOrderByCreatedDateDesc(com.goviconnect.entity.User author);

    @EntityGraph(attributePaths = {"author", "answers"})
    List<ForumQuestion> findByAuthorAndContentContainingIgnoreCaseAndActiveTrueOrderByCreatedDateDesc(com.goviconnect.entity.User author, String content);
}
