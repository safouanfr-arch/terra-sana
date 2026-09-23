<template>
  <div class="commande-page">
    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div class="commande-header">
      <h1>Finaliser ma commande</h1>
      <div class="steps-indicator">
        <div :class="['step', { active: step >= 1, completed: step > 1 }]">
          <span class="step-number">1</span>
          <span class="step-label">Point de collecte</span>
        </div>
        <div :class="['step', { active: step >= 2, completed: step > 2 }]">
          <span class="step-number">2</span>
          <span class="step-label">Créneau</span>
        </div>
        <div :class="['step', { active: step >= 3 }]">
          <span class="step-number">3</span>
          <span class="step-label">Confirmation</span>
        </div>
      </div>
    </div>

    <div class="commande-container">
      <div class="commande-content">
        <div v-if="step === 1" class="step-content">
          <h2>Choisissez votre point de collecte</h2>
          <div class="points-list">
            <div
              v-for="point in pointsCollecte"
              :key="point.id"
              :class="['point-card', 'clickable', { selected: selectedPoint?.id === point.id }]"
              @click="handleSelectPoint(point)"
            >
              <div class="point-header">
                <h3>{{ point.nom }}</h3>
              </div>
              <div class="point-details">
                <p><strong>Adresse:</strong> {{ point.adresse }}</p>
                <p><strong>Creneaux:</strong> {{ formatCreneaux(point.creneaux) }}</p>
              </div>
              <button class="btn btn-secondary" @click.stop="handleSelectPoint(point)">Choisir ce point</button>
            </div>
          </div>
        </div>

        <div v-if="step === 2 && selectedPoint" class="step-content">
          <div class="selected-info">
            <h3>Point de collecte sélectionné:</h3>
            <p><strong>{{ selectedPoint.nom }}</strong> - {{ selectedPoint.adresse }}</p>
            <button class="btn-text" @click="step = 1">Changer de point</button>
          </div>
          <h2>Choisissez votre créneau de retrait</h2>
          <p class="help-text">Les creneaux affiches sont actifs, prevus au moins 24 heures a l'avance et limites a J+8.</p>
          <div class="creneaux-grid">
            <div v-for="date in availableDatesWithCreneaux" :key="date.toISOString()" class="date-group">
              <h3 class="date-titre">{{ formatDateLong(date) }}</h3>
              <div class="creneaux-list">
                <button
                  v-for="(creneau, index) in getCreneauxForDate(date)"
                  :key="index"
                  :class="['creneau-btn', { selected: selectedDate === date.toISOString() && selectedCreneau?.horaire === creneau.horaire }]"
                  @click="handleSelectCreneau(date.toISOString(), creneau)"
                >
                  <span class="creneau-horaire">{{ creneau.horaire }}</span>
                </button>
              </div>
            </div>
          </div>
          <p v-if="availableDatesWithCreneaux.length === 0" class="no-creneau-global">Aucun creneau disponible dans la limite J+8</p>
        </div>

        <div v-if="step === 3 && selectedPoint && selectedCreneau" class="step-content">
          <h2>Confirmer votre commande</h2>
          <div class="confirmation-details">
            <div class="detail-section">
              <h3>Informations de retrait</h3>
              <div class="detail-card">
                <p><strong>Point:</strong> {{ selectedPoint.nom }}</p>
                <p><strong>Adresse:</strong> {{ selectedPoint.adresse }}</p>
                <p><strong>Date:</strong> {{ formatDateLong(new Date(selectedDate)) }}</p>
                <p><strong>Créneau:</strong> {{ selectedCreneau.horaire }}</p>
              </div>
              <button class="btn-text" @click="step = 2">Modifier</button>
            </div>

            <div class="detail-section">
              <h3>Vos articles ({{ panier.length }})</h3>
              <div class="articles-resume">
                <div v-for="item in panier" :key="item.id" class="article-ligne">
                  <span>
                    {{ item.nom }}
                    <small v-if="hasPromotion(item)" class="promo-inline">{{ item.promotionNom }}</small>
                  </span>
                  <span>x{{ item.quantite }}</span>
                  <span>{{ calculateLineTotal(item).toFixed(2) }} €</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h3>Commentaire (optionnel)</h3>
              <textarea v-model="commentaire" placeholder="Commentaire..." rows="3" />
            </div>
          </div>

          <div class="alert alert-info" style="margin-top: 1rem;">
            <span class="alert-icon">i</span>
            <div>
              <p><strong>Rappel important:</strong></p>
              <ul>
                <li><strong>Paiement sur place au point de collecte</strong> en carte ou en cash.</li>
                <li><u>Aucun paiement ne se fait via le site</u>.</li>
              </ul>
            </div>
          </div>

          <div class="confirmation-actions">
            <button class="btn btn-primary btn-large" @click="handleConfirmCommande">
              Confirmer et valider la commande
            </button>
          </div>
        </div>
      </div>

      <div class="commande-resume">
        <div class="resume-card">
          <h2>Récapitulatif</h2>
          <div class="resume-items">
            <h4>Articles ({{ panier.length }})</h4>
            <div v-for="item in panier.slice(0, 3)" :key="item.id" class="resume-item">
              <span>{{ item.nom }} x{{ item.quantite }}</span>
            </div>
            <p v-if="panier.length > 3" class="more-items">... et {{ panier.length - 3 }} autre(s)</p>
          </div>
          <div class="resume-ligne resume-total">
            <span><strong>Total à payer</strong></span>
            <span class="total-amount"><strong>{{ totaux.totalTVAC.toFixed(2) }} €</strong></span>
          </div>
          <div v-if="selectedPoint" class="resume-info-box">
            <p><strong>{{ selectedPoint.nom }}</strong></p>
            <p v-if="selectedDate && selectedCreneau">{{ formatDateShort(new Date(selectedDate)) }} a {{ selectedCreneau.horaire }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePanierStore } from '../../stores/panierStore.js'
