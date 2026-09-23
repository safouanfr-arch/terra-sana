<template>
  <div class="catalogue-page">
    <div class="catalogue-header">
      <div>
        <h1>Catalogue des produits</h1>
        <p>Decouvrez notre selection de produits frais et locaux.</p>
      </div>
      <router-link to="/points-retrait" class="btn btn-secondary">
        Voir les prochains retraits
      </router-link>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div class="catalogue-filters">
      <div class="catalogue-search-row">
        <div class="search-bar">
          <label class="search-label" for="catalogue-search">Recherche</label>
          <input
            id="catalogue-search"
            v-model="searchTerm"
            type="text"
            placeholder="Nom, description, origine..."
            class="search-input"
          />
        </div>

        <div class="results-count">
          <strong>{{ produitsFiltres.length }}</strong>
          <span>produits visibles</span>
        </div>
      </div>

      <div class="filters-row">
        <div class="filter-group">
          <label for="selectedCategory">Categorie</label>
          <select id="selectedCategory" v-model="selectedCategory">
            <option value="tous">Toutes les categories</option>
            <option v-for="cat in categoriesDisponibles" :key="cat.id" :value="cat.nom">
              {{ cat.nom }}
            </option>
          </select>
        </div>

        <div class="filter-group">
          <label for="sortBy">Trier par</label>
          <select id="sortBy" v-model="sortBy">
            <option value="nom">Nom (A-Z)</option>
            <option value="prix-asc">Prix croissant</option>
            <option value="prix-desc">Prix decroissant</option>
          </select>
        </div>
      </div>

      <div class="badges-filters">
        <span class="filter-label">Filtres rapides</span>
        <button
          :class="['badge-filter', 'badge-bio', { active: selectedBadges.bio }]"
          @click="toggleBadgeFilter('bio')"
        >
          Bio
        </button>
        <button
          :class="['badge-filter', 'badge-local', { active: selectedBadges.local }]"
          @click="toggleBadgeFilter('local')"
        >
          Local
        </button>
        <button
          :class="['badge-filter', 'badge-vegan', { active: selectedBadges.vegan }]"
          @click="toggleBadgeFilter('vegan')"
        >
          Vegan
        </button>
        <button
          :class="['badge-filter', 'badge-vegetarien', { active: selectedBadges.vegetarien }]"
          @click="toggleBadgeFilter('vegetarien')"
        >
          Vegetarien
        </button>
        <button v-if="hasActiveBadgeFilters" class="clear-filters" @click="clearBadgeFilters">
          Effacer les filtres
        </button>
      </div>
    </div>

    <div v-if="produitsFiltres.length === 0" class="empty-state">
      <div class="empty-icon">0</div>
      <h3>Aucun produit trouve</h3>
      <p>Essayez de modifier vos criteres de recherche.</p>
    </div>

    <div v-else class="produits-grid">
      <router-link
        v-for="produit in produitsFiltres"
        :key="produit.id"
        :to="`/produit/${produit.id}`"
        class="produit-card"
      >
        <img
          class="produit-image"
          :src="getImageSrc(produit)"
          :alt="`Image de ${produit.nom}`"
          @error="onImgError($event, produit)"
        />

        <div class="produit-content">
          <div class="produit-header-badges">
            <div class="produit-category">{{ produit.categorie }}</div>
            <div class="produit-badges">
              <span v-if="produit.badges?.bio" class="badge badge-bio" title="Bio">Bio</span>
              <span v-if="produit.badges?.local" class="badge badge-local" title="Local">Local</span>
              <span v-if="produit.badges?.vegan" class="badge badge-vegan" title="Vegan">Vegan</span>
              <span v-if="produit.badges?.vegetarien" class="badge badge-vegetarien" title="Vegetarien">Veg</span>
            </div>
          </div>

          <h3 class="produit-nom">{{ produit.nom }}</h3>
          <p class="produit-description">{{ getShortDescription(produit.description) }}</p>

          <div v-if="produit.origine || produit.producteur" class="produit-meta-line">
            <span v-if="produit.origine">{{ produit.origine }}</span>
            <span v-if="produit.origine && produit.producteur" class="meta-separator"></span>
            <span v-if="produit.producteur">{{ produit.producteur }}</span>
          </div>

          <div class="produit-footer">
            <div class="produit-prix">
              <span v-if="hasPromotion(produit)" class="prix-original">{{ calculatePrixTTCOriginal(produit).toFixed(2) }} €</span>
              <span class="prix">{{ calculatePrixTTC(produit).toFixed(2) }} €</span>
              <span class="unite">/ {{ produit.unite }}</span>
              <span v-if="hasPromotion(produit)" class="promotion-label">{{ produit.promotionNom }}</span>
            </div>
            <div class="produit-stock">
              <span v-if="produit.stock <= 5" class="stock-low">Stock bas ({{ produit.stock }})</span>
              <span v-else class="stock-ok">En stock ({{ produit.stock }})</span>
            </div>
          </div>

          <button
            :class="['btn', 'btn-add-cart', { added: addedToCart === produit.id }]"
            @click.prevent="handleAddToCart(produit, $event)"
          >
            {{ addedToCart === produit.id ? 'Ajoute' : 'Ajouter au panier' }}
          </button>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { usePanierStore } from '../../stores/panierStore'
