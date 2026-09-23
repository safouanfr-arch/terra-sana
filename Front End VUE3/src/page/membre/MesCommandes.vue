<template>
  <div class="mes-commandes-page">
    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div v-if="showSuccess && newCommande" class="page-container">
      <div class="success-message">
        <div class="success-icon">OK</div>
        <h2>Commande validée avec succès</h2>
        <p>Votre commande <strong>{{ newCommande.numero }}</strong> a été enregistrée côté backend.</p>

        <div class="success-details">
          <h3>Details de retrait</h3>
          <div class="detail-card">
            <p><strong>Point de collecte:</strong> {{ newCommande.pointCollecte?.nom }}</p>
            <p><strong>Adresse:</strong> {{ newCommande.pointCollecte?.adresse }}</p>
            <p><strong>Date:</strong> {{ formatDateLong(newCommande.dateRetrait) }}</p>
            <p><strong>Créneau:</strong> {{ newCommande.creneauRetrait?.horaire }}</p>
            <p v-if="newCommande.codeRetrait"><strong>Code retrait:</strong> {{ newCommande.codeRetrait }}</p>
          </div>
        </div>

        <div class="actions">
          <button class="btn btn-primary" @click="showSuccess = false">
            Voir toutes mes commandes
          </button>
          <router-link to="/catalogue" class="btn btn-secondary">
            Continuer mes achats
          </router-link>
        </div>
      </div>
    </div>

    <div v-else>
      <div class="commandes-header">
        <div>
          <h1>Mes commandes</h1>
          <p>Consultez l'historique de vos commandes</p>
        </div>
        <router-link to="/catalogue" class="btn btn-primary">
          Nouvelle commande
        </router-link>
      </div>

      <div v-if="commandes.length === 0" class="empty-state">
        <div class="empty-icon">0</div>
        <h3>Aucune commande</h3>
        <p>Vous n'avez pas encore passé de commande.</p>
        <router-link to="/catalogue" class="btn btn-primary">
          Parcourir le catalogue
        </router-link>
      </div>

      <div v-else class="commandes-list">
        <div v-for="commande in commandes" :key="commande.id" class="commande-card">
          <div class="commande-header">
            <div>
              <h3>Commande {{ commande.numero }}</h3>
              <span class="commande-date">
                Passee le {{ new Date(commande.date).toLocaleDateString('fr-FR') }}
              </span>
            </div>
            <span :class="['status-badge', getStatutBadge(commande.statut).class]">
              {{ getStatutBadge(commande.statut).label }}
            </span>
          </div>

          <div class="commande-body">
            <div class="commande-section">
              <h4>Point de retrait</h4>
              <p><strong>{{ commande.pointCollecte?.nom }}</strong></p>
              <p>{{ commande.pointCollecte?.adresse }}</p>
              <p class="retrait-info">
                {{ formatDateLong(commande.dateRetrait) }} - {{ commande.creneauRetrait?.horaire }}
              </p>
            </div>

            <div class="commande-section">
              <h4>Articles ({{ commande.produits.length }})</h4>
              <div class="articles-list">
                <div v-for="item in commande.produits" :key="`${commande.id}-${item.ligneId || item.id}`" class="article-item">
                  <span>
                    {{ item.nom }}
                    <small v-if="item.promotionActive" class="promo-inline">{{ item.promotionNom }}</small>
                  </span>
                  <span>x{{ item.quantite }}</span>
                  <span>{{ item.totalTTC.toFixed(2) }} €</span>
                </div>
              </div>
            </div>

            <div v-if="commande.commentaire" class="commande-section">
              <h4>Commentaire</h4>
              <p>{{ commande.commentaire }}</p>
            </div>
          </div>

          <div class="commande-footer">
            <div class="commande-total">
              <div class="total-ligne total-ttc">
                <span>Total à payer:</span>
                <span class="total-amount">{{ commande.totalTVAC.toFixed(2) }} €</span>
              </div>
              <div v-if="commande.codeRetrait" class="total-ligne">
                <span>Code retrait:</span>
                <span>{{ commande.codeRetrait }}</span>
              </div>
            </div>

            <div v-if="canCancel(commande)" class="commande-actions">
              <button class="btn btn-danger" @click="handleCancelCommande(commande)">
                Annuler la commande
              </button>
              <p class="cancel-info">Annulation possible tant que le retrait n'est pas passé</p>
            </div>

            <div v-else-if="commande.statut === 'CANCELLED'" class="alert alert-warning">
              <span class="alert-icon">!</span>
              <span>Commande annulée</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/authStore.js'
import { commandesApi, getApiErrorMessage, handleApiAccessError } from '../../services/api.js'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const commandes = ref([])
const showSuccess = ref(false)
const newCommande = ref(null)
const apiError = ref('')

onMounted(async () => {
  await loadCommandes()

  const createdId = route.query.created
  if (createdId) {
    newCommande.value = commandes.value.find(commande => String(commande.id) === String(createdId)) || null
    showSuccess.value = Boolean(newCommande.value)
    router.replace({ path: route.path })
  }
})

async function loadCommandes() {
  apiError.value = ''
  try {
    commandes.value = await commandesApi.listMy()
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    apiError.value = getApiErrorMessage(error)
    commandes.value = []
  }
}

function canCancel(commande) {
  if (!commande?.dateRetrait) return false
  return !['CANCELLED', 'DISTRIBUTED'].includes(commande.statut) && new Date(commande.dateRetrait) > new Date()
}

async function handleCancelCommande(commande) {
  if (!confirm(`Annuler la commande ${commande.numero} ? Le stock sera remis automatiquement.`)) {
    return
  }

  apiError.value = ''
  try {
    await commandesApi.cancel(commande.id)
    await loadCommandes()
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    apiError.value = getApiErrorMessage(error)
  }
}

function getStatutBadge(statut) {
  const statuts = {
    CONFIRMED: { label: 'Confirmee', class: 'success' },
    IN_PREP: { label: 'En preparation', class: 'info' },
    READY: { label: 'Prete', class: 'primary' },
    DISTRIBUTED: { label: 'Distribuee', class: 'success' },
    CANCELLED: { label: 'Annulee', class: 'danger' }
  }
  return statuts[statut] || statuts.CONFIRMED
}

function formatDateLong(value) {
  return new Date(value).toLocaleDateString('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  })
}
</script>

<style scoped>
.promo-inline {
  display: block;
  margin-top: 0.15rem;
  color: #8a5a2b;
  font-size: 0.75rem;
  font-weight: 700;
}
</style>

<style scoped>
/* Les styles seront hérités de App.css */
</style>
