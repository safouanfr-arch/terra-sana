package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.service.CommandeService.ConditionalTransitionResult;

@Component
@ConditionalOnProperty(name = "app.order-preparation.enabled", havingValue = "true", matchIfMissing = true)
public class OrderPreparationScheduler {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Brussels");
    private static final Logger log = LoggerFactory.getLogger(OrderPreparationScheduler.class);

    private final CommandeRepository commandeRepository;
    private final CommandeService commandeService;
    private final PreparationEmailService preparationEmailService;
    private final Clock clock;

    @Autowired
    public OrderPreparationScheduler(
            CommandeRepository commandeRepository,
            CommandeService commandeService,
            PreparationEmailService preparationEmailService
    ) {
        this(commandeRepository, commandeService, preparationEmailService, Clock.system(BUSINESS_ZONE));
    }

    OrderPreparationScheduler(
            CommandeRepository commandeRepository,
            CommandeService commandeService,
            PreparationEmailService preparationEmailService,
            Clock clock
    ) {
        this.commandeRepository = commandeRepository;
        this.commandeService = commandeService;
        this.preparationEmailService = preparationEmailService;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.order-preparation.cron:0 */5 * * * *}", zone = "Europe/Brussels")
    public void scheduleOrderPreparation() {
        processDueOrders();
    }

    int processDueOrders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.plusHours(24);
        List<Long> candidateIds = commandeRepository.findPreparationCandidateIds(
                now,
                deadline,
                StatutCommande.CONFIRMED
        );

        int transitionedCount = 0;
        Set<Long> transitionedCreneauIds = new LinkedHashSet<>();
        for (Long commandeId : candidateIds) {
            try {
                ConditionalTransitionResult result = commandeService.transitionStatusIfCurrent(
                        commandeId,
                        StatutCommande.CONFIRMED,
                        StatutCommande.IN_PREP
                );
                if (result == ConditionalTransitionResult.TRANSITIONED) {
                    transitionedCount++;
                    commandeRepository.findCreneauCollecteIdByCommandeId(commandeId)
                            .ifPresent(transitionedCreneauIds::add);
                }
            } catch (RuntimeException exception) {
                log.warn("Passage automatique en preparation ignore pour la commande {}: {}",
                        commandeId, exception.getMessage());
            }
        }

        for (Long creneauId : transitionedCreneauIds) {
            try {
                preparationEmailService.sendAutomatically(creneauId);
            } catch (RuntimeException exception) {
                log.warn("Envoi automatique du dossier ignore pour le creneau {}: {}",
                        creneauId, exception.getMessage());
            }
        }
        return transitionedCount;
    }
}