import {
  categoriesApi,
  produitsApi,
  getApiErrorMessage
} from '../../services/api.js'
import { getProductImageSrc, productPlaceholder } from '../../utils/imageHelper.js'

const panierStore = usePanierStore()

const produitsData = ref([])
const categories = ref([])
const apiError = ref('')
const searchTerm = ref('')
const selectedCategory = ref('tous')
const sortBy = ref('nom')
const addedToCart = ref(null)
const selectedBadges = ref({ bio: false, local: false, vegan: false, vegetarien: false })

onMounted(async () => {
  apiError.value = ''
  try {
    categories.value = await categoriesApi.listActive()
    produitsData.value = await produitsApi.listActive(categories.value)
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
    produitsData.value = []
  }
})

function calculatePrixTTC(produit) {
  const tauxTVA = Number(produit.tva || produit.tauxTVA || produit.tauxTva || 6) / 100
  return getPrixEffectifHT(produit) * (1 + tauxTVA)
}

function calculatePrixTTCOriginal(produit) {
  const tauxTVA = Number(produit.tva || produit.tauxTVA || produit.tauxTva || 6) / 100
  return Number(produit.prixAvantPromotion ?? produit.prixOriginal ?? produit.prixUnitaire ?? produit.prix ?? 0) * (1 + tauxTVA)
}

function hasPromotion(produit) {
  return Boolean(produit?.promotionActive && Number(produit?.reductionMontant ?? 0) > 0)
}

function getPrixEffectifHT(produit) {
  return Number(produit.prixEffectif ?? produit.prixPromotionnel ?? produit.prixUnitaire ?? produit.prix ?? 0)
}

function getImageSrc(produit) {
  return getProductImageSrc(produit)
}

function onImgError(ev, produit) {
  ev.target.src = productPlaceholder(produit?.nom, produit?.categorie)
}

function getShortDescription(description) {
  const value = (description || '').trim()
  if (!value) {
    return 'Produit disponible dans le catalogue Terra Sana.'
  }
  return value.length > 96 ? `${value.slice(0, 93).trim()}...` : value
}

function toggleBadgeFilter(badge) {
  selectedBadges.value[badge] = !selectedBadges.value[badge]
}

function clearBadgeFilters() {
  selectedBadges.value = { bio: false, local: false, vegan: false, vegetarien: false }
}

const hasActiveBadgeFilters = computed(() => Object.values(selectedBadges.value).some(Boolean))

const categoriesDisponibles = computed(() => {
  const categoriesAvecProduits = new Set(
    produitsData.value
      .filter(produit => produit.actif !== false && Number(produit.stock ?? 0) > 0)
      .map(produit => produit.categorie)
      .filter(Boolean)
  )

  return categories.value.filter(categorie => categoriesAvecProduits.has(categorie.nom))
})

function handleAddToCart(produit, event) {
  event.preventDefault()
  event.stopPropagation()
  panierStore.ajouterAuPanier(produit, 1)
  addedToCart.value = produit.id
  window.setTimeout(() => {
    addedToCart.value = null
  }, 1800)
}

