<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au tableau de bord</router-link>
        <h1>Gestion des commandes</h1>
        <p>Suivi opérationnel, historique, détails et export des commandes.</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary" @click="resetFilters">
          Réinitialiser les filtres
        </button>
        <button class="btn btn-primary" :disabled="filteredCommandes.length === 0" @click="exporterCSV">
          Exporter CSV
        </button>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <section class="withdrawal-section">
      <div class="withdrawal-header">
        <div>
          <h2>Retrait d'une commande</h2>
          <p>Recherchez la commande remise au membre avec son code de retrait.</p>
        </div>
        <form class="withdrawal-form" @submit.prevent="rechercherRetrait">
          <label for="withdrawal-code">Code de retrait</label>
          <div class="withdrawal-input-row">
            <input
              id="withdrawal-code"
              v-model="retraitCode"
              type="text"
              maxlength="20"
              autocomplete="off"
              placeholder="ABC123"
            />
            <button class="btn btn-secondary" type="submit" :disabled="retraitLoading || !retraitCode.trim()">
              {{ retraitLoading ? 'Recherche...' : 'Rechercher' }}
            </button>
          </div>
        </form>
      </div>

      <div v-if="retraitCommande" class="withdrawal-result">
        <div class="withdrawal-summary">
          <div><span>Commande</span><strong>{{ retraitCommande.numeroCommande }}</strong></div>
          <div><span>Code de retrait</span><strong>{{ retraitCommande.codeRetrait }}</strong></div>
          <div><span>Membre</span><strong>{{ getRetraitMembreNom(retraitCommande) }}</strong></div>
          <div><span>Point</span><strong>{{ retraitCommande.pointCollecte || 'N/A' }}</strong></div>
          <div>
            <span>Date et créneau de retrait</span>
            <strong>{{ formatDate(retraitCommande.dateRetrait) }} - {{ formatRetraitHoraire(retraitCommande) }}</strong>
          </div>
          <div><span>Montant</span><strong>{{ Number(retraitCommande.montantTotal || 0).toFixed(2) }} EUR</strong></div>
          <div><span>État</span><strong>{{ getStatutLabel(retraitCommande.statut) }}</strong></div>
          <div><span>Paiement</span><strong>{{ retraitCommande.paye ? 'Payé' : 'En attente' }}</strong></div>
          <div v-if="retraitCommande.datePaiement">
            <span>Date du paiement</span>
            <strong>{{ formatDateTime(retraitCommande.datePaiement) }}</strong>
          </div>
        </div>
        <button
          v-if="retraitCommande.statut === 'READY'"
          class="btn btn-primary"
          :disabled="retraitConfirming"
          @click="confirmerRetrait"
        >
          {{ retraitConfirming ? 'Confirmation...' : 'Confirmer le retrait et le paiement' }}
        </button>
        <div v-else-if="retraitCommande.statut === 'DISTRIBUTED'" class="withdrawal-confirmed" role="status">
          <strong>Retrait confirmé</strong>
          <span>La commande est payée et la vente a été enregistrée.</span>
        </div>
      </div>
    </section>

    <div class="filters-advanced">
      <div class="filter-row">
        <div class="filter-group">
          <label>Rechercher</label>
          <input
            v-model="searchTerm"
            type="text"
            placeholder="Membre, email, numéro, produit..."
            class="filter-input"
          />
        </div>

        <div class="filter-group">
          <label>Statut</label>
          <select v-model="filterStatut" class="filter-select">
            <option value="tous">Tous les statuts</option>
            <option value="terminees">Commandes clôturées</option>
            <option value="CONFIRMED">Confirmées</option>
            <option value="IN_PREP">En préparation</option>
            <option value="READY">Prêtes</option>
            <option value="DISTRIBUTED">Retirées</option>
            <option value="CANCELLED">Annulées</option>
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
          <label>Date de début</label>
          <input v-model="filterDateDebut" type="date" class="filter-input" />
        </div>

        <div class="filter-group">
          <label>Date de fin</label>
          <input v-model="filterDateFin" type="date" class="filter-input" />
        </div>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table commandes-table">
        <thead>
          <tr>
            <th>N° commande</th>
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
              <span class="status-badge" :class="`status-${String(commande.statut).toLowerCase()}`">
                {{ getStatutLabel(commande.statut) }}
              </span>
            </td>
            <td class="actions-cell">
              <button class="btn-icon" @click="openDetailsModal(commande)" title="Voir les détails">Voir</button>
              <button
                v-if="canSendReminder(commande)"
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
            <td colspan="6" class="table-total-label">Total affiché</td>
            <td><strong>{{ totalMontantFiltre.toFixed(2) }} EUR</strong></td>
            <td colspan="2"></td>
          </tr>
        </tfoot>
      </table>
    </div>

    <div v-if="filteredCommandes.length === 0" class="empty-state">
      <div class="empty-icon">0</div>
      <h3>Aucune commande trouvée</h3>
      <p>Essayez de modifier vos filtres.</p>
    </div>

    <div v-if="showDetailsModal && commandeDetails" class="modal-overlay" @click.self="closeDetailsModal">
      <div class="modal-content modal-large">
        <div class="modal-header">
          <h2>Détails de la commande {{ commandeDetails.numero }}</h2>
          <button class="btn-close" aria-label="Fermer" @click="closeDetailsModal">x</button>
        </div>

        <div class="modal-body">
          <div class="detail-section">
            <h3>Informations générales</h3>
            <div class="detail-grid">
              <div class="detail-item"><span class="detail-label">Commande</span><span class="detail-value">{{ commandeDetails.numero }}</span></div>
              <div class="detail-item"><span class="detail-label">Date</span><span class="detail-value">{{ formatDateTime(commandeDetails.dateCommande || commandeDetails.date) }}</span></div>
              <div class="detail-item"><span class="detail-label">Membre</span><span class="detail-value">{{ getMembreNom(commandeDetails) }}</span></div>
              <div class="detail-item"><span class="detail-label">Email</span><span class="detail-value">{{ getMembreEmail(commandeDetails) }}</span></div>
              <div class="detail-item"><span class="detail-label">Statut</span><span class="detail-value">{{ getStatutLabel(commandeDetails.statut) }}</span></div>
              <div class="detail-item"><span class="detail-label">Code retrait</span><span class="detail-value">{{ commandeDetails.codeRetrait || 'N/A' }}</span></div>
              <div class="detail-item"><span class="detail-label">Paiement</span><span class="detail-value">{{ commandeDetails.paye ? 'Payé' : 'En attente' }}</span></div>
              <div class="detail-item"><span class="detail-label">Date du paiement</span><span class="detail-value">{{ formatDateTime(commandeDetails.datePaiement) }}</span></div>
            </div>
          </div>

          <div class="detail-section">
            <h3>Retrait</h3>
            <div class="detail-grid">
              <div class="detail-item"><span class="detail-label">Point</span><span class="detail-value">{{ commandeDetails.pointCollecte?.nom || 'N/A' }}</span></div>
              <div class="detail-item"><span class="detail-label">Adresse</span><span class="detail-value">{{ commandeDetails.pointCollecte?.adresse || 'N/A' }}</span></div>
              <div class="detail-item"><span class="detail-label">Date de retrait prévue</span><span class="detail-value">{{ formatDate(commandeDetails.dateRetrait) }}</span></div>
              <div class="detail-item"><span class="detail-label">Créneau de retrait</span><span class="detail-value">{{ commandeDetails.creneauRetrait?.horaire || 'N/A' }}</span></div>
            </div>
          </div>

          <div class="detail-section">
            <h3>Produits</h3>
            <table class="detail-table">
              <thead>
                <tr>
                  <th>Produit</th>
                  <th>Quantité</th>
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
import { commandesApi, emailNotificationsApi, getApiErrorMessage, handleApiAccessError, retraitsApi } from '../../services/api.js'
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
const retraitCode = ref('')
const retraitCommande = ref(null)
const retraitLoading = ref(false)
const retraitConfirming = ref(false)

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
    CONFIRMED: 'Confirmée',
    IN_PREP: 'En préparation',
    READY: 'Prête',
    DISTRIBUTED: 'Retirée',
    CANCELLED: 'Annulée'
  }
  return labels[statut] || statut
}

