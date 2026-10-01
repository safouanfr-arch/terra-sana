<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au tableau de bord</router-link>
        <h1>Gestion des demandes d'adhésion</h1>
        <p>Validation des demandes et envoi de l'email d'activation.</p>
      </div>
      <div class="header-stats">
        <span class="stat-badge warning">
          En attente: <strong>{{ demandesEnAttente.length }}</strong>
        </span>
        <span class="stat-badge">
          Traitees: <strong>{{ demandesTraitees.length }}</strong>
        </span>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div v-if="successMessage" class="alert alert-info">{{ successMessage }}</div>
    <div v-if="loading" class="empty-state">
      <div class="empty-icon">...</div>
      <h3>Chargement des demandes</h3>
    </div>

    <template v-else>
      <div v-if="demandesEnAttente.length === 0" class="empty-state">
        <div class="empty-icon">OK</div>
        <h3>Aucune demande en attente</h3>
        <p>Toutes les demandes ont déjà été traitées.</p>
      </div>

      <div v-else class="demandes-list">
        <h2>Demandes en attente ({{ demandesEnAttente.length }})</h2>
        <div v-for="demande in demandesEnAttente" :key="demande.id" class="demande-card">
          <div class="demande-header">
            <div class="demande-user">
              <h3>{{ demande.prenom }} {{ demande.nom }}</h3>
              <span class="demande-date">{{ formatDate(demande.dateSoumission) }}</span>
            </div>
            <span class="status-badge pending">En attente</span>
          </div>

          <div class="demande-body">
            <div class="demande-info">
              <div class="info-row">
                <span class="info-label">Email:</span>
                <span class="info-value">{{ demande.email }}</span>
              </div>
              <div v-if="demande.telephone" class="info-row">
                <span class="info-label">Téléphone :</span>
                <span class="info-value">{{ demande.telephone }}</span>
              </div>
              <div v-if="demande.adresse || demande.ville" class="info-row">
                <span class="info-label">Adresse:</span>
                <span class="info-value">
                  {{ demande.adresse }}<br v-if="demande.adresse" />
                  {{ demande.codePostal }} {{ demande.ville }}
                </span>
              </div>
              <div class="conditions-trace">
                <span><strong>Conditions acceptées :</strong> {{ formatDateTime(demande.conditionsAccepteesAt) }}</span>
                <span><strong>Version :</strong> {{ demande.conditionsVersion || 'Historique non renseigne' }}</span>
              </div>
            </div>

            <div v-if="demande.justification" class="demande-justification">
              <strong>Justification:</strong>
              <p>{{ demande.justification }}</p>
            </div>
          </div>

          <div class="demande-actions">
            <button class="btn btn-success" @click="openAction(demande, 'valider')">
              Valider
            </button>
            <button class="btn btn-danger" @click="openAction(demande, 'refuser')">
              Refuser
            </button>
          </div>
        </div>
      </div>

      <div v-if="demandesTraitees.length > 0" class="demandes-traitees">
        <h2>Demandes traitées ({{ demandesTraitees.length }})</h2>
        <div class="demandes-list-compact">
          <div v-for="demande in demandesTraitees" :key="demande.id" class="demande-compact">
            <div>
              <div class="demande-identity">
                <span class="demande-name">{{ demande.prenom }} {{ demande.nom }}</span>
                <span class="demande-email">{{ demande.email }}</span>
              </div>
            </div>
            <span :class="['status-badge', demande.statut === 'approuvee' ? 'success' : 'danger']">
              {{ demande.statut === 'approuvee' ? 'Approuvée' : 'Refusée' }}
            </span>
          </div>
        </div>
      </div>
    </template>

    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2>{{ actionType === 'valider' ? 'Valider la demande' : 'Refuser la demande' }}</h2>
          <button class="modal-close" @click="closeModal">x</button>
        </div>

        <div class="modal-body">
          <p>
            Confirmer pour <strong>{{ selectedDemande?.prenom }} {{ selectedDemande?.nom }}</strong> ?
          </p>

          <div v-if="actionType === 'valider'" class="alert alert-info">
            Un compte membre sera créé si nécessaire et le lien d'activation sera envoyé par email.
          </div>

          <div class="form-group">
            <label for="commentaire">
              {{ actionType === 'refuser' ? 'Motif du refus (optionnel)' : 'Commentaire interne (optionnel)' }}
            </label>
            <textarea
              id="commentaire"
              v-model="commentaire"
              rows="3"
              :placeholder="actionType === 'valider' ? 'Note interne...' : 'Raison du refus...'"
            />
          </div>
        </div>

        <div class="modal-actions">
          <button
            :class="['btn', actionType === 'valider' ? 'btn-success' : 'btn-danger']"
            @click="confirmAction"
            :disabled="processing"
          >
            {{ processing ? 'Traitement...' : 'Confirmer' }}
          </button>
          <button class="btn btn-secondary" @click="closeModal">
            Annuler
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { demandesAdhesionApi, getApiErrorMessage, handleApiAccessError, isApiAccessError } from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const demandes = ref([])
const loading = ref(false)
const processing = ref(false)
const apiError = ref('')
const successMessage = ref('')
const selectedDemande = ref(null)
const showModal = ref(false)
const actionType = ref(null)
const commentaire = ref('')

const demandesEnAttente = computed(() =>
  demandes.value.filter(demande => demande.statut === 'en_attente')
)

const demandesTraitees = computed(() =>
  demandes.value.filter(demande => demande.statut !== 'en_attente')
)

onMounted(() => {
  loadDemandes()
})

async function loadDemandes() {
  loading.value = true
  apiError.value = ''
  try {
    demandes.value = await demandesAdhesionApi.list()
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      demandes.value = []
      apiError.value = getApiErrorMessage(error)
    }
  } finally {
    loading.value = false
  }
}

function openAction(demande, action) {
  successMessage.value = ''
  selectedDemande.value = demande
  actionType.value = action
  commentaire.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  selectedDemande.value = null
  actionType.value = null
  commentaire.value = ''
  processing.value = false
}

async function confirmAction() {
  if (!selectedDemande.value || !actionType.value) {
    return
  }

  processing.value = true
  apiError.value = ''
  successMessage.value = ''

  try {
    const updated = await demandesAdhesionApi.decide(
      selectedDemande.value.id,
      actionType.value === 'valider' ? 'APPROUVEE' : 'REFUSEE',
      commentaire.value
    )

    if (actionType.value === 'valider') {
      successMessage.value = `Demande approuvée pour ${selectedDemande.value.email}. L'email d'activation a été traité.`
    } else {
      successMessage.value = 'Demande refusée avec succès.'
    }

    closeModal()
    await loadDemandes()
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
      processing.value = false
    }
  }
}

function handleSessionError(error) {
  return handleApiAccessError(error, authStore, router)
}

function formatDate(value) {
  if (!value) {
    return 'N/A'
  }
  return new Date(value).toLocaleDateString('fr-FR')
}

function formatDateTime(value) {
  if (!value) {
    return 'Historique non renseigne'
  }
  return new Date(value).toLocaleString('fr-BE', {
    dateStyle: 'short',
    timeStyle: 'short'
  })
}
</script>

<style scoped>
.demande-link-line {
  margin-top: 0.35rem;
}

.demande-identity {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.75rem;
  align-items: baseline;
}

.conditions-trace {
  display: grid;
  gap: 0.2rem;
  margin-top: 0.75rem;
  color: #52615a;
  font-size: 0.88rem;
}
</style>
