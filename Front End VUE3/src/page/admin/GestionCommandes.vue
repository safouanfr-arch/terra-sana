<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au dashboard</router-link>
        <h1>Gestion des commandes</h1>
        <p>Suivi operationnel, historique, details et export des commandes backend.</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary" @click="resetFilters">
          Reinitialiser les filtres
        </button>
        <button class="btn btn-primary" :disabled="filteredCommandes.length === 0" @click="exporterCSV">
          Exporter CSV
        </button>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <div class="filters-advanced">
      <div class="filter-row">
        <div class="filter-group">
          <label>Rechercher</label>
          <input
            v-model="searchTerm"
            type="text"
            placeholder="Membre, email, numero, produit..."
            class="filter-input"
          />
        </div>

        <div class="filter-group">
          <label>Statut</label>
          <select v-model="filterStatut" class="filter-select">
            <option value="tous">Tous les statuts</option>
            <option value="terminees">Commandes cloturees</option>
            <option value="CONFIRMED">Confirmees</option>
            <option value="IN_PREP">En preparation</option>
            <option value="READY">Pretes</option>
            <option value="DISTRIBUTED">Distribuees</option>
            <option value="CANCELLED">Annulees</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Membre</label>
          <select v-model="filterMembre" class="filter-select">
            <option value="">Tous les membres</option>
            <option v-for="membre in membresUniques" :key="membre" :value="membre">{{ membre }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Point de collecte</label>
          <select v-model="filterPoint" class="filter-select">
            <option value="">Tous les points</option>
            <option v-for="point in pointsCollecte" :key="point" :value="point">{{ point }}</option>
          </select>
        </div>
      </div>

      <div class="filter-row">
        <div class="filter-group">
          <label>Date debut</label>
          <input v-model="filterDateDebut" type="date" class="filter-input" />
        </div>

        <div class="filter-group">
          <label>Date fin</label>
          <input v-model="filterDateFin" type="date" class="filter-input" />
        </div>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table commandes-table">
        <thead>
          <tr>
            <th>No commande</th>
            <th>Commande</th>
            <th>Membre</th>
            <th>Retrait</th>
            <th>Point</th>
            <th>Articles</th>
            <th>Montant</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="commande in filteredCommandes" :key="commande.id">
            <td><strong>{{ commande.numero }}</strong></td>
            <td>{{ formatDateTime(commande.dateCommande || commande.date) }}</td>
            <td>
              <strong>{{ getMembreNom(commande) }}</strong><br />
              <span class="text-small">{{ getMembreEmail(commande) }}</span>
            </td>
            <td>
              {{ formatDate(commande.dateRetrait) }}<br />
              <span class="text-small">{{ commande.creneauRetrait?.horaire || 'N/A' }}</span>
            </td>
            <td>{{ commande.pointCollecte?.nom || 'N/A' }}</td>
            <td class="text-center">{{ (commande.produits || []).length }}</td>
            <td><strong>{{ getMontant(commande).toFixed(2) }} EUR</strong></td>
            <td>
              <select
                :value="commande.statut"
                class="status-select"
                @change="updateStatut(commande, $event.target.value)"
              >
                <option value="CONFIRMED">Confirmee</option>
                <option value="IN_PREP">En preparation</option>
                <option value="READY">Prete</option>
                <option value="DISTRIBUTED">Distribuee</option>
                <option value="CANCELLED">Annulee</option>
              </select>
            </td>
            <td class="actions-cell">
              <button class="btn-icon" @click="openDetailsModal(commande)" title="Voir details">Voir</button>
              <button
                v-if="commande.statut !== 'CANCELLED'"
                class="btn-icon"
                @click="sendReminder(commande)"
                title="Envoyer rappel"
              >
                Envoyer rappel
              </button>
            </td>
          </tr>
        </tbody>
        <tfoot v-if="filteredCommandes.length > 0">
          <tr>
            <td colspan="6" class="table-total-label">Total affiche</td>
            <td><strong>{{ totalMontantFiltre.toFixed(2) }} EUR</strong></td>
            <td colspan="2"></td>
          </tr>
        </tfoot>
      </table>
    </div>

    <div v-if="filteredCommandes.length === 0" class="empty-state">
      <div class="empty-icon">0</div>
      <h3>Aucune commande trouvee</h3>
      <p>Essayez de modifier vos filtres.</p>
    </div>

    <div v-if="showDetailsModal && commandeDetails" class="modal-overlay" @click.self="closeDetailsModal">
      <div class="modal-content modal-large">
        <div class="modal-header">
          <h2>Details commande {{ commandeDetails.numero }}</h2>
          <button class="btn-close" @click="closeDetailsModal">x</button>
        </div>

        <div class="modal-body">
          <div class="detail-section">
            <h3>Informations generales</h3>
            <div class="detail-grid">
              <div class="detail-item"><span class="detail-label">Commande</span><span class="detail-value">{{ commandeDetails.numero }}</span></div>
              <div class="detail-item"><span class="detail-label">Date</span><span class="detail-value">{{ formatDateTime(commandeDetails.dateCommande || commandeDetails.date) }}</span></div>
              <div class="detail-item"><span class="detail-label">Membre</span><span class="detail-value">{{ getMembreNom(commandeDetails) }}</span></div>
              <div class="detail-item"><span class="detail-label">Email</span><span class="detail-value">{{ getMembreEmail(commandeDetails) }}</span></div>
              <div class="detail-item"><span class="detail-label">Statut</span><span class="detail-value">{{ getStatutLabel(commandeDetails.statut) }}</span></div>
              <div class="detail-item"><span class="detail-label">Code retrait</span><span class="detail-value">{{ commandeDetails.codeRetrait || 'N/A' }}</span></div>
            </div>
          </div>

          <div class="detail-section">
            <h3>Retrait</h3>
            <div class="detail-grid">
              <div class="detail-item"><span class="detail-label">Point</span><span class="detail-value">{{ commandeDetails.pointCollecte?.nom || 'N/A' }}</span></div>
              <div class="detail-item"><span class="detail-label">Adresse</span><span class="detail-value">{{ commandeDetails.pointCollecte?.adresse || 'N/A' }}</span></div>
              <div class="detail-item"><span class="detail-label">Date</span><span class="detail-value">{{ formatDate(commandeDetails.dateRetrait) }}</span></div>
              <div class="detail-item"><span class="detail-label">Creneau</span><span class="detail-value">{{ commandeDetails.creneauRetrait?.horaire || 'N/A' }}</span></div>
            </div>
          </div>

          <div class="detail-section">
            <h3>Produits</h3>
            <table class="detail-table">
              <thead>
                <tr>
                  <th>Produit</th>
                  <th>Quantite</th>
                  <th>Prix unitaire</th>
                  <th>TVA</th>
                  <th>Total TTC</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="produit in commandeDetails.produits || []" :key="produit.ligneId || produit.id">
                  <td><strong>{{ produit.nom }}</strong></td>
                  <td>{{ produit.quantite }} {{ produit.unite }}</td>
                  <td>{{ Number(produit.prixUnitaire || 0).toFixed(2) }} EUR</td>
                  <td>{{ produit.tauxTVA }}%</td>
                  <td><strong>{{ Number(produit.totalTTC || 0).toFixed(2) }} EUR</strong></td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="detail-section totaux-detail">
            <div class="total-row">
              <span>Total HT:</span>
              <strong>{{ Number(commandeDetails.totalHT || 0).toFixed(2) }} EUR</strong>
            </div>
            <div class="total-row">
              <span>Total TVA:</span>
              <strong>{{ Number(commandeDetails.totalTVA || 0).toFixed(2) }} EUR</strong>
            </div>
            <div class="total-row total-final">
              <span>Total TTC:</span>
              <strong>{{ getMontant(commandeDetails).toFixed(2) }} EUR</strong>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeDetailsModal">Fermer</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore.js'
import { commandesApi, emailNotificationsApi, getApiErrorMessage, handleApiAccessError } from '../../services/api.js'
import {
  addCsvKeyValues,
  addCsvSection,
  downloadCsv,
  exportDateStamp,
  formatCsvDate,
  formatCsvDateTime,
  formatCsvMoney,
  formatCsvQuantity
} from '../../utils/csvExport.js'

const commandes = ref([])
const apiError = ref('')
const successMessage = ref('')
const showDetailsModal = ref(false)
const commandeDetails = ref(null)

const filterStatut = ref('tous')
const filterPoint = ref('')
const filterMembre = ref('')
const filterDateDebut = ref('')
const filterDateFin = ref('')
const searchTerm = ref('')
const router = useRouter()
const authStore = useAuthStore()

onMounted(() => loadCommandes())

async function loadCommandes() {
  apiError.value = ''
  successMessage.value = ''
  try {
    commandes.value = await commandesApi.listAdmin()
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    apiError.value = getApiErrorMessage(error)
    commandes.value = []
  }
}

function getMembreNom(commande) {
  const membre = commande?.membre
  if (!membre) return 'Membre'
  return `${membre.prenom || ''} ${membre.nom || ''}`.trim() || membre.email || 'Membre'
}

function getMembreEmail(commande) {
  return commande?.membre?.email || ''
}

function getMontant(commande) {
  return Number(commande.montantTTC ?? commande.montant ?? commande.totalTVAC ?? 0)
}

const pointsCollecte = computed(() => {
  return [...new Set(commandes.value.map(c => c.pointCollecte?.nom).filter(Boolean))].sort()
})

const membresUniques = computed(() => {
  return [...new Set(commandes.value.map(getMembreNom).filter(Boolean))].sort()
})

const filteredCommandes = computed(() => {
  return commandes.value.filter(commande => {
    const matchStatut = filterStatut.value === 'tous'
      || (filterStatut.value === 'terminees' && ['DISTRIBUTED', 'CANCELLED'].includes(commande.statut))
      || commande.statut === filterStatut.value
    const matchPoint = !filterPoint.value || commande.pointCollecte?.nom === filterPoint.value
    const matchMembre = !filterMembre.value || getMembreNom(commande) === filterMembre.value
    const dateCommande = commande.dateCommande || commande.date

    if (filterDateDebut.value && new Date(dateCommande) < new Date(`${filterDateDebut.value}T00:00:00`)) {
      return false
    }
    if (filterDateFin.value && new Date(dateCommande) > new Date(`${filterDateFin.value}T23:59:59`)) {
      return false
    }

    const needle = searchTerm.value.trim().toLowerCase()
    const produitsTexte = (commande.produits || []).map(produit => produit.nom).join(' ')
    const haystack = [
      commande.numero,
      getMembreNom(commande),
      getMembreEmail(commande),
      commande.pointCollecte?.nom || '',
      produitsTexte
    ].join(' ').toLowerCase()
    const matchSearch = !needle || haystack.includes(needle)

    return matchStatut && matchPoint && matchMembre && matchSearch
  })
})

const totalMontantFiltre = computed(() =>
  filteredCommandes.value.reduce((sum, commande) => sum + getMontant(commande), 0)
)

function getStatutLabel(statut) {
  const labels = {
    CONFIRMED: 'Confirmee',
    IN_PREP: 'En preparation',
    READY: 'Prete',
    DISTRIBUTED: 'Distribuee',
    CANCELLED: 'Annulee'
  }
  return labels[statut] || statut
}

function openDetailsModal(commande) {
  commandeDetails.value = commande
  showDetailsModal.value = true
}

function closeDetailsModal() {
  showDetailsModal.value = false
  commandeDetails.value = null
}

async function updateStatut(commande, statut) {
  apiError.value = ''
  successMessage.value = ''
  try {
    const updatedCommande = await commandesApi.updateStatus(commande.id, statut)
    const index = commandes.value.findIndex(item => item.id === updatedCommande.id)
    if (index !== -1) {
      commandes.value[index] = updatedCommande
    }
    if (commandeDetails.value?.id === updatedCommande.id) {
      commandeDetails.value = updatedCommande
    }
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    apiError.value = getApiErrorMessage(error)
    await loadCommandes()
  }
}

async function sendReminder(commande) {
  apiError.value = ''
  successMessage.value = ''
  if (commande.statut === 'CANCELLED') {
    apiError.value = "Impossible d'envoyer un rappel pour une commande annulee."
    return
  }
  try {
    const email = await emailNotificationsApi.sendOrderReminder(commande.id)
    successMessage.value = email.statutEnvoi === 'SENT'
      ? `Le rappel J-1 pour ${commande.numero} a ete envoye.`
      : `Le rappel J-1 pour ${commande.numero} a ete enregistre mais l'envoi a echoue.`
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    apiError.value = getApiErrorMessage(error)
  }
}

function exporterCSV() {
  const rows = []

  addCsvSection(rows, 'Terra Sana - Export des commandes')
  addCsvKeyValues(rows, [
    ['Genere le', formatCsvDateTime(new Date())],
    ['Commandes exportees', filteredCommandes.value.length],
    ['Montant total TTC (EUR)', formatCsvMoney(totalMontantFiltre.value)]
  ])

  addCsvSection(rows, 'Filtres appliques')
  addCsvKeyValues(rows, [
    ['Recherche', searchTerm.value.trim() || 'Toutes'],
    ['Statut', getStatutFilterLabel()],
    ['Membre', filterMembre.value || 'Tous les membres'],
    ['Point de collecte', filterPoint.value || 'Tous les points'],
    ['Date debut', filterDateDebut.value ? formatCsvDate(filterDateDebut.value) : 'Non limitee'],
    ['Date fin', filterDateFin.value ? formatCsvDate(filterDateFin.value) : 'Non limitee']
  ])

  addCsvSection(rows, 'Synthese par statut')
  rows.push(['Statut', 'Nombre de commandes', 'Montant TTC (EUR)'])
  for (const item of buildStatutSummary()) {
    rows.push([item.statut, item.count, formatCsvMoney(item.total)])
  }

  addCsvSection(rows, 'Liste des commandes')
  rows.push([
    'No commande',
    'Date commande',
    'Membre',
    'Email',
    'Point de collecte',
    'Date retrait',
    'Creneau',
    'Statut',
    'Nb articles',
    'Code retrait',
    'Total HT (EUR)',
    'TVA (EUR)',
    'Total TTC (EUR)'
  ])

  for (const commande of filteredCommandes.value) {
    rows.push([
      commande.numero,
      formatCsvDateTime(commande.dateCommande || commande.date),
      getMembreNom(commande),
      getMembreEmail(commande),
      commande.pointCollecte?.nom || '',
      formatCsvDate(commande.dateRetrait),
      getCreneauLabel(commande),
      getStatutLabel(commande.statut),
      (commande.produits || []).length,
      commande.codeRetrait || '',
      formatCsvMoney(commande.totalHT),
      formatCsvMoney(commande.totalTVA),
      formatCsvMoney(getMontant(commande))
    ])
  }

  rows.push(['', '', '', '', '', '', '', '', '', 'Total affiche', '', '', formatCsvMoney(totalMontantFiltre.value)])

  addCsvSection(rows, 'Details des produits commandes')
  rows.push([
    'No commande',
    'Membre',
    'Produit',
    'Quantite',
    'Unite',
    'Prix unitaire TTC (EUR)',
    'TVA (%)',
    'Total ligne TTC (EUR)'
  ])

  for (const commande of filteredCommandes.value) {
    for (const produit of commande.produits || []) {
      rows.push([
        commande.numero,
        getMembreNom(commande),
        produit.nom,
        formatCsvQuantity(produit.quantite),
        produit.unite || '',
        formatCsvMoney(produit.prixUnitaire),
        produit.tauxTVA ?? '',
        formatCsvMoney(getProduitTotal(produit))
      ])
    }
  }

  downloadCsv(`terra-sana-commandes-${exportDateStamp()}.csv`, rows)
}

function formatDate(value) {
  if (!value) return 'N/A'
  return new Date(value).toLocaleDateString('fr-FR')
}

function formatDateTime(value) {
  if (!value) return 'N/A'
  return new Date(value).toLocaleString('fr-FR')
}

function resetFilters() {
  filterStatut.value = 'tous'
  filterPoint.value = ''
  filterMembre.value = ''
  filterDateDebut.value = ''
  filterDateFin.value = ''
  searchTerm.value = ''
}

function getCreneauLabel(commande) {
  const creneau = commande.creneauRetrait
  if (!creneau) return ''
  if (creneau.horaire) return creneau.horaire
  return [creneau.debut || creneau.heureDebut, creneau.fin || creneau.heureFin]
    .filter(Boolean)
    .join(' - ')
}

function getProduitTotal(produit) {
  return Number(produit.totalTTC ?? (Number(produit.prixUnitaire || 0) * Number(produit.quantite || 0)))
}

function getStatutFilterLabel() {
  if (filterStatut.value === 'tous') return 'Tous les statuts'
  if (filterStatut.value === 'terminees') return 'Commandes cloturees'
  return getStatutLabel(filterStatut.value)
}

function buildStatutSummary() {
  const map = new Map()

  for (const commande of filteredCommandes.value) {
    const statut = getStatutLabel(commande.statut)
    if (!map.has(statut)) {
      map.set(statut, { statut, count: 0, total: 0 })
    }
    const item = map.get(statut)
    item.count += 1
    item.total += getMontant(commande)
  }

  return [...map.values()].sort((left, right) => left.statut.localeCompare(right.statut))
}
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.commandes-table {
  min-width: 1120px;
}

.table-total-label {
  text-align: right;
  color: var(--text-secondary);
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.status-select {
  font-weight: 600;
  border-radius: 6px;
  padding: 0.25rem 0.5rem;
  background: #f5f5f5;
  border: 1px solid #ccc;
  color: #333;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 12px;
  max-width: 900px;
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-large {
  max-width: 1000px;
}

.modal-header,
.modal-footer {
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.modal-footer {
  border-top: 1px solid #e0e0e0;
  border-bottom: none;
  display: flex;
  justify-content: flex-end;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.6rem;
  cursor: pointer;
}

.detail-section {
  margin-bottom: 2rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem 1rem;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-label {
  color: #666;
}

.detail-value {
  font-weight: 600;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
}

.detail-table th,
.detail-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #e0e0e0;
  text-align: left;
}

.totaux-detail {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  padding: 0.4rem 0;
}

.total-final {
  border-top: 2px solid #333;
  margin-top: 0.5rem;
  padding-top: 0.75rem;
}

@media (max-width: 720px) {
  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1 1 auto;
  }
}
</style>
