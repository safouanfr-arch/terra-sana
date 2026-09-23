package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.model.enums.StatutCommande;

@Component
@ConditionalOnProperty(name = "app.order-reminder.enabled", havingValue = "true", matchIfMissing = true)
public class OrderReminderScheduler {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Brussels");
    private static final Logger log = LoggerFactory.getLogger(OrderReminderScheduler.class);
    private static final List<StatutCommande> ELIGIBLE_STATUSES = List.of(
            StatutCommande.CONFIRMED,
            StatutCommande.IN_PREP,
            StatutCommande.READY
    );

    private final CommandeRepository commandeRepository;
    private final GeneratedEmailService generatedEmailService;
    private final Clock clock;

    public OrderReminderScheduler(
            CommandeRepository commandeRepository,
            GeneratedEmailService generatedEmailService
    ) {
        this(commandeRepository, generatedEmailService, Clock.system(BUSINESS_ZONE));
    }

    OrderReminderScheduler(
            CommandeRepository commandeRepository,
            GeneratedEmailService generatedEmailService,
            Clock clock
    ) {
        this.commandeRepository = commandeRepository;
        this.generatedEmailService = generatedEmailService;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.order-reminder.cron:0 */15 * * * *}", zone = "Europe/Brussels")
    public void scheduleOrderReminders() {
        processOrderReminders();
    }

    int processOrderReminders() {
        LocalDate reminderDate = LocalDate.now(clock).plusDays(1);
        LocalDateTime startInclusive = reminderDate.atStartOfDay();
        LocalDateTime endExclusive = reminderDate.plusDays(1).atStartOfDay();
        List<Long> candidateIds = commandeRepository.findReminderCandidateIds(
                startInclusive,
                endExclusive,
                ELIGIBLE_STATUSES
        );

        int generatedCount = 0;
        for (Long commandeId : candidateIds) {
            try {
                if (generatedEmailService.sendOrderReminderIfDue(commandeId, reminderDate).isPresent()) {
                    generatedCount++;
                }
            } catch (RuntimeException exception) {
                log.warn("Rappel J-1 ignore pour la commande {}: {}", commandeId, exception.getMessage());
            }
        }
        return generatedCount;
    }
}
