package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dao.PointCollecteRepository;
import com.example.demo.dto.PointCollecteDto;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.PointCollecte;

@ExtendWith(MockitoExtension.class)
class PointCollecteServiceTest {

    @Mock
    private PointCollecteRepository pointCollecteRepository;

    @InjectMocks
    private PointCollecteService pointCollecteService;

    @Test
    void getAllActifsReturnsOnlyActiveCreneaux() {
        PointCollecte point = PointCollecte.builder()
                .id(1L)
                .nom("Point test")
                .adresse("Rue du test")
                .actif(true)
                .build();
        CreneauCollecte actif = creneau(point, 1L, true);
        CreneauCollecte inactif = creneau(point, 2L, false);
        point.setCreneaux(List.of(actif, inactif));
        when(pointCollecteRepository.findByActifTrue()).thenReturn(List.of(point));

        List<PointCollecteDto> result = pointCollecteService.getAllActifs();

        assertThat(result).singleElement().satisfies(dto ->
                assertThat(dto.getCreneaux())
                        .extracting(creneau -> creneau.getId())
                        .containsExactly(1L));
    }

    private CreneauCollecte creneau(PointCollecte point, Long id, boolean actif) {
        return CreneauCollecte.builder()
                .id(id)
                .pointCollecte(point)
                .creneau(LocalDate.now().plusDays(2))
                .heureDebut(LocalTime.of(18, 0))
                .heureFin(LocalTime.of(20, 0))
                .actif(actif)
                .build();
    }
}