const produitsFiltres = computed(() => {
  let produits = produitsData.value.filter(p => p.stock > 0 && (p.actif !== false))

  if (searchTerm.value) {
    const query = searchTerm.value.toLowerCase()
    produits = produits.filter(p =>
      p.nom.toLowerCase().includes(query) ||
      (p.description && p.description.toLowerCase().includes(query)) ||
      (p.origine && p.origine.toLowerCase().includes(query)) ||
      (p.producteur && p.producteur.toLowerCase().includes(query))
    )
  }

  if (selectedCategory.value !== 'tous') {
    produits = produits.filter(p => p.categorie === selectedCategory.value)
  }

  if (hasActiveBadgeFilters.value) {
    const badgesActifs = Object.keys(selectedBadges.value).filter(key => selectedBadges.value[key])
    produits = produits.filter(produit => badgesActifs.every(badgeKey => produit.badges?.[badgeKey] === true))
  }

  return [...produits].sort((a, b) => {
    switch (sortBy.value) {
      case 'nom':
        return a.nom.localeCompare(b.nom)
      case 'prix-asc':
        return getPrixEffectifHT(a) - getPrixEffectifHT(b)
      case 'prix-desc':
        return getPrixEffectifHT(b) - getPrixEffectifHT(a)
      default:
        return 0
    }
  })
})
</script>

<style scoped>
.catalogue-page {
  gap: 1.6rem;
}

.catalogue-header p {
  max-width: 56ch;
}

.catalogue-filters {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1rem;
  align-items: stretch;
}

.catalogue-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: end;
}

.search-bar {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
}

.search-label {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.search-input {
  width: 100%;
  box-sizing: border-box;
  min-height: 50px;
  padding: 0 1rem;
  font-size: 1rem;
}

.filters-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  align-items: end;
  min-width: 0;
}

.filter-group {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
}

.filter-group label {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.results-count {
  min-width: 160px;
  max-width: 100%;
  padding: 0.9rem 1rem;
  border-radius: 10px;
  border: 1px solid rgba(61, 73, 68, 0.1);
  background: rgba(255, 253, 248, 0.88);
  display: grid;
  justify-items: start;
}

.results-count strong {
  font-size: 1.35rem;
  line-height: 1;
  color: var(--text-primary);
}

.results-count span {
  margin-top: 0.25rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.badges-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
  min-width: 0;
  max-width: 100%;
}

.filter-label {
  margin-right: 0.25rem;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-secondary);
}

.clear-filters {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 0.85rem;
  border-radius: 999px;
  border: 1px solid rgba(138, 67, 67, 0.14);
  background: rgba(138, 67, 67, 0.08);
  color: var(--error-color);
  font-weight: 600;
}

.badge-filter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.produits-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.2rem;
}

.produit-card {
  display: grid;
  grid-template-rows: auto 1fr;
  overflow: hidden;
}

.produit-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
  border-radius: 0;
  background: #ece7de;
}

.produit-content {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  padding: 1.15rem;
}

.produit-header-badges {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.produit-category {
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.produit-badges {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.produit-nom {
  margin: 0;
  font-size: 1.48rem;
  line-height: 1.06;
}

.produit-description {
  margin: 0;
  min-height: 3.2em;
  color: var(--text-secondary);
  font-size: 0.96rem;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.produit-meta-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.55rem;
  min-height: 1.1rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.meta-separator {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: rgba(61, 73, 68, 0.28);
}

.produit-footer {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 0.75rem;
  margin-top: auto;
}

.produit-prix {
  display: grid;
  gap: 0.15rem;
}

.prix {
  font-size: 1.08rem;
  font-weight: 700;
  color: #26483e;
}

.prix-original {
  color: var(--text-muted);
  font-size: 0.82rem;
  text-decoration: line-through;
}

.promotion-label {
  width: fit-content;
  padding: 0.18rem 0.5rem;
  border-radius: 999px;
  background: rgba(184, 121, 63, 0.12);
  color: #8a5a2b;
  font-size: 0.74rem;
  font-weight: 700;
}

.unite {
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.produit-stock {
  text-align: right;
  font-size: 0.83rem;
  color: var(--text-secondary);
}

.stock-low {
  color: #8a5a2b;
}

.stock-ok {
  color: #476356;
}

.btn-add-cart {
  width: 100%;
  margin-top: 0.1rem;
}

@media (max-width: 900px) {
  .catalogue-header {
    align-items: stretch;
    flex-direction: column;
  }

  .catalogue-search-row,
  .filters-row {
    grid-template-columns: 1fr;
  }

  .results-count {
    min-width: 0;
    width: 100%;
  }
}

@media (max-width: 560px) {
  .catalogue-filters {
    padding: 0.95rem;
  }

  .badges-filters {
    align-items: stretch;
  }

  .filter-label {
    flex: 1 0 100%;
  }

  .badge-filter,
  .clear-filters {
    flex: 1 1 auto;
    justify-content: center;
    text-align: center;
  }
}
</style>
