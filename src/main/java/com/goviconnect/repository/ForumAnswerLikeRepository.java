package com.goviconnect.repository;

import com.goviconnect.entity.ForumAnswer;
import com.goviconnect.entity.ForumAnswerLike;
import com.goviconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumAnswerLikeRepository extends JpaRepository<ForumAnswerLike, Long> {

    Optional<ForumAnswerLike> findByAnswerAndUser(ForumAnswer answer, User user);

    List<ForumAnswerLike> findByUser(User user);
}