import { useAuthStore } from '../../stores/authStore.js'
import {
  pointsCollecteApi,
  commandesApi,
  produitsApi,
  getApiErrorMessage,
  handleApiAccessError
} from '../../services/api.js'

function formatCreneaux(creneaux = []) {
  return (Array.isArray(creneaux) ? creneaux : [])
    .map(creneau => creneau.horaire || `${creneau.debut || ''}-${creneau.fin || ''}`.trim())
    .filter(Boolean)
    .join(', ')
}

const router = useRouter()
const authStore = useAuthStore()
const panierStore = usePanierStore()

const step = ref(1)
const selectedPoint = ref(null)
const selectedCreneau = ref(null)
const selectedDate = ref('')
const commentaire = ref('')
const pointsCollecte = ref([])
const apiError = ref('')

const panier = computed(() => panierStore.panier)

onMounted(async () => {
  if (panier.value.length === 0) {
    router.push('/panier')
    return
  }

  apiError.value = ''
  try {
    pointsCollecte.value = await pointsCollecteApi.listActive()
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
    pointsCollecte.value = []
  }
})

function calculateLineTotal(item) {
  const prixHT = prixEffectifHT(item)
  const tauxTVA = Number(item.tauxTVA ?? item.tauxTva ?? item.tva ?? 6) / 100
  return prixHT * (1 + tauxTVA) * item.quantite
}

function hasPromotion(item) {
  return Boolean(item?.promotionActive && Number(item?.reductionMontant ?? 0) > 0)
}

const totaux = computed(() => {
  let totalHT = 0
  let totalTVA = 0

  panier.value.forEach(item => {
    const prixHT = prixEffectifHT(item)
    const tauxTVA = Number(item.tauxTVA ?? item.tauxTva ?? item.tva ?? 6) / 100
    const montantHT = prixHT * item.quantite
    const montantTVA = montantHT * tauxTVA

    totalHT += montantHT
    totalTVA += montantTVA
  })

  return {
    totalHT,
    totalTVA,
    totalTVAC: totalHT + totalTVA
  }
})

const availableDates = computed(() => {
  const dates = []
  const today = new Date()
  for (let i = 1; i <= 8; i++) {
    const date = new Date(today)
    date.setDate(date.getDate() + i)
    dates.push(date)
  }
  return dates
})

const availableDatesWithCreneaux = computed(() => {
  return availableDates.value.filter(date => getCreneauxForDate(date).length > 0)
})

function isDateWithinNext7Days(isoDateStr) {
  if (!isoDateStr) return false
  const d = new Date(isoDateStr)
  const today = new Date()
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1)
  const end = new Date(today.getTime() + 8 * 24 * 60 * 60 * 1000)
  return d >= start && d <= end
}

function getCreneauDateTime(creneau) {
  if (!creneau?.date) return null
  const date = String(creneau.date).slice(0, 10)
  const heureDebut = String(creneau.debut || creneau.heureDebut || '').slice(0, 5)
  if (!date || !heureDebut) return null

  const value = new Date(`${date}T${heureDebut}:00`)
  return Number.isNaN(value.getTime()) ? null : value
}

function isCreneauEligible(creneau) {
  if (!creneau || creneau.actif === false) return false
  const retrait = getCreneauDateTime(creneau)
  if (!retrait) return false

  const now = new Date()
  const minimum = new Date(now.getTime() + 24 * 60 * 60 * 1000)
  const end = new Date(now.getTime() + 8 * 24 * 60 * 60 * 1000)

  return retrait >= minimum && retrait <= end
}

