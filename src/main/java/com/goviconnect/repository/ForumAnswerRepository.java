package com.goviconnect.repository;

import com.goviconnect.entity.ForumAnswer;
import com.goviconnect.entity.ForumQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumAnswerRepository extends JpaRepository<ForumAnswer, Long> {
    List<ForumAnswer> findByQuestionOrderByCreatedDateAsc(ForumQuestion question);
}
