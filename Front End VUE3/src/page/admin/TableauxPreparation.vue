<template>
  <div class="admin-page preparation-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">← Retour au tableau de bord</router-link>
        <h1>Tableaux de préparation</h1>
        <p>Suivez les commandes à préparer à partir des données enregistrées.</p>
      </div>

      <div class="header-actions">
        <button class="btn btn-secondary" :disabled="commandesFiltrees.length === 0" @click="exporterCSV">
          Exporter les données CSV
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
          <label for="filter-creneau">Créneau</label>
          <select id="filter-creneau" v-model="filterCreneau">
            <option value="">Tous les créneaux</option>
            <option v-for="creneau in creneauxUniques" :key="creneau" :value="creneau">{{ creneau }}</option>
          </select>
        </div>

        <div class="form-group">
          <label for="filter-statut">Statut</label>
          <select id="filter-statut" v-model="filterStatut">
            <option value="ACTIVES">Actives à préparer</option>
            <option value="CONFIRMED">Confirmées</option>
            <option value="IN_PREP">En préparation</option>
            <option value="READY">Prêtes</option>
            <option value="DISTRIBUTED">Retirées</option>
            <option value="CANCELLED">Annulées</option>
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
        <span class="stat-label">Lignes à préparer</span>
        <strong class="stat-value">{{ totalLignes }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">Unités totales</span>
        <strong class="stat-value">{{ totalUnites }}</strong>
      </div>
      <div class="stat-card warning">
        <span class="stat-label">Montant total</span>
        <strong class="stat-value">{{ formatCurrency(totalMontant) }}</strong>
      </div>
    </div>

    <div v-if="loading" class="empty-state">
      <h3>Chargement des commandes</h3>
      <p>Le tableau de préparation se synchronise avec les commandes.</p>
    </div>

    <div v-else-if="commandesFiltrees.length === 0" class="empty-state">
      <h3>Aucune commande pour cette sélection</h3>
      <p>Essayez un autre point, une autre date ou un autre statut.</p>
    </div>

    <div v-else class="preparation-groups">
      <section v-for="groupe in groupesPreparation" :key="groupe.creneauId" class="preparation-group">
        <div class="group-header">
          <div>
            <span class="group-kicker">Point de collecte</span>
            <h2>{{ groupe.pointNom }}</h2>
            <p>{{ formatDateLabel(groupe.date) }} - {{ groupe.creneauLabel }}</p>
          </div>
          <div class="group-actions">
            <div class="group-counts">
              <strong>{{ groupe.commandes.length }} commande(s)</strong>
              <span>{{ groupe.sansIncident }} sans incident</span>
              <span v-if="groupe.avecIncident > 0" class="incident-count">
                {{ groupe.avecIncident }} avec incident
              </span>
            </div>
            <div class="preparation-email-action">
              <span>Dossier préparation :</span>
              <strong :class="emailStatusClass(groupe)">{{ emailStatusLabel(groupe) }}</strong>
              <button
                class="btn btn-secondary btn-small"
                :disabled="!canSendPreparationEmail(groupe) || emailingGroupId === groupe.creneauId"
                @click="envoyerDossierEmail(groupe)"
              >
                {{ emailingGroupId === groupe.creneauId ? 'Envoi...' : emailActionLabel(groupe) }}
              </button>
            </div>
            <button
              class="btn btn-secondary"
              :disabled="groupe.commandesPdf === 0 || downloadingGroupId === groupe.creneauId"
              :title="groupe.commandesPdf === 0 ? 'Aucune commande en préparation ou prête' : ''"
              @click="telechargerPdf(groupe)"
            >
              {{ downloadingGroupId === groupe.creneauId ? 'Génération...' : 'Télécharger le dossier PDF' }}
            </button>
            <button
              v-if="groupe.commandesEligibles > 0"
              class="btn btn-primary"
              :disabled="finalizingGroupId === groupe.creneauId"
              @click="finaliserGroupe(groupe)"
            >
              {{ finalizingGroupId === groupe.creneauId ? 'Finalisation...' : 'Finaliser la préparation' }}
            </button>
          </div>
        </div>

        <div class="group-content">
          <div class="orders-panel">
            <div class="panel-header">
              <h3>Commandes</h3>
              <span>{{ groupe.commandes.length }} résultat(s)</span>
            </div>

            <div class="orders-list">
              <article v-for="commande in groupe.commandes" :key="commande.id" class="order-card">
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

            <div v-if="commande.incidentPreparation" class="preparation-incident">
              <strong>Incident de préparation</strong>
              <span>{{ commande.motifIncidentPreparation }}</span>
            </div>

            <div class="order-lines">
              <div class="order-lines-header">
                <span>Produit</span>
                <span>Quantité</span>
                <span>Unité</span>
              </div>
              <div v-for="produit in commande.produits" :key="produit.ligneId || `${commande.id}-${produit.produitId}`" class="order-line">
                <span>{{ produit.nom }}</span>
                <span>{{ formatQuantite(produit.quantite) }}</span>
                <span>{{ produit.unite }}</span>
              </div>
            </div>

                <div class="order-actions">
                  <button
                    v-if="commande.statut === 'IN_PREP' && !commande.incidentPreparation"
                    class="btn btn-secondary btn-small"
                    :disabled="updatingId === commande.id"
                    @click="signalerIncident(commande)"
                  >
                    Signaler un incident
                  </button>
                  <button
                    v-if="commande.incidentPreparation"
                    class="btn btn-secondary btn-small"
                    :disabled="updatingId === commande.id"
                    @click="resoudreIncident(commande)"
                  >
                    Résoudre l'incident
                  </button>
                </div>
              </article>
            </div>
          </div>

          <aside class="summary-panel">
            <div class="panel-header">
              <h3>Pick-list du créneau</h3>
              <span>{{ groupe.pickList.length }} produit(s)</span>
            </div>

            <div class="pick-list">
              <div v-for="item in groupe.pickList" :key="item.key" class="pick-item">
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
  formatCsvQuantity
} from '../../utils/csvExport.js'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const apiError = ref('')
const successMessage = ref('')
const updatingId = ref(null)
const finalizingGroupId = ref(null)
const downloadingGroupId = ref(null)
const emailingGroupId = ref(null)
const commandes = ref([])
const preparationEmailStatuses = ref({})

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
    await loadPreparationEmailStatuses()
  } catch (error) {
    handleApiError(error)
  } finally {
    loading.value = false
  }
}

