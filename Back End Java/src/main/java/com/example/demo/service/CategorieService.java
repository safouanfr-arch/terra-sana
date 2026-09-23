package com.example.demo.service;

import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dto.CategorieDto;
import com.example.demo.mapper.CategorieMapper;
import com.example.demo.model.Categorie;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CategorieService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;

    public CategorieDto create(CategorieDto dto) {
        if (categorieRepository.existsByNom(dto.getNom())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une categorie avec ce nom existe deja.");
        }

        Categorie entity = CategorieMapper.fromDto(dto);
        return CategorieMapper.toDto(categorieRepository.save(entity));
    }

    public List<CategorieDto> getAll() {
        return categorieRepository.findAll().stream()
                .map(categorie -> {
                    CategorieDto dto = CategorieMapper.toDto(categorie);
                    dto.setCompteur(produitRepository.countByCategorie_Id(categorie.getId()));
                    return dto;
                })
                .toList();
    }

    public List<CategorieDto> getAllActives() {
        return categorieRepository.findByActifTrue().stream()
                .map(CategorieMapper::toDto)
                .toList();
    }

    public CategorieDto update(Long id, CategorieDto dto) {
        Categorie entity = categorieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categorie introuvable."));

        if (produitRepository.countByCategorie_Id(id) > 0 && Boolean.FALSE.equals(dto.getActif())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de desactiver la categorie : elle contient encore des produit(s).");
        }

        CategorieMapper.updateEntityFromDto(entity, dto);
        return CategorieMapper.toDto(categorieRepository.save(entity));
    }

    public void delete(Long id) {
        Categorie entity = categorieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categorie introuvable."));

        if (produitRepository.countByCategorie_Id(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de supprimer : la categorie contient encore des produit(s).");
        }

        categorieRepository.delete(entity);
    }
}
