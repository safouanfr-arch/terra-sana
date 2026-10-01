package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.demo.model.CreneauCollecte;

class DemoDataSeederTest {

    @ParameterizedTest
    @MethodSource("startupTimes")
    void existingSlotMustBeStrictlyMoreThanTwentyFourHoursAway(LocalDateTime now) {
        assertThat(DemoDataSeeder.isBeyondMinimumOrderLeadTime(slotAt(now.plusMinutes(30)), now)).isFalse();
        assertThat(DemoDataSeeder.isBeyondMinimumOrderLeadTime(slotAt(now.plusHours(6)), now)).isFalse();
        assertThat(DemoDataSeeder.isBeyondMinimumOrderLeadTime(slotAt(now.plusHours(23).plusMinutes(59)), now)).isFalse();
        assertThat(DemoDataSeeder.isBeyondMinimumOrderLeadTime(slotAt(now.plusHours(24)), now)).isFalse();
        assertThat(DemoDataSeeder.isBeyondMinimumOrderLeadTime(slotAt(now.plusHours(24).plusMinutes(1)), now)).isTrue();
    }

    private static Stream<Arguments> startupTimes() {
        return Stream.of(
                Arguments.of(LocalDateTime.of(2026, 10, 1, 8, 0)),
                Arguments.of(LocalDateTime.of(2026, 10, 1, 16, 0)),
                Arguments.of(LocalDateTime.of(2026, 10, 1, 23, 50)),
                Arguments.of(LocalDateTime.of(2026, 10, 1, 23, 59)),
                Arguments.of(LocalDateTime.of(2026, 10, 2, 0, 1))
        );
    }

    private static CreneauCollecte slotAt(LocalDateTime dateTime) {
        return CreneauCollecte.builder()
                .creneau(dateTime.toLocalDate())
                .heureDebut(dateTime.toLocalTime())
                .actif(true)
                .build();
    }
}
