<template>
  <div class="admin-page archives-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/membre" class="back-link">Retour a mon espace</router-link>
        <h1>Mes archives</h1>
        <p>Historique de vos commandes cloturees depuis les donnees reelles du backend.</p>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div class="filters-advanced">
      <div class="filter-row">
        <div class="filter-group">
          <label>Recherche</label>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Numero, point, produit..."
            class="filter-input"
          />
        </div>

        <div class="filter-group">
          <label>Statut</label>
          <select v-model="filterStatut" class="filter-select">
            <option value="terminees">Commandes cloturees</option>
            <option value="tous">Tous les statuts</option>
            <option value="CONFIRMED">Confirmee</option>
            <option value="IN_PREP">En preparation</option>
            <option value="READY">Prete</option>
            <option value="DISTRIBUTED">Distribuee</option>
            <option value="CANCELLED">Annulee</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Point de collecte</label>
          <select v-model="filterPoint" class="filter-select">
            <option value="">Tous les points</option>
            <option v-for="point in pointsUniques" :key="point" :value="point">{{ point }}</option>
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

        <div class="filter-group">
          <label>Actions</label>
          <div class="archives-filter-actions">
            <button class="btn btn-secondary" @click="resetFilters">Reinitialiser</button>
            <button class="btn btn-primary" :disabled="commandesFiltrees.length === 0" @click="exporterCSV">
              Exporter CSV
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-container">
      <div v-if="commandesFiltrees.length === 0" class="empty-state">
        <div class="empty-icon">0</div>
        <h3>Aucune commande</h3>
        <p>Aucune commande ne correspond a vos filtres.</p>
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>Commande</th>
            <th>Date</th>
            <th>Point</th>
            <th>Retrait</th>
            <th>Statut</th>
            <th>Montant</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="commande in commandesFiltrees" :key="commande.id || commande.numero">
            <td><strong>{{ commande.numero }}</strong></td>
            <td>{{ formatDateTime(commande.dateCommande || commande.date) }}</td>
            <td>{{ commande.pointCollecte?.nom || 'N/A' }}</td>
            <td>
              {{ formatDate(commande.dateRetrait) }}<br />
              <span class="text-small">{{ commande.creneauRetrait?.horaire || 'N/A' }}</span>
            </td>
            <td>
              <span :class="['status-badge', getStatutBadge(commande.statut).class]">
                {{ getStatutBadge(commande.statut).label }}
              </span>
            </td>
            <td><strong>{{ getMontant(commande).toFixed(2) }} EUR</strong></td>
            <td>
              <button class="btn-icon btn-primary" @click="commandeSelectionnee = commande">
                Details
              </button>
            </td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="5" class="table-total-label">Total affiche</td>
            <td><strong>{{ montantTotalFiltre.toFixed(2) }} EUR</strong></td>
            <td></td>
          </tr>
        </tfoot>
      </table>
    </div>

    <div v-if="commandeSelectionnee" class="modal-overlay" @click="commandeSelectionnee = null">
      <div class="modal modal-large" @click.stop>
        <div class="modal-header">
          <h2>Commande {{ commandeSelectionnee.numero }}</h2>
          <button class="modal-close" @click="commandeSelectionnee = null">x</button>
        </div>

        <div class="modal-body commande-detail">
          <div class="detail-section">
            <h3>Informations generales</h3>
            <p><strong>Statut :</strong> {{ getStatutBadge(commandeSelectionnee.statut).label }}</p>
            <p><strong>Date commande :</strong> {{ formatDateTime(commandeSelectionnee.dateCommande || commandeSelectionnee.date) }}</p>
          </div>

          <div class="detail-section">
            <h3>Retrait</h3>
            <p><strong>Point :</strong> {{ commandeSelectionnee.pointCollecte?.nom || 'N/A' }}</p>
            <p><strong>Adresse :</strong> {{ commandeSelectionnee.pointCollecte?.adresse || 'N/A' }}</p>
            <p><strong>Date :</strong> {{ formatDate(commandeSelectionnee.dateRetrait) }}</p>
            <p><strong>Creneau :</strong> {{ commandeSelectionnee.creneauRetrait?.horaire || 'N/A' }}</p>
          </div>

          <div class="detail-section">
            <h3>Produits</h3>
            <div
              v-for="produit in commandeSelectionnee.produits || []"
              :key="`${commandeSelectionnee.id}-${produit.ligneId || produit.id}`"
              class="produit-ligne"
            >
              <span>{{ produit.nom }} x{{ produit.quantite }}</span>
              <span>{{ Number(produit.totalTTC || produit.totalHT || 0).toFixed(2) }} EUR</span>
            </div>
          </div>

          <div class="detail-section">
            <h3>Montant</h3>
            <p><strong>Total :</strong> {{ getMontant(commandeSelectionnee).toFixed(2) }} EUR</p>
            <p v-if="commandeSelectionnee.codeRetrait"><strong>Code retrait :</strong> {{ commandeSelectionnee.codeRetrait }}</p>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-secondary" @click="commandeSelectionnee = null">Fermer</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { commandesApi, getApiErrorMessage, handleApiAccessError } from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore'
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

const router = useRouter()
const authStore = useAuthStore()

const commandes = ref([])
const apiError = ref('')
const filterStatut = ref('terminees')
const filterDateDebut = ref('')
const filterDateFin = ref('')
const searchQuery = ref('')
const filterPoint = ref('')
const commandeSelectionnee = ref(null)

onMounted(() => {
  loadCommandes()
})

async function loadCommandes() {
  apiError.value = ''
  try {
    commandes.value = await commandesApi.listMy()
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    commandes.value = []
    apiError.value = getApiErrorMessage(error)
  }
}

