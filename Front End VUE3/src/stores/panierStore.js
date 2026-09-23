import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const usePanierStore = defineStore('panier', () => {
  // State
  const panier = ref([])

  // Initialiser depuis localStorage
  const initFromStorage = () => {
    const saved = localStorage.getItem('panier')
    if (saved) {
      panier.value = JSON.parse(saved)
    }
  }

  // Sauvegarder dans localStorage
  const saveToStorage = () => {
    localStorage.setItem('panier', JSON.stringify(panier.value))
  }

  // Actions
  const ajouterAuPanier = (produit, quantite = 1) => {
    const stockDisponible = Number(produit.stock ?? produit.stockActuel ?? 0)
    if (stockDisponible <= 0) {
      return
    }

    const existant = panier.value.find(item => item.id === produit.id)
    
    if (existant) {
      existant.quantite = Math.min(existant.quantite + quantite, stockDisponible)
    } else {
      panier.value.push({
        ...produit,
        stock: stockDisponible,
        stockActuel: stockDisponible,
        quantite: Math.min(Math.max(1, quantite), stockDisponible)
      })
    }
    
    saveToStorage()
  }

  const retirerDuPanier = (produitId) => {
    panier.value = panier.value.filter(item => item.id !== produitId)
    saveToStorage()
  }

  const modifierQuantite = (produitId, quantite) => {
    if (quantite <= 0) {
      retirerDuPanier(produitId)
      return
    }
    
    const item = panier.value.find(item => item.id === produitId)
    if (item) {
      const stockDisponible = Number(item.stock ?? item.stockActuel ?? quantite)
      item.quantite = Math.min(quantite, stockDisponible)
      saveToStorage()
    }
  }

  const synchroniserAvecProduits = (produits = []) => {
    const produitsParId = new Map(
      (Array.isArray(produits) ? produits : [])
        .filter(produit => produit?.id != null)
        .map(produit => [String(produit.id), produit])
    )

    let quantitesAjustees = 0
    let produitsRetires = 0

    panier.value = panier.value.reduce((acc, item) => {
      const produit = produitsParId.get(String(item.id))

      if (!produit || produit.actif === false) {
        produitsRetires += 1
        return acc
      }

      const stockDisponible = Number(produit.stock ?? produit.stockActuel ?? 0)
      if (stockDisponible <= 0) {
        produitsRetires += 1
        return acc
      }

      const quantiteCorrigee = Math.max(1, Math.min(Number(item.quantite || 1), stockDisponible))
      if (quantiteCorrigee !== Number(item.quantite || 1)) {
        quantitesAjustees += 1
      }

      acc.push({
        ...item,
        ...produit,
        categorie: produit.categorie || item.categorie || '',
        quantite: quantiteCorrigee,
        stock: stockDisponible,
        stockActuel: stockDisponible
      })
      return acc
    }, [])

    saveToStorage()

    return {
      quantitesAjustees,
      produitsRetires,
      panierVide: panier.value.length === 0
    }
  }

  const viderPanier = () => {
    panier.value = []
    saveToStorage()
  }

  // Getters
  const getTotalArticles = computed(() => {
    return panier.value.reduce((total, item) => total + item.quantite, 0)
  })

  const getTotalPrix = computed(() => {
    return panier.value.reduce((total, item) => {
      const tauxTVA = Number(item.tauxTVA ?? item.tauxTva ?? item.tva ?? 6) / 100
      const prixHT = Number(item.prixEffectif ?? item.prixPromotionnel ?? item.prixUnitaire ?? item.prix ?? 0)
      return total + (prixHT * (1 + tauxTVA) * item.quantite)
    }, 0)
  })

  // Initialiser au démarrage
  initFromStorage()

  return {
    panier,
    ajouterAuPanier,
    retirerDuPanier,
    modifierQuantite,
    synchroniserAvecProduits,
    viderPanier,
    getTotalArticles,
    getTotalPrix
  }
})