function prixEffectifHT(item) {
  return Number(item.prixEffectif ?? item.prixPromotionnel ?? item.prixUnitaire ?? item.prix ?? 0)
}

const handleSelectPoint = (point) => {
  selectedPoint.value = point
  selectedCreneau.value = null
  selectedDate.value = ''
  step.value = 2
}

const getCreneauxForDate = (date) => {
  if (!selectedPoint.value) return []
  const targetDateStr = date.toISOString().slice(0, 10)

  return (selectedPoint.value.creneaux || [])
    .filter(c => c.date && c.date.slice(0, 10) === targetDateStr)
    .filter(isCreneauEligible)
    .map(c => ({
      ...c,
      horaire: c.horaire || `${c.debut || ''}-${c.fin || ''}`
    }))
}

const handleSelectCreneau = (date, creneau) => {
  selectedDate.value = date
  selectedCreneau.value = creneau
  step.value = 3
}

async function resynchroniserPanierDepuisBackend() {
  const produits = await produitsApi.listActive()
  return panierStore.synchroniserAvecProduits(produits)
}

const handleConfirmCommande = async () => {
  if (!selectedCreneau.value?.id) {
    apiError.value = 'Veuillez sélectionner un créneau valide.'
    step.value = 2
    return
  }

  if (!isDateWithinNext7Days(selectedDate.value) || !isCreneauEligible(selectedCreneau.value)) {
    apiError.value = 'Le creneau doit etre actif, situe au moins 24 heures dans le futur et rester dans la limite J+8.'
    step.value = 2
    return
  }

  apiError.value = ''

  try {
    const createdCommande = await commandesApi.create({
      creneauCollecteId: selectedCreneau.value.id,
      commentaire: commentaire.value,
      produits: panier.value.map(item => ({
        produitId: item.id,
        quantite: item.quantite
      }))
    })

    panierStore.viderPanier()
    router.push({
      path: '/mes-commandes',
      query: { created: String(createdCommande.id) }
    })
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }

    if (error?.response?.status === 409) {
      try {
        const syncResult = await resynchroniserPanierDepuisBackend()
        const baseMessage = getApiErrorMessage(error)

        if (syncResult.panierVide) {
          router.push({
            path: '/panier',
            query: { stockChanged: '1', reason: 'empty' }
          })
          return
        }

        router.push({
          path: '/panier',
          query: {
            stockChanged: '1',
            adjusted: String(syncResult.quantitesAjustees),
            removed: String(syncResult.produitsRetires)
          }
        })
        apiError.value = baseMessage
        return
      } catch (syncError) {
        apiError.value = getApiErrorMessage(syncError)
        return
      }
    }

    apiError.value = getApiErrorMessage(error)
  }
}

const formatDateLong = (date) => date.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })
const formatDateShort = (date) => date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })
</script>

<style scoped>
.commande-header {
  display: grid;
  grid-template-columns: minmax(240px, 0.9fr) minmax(360px, 1.1fr);
  align-items: center;
  gap: 1.6rem;
}

.steps-indicator {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: start;
  gap: 0;
}

.steps-indicator::before {
  content: "";
  position: absolute;
  top: 17px;
  left: 16.66%;
  right: 16.66%;
  height: 2px;
  background: rgba(61, 73, 68, 0.16);
}

.step {
  display: grid;
  justify-items: center;
  gap: 0.55rem;
  min-width: 0;
}

.step-number {
  position: relative;
  z-index: 1;
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  line-height: 1;
  font-size: 0.95rem;
  font-weight: 800;
}

.step-label {
  max-width: 120px;
  min-height: 2.4em;
  text-align: center;
  line-height: 1.25;
}

.article-ligne {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 54px 88px;
  gap: 0.75rem;
  align-items: start;
}

.article-ligne > span:last-child {
  text-align: right;
  font-weight: 700;
}

/* Les styles seront hérités de App.css */
.promo-inline {
  display: block;
  margin-top: 0.2rem;
  color: #8a5a2b;
  font-size: 0.78rem;
  font-weight: 700;
}

@media (max-width: 760px) {
  .commande-header {
    grid-template-columns: 1fr;
  }

  .steps-indicator {
    max-width: 520px;
  }
}

@media (max-width: 520px) {
  .steps-indicator {
    grid-template-columns: 1fr;
    gap: 0.85rem;
  }

  .steps-indicator::before {
    display: none;
  }

  .step {
    grid-template-columns: 34px minmax(0, 1fr);
    justify-items: start;
    align-items: center;
  }

  .step-label {
    max-width: none;
    min-height: 0;
    text-align: left;
  }

  .article-ligne {
    grid-template-columns: 1fr;
    gap: 0.2rem;
  }

  .article-ligne > span:last-child {
    text-align: left;
  }
}
</style>
