package com.example.demo.service;

import com.example.demo.dto.PointCollecteDto;
import com.example.demo.mapper.PointCollecteMapper;
import com.example.demo.model.PointCollecte;
import com.example.demo.dao.PointCollecteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointCollecteService {

    private final PointCollecteRepository pointCollecteRepository;

    public PointCollecteDto create(PointCollecteDto dto) {
        PointCollecte entity = PointCollecteMapper.toEntity(dto);

        entity = pointCollecteRepository.save(entity);
        return PointCollecteMapper.toDto(entity);
    }

    public List<PointCollecteDto> getAll() {
        return pointCollecteRepository.findAll()
                .stream()
                .map(PointCollecteMapper::toDto)
                .toList();
    }

    public List<PointCollecteDto> getAllActifs() {
        return pointCollecteRepository.findByActifTrue()
                .stream()
                .map(this::toMemberDto)
                .toList();
    }

    private PointCollecteDto toMemberDto(PointCollecte pointCollecte) {
        PointCollecteDto dto = PointCollecteMapper.toDto(pointCollecte);
        dto.setCreneaux(dto.getCreneaux().stream()
                .filter(creneau -> Boolean.TRUE.equals(creneau.getActif()))
                .toList());
        return dto;
    }

    public PointCollecteDto update(Long id, PointCollecteDto dto) {
        PointCollecte pointCollecte = pointCollecteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Point de collecte introuvable."));

        PointCollecteMapper.updateEntityFromDto(pointCollecte, dto);

        pointCollecte = pointCollecteRepository.save(pointCollecte);
        return PointCollecteMapper.toDto(pointCollecte);
    }

    public void delete(Long id) {
        if (!pointCollecteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Point de collecte introuvable.");
        }
        pointCollecteRepository.deleteById(id);
    }
}
