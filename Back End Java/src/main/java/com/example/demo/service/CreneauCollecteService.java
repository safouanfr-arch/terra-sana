package com.example.demo.service;

import com.example.demo.dto.CreneauCollecteDto;
import com.example.demo.mapper.CreneauCollecteMapper;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.PointCollecte;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.PointCollecteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CreneauCollecteService {

    private final CreneauCollecteRepository creneauCollecteRepository;
    private final PointCollecteRepository pointCollecteRepository;

    public CreneauCollecteDto create(CreneauCollecteDto dto) {
        validateHoraire(dto);
        PointCollecte pointCollecte = pointCollecteRepository.findById(dto.getPointCollecteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Point de collecte introuvable."));

        CreneauCollecte entity = CreneauCollecteMapper.toEntity(dto, pointCollecte);
        entity = creneauCollecteRepository.save(entity);
        return CreneauCollecteMapper.toDto(entity);
    }

    public CreneauCollecteDto update(Long id, CreneauCollecteDto dto) {
        validateHoraire(dto);
        CreneauCollecte creneau = creneauCollecteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creneau introuvable."));

        PointCollecte nouveauPoint = null;
        if (dto.getPointCollecteId() != null &&
                (creneau.getPointCollecte() == null ||
                 !dto.getPointCollecteId().equals(creneau.getPointCollecte().getId()))) {
            nouveauPoint = pointCollecteRepository.findById(dto.getPointCollecteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nouveau point de collecte introuvable."));
        }

        CreneauCollecteMapper.updateEntityFromDto(creneau, dto, nouveauPoint);

        creneau = creneauCollecteRepository.save(creneau);
        return CreneauCollecteMapper.toDto(creneau);
    }

    public void delete(Long id) {
        if (!creneauCollecteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Creneau introuvable.");
        }
        creneauCollecteRepository.deleteById(id);
    }

    private void validateHoraire(CreneauCollecteDto dto) {
        if (dto.getHeureDebut() != null
                && dto.getHeureFin() != null
                && !dto.getHeureFin().isAfter(dto.getHeureDebut())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'heure de fin doit etre posterieure a l'heure de debut."
            );
        }
    }
}
