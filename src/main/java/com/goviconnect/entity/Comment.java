package com.goviconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 1000)
    private String content;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentLike> likes;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    // Expert Reply fields
    @Column(length = 1000)
    private String expertReply;

    @Column
    private LocalDateTime expertReplyDate;

    // Transient fields to be populated at runtime for the UI
    @Transient
    private boolean likedByCurrentUser;

    @Transient
    private long likeCount;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
    }
}
