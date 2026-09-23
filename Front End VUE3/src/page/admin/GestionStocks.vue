<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au dashboard</router-link>
        <h1>Gestion des stocks</h1>
        <p>Consulter et ajuster les stocks reels des produits</p>
      </div>
      <div class="header-stats">
        <span class="stat-badge danger">
          Rupture: <strong>{{ stocksCritiques.length }}</strong>
        </span>
        <span class="stat-badge warning">
          Stock bas: <strong>{{ stocksBas.length }}</strong>
        </span>
        <span class="stat-badge">
          Total: <strong>{{ stocks.length }}</strong>
        </span>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div v-if="successMessage" class="alert alert-info">{{ successMessage }}</div>

    <div v-if="stocksCritiques.length > 0" class="alert alert-error">
      <span class="alert-icon">!</span>
      <div>
        <strong>Attention :</strong> {{ stocksCritiques.length }} produit(s) sont en rupture.
      </div>
    </div>

    <div class="filters-advanced">
      <div class="filter-row">
        <div class="filter-group">
          <label>Rechercher</label>
          <input
            v-model="searchTerm"
            type="text"
            placeholder="Produit ou categorie..."
            class="filter-input"
          />
        </div>

        <div class="filter-group">
          <label>Niveau de stock</label>
          <select v-model="stockFilter" class="filter-select">
            <option value="tous">Tous</option>
            <option value="rupture">Rupture</option>
            <option value="bas">Stock bas</option>
            <option value="correct">Disponible</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Statut produit</label>
          <select v-model="actifFilter" class="filter-select">
            <option value="tous">Tous</option>
            <option value="actif">Actifs</option>
            <option value="inactif">Inactifs</option>
          </select>
        </div>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>Produit</th>
            <th>Categorie</th>
            <th>Stock actuel</th>
            <th>Statut</th>
            <th>Actif</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="stock in filteredStocks" :key="stock.id">
            <td><strong>{{ stock.produit }}</strong></td>
            <td>{{ stock.categorie }}</td>
            <td>
              <strong :class="stock.status.class === 'danger' ? 'text-danger' : stock.status.class === 'warning' ? 'text-warning' : ''">
                {{ formatStock(stock.stockActuel) }} {{ stock.unite }}
              </strong>
            </td>
            <td>
              <span :class="['status-badge', stock.status.class]">
                {{ stock.status.label }}
              </span>
            </td>
            <td>
              <span :class="['status-badge', stock.actif ? 'success' : 'danger']">
                {{ stock.actif ? 'Actif' : 'Inactif' }}
              </span>
            </td>
            <td class="actions-cell">
              <button class="btn-icon btn-primary" @click="openAdjustModal(stock)">
                Ajuster
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!loading && filteredStocks.length === 0" class="empty-state">
        <div class="empty-icon">...</div>
        <h3>Aucun produit trouve</h3>
      </div>
    </div>

    <div class="stock-legend">
      <h3>Lecture rapide</h3>
      <div class="legend-items">
        <div class="legend-item">
          <span class="status-badge danger">Rupture</span>
          <p>Stock egal a 0</p>
        </div>
        <div class="legend-item">
          <span class="status-badge warning">Stock bas</span>
          <p>Stock strictement positif et inferieur ou egal a 5</p>
        </div>
        <div class="legend-item">
          <span class="status-badge success">Disponible</span>
          <p>Stock superieur a 5</p>
        </div>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2>Ajuster le stock</h2>
          <button class="modal-close" @click="closeModal">x</button>
        </div>

        <div class="modal-body">
          <p><strong>{{ selectedStock?.produit }}</strong></p>
          <p>Stock actuel: {{ formatStock(selectedStock?.stockActuel) }} {{ selectedStock?.unite }}</p>

          <div class="form-group">
            <label for="stockActuel">Nouveau stock</label>
            <input
              id="stockActuel"
              v-model="stockFormValue"
              type="number"
              min="0"
              step="0.01"
              class="filter-input"
            />
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-primary" @click="submitAdjustment" :disabled="saving">
            {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
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
import { categoriesApi, getApiErrorMessage, handleApiAccessError, produitsApi } from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const categories = ref([])
const stocks = ref([])
const loading = ref(false)
const saving = ref(false)
const apiError = ref('')
const successMessage = ref('')
const searchTerm = ref('')
const stockFilter = ref('tous')
const actifFilter = ref('tous')
const showModal = ref(false)
const selectedStock = ref(null)
const stockFormValue = ref('')

onMounted(() => {
  loadStocks()
})

async function loadStocks() {
  loading.value = true
  apiError.value = ''
  try {
    categories.value = await categoriesApi.list()
    const produits = await produitsApi.list(categories.value)
    stocks.value = produits
      .map(produit => ({
        id: produit.id,
        produit: produit.nom,
        categorie: produit.categorie || 'Sans categorie',
        stockActuel: Number(produit.stockActuel ?? produit.stock ?? 0),
        unite: produit.unite || 'piece',
        actif: produit.actif !== false,
        rawProduit: produit,
        status: getStockStatus(Number(produit.stockActuel ?? produit.stock ?? 0))
      }))
      .sort((a, b) => a.stockActuel - b.stockActuel)
  } catch (error) {
    handleApiError(error)
    stocks.value = []
  } finally {
    loading.value = false
  }
}

function getStockStatus(stockActuel) {
  if (stockActuel <= 0) {
    return { label: 'Rupture', class: 'danger' }
  }
  if (stockActuel <= 5) {
    return { label: 'Stock bas', class: 'warning' }
  }
  return { label: 'Disponible', class: 'success' }
}

const stocksCritiques = computed(() => stocks.value.filter(stock => stock.stockActuel <= 0))
const stocksBas = computed(() => stocks.value.filter(stock => stock.stockActuel > 0 && stock.stockActuel <= 5))

const filteredStocks = computed(() => {
  return stocks.value.filter(stock => {
    const normalizedSearch = searchTerm.value.trim().toLowerCase()
    const matchSearch = !normalizedSearch
      || stock.produit.toLowerCase().includes(normalizedSearch)
      || stock.categorie.toLowerCase().includes(normalizedSearch)

    const matchStock =
      stockFilter.value === 'tous'
      || (stockFilter.value === 'rupture' && stock.stockActuel <= 0)
      || (stockFilter.value === 'bas' && stock.stockActuel > 0 && stock.stockActuel <= 5)
      || (stockFilter.value === 'correct' && stock.stockActuel > 5)

    const matchActif =
      actifFilter.value === 'tous'
      || (actifFilter.value === 'actif' && stock.actif)
      || (actifFilter.value === 'inactif' && !stock.actif)

    return matchSearch && matchStock && matchActif
  })
})

function openAdjustModal(stock) {
  selectedStock.value = stock
  stockFormValue.value = String(stock.stockActuel)
  apiError.value = ''
  successMessage.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  selectedStock.value = null
  stockFormValue.value = ''
  saving.value = false
}

async function submitAdjustment() {
  if (!selectedStock.value) {
    return
  }

  const numericValue = Number(stockFormValue.value)
  if (Number.isNaN(numericValue) || numericValue < 0) {
    apiError.value = 'Le stock doit etre un nombre positif ou nul.'
    return
  }

  saving.value = true
  apiError.value = ''
  successMessage.value = ''
  try {
    await produitsApi.adjustStock(selectedStock.value.id, numericValue, categories.value)
    successMessage.value = `Stock mis a jour pour ${selectedStock.value.produit}.`
    closeModal()
    await loadStocks()
  } catch (error) {
    handleApiError(error)
    saving.value = false
  }
}

function handleApiError(error) {
  if (handleApiAccessError(error, authStore, router)) {
    return
  }
  apiError.value = getApiErrorMessage(error)
}

function formatStock(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return '0'
  }
  return Number(value).toLocaleString('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  })
}
</script>

<style scoped>
/* Les styles seront herites de App.css */
</style>
