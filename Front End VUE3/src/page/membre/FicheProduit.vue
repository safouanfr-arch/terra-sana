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
        <div :class="['fiche-image-main', getProductMediaClass(produit)]">
          <button
            type="button"
            class="image-zoom-trigger"
            aria-label="Agrandir l'image du produit"
            @click="openLightbox"
          >
            <img class="image-display" :src="getMainImageSrc()" :alt="`Image de ${produit?.nom}`" @error="onImgError($event)" />
            <span class="image-zoom-hint">Agrandir</span>
          </button>
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
        
      </div>

      <div class="fiche-info-section">
        <div class="fiche-category">{{ produit.categorie }}</div>
        <h1 class="fiche-titre">{{ produit.nom }}</h1>

        <div class="fiche-badges">
          <span v-if="produit.badges?.local" class="badge badge-local">Local</span>
          <span v-if="produit.badges?.bio" class="badge badge-bio">Bio</span>
          <span v-if="produit.badges?.vegetarien" class="badge badge-vegetarien">Végétarien</span>
          <span v-if="produit.badges?.vegan" class="badge badge-vegan">Vegan</span>
        </div>

        <p class="fiche-description">{{ produit.description }}</p>

        <dl class="fiche-details">
          <div v-if="produit.conditionnement" class="detail-item">
            <dt>Conditionnement</dt>
            <dd>{{ produit.conditionnement }}</dd>
          </div>
          <div v-if="produit.origine" class="detail-item">
            <dt>Origine</dt>
            <dd>{{ produit.origine }}</dd>
          </div>
          <div v-if="produit.producteur" class="detail-item">
            <dt>Producteur</dt>
            <dd>{{ produit.producteur }}</dd>
          </div>
          <div v-if="produit.datePeremption" class="detail-item">
            <dt>Péremption</dt>
            <dd>{{ formatDate(produit.datePeremption) }}</dd>
          </div>
        </dl>

        <section class="purchase-panel" aria-label="Ajouter le produit au panier">
          <div class="purchase-heading">
          <div class="fiche-prix">
            <span v-if="hasPromotion(produit)" class="prix-original">{{ calculatePrixTTCOriginal(produit).toFixed(2) }} €</span>
              <div class="prix-ligne">
                <span class="prix-montant">{{ calculatePrixTTC(produit).toFixed(2) }} €</span>
                <span class="prix-unite">/ {{ getPriceUnitLabel(produit) }}</span>
              </div>
            <span v-if="hasPromotion(produit)" class="promotion-label">{{ produit.promotionNom }}</span>
          </div>
            <span :class="['stock-status', produit.stock <= 5 ? 'stock-warning' : 'stock-ok']">
              {{ getStockLabel(produit) }}
            </span>
          </div>

          <div class="purchase-controls">
          <div class="quantite-selector">
              <label for="product-quantity">Quantité</label>
            <div class="quantite-controls">
                <button type="button" aria-label="Diminuer la quantité" @click="quantite = Math.max(1, quantite - 1)" :disabled="quantite <= 1">−</button>
              <input
                  id="product-quantity"
                  v-model.number="quantite"
                type="number"
                @input="quantite = Math.max(1, Math.min(produit.stock, quantite))"
                min="1"
                :max="produit.stock"
              />
                <button type="button" aria-label="Augmenter la quantité" @click="quantite = Math.min(produit.stock, quantite + 1)" :disabled="quantite >= produit.stock">+</button>
            </div>
          </div>

          <div class="total-price">
              <span class="total-label">Total TTC</span>
            <span class="total-montant">{{ (calculatePrixTTC(produit) * quantite).toFixed(2) }} €</span>
          </div>
        </div>

        <div class="fiche-buttons">
            <button type="button" :class="['btn', 'btn-primary', { 'btn-success': added }]" @click="handleAddToCart">
            {{ added ? 'Ajouté au panier' : 'Ajouter au panier' }}
          </button>
            <button type="button" class="btn btn-secondary" @click="handleBuyNow">Commander maintenant</button>
        </div>
        </section>
      </div>
    </div>

    <div v-if="produitsLies.length > 0" class="produits-lies">
      <h2>Produits similaires</h2>
      <div class="produits-grid">
        <router-link v-for="p in produitsLies" :key="p.id" :to="`/produit/${p.id}`" class="produit-card-mini">
          <div :class="['produit-media-mini', getProductMediaClass(p)]">
            <img class="produit-image-mini" :src="getProductImageSrc(p)" :alt="`Image de ${p.nom}`" @error="onMiniError($event, p)" />
          </div>
          <div class="produit-info-mini">
            <h4>{{ p.nom }}</h4>
            <span class="prix-mini">{{ calculatePrixTTC(p).toFixed(2) }} € / {{ getPriceUnitLabel(p) }}</span>
          </div>
        </router-link>
      </div>
    </div>
  </div>

  <Teleport to="body">
    <div
      v-if="lightboxOpen"
      class="product-lightbox"
      role="dialog"
      aria-modal="true"
      :aria-label="`Image agrandie de ${produit?.nom}`"
      @click.self="closeLightbox"
    >
      <button
        type="button"
        class="lightbox-close"
        aria-label="Fermer l'image agrandie"
        @click="closeLightbox"
      >
        &times;
      </button>
      <img
        class="lightbox-image"
        :src="getMainImageSrc()"
        :alt="`Image agrandie de ${produit?.nom}`"
        @error="onImgError($event)"
      />
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted, watch } from 'vue'
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
const lightboxOpen = ref(false)