const pointsUniques = computed(() => {
  return [...new Set(commandes.value.map(commande => commande.pointCollecte?.nom).filter(Boolean))].sort()
})

const commandesFiltrees = computed(() => {
  return commandes.value.filter(commande => {
    const dateCommande = commande.dateCommande || commande.date
    const pointNom = commande.pointCollecte?.nom || ''
    const produitsTexte = (commande.produits || []).map(produit => produit.nom).join(' ').toLowerCase()

    if (filterStatut.value === 'terminees') {
      if (!['DISTRIBUTED', 'CANCELLED'].includes(commande.statut)) return false
    } else if (filterStatut.value !== 'tous' && commande.statut !== filterStatut.value) {
      return false
    }
    if (filterPoint.value && pointNom !== filterPoint.value) return false

    if (filterDateDebut.value && new Date(dateCommande) < new Date(`${filterDateDebut.value}T00:00:00`)) return false
    if (filterDateFin.value && new Date(dateCommande) > new Date(`${filterDateFin.value}T23:59:59`)) return false

    if (searchQuery.value.trim()) {
      const query = searchQuery.value.trim().toLowerCase()
      const haystack = [
        commande.numero || '',
        pointNom,
        produitsTexte
      ].join(' ').toLowerCase()

      if (!haystack.includes(query)) return false
    }

    return true
  })
})

const montantTotalFiltre = computed(() =>
  commandesFiltrees.value.reduce((sum, commande) => sum + getMontant(commande), 0)
)

function getMontant(commande) {
  return Number(commande.montantTTC ?? commande.montant ?? commande.totalTVAC ?? 0)
}

function getStatutBadge(statut) {
  const map = {
    CONFIRMED: { label: 'Confirmee', class: 'success' },
    IN_PREP: { label: 'En preparation', class: 'warning' },
    READY: { label: 'Prete', class: 'info' },
    DISTRIBUTED: { label: 'Distribuee', class: 'success' },
    CANCELLED: { label: 'Annulee', class: 'danger' }
  }
  return map[statut] || { label: statut || 'Inconnu', class: 'secondary' }
}

function formatDate(value) {
  if (!value) return 'N/A'
  return new Date(value).toLocaleDateString('fr-FR')
}

function formatDateTime(value) {
  if (!value) return 'N/A'
  return new Date(value).toLocaleString('fr-FR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function resetFilters() {
  filterStatut.value = 'terminees'
  filterDateDebut.value = ''
  filterDateFin.value = ''
  searchQuery.value = ''
  filterPoint.value = ''
}

function exporterCSV() {
  const rows = []

  addCsvSection(rows, 'Terra Sana - Mes archives de commandes')
  addCsvKeyValues(rows, [
    ['Genere le', formatCsvDateTime(new Date())],
    ['Commandes exportees', commandesFiltrees.value.length],
    ['Montant total TTC (EUR)', formatCsvMoney(montantTotalFiltre.value)]
  ])

  addCsvSection(rows, 'Filtres appliques')
  addCsvKeyValues(rows, [
    ['Recherche', searchQuery.value.trim() || 'Toutes'],
    ['Statut', getStatutFilterLabel()],
    ['Point de collecte', filterPoint.value || 'Tous les points'],
    ['Date debut', filterDateDebut.value ? formatCsvDate(filterDateDebut.value) : 'Non limitee'],
    ['Date fin', filterDateFin.value ? formatCsvDate(filterDateFin.value) : 'Non limitee']
  ])

  addCsvSection(rows, 'Commandes')
  rows.push([
    'No commande',
    'Date commande',
    'Point de collecte',
    'Date retrait',
    'Creneau',
    'Statut',
    'Code retrait',
    'Total TTC (EUR)'
  ])

  for (const commande of commandesFiltrees.value) {
    rows.push([
      commande.numero,
      formatCsvDateTime(commande.dateCommande || commande.date),
      commande.pointCollecte?.nom || '',
      formatCsvDate(commande.dateRetrait),
      getCreneauLabel(commande),
      getStatutBadge(commande.statut).label,
      commande.codeRetrait || '',
      formatCsvMoney(getMontant(commande))
    ])
  }

  rows.push(['', '', '', '', '', 'Total affiche', '', formatCsvMoney(montantTotalFiltre.value)])

  addCsvSection(rows, 'Details des produits')
  rows.push(['No commande', 'Produit', 'Quantite', 'Unite', 'Total ligne TTC (EUR)'])

  for (const commande of commandesFiltrees.value) {
    for (const produit of commande.produits || []) {
      rows.push([
        commande.numero,
        produit.nom,
        formatCsvQuantity(produit.quantite),
        produit.unite || '',
        formatCsvMoney(produit.totalTTC ?? produit.totalHT)
      ])
    }
  }

  downloadCsv(`terra-sana-mes-archives-${exportDateStamp()}.csv`, rows)
}

function getCreneauLabel(commande) {
  const creneau = commande.creneauRetrait
  if (!creneau) return ''
  if (creneau.horaire) return creneau.horaire
  return [creneau.debut || creneau.heureDebut, creneau.fin || creneau.heureFin]
    .filter(Boolean)
    .join(' - ')
}

function getStatutFilterLabel() {
  if (filterStatut.value === 'terminees') return 'Commandes cloturees'
  if (filterStatut.value === 'tous') return 'Tous les statuts'
  return getStatutBadge(filterStatut.value).label
}
</script>

<style scoped>
.table-total-label {
  text-align: right;
  color: var(--text-secondary);
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
</style>
