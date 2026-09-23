<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au dashboard</router-link>
        <h1>Emails envoyes</h1>
        <p>Historique des emails envoyes par l'application aux clients et aux administrateurs.</p>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div v-if="loading && notifications.length === 0" class="empty-state">
      <div class="empty-icon">...</div>
      <h3>Chargement des emails</h3>
    </div>

    <div v-else>
      <div class="email-history-toolbar">
        <div class="form-group">
          <label for="email-type-filter">Type d'email</label>
          <select id="email-type-filter" v-model="filterType" class="form-control">
            <option value="">Tous les types</option>
            <option v-for="type in availableTypes" :key="type" :value="type">
              {{ typeLabels[type] || type }}
            </option>
          </select>
        </div>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Type</th>
              <th>Destinataire</th>
              <th>Sujet</th>
              <th>Statut</th>
              <th>Reference</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="notification in filteredNotifications" :key="notification.id">
              <td>{{ formatDateTime(notification.createdAt) }}</td>
              <td>{{ typeLabels[notification.type] || notification.type }}</td>
              <td>{{ notification.destinataire }}</td>
              <td class="subject-cell">{{ notification.sujet }}</td>
              <td>
                <span :class="['status-badge', statusClass(notification.statutEnvoi)]">
                  {{ statusLabels[notification.statutEnvoi] || notification.statutEnvoi }}
                </span>
                <small v-if="notification.erreurEnvoi" class="email-error">{{ notification.erreurEnvoi }}</small>
              </td>
              <td>{{ referenceLabel(notification) }}</td>
              <td>
                <button class="btn-icon" @click="openPreview(notification.id)">
                  Voir
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="filteredNotifications.length === 0" class="empty-state">
        <div class="empty-icon">0</div>
        <h3>Aucun email trouve</h3>
        <p>Les emails envoyes par les evenements metier apparaitront ici.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { emailNotificationsApi, getApiErrorMessage, handleApiAccessError } from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const notifications = ref([])
const loading = ref(false)
const apiError = ref('')
const filterType = ref('')

const typeLabels = {
  ORDER_CONFIRMATION: 'Confirmation commande',
  ORDER_REMINDER_J1: 'Rappel J-1',
  ORDER_CANCELLATION: 'Annulation commande',
  MEMBERSHIP_VALIDATION: 'Validation adhesion',
  MEMBERSHIP_REJECTION: 'Refus adhesion',
  MEMBERSHIP_REQUEST_RECEIVED: 'Demande recue',
  ORDER_READY: 'Commande prete',
  LOW_STOCK_ALERT: 'Alerte stock bas',
  OUT_OF_STOCK_ALERT: 'Rupture stock',
  PASSWORD_SETUP: 'Lien mot de passe'
}

const statusLabels = {
  PENDING: 'En attente',
  SENT: 'Envoye',
  FAILED: 'Echec'
}

const availableTypes = computed(() => [...new Set(notifications.value.map(item => item.type).filter(Boolean))].sort())

const filteredNotifications = computed(() => {
  return notifications.value.filter(item => !filterType.value || item.type === filterType.value)
})

onMounted(() => {
  loadNotifications()
})

async function loadNotifications() {
  loading.value = true
  apiError.value = ''
  try {
    notifications.value = await emailNotificationsApi.list()
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    apiError.value = getApiErrorMessage(error)
    notifications.value = []
  } finally {
    loading.value = false
  }
}

function openPreview(id) {
  const url = emailNotificationsApi.previewUrl(id)
  window.open(url, '_blank', 'noopener')
}

function referenceLabel(notification) {
  if (notification.commandeId) return `Commande #${notification.commandeId}`
  if (notification.produitId) return `Produit #${notification.produitId}`
  if (notification.membreId) return `Membre #${notification.membreId}`
  if (notification.demandeAdhesionId) return `Adhesion #${notification.demandeAdhesionId}`
  return '-'
}

function statusClass(status) {
  if (status === 'SENT') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('fr-FR')
}
</script>

<style scoped>
.subject-cell {
  min-width: 260px;
  max-width: 320px;
}

.email-error {
  display: block;
  max-width: 240px;
  margin-top: 0.35rem;
  color: #8f3f35;
  font-size: 0.78rem;
  line-height: 1.35;
}

.email-history-toolbar {
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid rgba(61, 73, 68, 0.1);
  border-radius: 10px;
  background: rgba(255, 253, 248, 0.94);
  box-shadow: var(--shadow-sm);
}

.email-history-toolbar .form-group {
  max-width: 360px;
  margin-bottom: 0;
}
</style>
