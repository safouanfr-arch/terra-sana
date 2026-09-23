<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">← Retour au dashboard</router-link>
        <h1>Gestion des produits</h1>
        <p>Créer, modifier et supprimer des produits</p>
      </div>
      <div style="display: flex; gap: 10px;">
        <button class="btn btn-primary" @click="handleAdd">
          Nouveau produit
        </button>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <!-- Filtres -->
    <div class="produits-filters-card">
      <div class="filter-row">
        <div class="form-group filter-search">
          <label>Recherche</label>
          <input v-model="searchQuery" type="text" placeholder="Nom du produit..." class="form-control" />
        </div>
        <div class="form-group filter-category">
          <label>Categorie</label>
          <select v-model="filterCategorie" class="form-control">
            <option value="">Toutes les categories</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.nom">
              {{ cat.nom }}
            </option>
          </select>
        </div>
        <div class="form-group filter-choices">
          <label>Tag</label>
          <div class="filter-chip-row">
            <button
              v-for="tag in tagOptions"
              :key="tag.key"
              type="button"
              :class="['filter-chip', `badge-${tag.key}`, { active: filterTags.includes(tag.key) }]"
              @click="toggleFilter(filterTags, tag.key)"
            >
              {{ tag.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table produits-table">
        <thead>
          <tr>
            <th>Image</th>
            <th>Nom</th>
            <th>Catégorie</th>
            <th>Prix</th>
            <th>Stock</th>
            <th>Tags</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="produit in produitsFiltres" :key="produit.id">
            <td>
              <img
                class="product-thumb"
                :src="getImageSrc(produit)"
                :alt="`Image de ${produit.nom}`"
                @error="onProductImgError($event, produit)"
              />
            </td>
            <td><strong>{{ produit.nom }}</strong></td>
            <td>{{ produit.categorie }}</td>
            <td>{{ produit.prix.toFixed(2) }} €</td>
            <td :class="produit.stock < 10 ? 'text-warning' : ''">{{ produit.stock }}</td>
            <td>
              <div class="badges-cell">
                <span v-if="produit.badges?.bio" class="badge badge-bio" title="Bio">Bio</span>
                <span v-if="produit.badges?.local" class="badge badge-local" title="Local">Local</span>
                <span v-if="produit.badges?.vegan" class="badge badge-vegan" title="Vegan">Vegan</span>
                <span v-if="produit.badges?.vegetarien" class="badge badge-vegetarien" title="Vegetarien">Veg</span>
              </div>
            </td>
            <td>
              <span :class="['status-badge', produit.actif ? 'success' : 'secondary']">
                {{ produit.actif ? 'Actif' : 'Inactif' }}
              </span>
            </td>
            <td class="actions-cell">
              <button class="btn-icon" @click="handleEdit(produit)" title="Modifier">Modifier</button>
              <button class="btn-icon btn-danger" @click="handleDelete(produit.id)" title="Supprimer">Supprimer</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click="showModal = false">
      <div class="modal modal-large" @click.stop>
        <div class="modal-header">
          <h2>{{ editMode ? 'Modifier le produit' : 'Nouveau produit' }}</h2>
          <button class="modal-close" @click="showModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label>Nom du produit *</label>
              <input v-model="formData.nom" type="text" class="form-control" placeholder="Ex: Tomates bio" />
            </div>
            <div class="form-group">
              <label>Image du produit</label>
              <div class="image-upload-field">
                <img
                  class="image-preview"
                  :src="getImageSrc(formData)"
                  :alt="`Apercu de ${formData.nom || 'produit'}`"
                  @error="onProductImgError($event, formData)"
                />
                <div class="image-upload-actions">
                  <input ref="imageInput" type="file" accept="image/*" @change="handleImageUpload" />
                  <button v-if="formData.image" type="button" class="btn btn-secondary" @click="clearImage">
                    Retirer l'image
                  </button>
                </div>
              </div>
              <small class="field-help">Image locale choisie depuis votre ordinateur, sans lien externe.</small>
            </div>
          </div>

          <div class="form-group">
            <label>Description</label>
            <textarea v-model="formData.description" rows="3" class="form-control" 
                      placeholder="Décrivez le produit en quelques mots..."></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>Catégorie *</label>
              <select v-model="formData.categorie" class="form-control">
                <option value="">Selectionnez une categorie</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.nom">{{ cat.nom }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>Unité</label>
              <input 
                v-model="formData.unite" 
                type="text" 
                class="form-control" 
                list="unite-suggestions"
                placeholder="Ex: kg, pièce, litre..."
              />
              <datalist id="unite-suggestions">
                <option value="kg">Kilogramme</option>
                <option value="g">Gramme</option>
                <option value="pièce">Pièce</option>
                <option value="litre">Litre</option>
                <option value="cl">Centilitre</option>
                <option value="ml">Millilitre</option>
                <option value="pot">Pot</option>
                <option value="barquette">Barquette</option>
                <option value="botte">Botte</option>
                <option value="bouquet">Bouquet</option>
                <option value="sachet">Sachet</option>
                <option value="paquet">Paquet</option>
                <option value="boîte">Boîte</option>
                <option value="douzaine">Douzaine</option>
                <option value="500g">500g</option>
                <option value="1kg">1kg</option>
                <option value="portion">Portion</option>
                <option value="tranche">Tranche</option>
                <option value="bouteille">Bouteille</option>
              </datalist>
              <small class="field-help">Tapez ou sélectionnez une unité (suggestions disponibles)</small>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>Prix (€) *</label>
              <input v-model.number="formData.prix" type="number" step="0.01" class="form-control" />
            </div>
            <div class="form-group">
              <label>Stock *</label>
              <input v-model.number="formData.stock" type="number" class="form-control" />
            </div>
            <div class="form-group">
              <label>TVA (%)</label>
              <select v-model.number="formData.tauxTVA" class="form-control">
                <option :value="6">6% (produits alimentaires)</option>
                <option :value="21">21% (autres)</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>Origine</label>
              <input v-model="formData.origine" type="text" class="form-control" placeholder="Ex: France" />
            </div>
            <div class="form-group">
              <label>Producteur</label>
              <input v-model="formData.producteur" type="text" class="form-control" placeholder="Ex: Ferme du Soleil" />
            </div>
          </div>

          <div class="form-group">
            <label>Tags</label>
            <div class="choice-grid tags-choice-grid">
              <button
                v-for="tag in tagOptions"
                :key="tag.key"
                type="button"
                :class="['tag-choice', `badge-${tag.key}`, { selected: formData.badges[tag.key] }]"
                @click="toggleTag(tag.key)"
              >
                {{ tag.label }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input v-model="formData.actif" type="checkbox" />
              <span>Produit actif (visible dans le catalogue)</span>
            </label>
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
import { ref, computed, onMounted } from 'vue'
import { categoriesApi, produitsApi, getApiErrorMessage } from '../../services/api.js'
import { getProductImageSrc, productPlaceholder } from '../../utils/imageHelper.js'

const produits = ref([])
const categories = ref([])
const apiError = ref('')
const searchQuery = ref('')
const filterCategorie = ref('')
const filterTags = ref([])

const showModal = ref(false)
const editMode = ref(false)
const currentProduit = ref(null)
const imageInput = ref(null)
const tagOptions = [
  { key: 'bio', label: 'Bio' },
  { key: 'local', label: 'Local' },
  { key: 'vegan', label: 'Vegan' },
  { key: 'vegetarien', label: 'Vegetarien' }
]
const formData = ref({
  nom: '',
  image: '',
  description: '',
  categorie: '',
  unite: 'pièce',
  prix: 0,
  stock: 0,
  tauxTVA: 6,
  origine: '',
  producteur: '',
  badges: { bio: false, local: false, vegan: false, vegetarien: false },
  photos: [],
  actif: true
})

async function loadCategories() {
  categories.value = await categoriesApi.list()
}

async function loadProduits() {
  apiError.value = ''
  try {
    await loadCategories()
    produits.value = await produitsApi.list(categories.value)
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
    produits.value = []
  }
}

onMounted(() => loadProduits())

const produitsFiltres = computed(() => {
  return produits.value.filter(p => {
    // Filtre par recherche
    if (searchQuery.value && !p.nom.toLowerCase().includes(searchQuery.value.toLowerCase())) {
      return false
    }
    
    // Filtre par catégorie
    if (filterCategorie.value && p.categorie !== filterCategorie.value) {
      return false
    }
    
    // Filtre par badge
    if (filterTags.value.length > 0 && !filterTags.value.every(tag => p.badges?.[tag])) {
      return false
    }
    
    return true
  })
})

function toggleFilter(target, value) {
  const list = Array.isArray(target) ? target : target.value
  const index = list.indexOf(value)
  if (index >= 0) {
    list.splice(index, 1)
    return
  }
  list.push(value)
}

const handleAdd = async () => {
  try {
    await loadCategories()
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  }
  editMode.value = false
  currentProduit.value = null
  formData.value = {
    nom: '',
    image: '',
    description: '',
    categorie: '',
    unite: 'pièce',
    prix: 0,
    stock: 0,
    tauxTVA: 6,
    origine: '',
    producteur: '',
    badges: { bio: false, local: false, vegan: false, vegetarien: false },
    photos: [],
    actif: true
  }
  if (imageInput.value) imageInput.value.value = ''
  showModal.value = true
}

const handleEdit = async (produit) => {
  try {
    await loadCategories()
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  }
  editMode.value = true
  currentProduit.value = produit
  formData.value = {
    ...produit,
    badges: { ...(produit.badges || { bio: false, local: false, vegan: false, vegetarien: false }) },
    photos: Array.isArray(produit.photos) ? [...produit.photos] : []
  }
  if (imageInput.value) imageInput.value.value = ''
  showModal.value = true
}

const handleDelete = async (id) => {
  if (confirm('Êtes-vous sûr de vouloir supprimer ce produit ?')) {
    try {
      await produitsApi.remove(id)
      await loadProduits()
    } catch (error) {
      apiError.value = getApiErrorMessage(error)
    }
  }
}

const handleSave = async () => {
  if (!formData.value.nom || !formData.value.categorie) {
    alert('Veuillez remplir les champs obligatoires (nom et catégorie)')
    return
  }

  apiError.value = ''
  try {
    if (editMode.value) {
      await produitsApi.update(currentProduit.value.id, formData.value, categories.value)
    } else {
      await produitsApi.create(formData.value, categories.value)
    }
    await loadProduits()
    showModal.value = false
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  }
}

function getImageSrc(produit) {
  return getProductImageSrc(produit)
}

function onProductImgError(event, produit) {
  event.target.src = productPlaceholder(produit?.nom, produit?.categorie)
}

function toggleTag(key) {
  formData.value.badges[key] = !formData.value.badges[key]
}

function handleImageUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    alert('Veuillez choisir un fichier image.')
    event.target.value = ''
    return
  }

  if (file.size > 2 * 1024 * 1024) {
    alert('Image trop lourde. Choisissez une image de moins de 2 Mo.')
    event.target.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    formData.value.image = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

function clearImage() {
  formData.value.image = ''
  if (imageInput.value) imageInput.value.value = ''
}
</script>

<style scoped>
.badges-cell {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-bio {
  background-color: #d4edda;
  color: #155724;
}

.badge-local {
  background-color: #d1ecf1;
  color: #0c5460;
}

.badge-vegan {
  background-color: #d4edda;
  color: #155724;
}

.badge-vegetarien {
  background-color: #fff3cd;
  color: #856404;
}

.produits-table {
  min-width: 1040px;
}

.product-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
  background: #ece7de;
  border: 1px solid rgba(61, 73, 68, 0.12);
}

.image-upload-field {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 1rem;
  align-items: center;
}

.image-preview {
  width: 112px;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid rgba(61, 73, 68, 0.12);
  background: #ece7de;
}

.image-upload-actions {
  display: grid;
  gap: 0.6rem;
}

.choice-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.tag-choice {
  border: 1px solid rgba(61, 73, 68, 0.12);
  border-radius: 999px;
  background: #fffdf8;
  color: #3d4944;
  padding: 0.55rem 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.tag-choice:hover {
  transform: translateY(-1px);
  border-color: rgba(47, 90, 79, 0.35);
}

.tag-choice.selected {
  border-color: rgba(47, 90, 79, 0.42);
  background: rgba(47, 90, 79, 0.12);
  box-shadow: inset 0 0 0 1px rgba(47, 90, 79, 0.12);
}

.tags-choice-grid {
  padding: 0.75rem;
  border: 1px solid rgba(61, 73, 68, 0.1);
  border-radius: 10px;
  background: rgba(252, 250, 246, 0.74);
}

.produits-filters-card {
  margin-bottom: 1.5rem;
  padding: 1rem;
  border: 1px solid rgba(61, 73, 68, 0.1);
  border-radius: 10px;
  background: rgba(252, 250, 246, 0.94);
  box-shadow: var(--shadow-sm);
}

.filter-search {
  flex: 1 1 260px;
  min-width: 220px;
}

.filter-category {
  flex: 0 1 260px;
  min-width: 220px;
}

.filter-choices {
  flex: 1.4 1 280px;
  min-width: 240px;
}

.filter-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.filter-chip {
  min-height: 34px;
  border: 1px solid rgba(61, 73, 68, 0.12);
  border-radius: 999px;
  background: #fffdf8;
  color: #3d4944;
  padding: 0 0.78rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
}

.filter-chip.active {
  border-color: rgba(47, 90, 79, 0.38);
  background: rgba(47, 90, 79, 0.12);
  color: #28483e;
}

.modal-large {
  max-width: 800px;
}

.filter-row {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
}
</style>
