package com.services.mini_doodle.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID meetingId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private OffsetDateTime startTime;

    @Column(nullable = false)
    private OffsetDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "participant_emails", columnDefinition = "jsonb")
    private List<String> participantEmails;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