const produit = computed(() => produitsData.value.find(p => p.id === parseInt(route.params.id)))

watch(produit, (nouveauProduit, ancienProduit) => {
  if (nouveauProduit?.id === ancienProduit?.id) return

  currentImage.value = nouveauProduit?.image ?? null
  currentImageType.value = 'emoji'
  quantite.value = 1
  added.value = false
  lightboxOpen.value = false
})

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
  window.addEventListener('keydown', handleKeydown)
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

  } catch (error) {
    apiError.value = getApiErrorMessage(error)
    produitsData.value = []
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
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

function openLightbox() {
  lightboxOpen.value = true
}

function closeLightbox() {
  lightboxOpen.value = false
}

function handleKeydown(event) {
  if (event.key === 'Escape' && lightboxOpen.value) {
    closeLightbox()
  }
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

function getProductMediaClass(produit) {
  const image = String(produit?.image || '').toLowerCase()
  if (image.includes('carottes-bio')) return 'produit-media--carottes'
  if (image.includes('jus-de-pomme')) return 'produit-media--jus'
  if (image.includes('pommes-de-terre')) return 'produit-media--pommes-de-terre'
  if (image.includes('oeufs')) return 'produit-media--oeufs'
  return 'produit-media--standard'
}

function getPriceUnitLabel(produit) {
  if (produit?.unite === 'kg') return 'kg'
  return produit?.conditionnement || produit?.unite || 'unité'
}

function getStockLabel(produit) {
  const stock = Number(produit?.stock ?? 0)
  const formattedStock = new Intl.NumberFormat('fr-BE', { maximumFractionDigits: 2 }).format(stock)
  const units = {
    kg: 'kg',
    litre: 'litres',
    barquette: 'boîtes',
    pièce: 'unités'
  }
  const unit = units[produit?.unite] || 'unités'
  return stock <= 5
    ? `Plus que ${formattedStock} ${unit}`
    : `En stock : ${formattedStock} ${unit}`
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
.image-zoom-trigger {
  position: relative;
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: zoom-in;
}

.image-display {
  width: 100%;
  aspect-ratio: 16/9;
  object-fit: cover;
  border-radius: 16px;
  background: #eef2f7;
  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
  display: block;
}

.image-zoom-hint {
  position: absolute;
  right: 0.75rem;
  bottom: 0.75rem;
  padding: 0.35rem 0.6rem;
  border-radius: 6px;
  background: rgba(24, 35, 31, 0.86);
  color: white;
  font-size: 0.8rem;
  font-weight: 700;
}

.product-lightbox {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
  padding: 3.5rem 1.25rem 1.25rem;
  background: rgba(12, 18, 15, 0.9);
}

.lightbox-image {
  display: block;
  max-width: min(1100px, 94vw);
  max-height: 88vh;
  object-fit: contain;
}

.lightbox-close {
  position: absolute;
  top: 1rem;
  right: 1rem;
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 50%;
  background: rgba(24, 35, 31, 0.75);
  color: white;
  cursor: pointer;
  font-size: 2rem;
  line-height: 1;
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

  .product-lightbox {
    padding: 4rem 0.75rem 0.75rem;
  }

  .lightbox-image {
    max-width: 100%;
    max-height: 82vh;
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

/* Product detail refresh */
.fiche-produit-page {
  max-width: 1180px;
  padding: 0 1.25rem 3rem;
  box-sizing: border-box;
}

.fiche-header {
  padding: 1rem 0 1.5rem;
  margin-bottom: 0;
}

.fiche-produit {
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: 3rem;
  max-width: none;
  margin: 0;
  padding: 0;
}

.fiche-image-main {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border: 1px solid rgba(61, 73, 68, 0.14);
  border-radius: 8px;
  background: #ece7de;
}

.image-zoom-trigger {
  height: 100%;
}

.image-display {
  height: 100%;
  aspect-ratio: auto;
  object-fit: cover;
  object-position: center;
  border-radius: 0;
  box-shadow: none;
}

.produit-media--carottes,
.produit-media-mini.produit-media--carottes {
  background: #1f382f;
}

.produit-media--jus,
.produit-media-mini.produit-media--jus {
  background: #fff;
}

.fiche-image-main.produit-media--carottes,
.fiche-image-main.produit-media--jus {
  aspect-ratio: 1;
}

.produit-media--pommes-de-terre .image-display,
.produit-media-mini.produit-media--pommes-de-terre .produit-image-mini {
  object-position: center 52%;
}

.produit-media--oeufs .image-display,
.produit-media-mini.produit-media--oeufs .produit-image-mini {
  object-position: center 58%;
}

.fiche-info-section {
  min-width: 0;
}

.fiche-category {
  margin-bottom: 0.45rem;
  color: #607168;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.fiche-titre {
  margin: 0;
  font-size: 2.35rem;
  line-height: 1.08;
  text-wrap: balance;
}

.fiche-badges {
  position: static;
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  margin: 1rem 0 0;
}

.fiche-description {
  margin: 1.4rem 0 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #4f5e57;
  font-size: 1rem;
  line-height: 1.7;
}

.fiche-details {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem 1.5rem;
  margin: 1.5rem 0 0;
  padding: 1.2rem 0;
  border-top: 1px solid rgba(61, 73, 68, 0.14);
  border-bottom: 1px solid rgba(61, 73, 68, 0.14);
}

.detail-item {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.detail-item dt {
  color: #738078;
  font-size: 0.76rem;
  font-weight: 700;
  text-transform: uppercase;
}

.detail-item dd {
  margin: 0;
  color: #26352e;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.purchase-panel {
  margin-top: 1.5rem;
  padding: 1.25rem;
  border: 1px solid rgba(61, 73, 68, 0.16);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(31, 49, 41, 0.06);
}

.purchase-heading,
.purchase-controls {
  display: flex;
  justify-content: space-between;
  gap: 1.25rem;
}

.purchase-heading {
  align-items: flex-start;
}

.purchase-controls {
  align-items: end;
  margin-top: 1.25rem;
  padding-top: 1.25rem;
  border-top: 1px solid rgba(61, 73, 68, 0.12);
}

.prix-ligne {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.fiche-prix .prix-montant {
  color: #213a32;
  font-size: 2rem;
}

.fiche-prix .prix-unite {
  margin-left: 0;
  color: #65746c;
  font-size: 0.92rem;
}

.stock-status {
  padding-top: 0.45rem;
  font-size: 0.86rem;
  font-weight: 600;
  text-align: right;
}

.quantite-selector,
.total-price {
  display: grid;
  gap: 0.45rem;
}

.quantite-selector label,
.total-label {
  color: #68766f;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

.quantite-controls {
  height: 42px;
  border-color: rgba(61, 73, 68, 0.2);
  border-radius: 6px;
}

.quantite-controls button {
  width: 42px;
  height: 42px;
  cursor: pointer;
}

.quantite-controls button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.quantite-controls input {
  width: 54px;
}

.total-price {
  justify-items: end;
}

.total-price .total-montant {
  color: #213a32;
  font-size: 1.25rem;
}

.fiche-buttons {
  margin-top: 1.25rem;
}

.fiche-buttons .btn {
  flex: 1;
  min-height: 46px;
}

.produits-lies {
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(61, 73, 68, 0.14);
}

.produit-card-mini {
  border-radius: 8px;
}

.produit-media-mini {
  aspect-ratio: 1;
  overflow: hidden;
  background: #ece7de;
}

.produit-image-mini {
  height: 100%;
  aspect-ratio: auto;
  object-position: center;
}

.produit-info-mini h4 {
  margin: 0 0 0.35rem;
}

@media (max-width: 900px) {
  .fiche-produit {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .fiche-image-section {
    max-width: 720px;
  }
}

@media (max-width: 640px) {
  .fiche-produit-page {
    padding-inline: 1rem;
  }

  .fiche-titre {
    font-size: 2rem;
  }

  .fiche-details {
    grid-template-columns: 1fr;
  }

  .purchase-heading,
  .purchase-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .stock-status,
  .total-price {
    justify-items: start;
    text-align: left;
  }
}
</style>
