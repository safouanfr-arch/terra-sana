package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.model.enums.EmailSendStatus;
import com.example.demo.model.enums.EmailNotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "TEMAIL_GENERE",
        indexes = {
                @Index(name = "idx_email_genere_type", columnList = "type_email"),
                @Index(name = "idx_email_genere_destinataire", columnList = "destinataire"),
                @Index(name = "idx_email_genere_creation", columnList = "creation"),
                @Index(name = "idx_email_genere_commande", columnList = "commande_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class GeneratedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id_email")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_email", length = 50, nullable = false)
    private EmailNotificationType type;

    @Column(name = "destinataire", length = 255, nullable = false)
    private String destinataire;

    @Column(name = "sujet", columnDefinition = "TEXT", nullable = false)
    private String sujet;

    @Column(name = "contenu_html", columnDefinition = "TEXT", nullable = false)
    private String contenuHtml;

    @Column(name = "creation", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_envoi", length = 20, columnDefinition = "varchar(20) default 'PENDING'")
    @Builder.Default
    private EmailSendStatus statutEnvoi = EmailSendStatus.PENDING;

    @Column(name = "tentative_envoi")
    private LocalDateTime dateTentativeEnvoi;

    @Column(name = "envoi_reussi")
    private LocalDateTime dateEnvoi;

    @Column(name = "erreur_envoi", length = 1000)
    private String erreurEnvoi;

    @Column(name = "commande_id")
    private Long commandeId;

    @Column(name = "membre_id")
    private Long membreId;

    @Column(name = "produit_id")
    private Long produitId;

    @Column(name = "demande_adhesion_id")
    private Long demandeAdhesionId;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.statutEnvoi == null) {
            this.statutEnvoi = EmailSendStatus.PENDING;
        }
    }
}
