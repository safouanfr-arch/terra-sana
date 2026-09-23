package com.example.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.LigneCommandeRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dao.MouvementStockRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dto.CommandeCreateItemDto;
import com.example.demo.dto.CommandeCreateRequestDto;
import com.example.demo.dto.CommandeResponseDto;
import com.example.demo.dto.CommandeStatutUpdateDto;
import com.example.demo.mapper.CommandeMapper;
import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.LigneCommande;
import com.example.demo.model.Membre;
import com.example.demo.model.MouvementStock;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.model.enums.TypeMouvementStock;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommandeService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final long MINIMUM_RETRAIT_HOURS = 24;
    private static final long MAXIMUM_RETRAIT_DAYS = 8;
    private static final DateTimeFormatter NUMERO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Logger log = LoggerFactory.getLogger(CommandeService.class);

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final MembreRepository membreRepository;
    private final CreneauCollecteRepository creneauCollecteRepository;
    private final ProduitRepository produitRepository;
    private final GeneratedEmailService generatedEmailService;
    private final PromotionService promotionService;
    private Clock clock = Clock.systemDefaultZone();

    @Transactional
    public CommandeResponseDto createForMember(Long membreId, CommandeCreateRequestDto dto) {
        CommandeCreateRequestDto payload = CommandeCreateRequestDto.builder()
                .membreId(membreId)
                .creneauCollecteId(dto.getCreneauCollecteId())
                .commentaire(dto.getCommentaire())
                .produits(dto.getProduits())
                .build();
        return create(payload);
    }

    @Transactional
    public CommandeResponseDto create(CommandeCreateRequestDto dto) {
        try {
            Membre membre = resolveMembre(dto);
            if (Boolean.FALSE.equals(membre.getActif())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce membre est suspendu et ne peut pas commander.");
            }

            CreneauCollecte creneau = creneauCollecteRepository.findById(dto.getCreneauCollecteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creneau de collecte introuvable."));
            if (!Boolean.TRUE.equals(creneau.getActif())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce creneau de collecte est inactif.");
            }
            if (creneau.getPointCollecte() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce creneau n'est rattache a aucun point de collecte.");
            }
            if (!Boolean.TRUE.equals(creneau.getPointCollecte().getActif())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Le point de collecte associe a ce creneau est inactif.");
            }

            LocalDateTime dateRetrait = LocalDateTime.of(creneau.getCreneau(), creneau.getHeureDebut());
            LocalDateTime maintenant = LocalDateTime.now(clock);
            if (dateRetrait.isBefore(maintenant)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de commander sur un creneau deja passe.");
            }
            if (dateRetrait.isBefore(maintenant.plusHours(MINIMUM_RETRAIT_HOURS))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La commande doit etre passee au minimum 24 heures avant le retrait.");
            }
            if (dateRetrait.isAfter(maintenant.plusDays(MAXIMUM_RETRAIT_DAYS))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Le retrait doit etre planifie au plus tard dans les 8 jours suivant la commande.");
            }

            Map<Long, BigDecimal> quantitesParProduit = aggregateQuantites(dto.getProduits());
            Map<Long, Produit> produitsParId = loadProduitsForUpdate(quantitesParProduit.keySet());
            validateStocks(quantitesParProduit, produitsParId);

            BigDecimal totalHT = BigDecimal.ZERO;
            BigDecimal totalTVA = BigDecimal.ZERO;
            BigDecimal totalTTC = BigDecimal.ZERO;
            List<LigneCommande> lignes = new ArrayList<>();

            for (CommandeCreateItemDto item : dto.getProduits()) {
                Produit produit = produitsParId.get(item.getProduitId());
                BigDecimal quantite = scale(item.getQuantite());
                PromotionCalculation promotion = promotionService.calculateForProduit(produit);
                BigDecimal prixUnitaire = scale(promotion.prixFinal());
                BigDecimal prixUnitaireOriginal = scale(promotion.prixOriginal());
                BigDecimal reductionUnitaire = scale(promotion.reductionMontant());
                BigDecimal tauxTVA = scale(produit.getTauxTva() != null ? produit.getTauxTva() : BigDecimal.valueOf(6));
                BigDecimal montantHT = scale(prixUnitaire.multiply(quantite));
                BigDecimal montantTTC = scale(montantHT.multiply(BigDecimal.ONE.add(tauxTVA.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP))));
                BigDecimal montantTVA = scale(montantTTC.subtract(montantHT));

                totalHT = totalHT.add(montantHT);
                totalTVA = totalTVA.add(montantTVA);
                totalTTC = totalTTC.add(montantTTC);

                lignes.add(LigneCommande.builder()
                        .produit(produit)
                        .quantite(quantite)
                        .prixUnitaire(prixUnitaire)
                        .prixUnitaireOriginal(prixUnitaireOriginal)
                        .reductionUnitaire(reductionUnitaire)
                        .promotionId(promotion.promotionId())
                        .promotionNom(promotion.promotionNom())
                        .tauxTVA(tauxTVA)
                        .montantHT(montantHT)
                        .montantTTC(montantTTC)
                        .build());
            }

            Commande commande = Commande.builder()
                    .numero(generateTemporaryNumero())
                    .codeRetrait(generateCodeRetrait())
                    .statut(StatutCommande.CONFIRMED)
                    .montantHT(scale(totalHT))
                    .montantTVA(scale(totalTVA))
                    .montantTTC(scale(totalTTC))
                    .paye(false)
                    .dateCommande(LocalDateTime.now())
                    .dateRetraitPrevu(dateRetrait)
                    .commentaire(dto.getCommentaire())
                    .membre(membre)
                    .creneauCollecte(creneau)
                    .build();

            commande = commandeRepository.save(commande);
            commande.setNumero(generateNumero(creneau, commande.getId()));
            commande = commandeRepository.save(commande);

            for (LigneCommande ligne : lignes) {
                ligne.setCommande(commande);
            }
            lignes = ligneCommandeRepository.saveAll(lignes);

            List<MouvementStock> mouvements = reserveStockForCommande(commande, quantitesParProduit, produitsParId);
            commande.setLignes(lignes);
            commande.setMouvementsStock(mouvements);
            Commande confirmationCommande = commande;
            safeEmailGeneration(
                    () -> generatedEmailService.generateOrderConfirmation(confirmationCommande),
                    "confirmation commande " + confirmationCommande.getNumero()
            );

            return CommandeMapper.toDto(commande);
        } catch (PessimisticLockingFailureException | ObjectOptimisticLockingFailureException exception) {
            throw stockConflict("Une autre validation est en cours sur ce stock. Veuillez recharger votre panier et reessayer.");
        }
    }

    @Transactional(readOnly = true)
    public List<CommandeResponseDto> getAll() {
        return commandeRepository.findAllByOrderByDateCommandeDesc()
                .stream()
                .map(CommandeMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommandeResponseDto> getByMembreId(Long membreId) {
        return commandeRepository.findByMembre_IdOrderByDateCommandeDesc(membreId)
                .stream()
                .map(CommandeMapper::toDto)
                .toList();
    }

    @Transactional
    public CommandeResponseDto cancel(Long id) {
        try {
            Commande commande = getCommandeOrThrow(id);

            if (commande.getStatut() == StatutCommande.CANCELLED) {
                return CommandeMapper.toDto(commande);
            }
            if (commande.getStatut() == StatutCommande.DISTRIBUTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une commande deja retiree ne peut plus etre annulee.");
            }
            if (!commande.getDateRetraitPrevu().isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une commande ne peut etre annulee qu'avant son retrait.");
            }

            Map<Long, BigDecimal> quantitesParProduit = new LinkedHashMap<>();
            for (LigneCommande ligne : commande.getLignes()) {
                Produit produit = ligne.getProduit();
                quantitesParProduit.merge(produit.getId(), ligne.getQuantite(), BigDecimal::add);
            }
            Map<Long, Produit> produitsParId = loadProduitsForUpdate(quantitesParProduit.keySet());

            List<MouvementStock> mouvements = restoreStockForCommande(commande, quantitesParProduit, produitsParId);
            commande.setStatut(StatutCommande.CANCELLED);
            commande.setDateAnnulation(LocalDateTime.now());
            commande.setPaye(false);
            commande = commandeRepository.save(commande);
            commande.setMouvementsStock(mouvements);
            Commande cancelledCommande = commande;
            safeEmailGeneration(
                    () -> generatedEmailService.generateOrderCancellation(cancelledCommande),
                    "annulation commande " + cancelledCommande.getNumero()
            );

            return CommandeMapper.toDto(commande);
        } catch (PessimisticLockingFailureException | ObjectOptimisticLockingFailureException exception) {
            throw stockConflict("Le stock de cette commande est en cours de modification. Veuillez reessayer.");
        }
    }

    @Transactional
    public CommandeResponseDto cancelForMembre(Long id, Long membreId) {
        Commande commande = getCommandeOrThrow(id);
        ensureCommandeBelongsToMembre(commande, membreId);
        return cancel(id);
    }

    @Transactional
    public CommandeResponseDto updateStatus(Long id, CommandeStatutUpdateDto dto) {
        if (dto.getStatut() == StatutCommande.CANCELLED) {
            return cancel(id);
        }

        Commande commande = getCommandeOrThrow(id);
        StatutCommande previousStatus = commande.getStatut();
        if (commande.getStatut() == StatutCommande.CANCELLED && dto.getStatut() != StatutCommande.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une commande annulee ne peut pas etre reactivee dans cette version.");
        }
        if (commande.getStatut() == StatutCommande.DISTRIBUTED && dto.getStatut() != StatutCommande.DISTRIBUTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une commande distribuee ne peut plus changer de statut.");
        }
        if (dto.getStatut() == StatutCommande.DISTRIBUTED && commande.getStatut() != StatutCommande.DISTRIBUTED) {
            List<MouvementStock> mouvements = registerSaleMovements(commande);
            commande.setMouvementsStock(mouvements);
            commande.setPaye(true);
            commande.setDatePaiement(LocalDateTime.now());
        }

        commande.setStatut(dto.getStatut());
        commande = commandeRepository.save(commande);
        if (previousStatus != StatutCommande.READY && dto.getStatut() == StatutCommande.READY) {
            Commande readyCommande = commande;
            safeEmailGeneration(
                    () -> generatedEmailService.generateOrderReady(readyCommande),
                    "commande prete " + readyCommande.getNumero()
            );
        }
        return CommandeMapper.toDto(commande);
    }

    private Commande getCommandeOrThrow(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable."));
    }

    private Membre resolveMembre(CommandeCreateRequestDto dto) {
        if (dto.getMembreId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un membreId est requis pour creer une commande.");
        }
        return membreRepository.findById(dto.getMembreId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable."));
    }

    private void ensureCommandeBelongsToMembre(Commande commande, Long membreId) {
        if (commande.getMembre() == null || !commande.getMembre().getId().equals(membreId)) {
            throw new AccessDeniedException("Cette commande n'appartient pas au membre connecte.");
        }
    }

    private Map<Long, BigDecimal> aggregateQuantites(List<CommandeCreateItemDto> produits) {
        Map<Long, BigDecimal> quantitesParProduit = new LinkedHashMap<>();
        for (CommandeCreateItemDto item : produits) {
            if (item.getQuantite() == null || item.getQuantite().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chaque ligne de commande doit avoir une quantite strictement positive.");
            }
            quantitesParProduit.merge(item.getProduitId(), scale(item.getQuantite()), BigDecimal::add);
        }
        return quantitesParProduit;
    }

    private Map<Long, Produit> loadProduitsForUpdate(Iterable<Long> produitIds) {
        Map<Long, Produit> produitsParId = new LinkedHashMap<>();
        for (Long produitId : produitIds) {
            Produit produit = produitRepository.findByIdForUpdate(produitId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable: " + produitId));
            if (Boolean.FALSE.equals(produit.getActif())) {
                throw stockConflict("Le produit " + produit.getNom() + " n'est plus disponible.");
            }
            produitsParId.put(produitId, produit);
        }
        return produitsParId;
    }

    private void validateStocks(Map<Long, BigDecimal> quantitesParProduit, Map<Long, Produit> produitsParId) {
        for (Map.Entry<Long, BigDecimal> entry : quantitesParProduit.entrySet()) {
            Produit produit = produitsParId.get(entry.getKey());
            BigDecimal stockActuel = produit.getStockActuel() != null ? produit.getStockActuel() : BigDecimal.ZERO;
            if (stockActuel.compareTo(entry.getValue()) < 0) {
                throw stockConflict("Stock insuffisant pour le produit " + produit.getNom() + ". Votre panier doit etre actualise.");
            }
        }
    }

    private List<MouvementStock> reserveStockForCommande(
            Commande commande,
            Map<Long, BigDecimal> quantitesParProduit,
            Map<Long, Produit> produitsParId
    ) {
        List<MouvementStock> mouvements = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : quantitesParProduit.entrySet()) {
            Produit produit = produitsParId.get(entry.getKey());
            BigDecimal quantite = scale(entry.getValue());
            BigDecimal stockAvant = scale(produit.getStockActuel() != null ? produit.getStockActuel() : BigDecimal.ZERO);
            BigDecimal stockApres = scale(stockAvant.subtract(quantite));

            produit.setStockActuel(stockApres);
            produitRepository.save(produit);
            safeEmailGeneration(
                    () -> generatedEmailService.generateStockAlertIfNeeded(produit, stockAvant, stockApres),
                    "alerte stock produit " + produit.getNom()
            );

            mouvements.add(mouvementStockRepository.save(buildMouvement(
                    TypeMouvementStock.RESERVATION,
                    produit,
                    commande,
                    quantite,
                    stockAvant,
                    stockApres
            )));
        }
        return mouvements;
    }

    private List<MouvementStock> restoreStockForCommande(
            Commande commande,
            Map<Long, BigDecimal> quantitesParProduit,
            Map<Long, Produit> produitsParId
    ) {
        List<MouvementStock> mouvements = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : quantitesParProduit.entrySet()) {
            Produit produit = produitsParId.get(entry.getKey());
            BigDecimal quantite = scale(entry.getValue());
            BigDecimal stockAvant = scale(produit.getStockActuel() != null ? produit.getStockActuel() : BigDecimal.ZERO);
            BigDecimal stockApres = scale(stockAvant.add(quantite));

            produit.setStockActuel(stockApres);
            produitRepository.save(produit);

            mouvements.add(mouvementStockRepository.save(buildMouvement(
                    TypeMouvementStock.ANNULATION,
                    produit,
                    commande,
                    quantite,
                    stockAvant,
                    stockApres
            )));
        }
        return mouvements;
    }

    private List<MouvementStock> registerSaleMovements(Commande commande) {
        Map<Long, BigDecimal> quantitesParProduit = new LinkedHashMap<>();
        Map<Long, Produit> produitsParId = new LinkedHashMap<>();

        for (LigneCommande ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();
            quantitesParProduit.merge(produit.getId(), ligne.getQuantite(), BigDecimal::add);
            produitsParId.put(produit.getId(), produit);
        }

        List<MouvementStock> mouvements = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : quantitesParProduit.entrySet()) {
            Produit produit = produitsParId.get(entry.getKey());
            BigDecimal stock = scale(produit.getStockActuel() != null ? produit.getStockActuel() : BigDecimal.ZERO);
            mouvements.add(mouvementStockRepository.save(buildMouvement(
                    TypeMouvementStock.VENTE,
                    produit,
                    commande,
                    scale(entry.getValue()),
                    stock,
                    stock
            )));
        }
        return mouvements;
    }

    private MouvementStock buildMouvement(
            TypeMouvementStock type,
            Produit produit,
            Commande commande,
            BigDecimal quantite,
            BigDecimal stockAvant,
            BigDecimal stockApres
    ) {
        return MouvementStock.builder()
                .id(generateMouvementId())
                .type(type)
                .produit(produit)
                .commande(commande)
                .quantite(scale(quantite))
                .stockAvant(scale(stockAvant))
                .stockApres(scale(stockApres))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String generateNumero(CreneauCollecte creneau, Long commandeId) {
        Long pointCollecteId = creneau.getPointCollecte().getId();
        return "PC"
                + String.format("%02d", pointCollecteId)
                + "-"
                + creneau.getCreneau().format(NUMERO_DATE_FORMAT)
                + "-"
                + String.format("%06d", commandeId);
    }

    private String generateTemporaryNumero() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String generateCodeRetrait() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String generateMouvementId() {
        return "MVT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException stockConflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private void safeEmailGeneration(RunnableWithException action, String context) {
        try {
            action.run();
        } catch (Exception exception) {
            log.warn("Generation d'email ignoree pour {}: {}", context, exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }
}
