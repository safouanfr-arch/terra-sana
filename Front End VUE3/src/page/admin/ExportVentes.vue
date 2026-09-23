<template>
  <div class="admin-page export-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link"><- Retour au dashboard</router-link>
        <h1>Export des ventes</h1>
        <p>Generez un export sur base des vraies commandes backend.</p>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <div class="export-layout">
      <section class="filters-panel">
        <h2>Configuration</h2>

        <div class="form-group">
          <label for="date-debut">Date de debut</label>
          <input id="date-debut" v-model="dateDebut" type="date" />
        </div>

        <div class="form-group">
          <label for="date-fin">Date de fin</label>
          <input id="date-fin" v-model="dateFin" type="date" />
        </div>

        <div class="form-group">
          <label for="point-collecte">Point de collecte</label>
          <select id="point-collecte" v-model="filterPoint">
            <option value="">Tous les points</option>
            <option v-for="point in pointsCollecte" :key="point" :value="point">{{ point }}</option>
          </select>
        </div>

        <div class="form-group">
          <label for="filter-statut">Statut</label>
          <select id="filter-statut" v-model="filterStatut">
            <option value="VALIDES">Toutes les commandes non annulees</option>
            <option value="CONFIRMED">Confirmees</option>
            <option value="IN_PREP">En preparation</option>
            <option value="READY">Pretes</option>
            <option value="DISTRIBUTED">Distribuees</option>
            <option value="CANCELLED">Annulees</option>
          </select>
        </div>

        <div class="form-group">
          <label for="format-export">Format</label>
          <select id="format-export" v-model="format">
            <option value="csv">CSV</option>
            <option value="json">JSON</option>
          </select>
        </div>

        <label class="checkbox-row">
          <input v-model="includeDetails" type="checkbox" />
          <span>Inclure le detail des commandes</span>
        </label>

        <div class="panel-actions">
          <button class="btn btn-primary" :disabled="loading || filteredCommandes.length === 0" @click="handleExport">
            {{ exporting ? 'Generation...' : 'Telecharger' }}
          </button>
        </div>
      </section>

      <section class="preview-panel">
        <div class="panel-header">
          <h2>Apercu</h2>
          <span>{{ filteredCommandes.length }} commande(s)</span>
        </div>

        <div v-if="loading" class="empty-state">
          <h3>Chargement des ventes</h3>
          <p>Les donnees reelles du backend sont en cours de chargement.</p>
        </div>

        <template v-else>
          <div class="preview-stats">
            <div class="preview-stat-card">
              <span class="stat-label">Commandes</span>
              <strong class="stat-value">{{ stats.nbCommandes }}</strong>
            </div>
            <div class="preview-stat-card">
              <span class="stat-label">Montant total</span>
              <strong class="stat-value">{{ formatCurrency(stats.montantTotal) }}</strong>
            </div>
            <div class="preview-stat-card">
              <span class="stat-label">Panier moyen</span>
              <strong class="stat-value">{{ formatCurrency(stats.panierMoyen) }}</strong>
            </div>
            <div class="preview-stat-card">
              <span class="stat-label">Clients actifs</span>
              <strong class="stat-value">{{ stats.nbClients }}</strong>
            </div>
          </div>

          <div class="info-card">
            <h3>Perimetre de l'export</h3>
            <ul>
              <li>Les chiffres proviennent des commandes admin backend.</li>
              <li>Par defaut, les commandes annulees sont exclues.</li>
              <li>Le point de collecte et la periode peuvent etre filtres avant export.</li>
            </ul>
          </div>

          <div v-if="filteredCommandes.length === 0" class="empty-state">
            <h3>Aucune commande pour ces filtres</h3>
            <p>Adaptez la periode ou le statut pour generer un export.</p>
          </div>

          <div v-else class="preview-sample">
            <h3>Exemple de contenu</h3>
            <pre class="csv-preview">{{ previewContent }}</pre>
          </div>
        </template>
      </section>
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
  formatCsvQuantity,
  toCsv
} from '../../utils/csvExport.js'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const exporting = ref(false)
const apiError = ref('')
const successMessage = ref('')

const format = ref('csv')
const includeDetails = ref(true)
const filterPoint = ref('')
const filterStatut = ref('VALIDES')
const dateDebut = ref('')
const dateFin = ref('')
const commandes = ref([])

onMounted(() => {
  loadCommandes()
})