function canSendReminder(commande) {
  return !['CANCELLED', 'DISTRIBUTED'].includes(commande?.statut)
}

function openDetailsModal(commande) {
  commandeDetails.value = commande
  showDetailsModal.value = true
}

function closeDetailsModal() {
  showDetailsModal.value = false
  commandeDetails.value = null
}

async function rechercherRetrait() {
  const code = retraitCode.value.trim().toUpperCase()
  if (!code) return

  retraitLoading.value = true
  apiError.value = ''
  successMessage.value = ''
  try {
    retraitCode.value = code
    retraitCommande.value = await retraitsApi.search(code)
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    retraitCommande.value = null
    apiError.value = getApiErrorMessage(error)
  } finally {
    retraitLoading.value = false
  }
}

async function confirmerRetrait() {
  if (!retraitCommande.value || retraitCommande.value.statut !== 'READY') return
  if (!window.confirm(`Confirmer le retrait de la commande ${retraitCommande.value.numeroCommande} ?`)) return

  retraitConfirming.value = true
  apiError.value = ''
  successMessage.value = ''
  try {
    retraitCommande.value = await retraitsApi.confirm(retraitCode.value)
    await loadCommandes()
    successMessage.value = `La commande ${retraitCommande.value.numeroCommande} a été retirée et marquée comme payée.`
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    apiError.value = getApiErrorMessage(error)
  } finally {
    retraitConfirming.value = false
  }
}

