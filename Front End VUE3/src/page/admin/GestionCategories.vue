<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">← Retour au dashboard</router-link>
        <h1>Gestion des categories</h1>
        <p>Gérer les catégories de produits</p>
      </div>
      <button class="btn btn-primary" @click="handleAdd">Nouvelle categorie</button>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div class="categories-grid">
      <div v-for="categorie in categories" :key="categorie.id" class="category-card">
        <div class="category-header">
          <h3>{{ categorie.nom }}</h3>
          <span class="category-count">{{ categorie.nbProduits }} produits</span>
        </div>
        <p>{{ categorie.description }}</p>
        <div class="category-actions">
          <button class="btn-icon" @click="handleEdit(categorie)">Modifier</button>
          <button class="btn-icon btn-danger" @click="handleDelete(categorie.id)">Supprimer</button>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click="showModal = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2>{{ editMode ? 'Modifier la categorie' : 'Nouvelle categorie' }}</h2>
          <button class="modal-close" @click="showModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>Nom de la catégorie *</label>
            <input v-model="formData.nom" type="text" />
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="formData.description" rows="3" />
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn btn-primary" @click="handleSave">Enregistrer</button>
          <button class="btn btn-secondary" @click="showModal = false">Annuler</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { categoriesApi, getApiErrorMessage } from '../../services/api.js'

const categories = ref([])
const apiError = ref('')

onMounted(() => loadCategories())

const showModal = ref(false)
const editMode = ref(false)
const currentCategorie = ref(null)
const formData = ref({ nom: '', description: '', actif: true })

async function loadCategories() {
  apiError.value = ''
  try {
    categories.value = await categoriesApi.list()
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
    categories.value = []
  }
}

const handleAdd = () => {
  editMode.value = false
  currentCategorie.value = null
  formData.value = { nom: '', description: '', actif: true }
  showModal.value = true
}

const handleEdit = (categorie) => {
  editMode.value = true
  currentCategorie.value = categorie
  formData.value = { nom: categorie.nom, description: categorie.description, actif: categorie.actif }
  showModal.value = true
}

const handleDelete = async (id) => {
  const cat = categories.value.find(c => c.id === id)
  if (cat.nbProduits > 0) {
    alert(`Impossible de supprimer cette catégorie car elle contient ${cat.nbProduits} produit(s).`)
    return
  }
  if (confirm('Êtes-vous sûr de vouloir supprimer cette catégorie ?')) {
    try {
      await categoriesApi.remove(id)
      await loadCategories()
    } catch (error) {
      apiError.value = getApiErrorMessage(error)
    }
  }
}

const handleSave = async () => {
  apiError.value = ''
  try {
    if (editMode.value) {
      await categoriesApi.update(currentCategorie.value.id, formData.value)
    } else {
      await categoriesApi.create(formData.value)
    }
    await loadCategories()
    showModal.value = false
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  }
}
</script>

<style scoped>
/* Les styles seront hérités de App.css */
</style>