async function loadCommandes() {
  loading.value = true
  apiError.value = ''

  try {
    commandes.value = await commandesApi.listAdmin()
    hydrateDefaultDateRange()
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

function hydrateDefaultDateRange() {
  const dates = commandes.value
    .map(getDateKey)
    .filter(Boolean)
    .sort()

  if (!dates.length) {
    return
  }

  if (!dateDebut.value) {
    dateDebut.value = dates[0]
  }

  if (!dateFin.value) {
    dateFin.value = dates[dates.length - 1]
  }
}

const pointsCollecte = computed(() => [...new Set(commandes.value.map(getPointNom).filter(Boolean))].sort())

const filteredCommandes = computed(() => {
  return commandes.value.filter(commande => {
    const dateKey = getDateKey(commande)
    if (dateDebut.value && dateKey && dateKey < dateDebut.value) {
      return false
    }
    if (dateFin.value && dateKey && dateKey > dateFin.value) {
      return false
    }

    if (filterPoint.value && getPointNom(commande) !== filterPoint.value) {
      return false
    }

    if (filterStatut.value === 'VALIDES') {
      if (commande.statut === 'CANCELLED') {
        return false
      }
    } else if (commande.statut !== filterStatut.value) {
      return false
    }

    return true
  })
})

const stats = computed(() => {
  const montantTotal = filteredCommandes.value.reduce((sum, commande) => sum + getMontant(commande), 0)
  const nbCommandes = filteredCommandes.value.length
  const emails = new Set(
    filteredCommandes.value
      .map(commande => commande.membre?.email)
      .filter(Boolean)
  )

  return {
    nbCommandes,
    montantTotal,
    panierMoyen: nbCommandes ? montantTotal / nbCommandes : 0,
    nbClients: emails.size
  }
})

const previewContent = computed(() => {
  if (format.value === 'json') {
    return JSON.stringify(buildExportPayload(), null, 2).slice(0, 1600)
  }

  return buildCsvContent().split('\n').slice(0, 10).join('\n')
})

function handleExport() {
  exporting.value = true
  apiError.value = ''
  successMessage.value = ''

  try {
    if (format.value === 'json') {
      downloadText(
        JSON.stringify(buildExportPayload(), null, 2),
        buildFileName('json'),
        'application/json;charset=utf-8;'
      )
    } else {
      downloadCsv(buildFileName('csv'), buildCsvRows())
    }

    successMessage.value = `Export ${format.value.toUpperCase()} genere avec succes.`
  } catch (error) {
    apiError.value = error.message || 'Impossible de generer le fichier.'
  } finally {
    exporting.value = false
  }
}

function buildExportPayload() {
  if (includeDetails.value) {
    return {
      periode: {
        dateDebut: dateDebut.value || null,
        dateFin: dateFin.value || null,
        pointCollecte: filterPoint.value || null,
        statut: filterStatut.value
      },
      stats: stats.value,
      commandes: filteredCommandes.value.map(commande => ({
        numero: commande.numero,
        dateRetrait: commande.dateRetrait,
        membre: getMembreNom(commande.membre),
        email: commande.membre?.email || '',
        pointCollecte: getPointNom(commande),
        statut: commande.statut,
        montantTTC: getMontant(commande),
        lignes: (commande.produits || []).map(produit => ({
          produit: produit.nom,
          quantite: produit.quantite,
          unite: produit.unite,
          prixUnitaire: produit.prixUnitaire,
          totalTTC: produit.totalTTC
        }))
      }))
    }
  }

  return {
    periode: {
      dateDebut: dateDebut.value || null,
      dateFin: dateFin.value || null,
      pointCollecte: filterPoint.value || null,
      statut: filterStatut.value
    },
    stats: stats.value,
    syntheseParJour: buildSummaryByDay()
  }
}

function buildCsvContent() {
  return toCsv(buildCsvRows())
}

function buildCsvRows() {
  const rows = []

  addCsvSection(rows, 'Terra Sana - Export des ventes')
  addCsvKeyValues(rows, [
    ['Genere le', formatCsvDateTime(new Date())],
    ['Commandes exportees', stats.value.nbCommandes],
    ['Clients distincts', stats.value.nbClients],
    ['Panier moyen (EUR)', formatCsvMoney(stats.value.panierMoyen)],
    ['Montant total TTC (EUR)', formatCsvMoney(stats.value.montantTotal)]
  ])

  addCsvSection(rows, 'Filtres appliques')
  addCsvKeyValues(rows, [
    ['Date de debut', dateDebut.value ? formatCsvDate(dateDebut.value) : 'Non limitee'],
    ['Date de fin', dateFin.value ? formatCsvDate(dateFin.value) : 'Non limitee'],
    ['Point de collecte', filterPoint.value || 'Tous les points'],
    ['Statut', getStatutFilterLabel()],
    ['Detail des commandes', includeDetails.value ? 'Oui' : 'Non']
  ])

  addCsvSection(rows, 'Synthese par jour')
  rows.push(['Date', 'Nombre de commandes', 'Montant total TTC (EUR)'])
  for (const item of buildSummaryByDay()) {
    rows.push([formatCsvDate(item.date), item.nbCommandes, formatCsvMoney(item.montantTotal)])
  }

  if (!includeDetails.value) {
    return rows
  }

  addCsvSection(rows, 'Commandes')
  rows.push([
    'No commande',
    'Date commande',
    'Date retrait',
    'Creneau',
    'Membre',
    'Email',
    'Point de collecte',
    'Statut',
    'Nb lignes',
    'Total HT (EUR)',
    'TVA (EUR)',
    'Total TTC (EUR)'
  ])

  for (const commande of filteredCommandes.value) {
    rows.push([
      commande.numero,
      formatCsvDateTime(commande.dateCommande || commande.date),
      formatCsvDate(commande.dateRetrait),
      getCreneauLabel(commande),
      getMembreNom(commande.membre),
      commande.membre?.email || '',
      getPointNom(commande),
      getStatutLabel(commande.statut),
      (commande.produits || []).length,
      formatCsvMoney(commande.totalHT),
      formatCsvMoney(commande.totalTVA),
      formatCsvMoney(getMontant(commande))
    ])
  }

  addCsvSection(rows, 'Details des lignes de commande')
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
        getMembreNom(commande.membre),
        produit.nom,
        formatCsvQuantity(produit.quantite),
        produit.unite || '',
        formatCsvMoney(produit.prixUnitaire),
        produit.tauxTVA ?? '',
        formatCsvMoney(getProduitTotal(produit))
      ])
    }
  }

  return rows
}

