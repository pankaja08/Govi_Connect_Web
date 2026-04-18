package com.goviconnect.repository;

import com.goviconnect.entity.BlogImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogImageRepository extends JpaRepository<BlogImage, Long> {
    boolean existsBySha256Hash(String sha256Hash);
}
