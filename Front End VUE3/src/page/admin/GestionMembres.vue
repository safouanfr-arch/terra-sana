<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au dashboard</router-link>
        <h1>Gestion des membres</h1>
        <p>Consulter les membres et suspendre ou reactiver les comptes.</p>
      </div>
      <div class="header-stats">
        <span class="stat-badge success">Actifs: <strong>{{ statsActifs }}</strong></span>
        <span class="stat-badge danger">Suspendus: <strong>{{ statsSuspendus }}</strong></span>
        <span class="stat-badge">Total: <strong>{{ statsTotal }}</strong></span>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div class="filters-section">
      <div class="search-bar">
        <input
          v-model="searchTerm"
          type="text"
          placeholder="Rechercher par nom, prenom ou email..."
          class="search-input"
        />
      </div>
      <div class="filter-buttons">
        <button :class="['filter-btn', { active: filterStatut === 'tous' }]" @click="filterStatut = 'tous'">
          Tous ({{ statsTotal }})
        </button>
        <button :class="['filter-btn', { active: filterStatut === 'actif' }]" @click="filterStatut = 'actif'">
          Actifs ({{ statsActifs }})
        </button>
        <button :class="['filter-btn', { active: filterStatut === 'suspendu' }]" @click="filterStatut = 'suspendu'">
          Suspendus ({{ statsSuspendus }})
        </button>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>Membre</th>
            <th>Email</th>
            <th>Telephone</th>
            <th>Adresse</th>
            <th>Inscription</th>
            <th>Derniere connexion</th>
            <th>Commandes</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="membre in filteredMembres" :key="membre.id">
            <td><strong>{{ membre.prenom }} {{ membre.nom }}</strong></td>
            <td>{{ membre.email }}</td>
            <td>{{ membre.telephone || 'N/A' }}</td>
            <td>
              <div v-if="membre.adresse || membre.ville" class="member-address">
                <span v-if="membre.adresse">{{ membre.adresse }}</span>
                <span>{{ membre.codePostal }} {{ membre.ville }}</span>
              </div>
              <span v-else class="muted">N/A</span>
            </td>
            <td>{{ formatDate(membre.dateCreation) }}</td>
            <td>{{ formatDateTime(membre.dateDerniereConnexion) }}</td>
            <td class="text-center">{{ membre.nbCommandes }}</td>
            <td>
              <span :class="['status-badge', membre.statut === 'actif' ? 'success' : 'danger']">
                {{ membre.statut === 'actif' ? 'Actif' : 'Suspendu' }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button
                  v-if="membre.statut === 'actif'"
                  class="btn-icon btn-danger"
                  @click="openStatusModal(membre, false)"
                >
                  Suspendre
                </button>
                <button
                  v-else
                  class="btn-icon btn-success"
                  @click="openStatusModal(membre, true)"
                >
                  Reactiver
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="filteredMembres.length === 0" class="empty-state">
        <div class="empty-icon">0</div>
        <h3>Aucun membre trouve</h3>
        <p>Modifiez la recherche ou le filtre de statut.</p>
      </div>
    </div>

    <div v-if="showStatusModal" class="modal-overlay" @click="closeModals">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2>{{ targetActif ? 'Reactiver le membre' : 'Suspendre le membre' }}</h2>
          <button class="modal-close" @click="closeModals">x</button>
        </div>
        <div class="modal-body">
          <p>
            Confirmer le changement de statut pour
            <strong>{{ selectedMembre?.prenom }} {{ selectedMembre?.nom }}</strong> ?
          </p>
          <div :class="['alert', targetActif ? 'alert-info' : 'alert-warning']">
            {{ targetActif ? 'Le membre pourra de nouveau se connecter.' : 'Le membre ne pourra plus se connecter.' }}
          </div>
        </div>
        <div class="modal-actions">
          <button :class="['btn', targetActif ? 'btn-success' : 'btn-danger']" @click="confirmStatusChange">
            Confirmer
          </button>
          <button class="btn btn-secondary" @click="closeModals">Annuler</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { membresApi, getApiErrorMessage, handleApiAccessError } from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore.js'

const router = useRouter()
const authStore = useAuthStore()

const allMembres = ref([])
const membres = ref([])
const apiError = ref('')
const searchTerm = ref('')
const filterStatut = ref('tous')
const selectedMembre = ref(null)
const targetActif = ref(true)
const showStatusModal = ref(false)

onMounted(() => refreshMembres())

watch(filterStatut, () => {
  loadMembres(filterStatut.value)
})

async function refreshMembres() {
  apiError.value = ''
  try {
    allMembres.value = await membresApi.list()
    await loadMembres(filterStatut.value)
  } catch (error) {
    handleApiError(error)
    allMembres.value = []
    membres.value = []
  }
}

async function loadMembres(statut = 'tous') {
  apiError.value = ''
  try {
    membres.value = statut === 'tous'
      ? [...allMembres.value]
      : await membresApi.list(statut)
  } catch (error) {
    handleApiError(error)
    membres.value = []
  }
}

const statsActifs = computed(() => allMembres.value.filter(m => m.statut === 'actif').length)
const statsSuspendus = computed(() => allMembres.value.filter(m => m.statut === 'suspendu').length)
const statsTotal = computed(() => allMembres.value.length)

const filteredMembres = computed(() => {
  const query = searchTerm.value.trim().toLowerCase()
  if (!query) {
    return membres.value
  }

  return membres.value.filter(membre =>
    membre.nom.toLowerCase().includes(query) ||
    membre.prenom.toLowerCase().includes(query) ||
    membre.email.toLowerCase().includes(query)
  )
})

function openStatusModal(membre, actif) {
  selectedMembre.value = membre
  targetActif.value = actif
  showStatusModal.value = true
}

function closeModals() {
  showStatusModal.value = false
  selectedMembre.value = null
}

async function confirmStatusChange() {
  if (!selectedMembre.value) {
    return
  }

  apiError.value = ''
  try {
    await membresApi.update(selectedMembre.value.id, selectedMembre.value, targetActif.value)
    await refreshMembres()
    closeModals()
  } catch (error) {
    handleApiError(error)
  }
}

function handleApiError(error) {
  if (handleApiAccessError(error, authStore, router)) {
    return
  }
  apiError.value = getApiErrorMessage(error)
}

function formatDate(value) {
  if (!value) return 'N/A'
  return new Date(value).toLocaleDateString('fr-FR')
}

function formatDateTime(value) {
  if (!value) return 'N/A'
  return new Date(value).toLocaleString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

</script>

<style scoped>
.member-address {
  display: grid;
  gap: 0.1rem;
  font-size: 0.9rem;
}

.muted {
  color: var(--text-muted);
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}
</style>
