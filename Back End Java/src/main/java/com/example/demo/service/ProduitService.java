package com.example.demo.service;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.MouvementStockRepository;
import com.example.demo.dto.ProduitDto;
import com.example.demo.dto.ProduitStockAdjustmentDto;
import com.example.demo.mapper.ProduitMapper;
import com.example.demo.model.Administrateur;
import com.example.demo.model.Categorie;
import com.example.demo.model.MouvementStock;
import com.example.demo.model.Produit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;


@Service
@RequiredArgsConstructor

public class ProduitService {
    private static final Logger log = LoggerFactory.getLogger(ProduitService.class);
	private final ProduitRepository produitRepository;
	private final CategorieRepository categorieRepository;
	private final MouvementStockRepository mouvementStockRepository;
	private final AdministrateurRepository administrateurRepository;
    private final GeneratedEmailService generatedEmailService;
    private final PromotionService promotionService;
	
	@Transactional
	public  ProduitDto create(ProduitDto dto) {

		Categorie categorie = categorieRepository.findById(dto.getCategorieId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categorie introuvable."));
		if (produitRepository.existsByNomAndUniteAndCategorieId(dto.getNom(), dto.getUnite(), dto.getCategorieId())) {
		        throw new ResponseStatusException(HttpStatus.CONFLICT, "Un produit avec ce nom existe deja dans cette categorie.");
		}
		 
		Produit entity= ProduitMapper.toEntity(dto, categorie);
		entity = produitRepository.save(entity);
		return toDto(entity);
	}
	
	@Transactional
	public void delete(Long id) {
		Produit entity = produitRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable."));
		produitRepository.delete(entity);
	}
	
	@Transactional
	public  ProduitDto update(Long id, ProduitDto dto) {
		Produit entity = produitRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable."));
		
		
		Categorie categorie = categorieRepository.findById(dto.getCategorieId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categorie introuvable."));
		
		ProduitMapper.updateEntityFromDto(entity,dto, categorie);		
		
		produitRepository.save(entity);
		return toDto(entity);
	}
	
	
	@Transactional(readOnly = true)
	public List<ProduitDto> getAll() {
	    return produitRepository.findAll()
	            .stream()
	            .map(this::toDto)
	            .toList();
	}

	
	@Transactional(readOnly = true)
	public List<ProduitDto> getAllActives() {
		return produitRepository.findByActifTrue()
	            .stream()
	            .map(this::toDto)
	            .toList();
	}

	@Transactional(readOnly = true)
	public ProduitDto getById(Long id) {
		Produit entity = produitRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable."));
		return toDto(entity);
	}

	@Transactional
	public ProduitDto adjustStock(Long id, ProduitStockAdjustmentDto dto, Long administrateurId) {
		try {
			Produit produit = produitRepository.findByIdForUpdate(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
			Administrateur administrateur = administrateurRepository.findById(administrateurId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrateur introuvable"));

			BigDecimal stockAvant = scale(produit.getStockActuel() != null ? produit.getStockActuel() : BigDecimal.ZERO);
			BigDecimal stockApres = scale(dto.getStockActuel());

			if (stockApres.compareTo(BigDecimal.ZERO) < 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le stock ne peut pas etre negatif.");
			}

			if (stockAvant.compareTo(stockApres) == 0) {
				return toDto(produit);
			}

			produit.setStockActuel(stockApres);
			produitRepository.save(produit);
	        safeEmailGeneration(() -> generatedEmailService.generateStockAlertIfNeeded(produit, stockAvant, stockApres), "ajustement stock produit " + produit.getNom());

			MouvementStock mouvement = MouvementStock.builder()
					.id(generateMouvementId())
					.type(com.example.demo.model.enums.TypeMouvementStock.AJUSTEMENT)
					.quantite(scale(stockApres.subtract(stockAvant).abs()))
					.stockAvant(stockAvant)
					.stockApres(stockApres)
					.timestamp(LocalDateTime.now())
					.produit(produit)
					.administrateur(administrateur)
					.build();
			mouvementStockRepository.save(mouvement);

			return toDto(produit);
		} catch (PessimisticLockingFailureException | ObjectOptimisticLockingFailureException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce stock est en cours de modification. Veuillez recharger et reessayer.");
		}
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private String generateMouvementId() {
		return "MVT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
	}

    private void safeEmailGeneration(RunnableWithException action, String context) {
        try {
            action.run();
        } catch (Exception exception) {
            log.warn("Generation d'email ignoree pour {}: {}", context, exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }

	private ProduitDto toDto(Produit produit) {
		return promotionService.applyPromotionToDto(produit, ProduitMapper.toDto(produit));
	}
	
}
