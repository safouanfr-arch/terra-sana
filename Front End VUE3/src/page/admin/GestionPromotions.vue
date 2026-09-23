<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au dashboard</router-link>
        <h1>Gestion des promotions</h1>
        <p>Creation, edition et ciblage backend des promotions.</p>
      </div>
      <button class="btn btn-primary" @click="handleAdd">Nouvelle promotion</button>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <div v-if="loading" class="empty-state">
      <div class="empty-icon">...</div>
      <h3>Chargement des promotions</h3>
    </div>

    <div v-else-if="promotions.length === 0" class="empty-state">
      <div class="empty-icon">%</div>
      <h3>Aucune promotion</h3>
      <p>Créez votre premiere promotion pour l'administration.</p>
    </div>

    <div v-else class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>Nom</th>
            <th>Reduction</th>
            <th>Periode</th>
            <th>Ciblage</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="promo in promotions" :key="promo.id">
            <td>
              <strong>{{ promo.nom }}</strong>
              <div v-if="promo.description" class="table-subtext">{{ promo.description }}</div>
            </td>
            <td>
              <span class="promo-badge">
                {{ promo.type === 'pourcentage' ? `${promo.reduction}%` : `${promo.reduction} EUR` }}
              </span>
            </td>
            <td>
              {{ formatDate(promo.dateDebut) }} - {{ formatDate(promo.dateFin) }}
            </td>
            <td>{{ formatTargeting(promo) }}</td>
            <td>
              <span :class="['status-badge', promo.actif ? 'success' : 'secondary']">
                {{ promo.actif ? 'Active' : 'Inactive' }}
              </span>
            </td>
            <td class="actions-cell">
              <button class="btn-icon" @click="handleEdit(promo)" title="Modifier">Editer</button>
              <button class="btn-icon btn-danger" @click="handleDelete(promo.id)" title="Supprimer">Supprimer</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2>{{ editMode ? 'Modifier la promotion' : 'Nouvelle promotion' }}</h2>
          <button class="modal-close" @click="closeModal">x</button>
        </div>

        <div class="modal-body">
          <div class="form-group">
            <label>Nom de la promotion *</label>
            <input v-model.trim="formData.nom" type="text" />
          </div>

          <div class="form-group">
            <label>Description</label>
            <textarea v-model.trim="formData.description" rows="3" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>Type de reduction *</label>
              <select v-model="formData.type">
                <option value="pourcentage">Pourcentage</option>
                <option value="montant">Montant fixe</option>
              </select>
            </div>
            <div class="form-group">
              <label>{{ formData.type === 'pourcentage' ? 'Reduction (%) *' : 'Reduction TTC (EUR) *' }}</label>
              <input
                v-model.number="formData.reduction"
                type="number"
                min="0"
                step="0.01"
                :placeholder="formData.type === 'pourcentage' ? 'Ex: 10' : 'Ex: 5.00'"
              />
              <small v-if="formData.type === 'montant'" class="field-help">
                Montant retire du prix TTC affiche au client.
              </small>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>Date de debut *</label>
              <input v-model="formData.dateDebut" type="date" />
            </div>
            <div class="form-group">
              <label>Date de fin *</label>
              <input v-model="formData.dateFin" type="date" />
            </div>
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input v-model="formData.actif" type="checkbox" />
              <span>Promotion active</span>
            </label>
          </div>

          <div class="form-section">
            <h3>Ciblage de la promotion</h3>
            <p class="help-text">Vous pouvez viser des produits precis, des categories entieres, ou les deux.</p>

            <div class="form-group">
              <label>Produits specifiques</label>
              <div class="checkbox-grid">
                <label v-for="produit in allProduits" :key="produit.id" class="checkbox-item">
                  <input type="checkbox" :value="produit.id" v-model="formData.produits" />
                  <span>{{ produit.nom }}</span>
                </label>
              </div>
            </div>

            <div class="form-group">
              <label>Categories entieres</label>
              <div class="checkbox-grid">
                <label v-for="categorie in allCategories" :key="categorie.id" class="checkbox-item">
                  <input type="checkbox" :value="categorie.id" v-model="formData.categories" />
                  <span>{{ categorie.nom }}</span>
                </label>
              </div>
            </div>

            <div v-if="affectedProductsCount > 0" class="alert alert-info">
              La promotion touchera <strong>{{ affectedProductsCount }}</strong> produit(s).
            </div>
            <div v-else class="alert alert-warning">
              Aucun produit selectionne. La promotion sera enregistree mais ne s'appliquera a aucun article.
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-primary" @click="handleSave" :disabled="saving">
            {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
          </button>
          <button class="btn btn-secondary" @click="closeModal">Annuler</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  categoriesApi,
  getApiErrorMessage,
  handleApiAccessError,
  isApiAccessError,
  produitsApi,
  promotionsApi
} from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const apiError = ref('')
const successMessage = ref('')

