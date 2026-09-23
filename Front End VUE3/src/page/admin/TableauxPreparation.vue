<template>
  <div class="admin-page preparation-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link"><- Retour au dashboard</router-link>
        <h1>Tableaux de preparation</h1>
        <p>Suivez les commandes a preparer depuis les donnees reelles du backend.</p>
      </div>

      <div class="header-actions">
        <button class="btn btn-secondary" :disabled="commandesFiltrees.length === 0" @click="exporterCSV">
          Export CSV
        </button>
        <button class="btn btn-primary" :disabled="commandesFiltrees.length === 0" @click="imprimer">
          Imprimer
        </button>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <div class="filters-card">
      <div class="filters-grid">
        <div class="form-group">
          <label for="filter-point">Point de collecte</label>
          <select id="filter-point" v-model="filterPoint">
            <option value="">Tous les points</option>
            <option v-for="point in pointsUniques" :key="point" :value="point">{{ point }}</option>
          </select>
        </div>

        <div class="form-group">
          <label for="filter-date">Date de retrait</label>
          <select id="filter-date" v-model="filterDate">
            <option value="">Toutes les dates</option>
            <option v-for="date in datesUniques" :key="date" :value="date">
              {{ formatDateLabel(date) }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label for="filter-creneau">Creneau</label>
          <select id="filter-creneau" v-model="filterCreneau">
            <option value="">Tous les creneaux</option>
            <option v-for="creneau in creneauxUniques" :key="creneau" :value="creneau">{{ creneau }}</option>
          </select>
        </div>

        <div class="form-group">
          <label for="filter-statut">Statut</label>
          <select id="filter-statut" v-model="filterStatut">
            <option value="ACTIVES">Actives a preparer</option>
            <option value="CONFIRMED">Confirmees</option>
            <option value="IN_PREP">En preparation</option>
            <option value="READY">Pretes</option>
            <option value="DISTRIBUTED">Distribuees</option>
            <option value="CANCELLED">Annulees</option>
          </select>
        </div>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <span class="stat-label">Commandes</span>
        <strong class="stat-value">{{ commandesFiltrees.length }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">Lignes a preparer</span>
        <strong class="stat-value">{{ totalLignes }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">Unites totales</span>
        <strong class="stat-value">{{ totalUnites }}</strong>
      </div>
      <div class="stat-card warning">
        <span class="stat-label">Montant total</span>
        <strong class="stat-value">{{ formatCurrency(totalMontant) }}</strong>
      </div>
    </div>

    <div v-if="loading" class="empty-state">
      <h3>Chargement des commandes</h3>
      <p>Le tableau de preparation se synchronise avec le backend.</p>
    </div>

    <div v-else-if="commandesFiltrees.length === 0" class="empty-state">
      <h3>Aucune commande pour cette selection</h3>
      <p>Essayez un autre point, une autre date ou un autre statut.</p>
    </div>

    <div v-else class="preparation-layout">
      <section class="orders-panel">
        <div class="panel-header">
          <h2>Commandes</h2>
          <span>{{ commandesFiltrees.length }} resultat(s)</span>
        </div>

        <div class="orders-list">
          <article v-for="commande in commandesFiltrees" :key="commande.id" class="order-card">
            <div class="order-top">
              <div>
                <h3>{{ commande.numero }}</h3>
                <p>{{ getMembreNom(commande.membre) }}</p>
              </div>
              <span class="status-badge" :class="statusClass(commande.statut)">
                {{ statusLabel(commande.statut) }}
              </span>
            </div>

            <div class="order-meta">
              <span>{{ getPointNom(commande) }}</span>
              <span>{{ formatDateTime(commande.dateRetrait) }}</span>
              <span>{{ getCreneauLabel(commande) }}</span>
              <span>{{ formatCurrency(getMontant(commande)) }}</span>
            </div>

            <div class="order-lines">
              <div class="order-lines-header">
                <span>Produit</span>
                <span>Quantite</span>
                <span>Unite</span>
              </div>
              <div v-for="produit in commande.produits" :key="produit.ligneId || `${commande.id}-${produit.produitId}`" class="order-line">
                <span>{{ produit.nom }}</span>
                <span>{{ formatQuantite(produit.quantite) }}</span>
                <span>{{ produit.unite }}</span>
              </div>
            </div>

            <div class="order-actions">
              <button
                v-if="commande.statut === 'CONFIRMED'"
                class="btn btn-secondary btn-small"
                :disabled="updatingId === commande.id"
                @click="changerStatut(commande, 'IN_PREP')"
              >
                Passer en preparation
              </button>
              <button
                v-else-if="commande.statut === 'IN_PREP'"
                class="btn btn-secondary btn-small"
                :disabled="updatingId === commande.id"
                @click="changerStatut(commande, 'READY')"
              >
                Marquer prete
              </button>
              <button
                v-else-if="commande.statut === 'READY'"
                class="btn btn-primary btn-small"
                :disabled="updatingId === commande.id"
                @click="changerStatut(commande, 'DISTRIBUTED')"
              >
                Marquer distribuee
              </button>
              <span v-else class="action-note">Aucune action supplementaire</span>
            </div>
          </article>
        </div>
      </section>

      <aside class="summary-panel">
        <div class="panel-header">
          <h2>Pick-list</h2>
          <span>{{ pickList.length }} produit(s)</span>
        </div>

        <div class="pick-list">
          <div v-for="item in pickList" :key="item.key" class="pick-item">
            <div>
              <strong>{{ item.nom }}</strong>
              <p>{{ item.commandes }} commande(s)</p>
            </div>
            <div class="pick-qty">
              <span>{{ formatQuantite(item.quantiteTotale) }}</span>
              <small>{{ item.unite }}</small>
            </div>
          </div>
        </div>
      </aside>
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

const loading = ref(false)
const apiError = ref('')
const successMessage = ref('')
const updatingId = ref(null)
const commandes = ref([])

const filterPoint = ref('')
const filterDate = ref('')
const filterCreneau = ref('')
const filterStatut = ref('ACTIVES')

onMounted(() => {
  loadCommandes()
})

async function loadCommandes() {
  loading.value = true
  apiError.value = ''

  try {
    commandes.value = await commandesApi.listAdmin()
  } catch (error) {
    handleApiError(error)
  } finally {
    loading.value = false
  }
}

function handleApiError(error) {
  if (handleApiAccessError(error, authStore, router)) {
    return
  }
  apiError.value = getApiErrorMessage(error)
}

const pointsUniques = computed(() => [...new Set(commandes.value.map(getPointNom).filter(Boolean))].sort())

const datesUniques = computed(() => [...new Set(commandes.value.map(getDateKey).filter(Boolean))].sort())

const creneauxUniques = computed(() => [...new Set(commandes.value.map(getCreneauLabel).filter(Boolean))].sort())

const commandesFiltrees = computed(() => {
  return [...commandes.value]
    .filter(commande => {
      if (filterStatut.value === 'ACTIVES') {
        if (['DISTRIBUTED', 'CANCELLED'].includes(commande.statut)) {
          return false
        }
      } else if (commande.statut !== filterStatut.value) {
        return false
      }

      if (filterPoint.value && getPointNom(commande) !== filterPoint.value) {
        return false
      }

      if (filterDate.value && getDateKey(commande) !== filterDate.value) {
        return false
      }

      if (filterCreneau.value && getCreneauLabel(commande) !== filterCreneau.value) {
        return false
      }

      return true
    })
    .sort((left, right) => {
      const leftDate = new Date(left.dateRetrait || left.dateCommande || 0).getTime()
      const rightDate = new Date(right.dateRetrait || right.dateCommande || 0).getTime()
      return leftDate - rightDate
    })
})

const pickList = computed(() => {
  const map = new Map()

  for (const commande of commandesFiltrees.value) {
    for (const produit of commande.produits || []) {
      const key = produit.produitId || produit.id || produit.nom
      if (!map.has(key)) {
        map.set(key, {
          key,
          nom: produit.nom,
          unite: produit.unite || 'piece',
          quantiteTotale: 0,
          commandes: 0
        })
      }

      const item = map.get(key)
      item.quantiteTotale += Number(produit.quantite || 0)
      item.commandes += 1
    }
  }

  return [...map.values()].sort((left, right) => left.nom.localeCompare(right.nom))
})

const totalLignes = computed(() => commandesFiltrees.value.reduce((sum, commande) => sum + (commande.produits?.length || 0), 0))
const totalUnites = computed(() => pickList.value.reduce((sum, item) => sum + item.quantiteTotale, 0))
const totalMontant = computed(() => commandesFiltrees.value.reduce((sum, commande) => sum + getMontant(commande), 0))

async function changerStatut(commande, statut) {
  updatingId.value = commande.id
  apiError.value = ''
  successMessage.value = ''

  try {
    const updatedCommande = await commandesApi.updateStatus(commande.id, statut)
    const index = commandes.value.findIndex(item => item.id === updatedCommande.id)
    if (index !== -1) {
      commandes.value[index] = updatedCommande
    }
    successMessage.value = `Commande ${updatedCommande.numero} mise a jour.`
  } catch (error) {
    handleApiError(error)
  } finally {
    updatingId.value = null
  }
}

function exporterCSV() {
  const rows = []

  addCsvSection(rows, 'Terra Sana - Tableau de preparation')
  addCsvKeyValues(rows, [
    ['Genere le', formatCsvDateTime(new Date())],
    ['Commandes exportees', commandesFiltrees.value.length],
    ['Lignes a preparer', totalLignes.value],
    ['Unites totales', formatCsvQuantity(totalUnites.value)],
    ['Montant total TTC (EUR)', formatCsvMoney(totalMontant.value)]
  ])

  addCsvSection(rows, 'Filtres appliques')
  addCsvKeyValues(rows, [
    ['Point de collecte', filterPoint.value || 'Tous les points'],
    ['Date de retrait', filterDate.value ? formatCsvDate(filterDate.value) : 'Toutes les dates'],
    ['Creneau', filterCreneau.value || 'Tous les creneaux'],
    ['Statut', getStatutFilterLabel()]
  ])

  addCsvSection(rows, 'Pick-list globale')
  rows.push(['A cocher', 'Produit', 'Quantite totale', 'Unite', 'Commandes concernees'])
  for (const item of pickList.value) {
    rows.push(['[ ]', item.nom, formatCsvQuantity(item.quantiteTotale), item.unite, item.commandes])
  }

  addCsvSection(rows, 'Commandes concernees')
  rows.push([
    'No commande',
    'Membre',
    'Point de collecte',
    'Date retrait',
    'Creneau',
    'Statut',
    'Nb lignes',
    'Montant TTC (EUR)',
    'Produits'
  ])

  for (const commande of commandesFiltrees.value) {
    rows.push([
      commande.numero,
      getMembreNom(commande.membre),
      getPointNom(commande),
      formatCsvDate(commande.dateRetrait),
      getCreneauLabel(commande),
      statusLabel(commande.statut),
      (commande.produits || []).length,
      formatCsvMoney(getMontant(commande)),
      (commande.produits || [])
        .map(produit => `${produit.nom} x${formatCsvQuantity(produit.quantite)} ${produit.unite || ''}`.trim())
        .join(' | ')
    ])
  }

  addCsvSection(rows, 'Details a preparer par commande')
  rows.push([
    'A cocher',
    'No commande',
    'Membre',
    'Produit',
    'Quantite',
    'Unite',
    'Point de collecte',
    'Date retrait',
    'Creneau'
  ])

  for (const commande of commandesFiltrees.value) {
    for (const produit of commande.produits || []) {
      rows.push([
        '[ ]',
        commande.numero,
        getMembreNom(commande.membre),
        produit.nom,
        formatCsvQuantity(produit.quantite),
        produit.unite || '',
        getPointNom(commande),
        formatCsvDate(commande.dateRetrait),
        getCreneauLabel(commande)
      ])
    }
  }

  downloadCsv(`terra-sana-tableau-preparation-${exportDateStamp()}.csv`, rows)
}

function imprimer() {
  window.print()
}

function getMembreNom(membre) {
  if (!membre) return 'Membre'
  return `${membre.prenom || ''} ${membre.nom || ''}`.trim() || membre.email || 'Membre'
}

function getPointNom(commande) {
  return commande.pointCollecte?.nom || ''
}

function getDateKey(commande) {
  if (!commande.dateRetrait) return ''
  return String(commande.dateRetrait).slice(0, 10)
}

function getCreneauLabel(commande) {
  const creneau = commande.creneauRetrait
  if (!creneau) return ''
  if (creneau.horaire) return creneau.horaire
  const parts = [creneau.debut || creneau.heureDebut, creneau.fin || creneau.heureFin].filter(Boolean)
  return parts.join(' - ')
}

function getMontant(commande) {
  return Number(commande.montantTTC ?? commande.montant ?? 0)
}

function formatDateLabel(dateString) {
  if (!dateString) return ''
  const date = new Date(`${dateString}T00:00:00`)
  return date.toLocaleDateString('fr-BE', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  })
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  return date.toLocaleDateString('fr-BE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

function formatCurrency(value) {
  return new Intl.NumberFormat('fr-BE', {
    style: 'currency',
    currency: 'EUR'
  }).format(Number(value || 0))
}

function formatQuantite(value) {
  return Number(value || 0).toLocaleString('fr-BE', {
    minimumFractionDigits: Number.isInteger(Number(value || 0)) ? 0 : 2,
    maximumFractionDigits: 2
  })
}

function statusLabel(statut) {
  const map = {
    CONFIRMED: 'Confirmee',
    IN_PREP: 'En preparation',
    READY: 'Prete',
    DISTRIBUTED: 'Distribuee',
    CANCELLED: 'Annulee'
  }
  return map[statut] || statut
}

function getStatutFilterLabel() {
  if (filterStatut.value === 'ACTIVES') return 'Actives a preparer'
  return statusLabel(filterStatut.value)
}

function statusClass(statut) {
  const map = {
    CONFIRMED: 'info',
    IN_PREP: 'warning',
    READY: 'success',
    DISTRIBUTED: 'secondary',
    CANCELLED: 'danger'
  }
  return map[statut] || 'secondary'
}
</script>

<style scoped>
.preparation-page {
  display: grid;
  gap: 1.5rem;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.filters-card,
.stat-card,
.orders-panel,
.summary-panel {
  background: var(--surface, #ffffff);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.filters-card {
  padding: 1.25rem;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.form-group {
  display: grid;
  gap: 0.4rem;
}

.form-group label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #334155;
}

.form-group select {
  min-height: 44px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.stat-card {
  padding: 1rem 1.1rem;
  display: grid;
  gap: 0.35rem;
}

.stat-card.warning {
  background: rgba(251, 191, 36, 0.08);
}

.stat-label {
  font-size: 0.85rem;
  color: #64748b;
}

.stat-value {
  font-size: 1.5rem;
  color: #0f172a;
}

.empty-state {
  padding: 3rem 1.5rem;
  background: var(--surface, #ffffff);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  text-align: center;
}

.empty-state h3 {
  margin-bottom: 0.5rem;
}

.preparation-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.9fr);
  gap: 1.5rem;
  align-items: start;
}

.panel-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.panel-header h2 {
  margin: 0;
  font-size: 1.05rem;
}

.orders-list,
.pick-list {
  padding: 1rem;
  display: grid;
  gap: 1rem;
}

.order-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 12px;
  padding: 1rem;
  background: #fcfdfd;
}

.order-top {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
  margin-bottom: 0.75rem;
}

.order-top h3 {
  margin: 0 0 0.25rem;
  font-size: 1rem;
}

.order-top p {
  margin: 0;
  color: #64748b;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
}

.status-badge.info {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.status-badge.warning {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.status-badge.success {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.status-badge.secondary {
  background: rgba(148, 163, 184, 0.18);
  color: #334155;
}

.status-badge.danger {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  color: #475569;
  font-size: 0.88rem;
  margin-bottom: 0.9rem;
}

.order-lines {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 10px;
  overflow: hidden;
}

.order-lines-header,
.order-line {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 90px 80px;
  gap: 1rem;
  padding: 0.7rem 0.85rem;
}

.order-lines-header {
  background: #f8fafc;
  font-size: 0.78rem;
  font-weight: 700;
  color: #475569;
}

.order-line {
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  font-size: 0.9rem;
}

.order-actions {
  margin-top: 0.9rem;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.btn-small {
  min-height: 40px;
}

.action-note {
  font-size: 0.85rem;
  color: #64748b;
}

.pick-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 0.85rem 0.9rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 10px;
  background: #fcfdfd;
}

.pick-item p {
  margin: 0.25rem 0 0;
  color: #64748b;
  font-size: 0.85rem;
}

.pick-qty {
  text-align: right;
}

.pick-qty span {
  display: block;
  font-weight: 700;
  color: #0f172a;
}

.pick-qty small {
  color: #64748b;
}

@media (max-width: 1080px) {
  .filters-grid,
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .preparation-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .filters-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1 1 auto;
  }

  .order-top,
  .order-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .order-lines-header,
  .order-line {
    grid-template-columns: minmax(0, 1fr) 70px 70px;
    gap: 0.5rem;
  }
}
</style>
