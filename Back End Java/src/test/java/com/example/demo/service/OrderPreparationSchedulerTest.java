package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.service.CommandeService.ConditionalTransitionResult;

@ExtendWith(MockitoExtension.class)
class OrderPreparationSchedulerTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private CommandeService commandeService;

    @Mock
    private PreparationEmailService preparationEmailService;

    @Test
    void springCanInstantiateSchedulerWithProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CommandeRepository.class, () -> commandeRepository);
            context.registerBean(CommandeService.class, () -> commandeService);
            context.registerBean(PreparationEmailService.class, () -> preparationEmailService);
            context.register(OrderPreparationScheduler.class);
            context.refresh();

            assertThat(context.getBean(OrderPreparationScheduler.class)).isNotNull();
        }
    }

    @Test
    void schedulerQueriesConfirmedOrdersInTheNext24Hours() {
        ZoneId zone = OrderPreparationScheduler.BUSINESS_ZONE;
        LocalDateTime now = LocalDateTime.of(2026, 10, 10, 9, 15);
        Clock clock = Clock.fixed(now.atZone(zone).toInstant(), zone);
        OrderPreparationScheduler scheduler = new OrderPreparationScheduler(
                commandeRepository,
                commandeService,
                preparationEmailService,
                clock
        );
        when(commandeRepository.findPreparationCandidateIds(
                now,
                now.plusHours(24),
                StatutCommande.CONFIRMED
        )).thenReturn(List.of(4L, 6L));
        when(commandeService.transitionStatusIfCurrent(
                4L,
                StatutCommande.CONFIRMED,
                StatutCommande.IN_PREP
        )).thenReturn(ConditionalTransitionResult.TRANSITIONED);
        when(commandeService.transitionStatusIfCurrent(
                6L,
                StatutCommande.CONFIRMED,
                StatutCommande.IN_PREP
        )).thenReturn(ConditionalTransitionResult.TRANSITIONED);

        int transitioned = scheduler.processDueOrders();

        assertThat(transitioned).isEqualTo(2);
        verify(commandeRepository).findPreparationCandidateIds(
                now,
                now.plusHours(24),
                StatutCommande.CONFIRMED
        );
    }

    @Test
    void repeatedRunsOnlyCountOneRealTransition() {
        ZoneId zone = OrderPreparationScheduler.BUSINESS_ZONE;
        LocalDateTime now = LocalDateTime.of(2026, 10, 10, 9, 15);
        Clock clock = Clock.fixed(now.atZone(zone).toInstant(), zone);
        OrderPreparationScheduler scheduler = new OrderPreparationScheduler(
                commandeRepository,
                commandeService,
                preparationEmailService,
                clock
        );
        when(commandeRepository.findPreparationCandidateIds(
                now,
                now.plusHours(24),
                StatutCommande.CONFIRMED
        )).thenReturn(List.of(9L));
        when(commandeService.transitionStatusIfCurrent(
                9L,
                StatutCommande.CONFIRMED,
                StatutCommande.IN_PREP
        )).thenReturn(
                ConditionalTransitionResult.TRANSITIONED,
                ConditionalTransitionResult.NOT_ELIGIBLE
        );
        when(commandeRepository.findCreneauCollecteIdByCommandeId(9L)).thenReturn(Optional.of(15L));

        int firstRun = scheduler.processDueOrders();
        int secondRun = scheduler.processDueOrders();

        assertThat(firstRun).isEqualTo(1);
        assertThat(secondRun).isZero();
        verify(commandeService, times(2)).transitionStatusIfCurrent(
                9L,
                StatutCommande.CONFIRMED,
                StatutCommande.IN_PREP
        );
        verify(preparationEmailService, times(1)).sendAutomatically(15L);
    }

    @Test
    void severalOrdersOfSameSlotTriggerOneEmailAfterAllTransitions() {
        OrderPreparationScheduler scheduler = schedulerAt(LocalDateTime.of(2026, 10, 10, 9, 15));
        stubCandidates(List.of(1L, 2L));
        when(commandeService.transitionStatusIfCurrent(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq(StatutCommande.CONFIRMED),
                org.mockito.ArgumentMatchers.eq(StatutCommande.IN_PREP)
        )).thenReturn(ConditionalTransitionResult.TRANSITIONED);
        when(commandeRepository.findCreneauCollecteIdByCommandeId(1L)).thenReturn(Optional.of(20L));
        when(commandeRepository.findCreneauCollecteIdByCommandeId(2L)).thenReturn(Optional.of(20L));

        scheduler.processDueOrders();

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(commandeService, preparationEmailService);
        order.verify(commandeService).transitionStatusIfCurrent(1L, StatutCommande.CONFIRMED, StatutCommande.IN_PREP);
        order.verify(commandeService).transitionStatusIfCurrent(2L, StatutCommande.CONFIRMED, StatutCommande.IN_PREP);
        order.verify(preparationEmailService).sendAutomatically(20L);
        verify(preparationEmailService, times(1)).sendAutomatically(20L);
    }

    @Test
    void twoSlotsTriggerTwoPreparationEmails() {
        OrderPreparationScheduler scheduler = schedulerAt(LocalDateTime.of(2026, 10, 10, 9, 15));
        stubCandidates(List.of(3L, 4L));
        when(commandeService.transitionStatusIfCurrent(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq(StatutCommande.CONFIRMED),
                org.mockito.ArgumentMatchers.eq(StatutCommande.IN_PREP)
        )).thenReturn(ConditionalTransitionResult.TRANSITIONED);
        when(commandeRepository.findCreneauCollecteIdByCommandeId(3L)).thenReturn(Optional.of(30L));
        when(commandeRepository.findCreneauCollecteIdByCommandeId(4L)).thenReturn(Optional.of(40L));

        scheduler.processDueOrders();

        verify(preparationEmailService).sendAutomatically(30L);
        verify(preparationEmailService).sendAutomatically(40L);
    }

    @Test
    void emailFailureNeverRevertsSuccessfulTransitions() {
        OrderPreparationScheduler scheduler = schedulerAt(LocalDateTime.of(2026, 10, 10, 9, 15));
        stubCandidates(List.of(5L));
        AtomicReference<StatutCommande> status = new AtomicReference<>(StatutCommande.CONFIRMED);
        when(commandeService.transitionStatusIfCurrent(5L, StatutCommande.CONFIRMED, StatutCommande.IN_PREP))
                .thenAnswer(invocation -> {
                    status.set(StatutCommande.IN_PREP);
                    return ConditionalTransitionResult.TRANSITIONED;
                });
        when(commandeRepository.findCreneauCollecteIdByCommandeId(5L)).thenReturn(Optional.of(50L));
        doThrow(new RuntimeException("SMTP indisponible"))
                .when(preparationEmailService).sendAutomatically(50L);

        int transitioned = scheduler.processDueOrders();

        assertThat(transitioned).isEqualTo(1);
        assertThat(status.get()).isEqualTo(StatutCommande.IN_PREP);
    }

    @Test
    void noRealTransitionTriggersNoPreparationEmail() {
        OrderPreparationScheduler scheduler = schedulerAt(LocalDateTime.of(2026, 10, 10, 9, 15));
        stubCandidates(List.of(6L));
        when(commandeService.transitionStatusIfCurrent(6L, StatutCommande.CONFIRMED, StatutCommande.IN_PREP))
                .thenReturn(ConditionalTransitionResult.NOT_ELIGIBLE);

        scheduler.processDueOrders();

        verify(preparationEmailService, never()).sendAutomatically(org.mockito.ArgumentMatchers.anyLong());
    }

    private OrderPreparationScheduler schedulerAt(LocalDateTime now) {
        ZoneId zone = OrderPreparationScheduler.BUSINESS_ZONE;
        return new OrderPreparationScheduler(
                commandeRepository,
                commandeService,
                preparationEmailService,
                Clock.fixed(now.atZone(zone).toInstant(), zone)
        );
    }

    private void stubCandidates(List<Long> ids) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 10, 9, 15);
        when(commandeRepository.findPreparationCandidateIds(
                now,
                now.plusHours(24),
                StatutCommande.CONFIRMED
        )).thenReturn(ids);
    }
}
