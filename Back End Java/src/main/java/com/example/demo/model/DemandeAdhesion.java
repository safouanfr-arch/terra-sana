package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.Locale;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.demo.model.enums.StatutDemande;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "TDEMANDE_ADHESION",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_demande_adhesion_email_actif",
                        columnNames = "email_actif_normalise"
                ),
                @UniqueConstraint(
                        name = "uk_demande_adhesion_confirmation_hash",
                        columnNames = "confirmation_email_token_hash"
                )
        },
        indexes = {
                @Index(name = "idx_demande_adhesion_email", columnList = "email"),
                @Index(name = "idx_demande_adhesion_statut", columnList = "statut"),
                @Index(name = "idx_demande_adhesion_soumission", columnList = "soumission")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"administrateur"})
public class DemandeAdhesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Iddemande")
    private Long id;

    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Column(name = "prenom", length = 100, nullable = false)
    private String prenom;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "telephone", length = 20, nullable = false)
    private String telephone;

    @Column(name = "adresse", length = 255, nullable = false)
    private String adresse;

    @Column(name = "ville", length = 100, nullable = false)
    private String ville;

    @Column(name = "code_postal", length = 10, nullable = false)
    private String codePostal;

    @Builder.Default
    @Column(name = "pays", length = 100, nullable = false)
    private String pays = "Belgique";

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "message_refus", columnDefinition = "TEXT")
    private String messageRefus;

    @Column(name = "confirmation_email_token_hash", length = 64)
    private String confirmationEmailTokenHash;

    @Column(name = "confirmation_email_expiration")
    private LocalDateTime confirmationEmailExpiresAt;

    @Column(name = "confirmation_email")
    private LocalDateTime dateConfirmationEmail;

    @Column(name = "dernier_envoi_confirmation")
    private LocalDateTime dateDernierEnvoiConfirmation;

    @Column(name = "conditions_acceptees")
    private LocalDateTime conditionsAccepteesAt;

    @Column(name = "conditions_version", length = 30)
    private String conditionsVersion;

    @Column(name = "email_actif_normalise", length = 255)
    private String emailActifNormalise;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "statut", length = 20, nullable = false)
    @Builder.Default
    private StatutDemande statut = StatutDemande.A_CONFIRMER;

    @Column(name = "soumission", nullable = false)
    private LocalDateTime dateSoumission;

    @Column(name = "traitement")
    private LocalDateTime dateTraitement;

    @ManyToOne
    @JoinColumn(name = "FKadministrateur")
    private Administrateur administrateur;

    public void approuver(Administrateur admin) {
        this.statut = StatutDemande.APPROUVEE;
        this.administrateur = admin;
        this.dateTraitement = LocalDateTime.now();
        libererEmailActifEtNettoyerConfirmation();
    }

    public void refuser(Administrateur admin, String motif) {
        this.statut = StatutDemande.REFUSEE;
        this.administrateur = admin;
        this.dateTraitement = LocalDateTime.now();
        if (motif != null) {
            this.messageRefus = motif;
        }
        libererEmailActifEtNettoyerConfirmation();
    }

    public void confirmerEmail(LocalDateTime confirmedAt) {
        this.statut = StatutDemande.EN_ATTENTE;
        this.dateConfirmationEmail = confirmedAt;
    }

    public void renouvelerConfirmation(String tokenHash, LocalDateTime expiresAt, LocalDateTime sentAt) {
        this.confirmationEmailTokenHash = tokenHash;
        this.confirmationEmailExpiresAt = expiresAt;
        this.dateDernierEnvoiConfirmation = sentAt;
    }

    private void libererEmailActifEtNettoyerConfirmation() {
        this.emailActifNormalise = null;
        this.confirmationEmailTokenHash = null;
        this.confirmationEmailExpiresAt = null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.dateSoumission == null) {
            this.dateSoumission = LocalDateTime.now();
        }
        synchronizeActiveEmail();
    }

    @PreUpdate
    protected void onUpdate() {
        synchronizeActiveEmail();
    }

    private void synchronizeActiveEmail() {
        if ((this.statut == StatutDemande.A_CONFIRMER || this.statut == StatutDemande.EN_ATTENTE)
                && this.email != null) {
            this.emailActifNormalise = this.email.trim().toLowerCase(Locale.ROOT);
        } else {
            this.emailActifNormalise = null;
        }
    }
}
