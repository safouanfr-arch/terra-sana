package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dto.GeneratedEmailDto;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.enums.StatutCommande;

@ExtendWith(MockitoExtension.class)
class OrderReminderSchedulerTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private GeneratedEmailService generatedEmailService;

    @Test
    void repeatedSchedulerRunsProduceOnlyOneReminder() {
        ZoneId zone = OrderReminderScheduler.BUSINESS_ZONE;
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 5, 1, 10, 0).atZone(zone).toInstant(),
                zone
        );
        OrderReminderScheduler scheduler = new OrderReminderScheduler(
                commandeRepository,
                generatedEmailService,
                clock
        );
        LocalDate reminderDate = LocalDate.of(2026, 5, 2);
        List<StatutCommande> statuses = List.of(
                StatutCommande.CONFIRMED,
                StatutCommande.IN_PREP,
                StatutCommande.READY
        );
        GeneratedEmailDto generated = GeneratedEmailDto.builder()
                .type(EmailNotificationType.ORDER_REMINDER_J1)
                .commandeId(9L)
                .build();
        when(commandeRepository.findReminderCandidateIds(
                reminderDate.atStartOfDay(),
                reminderDate.plusDays(1).atStartOfDay(),
                statuses
        )).thenReturn(List.of(9L));
        when(generatedEmailService.sendOrderReminderIfDue(9L, reminderDate))
                .thenReturn(Optional.of(generated), Optional.empty());

        int firstRun = scheduler.processOrderReminders();
        int secondRun = scheduler.processOrderReminders();

        assertThat(firstRun).isEqualTo(1);
        assertThat(secondRun).isZero();
        verify(commandeRepository, times(2)).findReminderCandidateIds(
                eq(reminderDate.atStartOfDay()),
                eq(reminderDate.plusDays(1).atStartOfDay()),
                eq(statuses)
        );
        verify(generatedEmailService, times(2)).sendOrderReminderIfDue(9L, reminderDate);
    }
}