function getRetraitMembreNom(retrait) {
  return `${retrait.membrePrenom || ''} ${retrait.membreNom || ''}`.trim() || 'Membre'
}

function formatRetraitHoraire(retrait) {
  const debut = String(retrait.heureDebut || '').slice(0, 5)
  const fin = String(retrait.heureFin || '').slice(0, 5)
  return [debut, fin].filter(Boolean).join(' - ') || 'N/A'
}

async function sendReminder(commande) {
  apiError.value = ''
  successMessage.value = ''
  if (!canSendReminder(commande)) {
    apiError.value = "Impossible d'envoyer un rappel pour une commande annulée ou retirée."
    return
  }
  try {
    const email = await emailNotificationsApi.sendOrderReminder(commande.id)
    successMessage.value = email.statutEnvoi === 'SENT'
      ? `Le rappel J-1 pour ${commande.numero} a été envoyé.`
      : `Le rappel J-1 pour ${commande.numero} a été enregistré mais l'envoi a échoué.`
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
    ['Généré le', formatCsvDateTime(new Date())],
    ['Commandes exportees', filteredCommandes.value.length],
    ['Montant total TTC (EUR)', formatCsvMoney(totalMontantFiltre.value)]
  ])

  addCsvSection(rows, 'Filtres appliques')
  addCsvKeyValues(rows, [
    ['Recherche', searchTerm.value.trim() || 'Toutes'],
    ['Statut', getStatutFilterLabel()],
    ['Membre', filterMembre.value || 'Tous les membres'],
    ['Point de collecte', filterPoint.value || 'Tous les points'],
    ['Date de début', filterDateDebut.value ? formatCsvDate(filterDateDebut.value) : 'Non limitée'],
    ['Date de fin', filterDateFin.value ? formatCsvDate(filterDateFin.value) : 'Non limitée']
  ])

  addCsvSection(rows, 'Synthese par statut')
  rows.push(['Statut', 'Nombre de commandes', 'Montant TTC (EUR)'])
  for (const item of buildStatutSummary()) {
    rows.push([item.statut, item.count, formatCsvMoney(item.total)])
  }

  addCsvSection(rows, 'Liste des commandes')
  rows.push([
    'N° commande',
    'Date commande',
    'Membre',
    'Email',
    'Point de collecte',
    'Date retrait',
    'Créneau',
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

  rows.push(['', '', '', '', '', '', '', '', '', 'Total affiché', '', '', formatCsvMoney(totalMontantFiltre.value)])

  addCsvSection(rows, 'Détails des produits commandés')
  rows.push([
    'N° commande',
    'Membre',
    'Produit',
    'Quantité',
    'Unité',
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
  if (filterStatut.value === 'terminees') return 'Commandes clôturées'
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

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  font-weight: 600;
  border-radius: 6px;
  padding: 0.25rem 0.55rem;
  background: #f5f5f5;
  border: 1px solid #ccc;
  color: #333;
}

.status-confirmed { background: #eff6ff; border-color: #bfdbfe; color: #1d4ed8; }
.status-in_prep { background: #fff7ed; border-color: #fed7aa; color: #b45309; }
.status-ready { background: #f0fdf4; border-color: #bbf7d0; color: #15803d; }
.status-distributed { background: #ecfdf5; border-color: #a7f3d0; color: #047857; }
.status-cancelled { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }

.withdrawal-section {
  display: grid;
  gap: 1rem;
  padding: 1.25rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 8px;
  background: #ffffff;
}

.withdrawal-header {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 1.5rem;
}

.withdrawal-header h2,
.withdrawal-header p {
  margin: 0;
}

.withdrawal-header p {
  margin-top: 0.3rem;
  color: var(--text-secondary);
}

.withdrawal-form {
  display: grid;
  gap: 0.4rem;
  min-width: min(100%, 390px);
}

.withdrawal-form label {
  font-size: 0.86rem;
  font-weight: 700;
}

.withdrawal-input-row {
  display: flex;
  gap: 0.65rem;
}

.withdrawal-input-row input {
  min-width: 0;
  flex: 1;
  text-transform: uppercase;
}

.withdrawal-result {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(15, 23, 42, 0.1);
}

.withdrawal-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem 1.25rem;
  flex: 1;
}

.withdrawal-summary div {
  display: grid;
  gap: 0.2rem;
}

.withdrawal-summary span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.withdrawal-confirmed {
  display: grid;
  gap: 0.25rem;
  max-width: 290px;
  padding: 0.75rem 0.9rem;
  border: 1px solid #86c79e;
  border-radius: 6px;
  background: #edf8f1;
  color: #175c35;
}

.withdrawal-confirmed span {
  font-size: 0.86rem;
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

  .withdrawal-header,
  .withdrawal-result,
  .withdrawal-input-row {
    flex-direction: column;
    align-items: stretch;
  }

  .withdrawal-form {
    min-width: 0;
  }

  .withdrawal-summary {
    grid-template-columns: 1fr;
  }
}
</style>