const promotions = ref([])
const allProduits = ref([])
const allCategories = ref([])

const showModal = ref(false)
const editMode = ref(false)
const currentPromoId = ref(null)
const formData = ref(createEmptyForm())

const affectedProductsCount = computed(() => {
  const direct = formData.value.produits || []
  const fromCategories = allProduits.value
    .filter(produit => formData.value.categories.includes(produit.categorieId))
    .map(produit => produit.id)
  return new Set([...direct, ...fromCategories]).size
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  apiError.value = ''
  try {
    allCategories.value = await categoriesApi.list()
    allProduits.value = await produitsApi.list(allCategories.value)
    promotions.value = await promotionsApi.list()
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
      promotions.value = []
    }
  } finally {
    loading.value = false
  }
}

function createEmptyForm() {
  return {
    nom: '',
    description: '',
    reduction: '',
    type: 'pourcentage',
    dateDebut: '',
    dateFin: '',
    actif: true,
    produits: [],
    categories: []
  }
}

function handleAdd() {
  apiError.value = ''
  successMessage.value = ''
  editMode.value = false
  currentPromoId.value = null
  formData.value = createEmptyForm()
  showModal.value = true
}

function handleEdit(promo) {
  apiError.value = ''
  successMessage.value = ''
  editMode.value = true
  currentPromoId.value = promo.id
  formData.value = {
    id: promo.id,
    nom: promo.nom,
    description: promo.description || '',
    reduction: promo.reduction,
    type: promo.type,
    dateDebut: promo.dateDebut,
    dateFin: promo.dateFin,
    actif: promo.actif !== false,
    produits: [...promo.produits],
    categories: [...promo.categories]
  }
  showModal.value = true
}

async function handleDelete(id) {
  if (!window.confirm('Supprimer cette promotion ?')) {
    return
  }

  apiError.value = ''
  successMessage.value = ''
  try {
    await promotionsApi.remove(id)
    successMessage.value = 'Promotion supprimee.'
    await loadData()
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
    }
  }
}

async function handleSave() {
  apiError.value = ''
  successMessage.value = ''

  if (!formData.value.nom || !formData.value.dateDebut || !formData.value.dateFin) {
    apiError.value = 'Nom, dates et reduction sont requis.'
    return
  }
  if (!Number(formData.value.reduction)) {
    apiError.value = 'La reduction doit etre superieure a zero.'
    return
  }

  saving.value = true
  try {
    if (editMode.value && currentPromoId.value) {
      await promotionsApi.update(currentPromoId.value, formData.value)
      successMessage.value = 'Promotion mise a jour.'
    } else {
      await promotionsApi.create(formData.value)
      successMessage.value = 'Promotion creee.'
    }
    closeModal()
    await loadData()
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
    }
  } finally {
    saving.value = false
  }
}

function closeModal() {
  showModal.value = false
  editMode.value = false
  currentPromoId.value = null
  formData.value = createEmptyForm()
}

function formatDate(value) {
  return value ? new Date(`${value}T00:00:00`).toLocaleDateString('fr-FR') : 'N/A'
}

function formatTargeting(promo) {
  const parts = []
  if (promo.produits.length > 0) {
    parts.push(`${promo.produits.length} produit(s)`)
  }
  if (promo.categories.length > 0) {
    parts.push(`${promo.categories.length} categorie(s)`)
  }
  return parts.length > 0 ? parts.join(' + ') : 'Aucun ciblage'
}

function handleSessionError(error) {
  return handleApiAccessError(error, authStore, router)
}
</script>

<style scoped>
.table-subtext {
  margin-top: 0.35rem;
  color: var(--muted-text, #64748b);
  font-size: 0.9rem;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.6rem;
}

.checkbox-item {
  display: flex !important;
  align-items: center;
  gap: 0.65rem;
  margin: 0 !important;
  padding: 0.7rem 0.8rem;
  border: 1px solid rgba(61, 73, 68, 0.1);
  border-radius: 10px;
  background: rgba(255, 253, 248, 0.96);
  cursor: pointer;
}

.checkbox-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  min-width: 16px;
  margin: 0;
}

.checkbox-item span {
  line-height: 1.3;
}
</style>
