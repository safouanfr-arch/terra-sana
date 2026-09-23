package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.PointCollecteRepository;
import com.example.demo.dto.CreneauCollecteDto;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.PointCollecte;

@ExtendWith(MockitoExtension.class)
class CreneauCollecteServiceTest {

    @Mock
    private CreneauCollecteRepository creneauCollecteRepository;

    @Mock
    private PointCollecteRepository pointCollecteRepository;

    @InjectMocks
    private CreneauCollecteService creneauCollecteService;

    @Test
    void updateKeepsExistingCreneauIdAndUpdatesDateAndHours() {
        PointCollecte point = PointCollecte.builder()
                .id(3L)
                .nom("Depot")
                .adresse("Rue Test")
                .actif(true)
                .build();
        CreneauCollecte creneau = CreneauCollecte.builder()
                .id(9L)
                .pointCollecte(point)
                .creneau(LocalDate.of(2026, 5, 1))
                .heureDebut(LocalTime.of(18, 0))
                .heureFin(LocalTime.of(20, 0))
                .actif(true)
                .build();

        when(creneauCollecteRepository.findById(9L)).thenReturn(Optional.of(creneau));
        when(creneauCollecteRepository.save(creneau)).thenReturn(creneau);

        CreneauCollecteDto result = creneauCollecteService.update(9L, CreneauCollecteDto.builder()
                .pointCollecteId(3L)
                .date(LocalDate.of(2026, 5, 2))
                .heureDebut(LocalTime.of(17, 30))
                .heureFin(LocalTime.of(19, 30))
                .actif(true)
                .build());

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getPointCollecteId()).isEqualTo(3L);
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(result.getHeureDebut()).isEqualTo(LocalTime.of(17, 30));
        assertThat(result.getHeureFin()).isEqualTo(LocalTime.of(19, 30));
    }

    @Test
    void createRejectsEndHourBeforeStartHour() {
        CreneauCollecteDto dto = CreneauCollecteDto.builder()
                .pointCollecteId(3L)
                .date(LocalDate.of(2026, 5, 2))
                .heureDebut(LocalTime.of(22, 0))
                .heureFin(LocalTime.of(20, 0))
                .actif(true)
                .build();

        assertThatThrownBy(() -> creneauCollecteService.create(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("L'heure de fin");
    }
}