function buildSummaryByDay() {
  const byDay = new Map()

  for (const commande of filteredCommandes.value) {
    const date = getDateKey(commande) || 'Sans date'
    if (!byDay.has(date)) {
      byDay.set(date, {
        date,
        nbCommandes: 0,
        montantTotal: 0
      })
    }

    const item = byDay.get(date)
    item.nbCommandes += 1
    item.montantTotal += getMontant(commande)
  }

  return [...byDay.values()].sort((left, right) => left.date.localeCompare(right.date))
}

function downloadText(content, fileName, mimeType) {
  const blob = new Blob(['\ufeff' + content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function buildFileName(extension) {
  return `terra-sana-export-ventes-${exportDateStamp()}.${extension}`
}

function getPointNom(commande) {
  return commande.pointCollecte?.nom || ''
}

function getDateKey(commande) {
  const source = commande.dateRetrait || commande.dateCommande
  return source ? String(source).slice(0, 10) : ''
}

function getMembreNom(membre) {
  if (!membre) return 'Membre'
  return `${membre.prenom || ''} ${membre.nom || ''}`.trim() || membre.email || 'Membre'
}

function getMontant(commande) {
  return Number(commande.montantTTC ?? commande.montant ?? 0)
}

function formatCurrency(value) {
  return new Intl.NumberFormat('fr-BE', {
    style: 'currency',
    currency: 'EUR'
  }).format(Number(value || 0))
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

function getStatutLabel(statut) {
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
  if (filterStatut.value === 'VALIDES') return 'Toutes les commandes non annulees'
  return getStatutLabel(filterStatut.value)
}
</script>

<style scoped>
.export-page {
  display: grid;
  gap: 1.5rem;
}

.export-layout {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
}

.filters-panel,
.preview-panel {
  background: var(--surface, #ffffff);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.filters-panel {
  padding: 1.25rem;
  display: grid;
  gap: 1rem;
}

.filters-panel h2,
.preview-panel h2,
.preview-panel h3 {
  margin: 0;
}

.form-group {
  display: grid;
  gap: 0.45rem;
}

.form-group label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #334155;
}

.checkbox-row {
  display: flex;
  gap: 0.75rem;
  align-items: start;
  color: #334155;
}

.checkbox-row input {
  margin-top: 0.2rem;
}

.panel-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.panel-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.preview-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  padding: 1.25rem;
}

.preview-stat-card,
.info-card,
.preview-sample {
  margin: 0 1.25rem 1.25rem;
  padding: 1rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 12px;
  background: #fcfdfd;
}

.stat-label {
  display: block;
  font-size: 0.82rem;
  color: #64748b;
  margin-bottom: 0.4rem;
}

.stat-value {
  font-size: 1.35rem;
  color: #0f172a;
}

.info-card ul {
  margin: 0.75rem 0 0;
  padding-left: 1.1rem;
  color: #475569;
}

.empty-state {
  margin: 1.25rem;
  padding: 2rem 1.25rem;
  border: 1px dashed rgba(15, 23, 42, 0.14);
  border-radius: 12px;
  text-align: center;
}

.csv-preview {
  margin: 0.85rem 0 0;
  padding: 0.9rem;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  font-size: 0.82rem;
  overflow-x: auto;
  white-space: pre-wrap;
}

@media (max-width: 960px) {
  .export-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .preview-stats {
    grid-template-columns: 1fr;
  }

  .panel-actions .btn {
    flex: 1 1 auto;
  }
}
</style>