async function loadPreparationEmailStatuses() {
  const creneauIds = [...new Set(
    commandes.value
      .map(commande => commande.creneauRetrait?.id)
      .filter(Boolean)
  )]
  try {
    const entries = await Promise.all(creneauIds.map(async creneauId => [
      creneauId,
      await commandesApi.getPreparationEmailStatus(creneauId)
    ]))
    preparationEmailStatuses.value = Object.fromEntries(entries)
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

function buildPickList(commandesDuGroupe) {
  const map = new Map()

  for (const commande of commandesDuGroupe) {
    for (const produit of commande.produits || []) {
      const key = produit.produitId || produit.id || produit.nom
      if (!map.has(key)) {
        map.set(key, {
          key,
          nom: produit.nom,
          unite: produit.unite || 'piece',
          quantiteTotale: 0,
          commandeIds: new Set()
        })
      }

      const item = map.get(key)
      item.quantiteTotale += Number(produit.quantite || 0)
      item.commandeIds.add(commande.id)
    }
  }

  return [...map.values()]
    .map(item => ({
      key: item.key,
      nom: item.nom,
      unite: item.unite,
      quantiteTotale: item.quantiteTotale,
      commandes: item.commandeIds.size
    }))
    .sort((left, right) => left.nom.localeCompare(right.nom))
}

const groupesPreparation = computed(() => {
  const groupes = new Map()

  for (const commande of commandesFiltrees.value) {
    const creneauId = commande.creneauRetrait?.id
    if (!creneauId) continue

    if (!groupes.has(creneauId)) {
      groupes.set(creneauId, {
        creneauId,
        pointNom: getPointNom(commande),
        date: commande.creneauRetrait?.date || getDateKey(commande),
        creneauLabel: getCreneauLabel(commande),
        commandes: []
      })
    }
    groupes.get(creneauId).commandes.push(commande)
  }

  return [...groupes.values()]
    .map(groupe => ({
      ...groupe,
      pickList: buildPickList(groupe.commandes),
      avecIncident: groupe.commandes.filter(commande => commande.incidentPreparation).length,
      sansIncident: groupe.commandes.filter(commande => !commande.incidentPreparation).length,
      commandesPdf: groupe.commandes.filter(
        commande => ['IN_PREP', 'READY'].includes(commande.statut)
      ).length,
      commandesEligibles: groupe.commandes.filter(
        commande => commande.statut === 'IN_PREP' && !commande.incidentPreparation
      ).length
    }))
    .sort((left, right) => {
      const dateComparison = String(left.date).localeCompare(String(right.date))
      if (dateComparison !== 0) return dateComparison
      return left.creneauLabel.localeCompare(right.creneauLabel)
    })
})

const totalLignes = computed(() => commandesFiltrees.value.reduce((sum, commande) => sum + (commande.produits?.length || 0), 0))
const totalUnites = computed(() => groupesPreparation.value.reduce(
  (sum, groupe) => sum + groupe.pickList.reduce((groupSum, item) => groupSum + item.quantiteTotale, 0),
  0
))
const totalMontant = computed(() => commandesFiltrees.value.reduce((sum, commande) => sum + getMontant(commande), 0))

async function finaliserGroupe(groupe) {
  if (!window.confirm(`Finaliser la préparation de ${groupe.commandesEligibles} commande(s) pour ce créneau ?`)) {
    return
  }

  finalizingGroupId.value = groupe.creneauId
  apiError.value = ''
  successMessage.value = ''

  try {
    const result = await commandesApi.finalizePreparationGroup(groupe.creneauId)
    await loadCommandes()
    successMessage.value = `${result.commandesFinalisees} commande(s) marquée(s) comme prête(s), `
      + `${result.commandesExcluesIncident} exclue(s) pour incident.`
  } catch (error) {
    handleApiError(error)
  } finally {
    finalizingGroupId.value = null
  }
}

async function telechargerPdf(groupe) {
  downloadingGroupId.value = groupe.creneauId
  apiError.value = ''
  successMessage.value = ''

  try {
    const { blob, fileName } = await commandesApi.downloadPreparationPdf(groupe.creneauId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    successMessage.value = `Le dossier PDF du créneau ${groupe.creneauLabel} a été généré.`
  } catch (error) {
    handleApiError(error)
  } finally {
    downloadingGroupId.value = null
  }
}

async function envoyerDossierEmail(groupe) {
  const action = emailActionLabel(groupe)
  if (!window.confirm(`${action} pour le créneau ${groupe.creneauLabel} ?`)) {
    return
  }

  emailingGroupId.value = groupe.creneauId
  apiError.value = ''
  successMessage.value = ''
  try {
    const status = await commandesApi.sendPreparationEmail(groupe.creneauId)
    preparationEmailStatuses.value = {
      ...preparationEmailStatuses.value,
      [groupe.creneauId]: status
    }
    successMessage.value = status.statutEnvoi === 'SENT'
      ? `Le dossier du créneau ${groupe.creneauLabel} a été envoyé.`
      : `La tentative d'envoi du dossier ${groupe.creneauLabel} a échoué.`
  } catch (error) {
    handleApiError(error)
  } finally {
    emailingGroupId.value = null
  }
}

function getPreparationEmailStatus(groupe) {
  return preparationEmailStatuses.value[groupe.creneauId] || null
}

function emailStatusLabel(groupe) {
  const status = getPreparationEmailStatus(groupe)
  if (status && status.envoiConfigure === false) return 'Envoi non configuré'
  return {
    PENDING: 'Envoi en cours',
    SENT: 'Envoyé',
    FAILED: 'Échec'
  }[status?.statutEnvoi] || 'Non envoyé'
}

function emailActionLabel(groupe) {
  const statut = getPreparationEmailStatus(groupe)?.statutEnvoi
  if (statut === 'SENT') return 'Renvoyer'
  if (statut === 'FAILED') return "Réessayer l'envoi"
  return 'Envoyer'
}

function emailStatusClass(groupe) {
  const statut = getPreparationEmailStatus(groupe)?.statutEnvoi
  return {
    PENDING: 'email-status-pending',
    SENT: 'email-status-sent',
    FAILED: 'email-status-failed'
  }[statut] || 'email-status-none'
}

function canSendPreparationEmail(groupe) {
  const status = getPreparationEmailStatus(groupe)
  return groupe.commandesPdf > 0
    && status?.envoiConfigure !== false
    && status?.statutEnvoi !== 'PENDING'
}

async function signalerIncident(commande) {
  const motif = window.prompt("Motif de l'incident de préparation :")
  if (motif === null) return
  if (!motif.trim()) {
    apiError.value = "Le motif de l'incident est obligatoire."
    return
  }
  if (motif.trim().length > 500) {
    apiError.value = "Le motif de l'incident ne peut pas dépasser 500 caractères."
    return
  }

  updatingId.value = commande.id
  apiError.value = ''
  successMessage.value = ''
  try {
    const updatedCommande = await commandesApi.reportPreparationIncident(commande.id, motif.trim())
    replaceCommande(updatedCommande)
    successMessage.value = `Incident signalé pour la commande ${updatedCommande.numero}.`
  } catch (error) {
    handleApiError(error)
  } finally {
    updatingId.value = null
  }
}

async function resoudreIncident(commande) {
  if (!window.confirm(`Résoudre l'incident de la commande ${commande.numero} ?`)) return

  updatingId.value = commande.id
  apiError.value = ''
  successMessage.value = ''
  try {
    const updatedCommande = await commandesApi.resolvePreparationIncident(commande.id)
    replaceCommande(updatedCommande)
    successMessage.value = `Incident résolu pour la commande ${updatedCommande.numero}.`
  } catch (error) {
    handleApiError(error)
  } finally {
    updatingId.value = null
  }
}

function replaceCommande(updatedCommande) {
  const index = commandes.value.findIndex(item => item.id === updatedCommande.id)
  if (index !== -1) {
    commandes.value[index] = updatedCommande
  }
}

function exporterCSV() {
  const rows = []

  addCsvSection(rows, 'Terra Sana - Tableau de préparation')
  addCsvKeyValues(rows, [
    ['Généré le', formatCsvDateTime(new Date())],
    ['Commandes exportées', commandesFiltrees.value.length],
    ['Lignes à préparer', totalLignes.value],
    ['Unités totales', formatCsvQuantity(totalUnites.value)],
    ['Montant total TTC (EUR)', formatCsvMoney(totalMontant.value)]
  ])

  addCsvSection(rows, 'Filtres appliqués')
  addCsvKeyValues(rows, [
    ['Point de collecte', filterPoint.value || 'Tous les points'],
    ['Date de retrait', filterDate.value ? formatCsvDate(filterDate.value) : 'Toutes les dates'],
    ['Créneau', filterCreneau.value || 'Tous les créneaux'],
    ['Statut', getStatutFilterLabel()]
  ])

  for (const groupe of groupesPreparation.value) {
    addCsvSection(rows, `Pick-list - ${groupe.pointNom} - ${formatCsvDate(groupe.date)} - ${groupe.creneauLabel}`)
    addCsvKeyValues(rows, [
      ['Commandes', groupe.commandes.length],
      ['Sans incident', groupe.sansIncident],
      ['Avec incident', groupe.avecIncident]
    ])
    rows.push(['À cocher', 'Produit', 'Quantité totale', 'Unité', 'Commandes concernées'])
    for (const item of groupe.pickList) {
      rows.push(['[ ]', item.nom, formatCsvQuantity(item.quantiteTotale), item.unite, item.commandes])
    }
  }

  addCsvSection(rows, 'Commandes concernées')
  rows.push([
    'N° commande',
    'Membre',
    'Point de collecte',
    'Date retrait',
    'Créneau',
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

  addCsvSection(rows, 'Détails à préparer par commande')
  rows.push([
    'À cocher',
    'N° commande',
    'Membre',
    'Produit',
    'Quantité',
    'Unité',
    'Point de collecte',
    'Date retrait',
    'Créneau'
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
    CONFIRMED: 'Confirmée',
    IN_PREP: 'En préparation',
    READY: 'Prête',
    DISTRIBUTED: 'Retirée',
    CANCELLED: 'Annulée'
  }
  return map[statut] || statut
}

function getStatutFilterLabel() {
  if (filterStatut.value === 'ACTIVES') return 'Actives à préparer'
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

.preparation-groups {
  display: grid;
  gap: 2rem;
}

.preparation-group {
  display: grid;
  gap: 1rem;
  padding-top: 1.5rem;
  border-top: 2px solid rgba(47, 93, 80, 0.22);
}

.group-header,
.group-actions,
.group-counts {
  display: flex;
  gap: 1rem;
}

.preparation-email-action {
  display: grid;
  gap: 0.2rem;
  min-width: 150px;
  font-size: 0.82rem;
}

.preparation-email-action > span {
  color: #64748b;
}

.email-status-none,
.email-status-pending {
  color: #475569;
}

.email-status-sent {
  color: #28725f;
}

.email-status-failed {
  color: #b42318;
}

.group-header {
  justify-content: space-between;
  align-items: end;
}

.group-header h2 {
  margin: 0.15rem 0 0.25rem;
  font-size: 1.35rem;
}

.group-header p {
  margin: 0;
  color: #475569;
}

.group-kicker {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

.group-actions {
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.group-counts {
  flex-direction: column;
  gap: 0.15rem;
  text-align: right;
  color: #475569;
  font-size: 0.86rem;
}

.incident-count {
  color: #b45309;
  font-weight: 700;
}

.group-content {
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

.panel-header h2,
.panel-header h3 {
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
  gap: 0.6rem;
}

.preparation-incident {
  display: grid;
  gap: 0.2rem;
  margin-bottom: 0.9rem;
  padding: 0.7rem 0.85rem;
  border-left: 3px solid #b45309;
  background: #fff7ed;
  color: #7c2d12;
  font-size: 0.88rem;
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

  .group-content {
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
  .order-actions,
  .group-header,
  .group-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .group-counts {
    text-align: left;
  }

  .order-lines-header,
  .order-line {
    grid-template-columns: minmax(0, 1fr) 70px 70px;
    gap: 0.5rem;
  }
}
</style>
