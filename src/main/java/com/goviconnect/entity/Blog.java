package com.goviconnect.entity;

import com.goviconnect.enums.BlogStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String heading;

    @Lob
    @Column(nullable = false, length = 15000)
    private String textContent;

    @Column(length = 255)
    private String imageUrl;

    @Column(length = 100)
    private String locationTag;

    @Column(length = 100)
    private String seasonTag;

    @Column(length = 100)
    private String cropTag;

    @Column(length = 100)
    private String farmingMethodTag;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL)
    private List<Comment> comments;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlogStatus approvalStatus;

    @Column(length = 500)
    private String rejectionReason;

    @Column
    private LocalDateTime scheduledDate;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
        if (this.approvalStatus == null) {
            this.approvalStatus = BlogStatus.PENDING;
        }
    }

    @Transient
    public int getEstimatedReadingTime() {
        if (textContent == null || textContent.trim().isEmpty()) return 1;
        int wordCount = textContent.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil((double) wordCount / 200));
    }
}

