<template>
  <div v-if="!produit" class="page-container">
    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div class="error-message">
      <div class="error-icon">0</div>
      <h2>Produit non trouvé</h2>
      <p>Le produit que vous recherchez n'existe pas ou n'est plus disponible.</p>
      <router-link to="/catalogue" class="btn btn-primary">Retour au catalogue</router-link>
    </div>
  </div>

  <div v-else class="fiche-produit-page">
    <div class="fiche-header">
      <router-link to="/catalogue" class="back-link">← Retour au catalogue</router-link>
    </div>

    <div class="fiche-produit">
      <div class="fiche-image-section">
        <div class="fiche-image-main">
          <img class="image-display" :src="getMainImageSrc()" :alt="`Image de ${produit?.nom}`" @error="onImgError($event)" />
        </div>
        
        <!-- Photo gallery thumbnails -->
        <div v-if="hasPhotos" class="fiche-image-gallery">
          <!-- Original emoji as first option -->
          <div 
            :class="['gallery-thumbnail', { active: currentImageType === 'emoji' }]"
            @click="selectImage('emoji', produit.image)"
          >
            <img
              v-if="isImageSource(produit.image)"
              class="photo-thumb-img"
              :src="resolveProductImageSrc(produit.image)"
              :alt="`Image de ${produit.nom}`"
            />
            <span v-else class="emoji-thumb">{{ produit.image }}</span>
          </div>
          
          <!-- Additional photos -->
          <div 
            v-for="(photo, index) in productPhotos" 
            :key="index"
            :class="['gallery-thumbnail', { active: currentImageType === 'photo' && currentImage === photo }]"
            @click="selectImage('photo', photo)"
          >
            <div class="photo-thumb" :style="{ backgroundImage: `url(${resolveProductImageSrc(photo)})` }">
              <span v-if="!photo" class="emoji-thumb">IMG</span>
            </div>
          </div>
        </div>
        
        <div class="fiche-badges">
          <span v-if="produit.badges?.local" class="badge badge-local">Local</span>
          <span v-if="produit.badges?.bio" class="badge badge-bio">Bio</span>
          <span v-if="produit.badges?.vegetarien" class="badge badge-vegetarien">Vegetarien</span>
          <span v-if="produit.badges?.vegan" class="badge badge-vegan">Vegan</span>
        </div>
      </div>

      <div class="fiche-info-section">
        <div class="fiche-category">{{ produit.categorie }}</div>
        <h1 class="fiche-titre">{{ produit.nom }}</h1>
        
        <div class="fiche-prix-section">
          <div class="fiche-prix">
            <span v-if="hasPromotion(produit)" class="prix-original">{{ calculatePrixTTCOriginal(produit).toFixed(2) }} €</span>
            <span class="prix-montant">{{ calculatePrixTTC(produit).toFixed(2) }} €</span>
            <span class="prix-unite">/ {{ produit.unite }}</span>
            <span v-if="hasPromotion(produit)" class="promotion-label">{{ produit.promotionNom }}</span>
          </div>
          <div class="fiche-stock">
            <span v-if="produit.stock <= 5" class="stock-warning">
              Plus que {{ produit.stock }} disponible(s)
            </span>
            <span v-else class="stock-ok">
              En stock ({{ produit.stock }} disponible(s))
            </span>
          </div>
        </div>

        <div class="fiche-description">
          <h3>Description</h3>
          <p>{{ produit.description }}</p>
        </div>

        <div class="fiche-details">
          <div class="detail-item">
            <span class="detail-label">Producteur:</span>
            <span class="detail-value">{{ produit.producteur }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Origine:</span>
            <span class="detail-value">{{ produit.origine }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Unite:</span>
            <span class="detail-value">{{ produit.unite }}</span>
          </div>
          <div v-if="produit.datePeremption" class="detail-item">
            <span class="detail-label">Peremption:</span>
            <span class="detail-value">{{ formatDate(produit.datePeremption) }}</span>
          </div>
        </div>

        <div class="fiche-actions">
          <div class="quantite-selector">
            <label>Quantité:</label>
            <div class="quantite-controls">
              <button @click="quantite = Math.max(1, quantite - 1)" :disabled="quantite <= 1">−</button>
              <input 
                type="number" 
                v-model.number="quantite" 
                @input="quantite = Math.max(1, Math.min(produit.stock, quantite))"
                min="1"
                :max="produit.stock"
              />
              <button @click="quantite = Math.min(produit.stock, quantite + 1)" :disabled="quantite >= produit.stock">+</button>
            </div>
          </div>

          <div class="total-price">
            <span class="total-label">Total:</span>
            <span class="total-montant">{{ (calculatePrixTTC(produit) * quantite).toFixed(2) }} €</span>
          </div>
        </div>

        <div class="fiche-buttons">
          <button :class="['btn', 'btn-primary', { 'btn-success': added }]" @click="handleAddToCart">
            {{ added ? 'Ajoute au panier' : 'Ajouter au panier' }}
          </button>
          <button class="btn btn-secondary" @click="handleBuyNow">Commander maintenant</button>
        </div>
      </div>
    </div>

    <div v-if="produitsLies.length > 0" class="produits-lies">
      <h2>Produits similaires</h2>
      <div class="produits-grid">
        <router-link v-for="p in produitsLies" :key="p.id" :to="`/produit/${p.id}`" class="produit-card-mini">
          <img class="produit-image-mini" :src="getProductImageSrc(p)" :alt="`Image de ${p.nom}`" @error="onMiniError($event, p)" />
          <div class="produit-info-mini">
            <h4>{{ p.nom }}</h4>
            <span class="prix-mini">{{ calculatePrixTTC(p).toFixed(2) }} € / {{ p.unite }}</span>
          </div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// ...existing code...
import { usePanierStore } from '../../stores/panierStore'
import { categoriesApi, produitsApi, getApiErrorMessage } from '../../services/api.js'
import { getProductImageSrc, productPlaceholder, resolveProductImageSrc } from '../../utils/imageHelper.js'

const route = useRoute()
const router = useRouter()
const panierStore = usePanierStore()

const produitsData = ref([])
const apiError = ref('')
const quantite = ref(1)
const added = ref(false)
const currentImage = ref(null)
const currentImageType = ref('emoji')

const produit = computed(() => produitsData.value.find(p => p.id === parseInt(route.params.id)))

const productPhotos = computed(() => {
  if (!produit.value) return []
  if (Array.isArray(produit.value.photos)) {
    return produit.value.photos
  }
  return []
})

const hasPhotos = computed(() => {
  return productPhotos.value.length > 0
})

onMounted(async () => {
  apiError.value = ''
  try {
    const categories = await categoriesApi.listActive()
    const produitsActifs = await produitsApi.listActive(categories)
    const currentProduit = produitsActifs.find(p => p.id === Number(route.params.id))
    if (!currentProduit) {
      throw new Error('Produit indisponible.')
    }
    produitsData.value = [
      currentProduit,
      ...produitsActifs.filter(p => p.id !== currentProduit.id)
    ]

    if (produit.value) {
      currentImage.value = produit.value.image
      currentImageType.value = 'emoji'
    }
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
    produitsData.value = []
  }
})

const selectImage = (type, imageUrl) => {
  currentImageType.value = type
  currentImage.value = imageUrl
}

function getMainImageSrc() {
  if (isImageSource(currentImage.value)) {
    return resolveProductImageSrc(currentImage.value)
  }
  return getProductImageSrc(produit.value)
}

function isImageSource(value) {
  return typeof value === 'string' &&
    (value.startsWith('/') || value.startsWith('http') || value.startsWith('data:image/'))
}

function onImgError(ev) {
  ev.target.src = productPlaceholder(produit.value?.nom, produit.value?.categorie)
}

function onMiniError(ev, p) {
  ev.target.src = productPlaceholder(p?.nom, p?.categorie)
}

const calculatePrixTTC = (produit) => {
  if (!produit) return 0
  return getPrixEffectifHT(produit) * (1 + (produit.tva || produit.tauxTVA || 6) / 100)
}

const calculatePrixTTCOriginal = (produit) => {
  if (!produit) return 0
  return Number(produit.prixAvantPromotion ?? produit.prixOriginal ?? produit.prixUnitaire ?? produit.prix ?? 0) * (1 + (produit.tva || produit.tauxTVA || 6) / 100)
}

const hasPromotion = (produit) => {
  return Boolean(produit?.promotionActive && Number(produit?.reductionMontant ?? 0) > 0)
}

function getPrixEffectifHT(produit) {
  return Number(produit.prixEffectif ?? produit.prixPromotionnel ?? produit.prixUnitaire ?? produit.prix ?? 0)
}

const produitsLies = computed(() => {
  if (!produit.value) return []
  return produitsData.value
    .filter(p => p.categorieId === produit.value.categorieId && p.id !== produit.value.id && p.stock > 0)
    .slice(0, 3)
})

const formatDate = (dateString) => new Date(dateString).toLocaleDateString('fr-FR')

const handleAddToCart = () => {
  panierStore.ajouterAuPanier(produit.value, quantite.value)
  added.value = true
  setTimeout(() => { added.value = false }, 2000)
}

const handleBuyNow = () => {
  panierStore.ajouterAuPanier(produit.value, quantite.value)
  router.push('/panier')
}
</script>

<style scoped>
/* Layout */
.fiche-produit-page {
  max-width: 1100px;
  margin: 0 auto;
}

.fiche-produit {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 2rem;
  align-items: start;
}

@media (max-width: 900px) {
  .fiche-produit {
    grid-template-columns: 1fr;
  }
}

/* Main image */
.image-display {
  width: 100%;
  aspect-ratio: 16/9;
  object-fit: cover;
  border-radius: 16px;
  background: #eef2f7;
  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
}

.fiche-image-gallery {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.gallery-thumbnail {
  width: 80px;
  height: 80px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  transition: all 0.3s ease;
}

.gallery-thumbnail:hover {
  border-color: #007bff;
  transform: scale(1.05);
}

.gallery-thumbnail.active {
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.25);
}

.emoji-thumb {
  font-size: 2.5rem;
}

.photo-thumb {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.fiche-badges {
  position: static;
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.badge {
  display: inline-block;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 600;
}

.badge-bio {
  background-color: #d4edda;
  color: #155724;
}

.badge-local {
  background-color: #d1ecf1;
  color: #0c5460;
}

.badge-vegan {
  background-color: #d4edda;
  color: #155724;
}

.badge-vegetarien {
  background-color: #fff3cd;
  color: #856404;
}

/* Right column */
.fiche-category {
  color: #6b7280;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.fiche-titre {
  margin: 0 0 1rem;
  font-size: 2rem;
}

.fiche-prix-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(0,0,0,0.04);
}

.fiche-prix .prix-montant {
  font-size: 2rem;
  font-weight: 800;
  color: #111827;
}

.fiche-prix .prix-original {
  display: block;
  margin-bottom: 0.15rem;
  color: #8a8f98;
  font-size: 0.95rem;
  text-decoration: line-through;
}

.fiche-prix .promotion-label {
  display: block;
  width: fit-content;
  margin-top: 0.45rem;
  padding: 0.22rem 0.55rem;
  border-radius: 999px;
  background: rgba(184, 121, 63, 0.12);
  color: #8a5a2b;
  font-size: 0.78rem;
  font-weight: 700;
}

.fiche-prix .prix-unite {
  color: #6b7280;
  margin-left: 0.25rem;
}
.fiche-prix .prix-info {
  display: inline-block;
  margin-left: 0.5rem;
  color: #9ca3af;
}

.fiche-stock .stock-ok { color: #16a34a; }
.fiche-stock .stock-warning { color: #b45309; }

.fiche-description {
  margin-top: 1.25rem;
  padding: 1rem;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.fiche-details {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem 1rem;
  margin-top: 1rem;
}
.detail-item {
  display: flex;
  gap: 0.5rem;
}
.detail-label { color: #6b7280; }

.fiche-actions {
  margin-top: 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}
.quantite-controls {
  display: inline-flex;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  overflow: hidden;
}
.quantite-controls button {
  width: 38px;
  height: 38px;
  border: none;
  background: #f3f4f6;
  font-size: 18px;
}
.quantite-controls input {
  width: 60px;
  text-align: center;
  border: none;
  outline: none;
}
.total-price .total-montant {
  font-weight: 700;
}

.fiche-buttons {
  margin-top: 1rem;
  display: flex;
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .fiche-prix-section,
  .fiche-actions,
  .fiche-buttons {
    flex-direction: column;
    align-items: stretch;
  }

  .fiche-details {
    grid-template-columns: 1fr;
  }
}

/* Similar products */
.produits-lies { margin-top: 2rem; }
.produits-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
}
.produit-card-mini {
  display: block;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  transition: transform .2s ease, box-shadow .2s ease;
}
.produit-card-mini:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(0,0,0,0.06); }
.produit-image-mini {
  width: 100%;
  aspect-ratio: 4/3;
  object-fit: cover;
  display: block;
  background: #f2f4f7;
}
.produit-info-mini { padding: .75rem 1rem; }
.prix-mini { color: #6b7280; }
</style>
