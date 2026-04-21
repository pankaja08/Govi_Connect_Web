package com.goviconnect.repository;

import com.goviconnect.entity.Blog;
import com.goviconnect.entity.User;
import com.goviconnect.enums.BlogStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Blog> findByOrderByCreatedDateDesc();

    @EntityGraph(attributePaths = {"author"})
    List<Blog> findByAuthor(User author);

    Optional<Blog> findByIdAndAuthor(Long id, User author);

    @EntityGraph(attributePaths = {"author"})
    List<Blog> findTop6ByOrderByCreatedDateDesc();

    // ---- Approval-filtered queries ----

    @EntityGraph(attributePaths = {"author"})
    List<Blog> findByApprovalStatusOrderByCreatedDateDesc(BlogStatus approvalStatus);

    @EntityGraph(attributePaths = {"author"})
    List<Blog> findTop6ByApprovalStatusOrderByCreatedDateDesc(BlogStatus approvalStatus);

    @EntityGraph(attributePaths = {"author"})
    List<Blog> findByApprovalStatus(BlogStatus approvalStatus);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.approvalStatus = :status AND (b.scheduledDate IS NULL OR b.scheduledDate <= CURRENT_TIMESTAMP) ORDER BY b.createdDate DESC")
    List<Blog> findPublicBlogs(@Param("status") BlogStatus approvalStatus);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b WHERE b.approvalStatus = :status AND (b.scheduledDate IS NULL OR b.scheduledDate <= CURRENT_TIMESTAMP) ORDER BY b.createdDate DESC LIMIT 6")
    List<Blog> findTop6PublicBlogs(@Param("status") BlogStatus approvalStatus);

    long countByApprovalStatus(BlogStatus approvalStatus);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT b FROM Blog b " +
            "LEFT JOIN b.savedByUsers sbu " +
            "WHERE " +
            "b.approvalStatus = :status AND " +
            "(b.scheduledDate IS NULL OR b.scheduledDate <= CURRENT_TIMESTAMP) AND " +
            "(:keyword IS NULL OR :keyword = '' OR b.heading LIKE :keyword OR b.textContent LIKE :keyword) AND " +
            "(:location IS NULL OR :location = '' OR b.locationTag = :location) AND " +
            "(:season IS NULL OR :season = '' OR b.seasonTag = :season) AND " +
            "(:crop IS NULL OR :crop = '' OR b.cropTag = :crop) AND " +
            "(:method IS NULL OR :method = '' OR b.farmingMethodTag = :method) AND " +
            "(:savedOnly = false OR sbu.id = :userId) " +
            "ORDER BY b.createdDate DESC")
    List<Blog> findFilteredBlogs(
            @Param("status") BlogStatus status,
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("season") String season,
            @Param("crop") String crop,
            @Param("method") String method,
            @Param("savedOnly") boolean savedOnly,
            @Param("userId") Long userId);

    void deleteByAuthor(User author);
}
