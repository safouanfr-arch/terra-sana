package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.model.enums.EmailNotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "TEMAIL_TEMPLATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class EmailTemplate {

    @Id
    @EqualsAndHashCode.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "type_template", length = 50, nullable = false)
    private EmailNotificationType type;

    @Column(name = "objet_template", columnDefinition = "TEXT", nullable = false)
    private String subjectTemplate;

    @Column(name = "contenu_template", columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private Boolean active = true;

    @Column(name = "modification", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
