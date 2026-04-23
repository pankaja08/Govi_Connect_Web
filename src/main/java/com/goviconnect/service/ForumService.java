package com.goviconnect.service;

import com.goviconnect.entity.ForumAnswer;
import com.goviconnect.entity.ForumQuestion;
import com.goviconnect.entity.User;
import com.goviconnect.repository.ForumAnswerRepository;
import com.goviconnect.repository.ForumQuestionRepository;
import com.goviconnect.repository.ForumAnswerLikeRepository;
import com.goviconnect.entity.ForumAnswerLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumQuestionRepository questionRepository;
    private final ForumAnswerRepository answerRepository;
    private final ForumAnswerLikeRepository answerLikeRepository;

    @Value("${app.upload.forum.dir:uploads/forum-questions}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<ForumQuestion> getAllActiveQuestions() {
        return questionRepository.findByActiveTrueOrderByCreatedDateDesc();
    }

    @Transactional(readOnly = true)
    public List<ForumQuestion> searchQuestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllActiveQuestions();
        }
        return questionRepository.findByContentContainingIgnoreCaseAndActiveTrueOrderByCreatedDateDesc(query);
    }

    @Transactional
    public ForumQuestion askQuestion(User author, String content, String category, MultipartFile image)
            throws IOException {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Question content cannot be empty.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty.");
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = saveImage(image);
        }

        ForumQuestion question = ForumQuestion.builder()
                .author(author)
                .content(content)
                .category(category)
                .imageUrl(imageUrl)
                .active(true)
                .build();
        return questionRepository.save(question);
    }

    @Transactional
    public ForumQuestion askCropQuestion(User author, String content, String category, String imageUrl) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Question content cannot be empty.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty.");
        }

        ForumQuestion question = ForumQuestion.builder()
                .author(author)
                .content(content)
                .category(category)
                .imageUrl(imageUrl)
                .active(true)
                .build();
        return questionRepository.save(question);
    }

    @Transactional
    public ForumQuestion editQuestion(Long questionId, User author, String newContent, String newCategory) {
        ForumQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));

        if (!question.getAuthor().getId().equals(author.getId())) {
            throw new IllegalArgumentException("You are not authorized to edit this question.");
        }

        if (question.getCreatedDate() == null
                || question.getCreatedDate().plusHours(1).isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Editing is only allowed within 1 hour of posting.");
        }

        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Question content cannot be empty.");
        }
        if (newCategory == null || newCategory.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty.");
        }

        question.setContent(newContent);
        question.setCategory(newCategory);
        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId, User author) {
        ForumQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));

        if (!question.getAuthor().getId().equals(author.getId())) {
            throw new IllegalArgumentException("You are not authorized to delete this question.");
        }

        if (question.getCreatedDate() == null
                || question.getCreatedDate().plusHours(1).isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Deleting is only allowed within 1 hour of posting.");
        }

        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public List<ForumQuestion> getMyActiveQuestions(User author) {
        return questionRepository.findByAuthorAndActiveTrueOrderByCreatedDateDesc(author);
    }

    @Transactional(readOnly = true)
    public List<ForumQuestion> searchMyQuestions(User author, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getMyActiveQuestions(author);
        }
        return questionRepository.findByAuthorAndContentContainingIgnoreCaseAndActiveTrueOrderByCreatedDateDesc(author,
                query);
    }

    @Transactional
    public ForumAnswer answerQuestion(Long questionId, User expert, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer content cannot be empty.");
        }
        String plainText = org.jsoup.Jsoup.parse(content).text().trim();
        String[] words = plainText.split("\\s+");
        if (words.length < 10) {
            throw new IllegalArgumentException("Your official answer is too short. Please provide at least 10 words.");
        }
        ForumQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));

        ForumAnswer answer = ForumAnswer.builder()
                .question(question)
                .officer(expert)
                .content(content)
                .build();
        return answerRepository.save(answer);
    }

    @Transactional
    public ForumAnswer editAnswer(Long answerId, User expert, String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer content cannot be empty.");
        }
        String plainText = org.jsoup.Jsoup.parse(newContent).text().trim();
        String[] words = plainText.split("\\s+");
        if (words.length < 10) {
            throw new IllegalArgumentException("Your official answer is too short. Please provide at least 10 words.");
        }

        ForumAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found."));

        if (!answer.getOfficer().getId().equals(expert.getId())) {
            throw new IllegalArgumentException("You are not authorized to edit this answer.");
        }

        answer.setContent(newContent);
        return answerRepository.save(answer);
    }

    @Transactional
    public void deleteAnswer(Long answerId, User expert) {
        ForumAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found."));

        if (!answer.getOfficer().getId().equals(expert.getId())) {
            throw new IllegalArgumentException("You are not authorized to delete this answer.");
        }

        answerRepository.delete(answer);
    }

    @Transactional
    public java.util.Map<String, Object> toggleLikeAnswer(Long answerId, User user) {
        ForumAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found."));

        java.util.Optional<ForumAnswerLike> existingLike = answerLikeRepository.findByAnswerAndUser(answer, user);
        boolean liked;

        if (existingLike.isPresent()) {
            answerLikeRepository.delete(existingLike.get());
            answer.getLikes().remove(existingLike.get());
            liked = false;
        } else {
            ForumAnswerLike newLike = ForumAnswerLike.builder()
                    .answer(answer)
                    .user(user)
                    .build();
            answerLikeRepository.save(newLike);
            answer.getLikes().add(newLike);
            liked = true;
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("liked", liked);
        response.put("likesCount", answer.getLikes().size());
        return response;
    }

    @Transactional(readOnly = true)
    public java.util.Set<Long> getLikedAnswerIdsByUser(User user) {
        return answerLikeRepository.findByUser(user).stream()
                .map(like -> like.getAnswer().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String saveImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/forum-questions/" + filename;
    }
}
