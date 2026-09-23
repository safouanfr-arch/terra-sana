<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au dashboard</router-link>
        <h1>Gestion des points de collecte</h1>
        <p>Gerer les lieux et creneaux de retrait des commandes.</p>
      </div>
      <div style="display: flex; gap: 10px;">
        <button class="btn btn-primary" @click="handleAdd">Nouveau point de collecte</button>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div class="points-grid">
      <div v-for="point in points" :key="point.id" :class="['point-card', { inactive: !point.actif }]">
        <div class="point-header">
          <h3>{{ point.nom }}</h3>
          <span :class="['status-badge', point.actif ? 'success' : 'secondary']">
            {{ point.actif ? 'Actif' : 'Inactif' }}
          </span>
        </div>
        <div class="point-info">
          <p><strong>Adresse:</strong> {{ point.adresse }}</p>
          <p><strong>Creneaux:</strong> {{ formatCreneaux(point.creneaux) }}</p>
        </div>
        <div class="point-actions">
          <button class="btn-icon" @click="handleEdit(point)">Modifier</button>
          <button class="btn-icon btn-danger" @click="handleDelete(point.id)">Supprimer</button>
        </div>
      </div>
    </div>

    <div v-if="showModal" class="modal-overlay" @click="showModal = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2>{{ editMode ? 'Modifier le point de collecte' : 'Nouveau point de collecte' }}</h2>
          <button class="modal-close" @click="showModal = false">x</button>
        </div>

        <div class="modal-body">
          <div class="form-group">
            <label>Nom du point *</label>
            <input v-model="formData.nom" type="text" />
          </div>

          <div class="form-group">
            <label>Adresse complete *</label>
            <textarea v-model="formData.adresse" rows="2" />
          </div>

          <div class="form-group">
            <label>Creneaux de collecte *</label>
            <div class="creneaux-container">
              <div v-for="(creneau, idx) in formData.creneaux" :key="idx" class="creneau-item">
                <div class="creneau-row">
                  <div class="input-group">
                    <label class="input-label">Date</label>
                    <input v-model="creneau.date" type="date" class="form-control date-input" />
                  </div>
                  <div class="input-group">
                    <label class="input-label">Debut</label>
                    <input v-model="creneau.debut" type="time" class="form-control time-input" />
                  </div>
                  <div class="input-group">
                    <label class="input-label">Fin</label>
                    <input
                      v-model="creneau.fin"
                      type="time"
                      class="form-control time-input"
                      :class="{ error: isInvalidCreneau(creneau) }"
                      :min="creneau.debut || undefined"
                    />
                  </div>
                  <button class="btn-icon-delete" @click="removeCreneau(idx)" title="Supprimer ce creneau">
                    <span class="delete-icon">x</span>
                  </button>
                </div>
                <small v-if="isInvalidCreneau(creneau)" class="creneau-error">
                  L'heure de fin doit etre posterieure a l'heure de debut.
                </small>
              </div>
            </div>
            <button class="btn btn-secondary btn-add-creneau" type="button" @click="addCreneau">
              Ajouter un creneau
            </button>
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input v-model="formData.actif" type="checkbox" />
              <span>Point de collecte actif</span>
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
import { onMounted, ref } from 'vue'
import {
  creneauxApi,
  getApiErrorMessage,
  pointsCollecteApi
} from '../../services/api.js'

const points = ref([])
const apiError = ref('')
const showModal = ref(false)
const editMode = ref(false)
const currentPoint = ref(null)
const formData = ref({ nom: '', adresse: '', creneaux: [], actif: true })

onMounted(() => loadPoints())

function formatCreneaux(creneaux = []) {
  return (Array.isArray(creneaux) ? creneaux : [])
    .map(creneau => creneau.horaire || `${creneau.date || ''} ${creneau.debut || ''}-${creneau.fin || ''}`.trim())
    .filter(Boolean)
    .join(', ')
}

async function loadPoints() {
  apiError.value = ''
  try {
    points.value = await pointsCollecteApi.list()
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
    points.value = []
  }
}

function addCreneau() {
  formData.value.creneaux.push({ date: '', debut: '', fin: '', actif: true })
}

function removeCreneau(index) {
  formData.value.creneaux.splice(index, 1)
}

function isInvalidCreneau(creneau) {
  return Boolean(creneau.debut && creneau.fin && creneau.fin <= creneau.debut)
}

function handleAdd() {
  editMode.value = false
  currentPoint.value = null
  formData.value = { nom: '', adresse: '', creneaux: [], actif: true }
  showModal.value = true
}

function handleEdit(point) {
  editMode.value = true
  currentPoint.value = point
  formData.value = {
    nom: point.nom,
    adresse: point.adresse,
    actif: point.actif !== false,
    creneaux: Array.isArray(point.creneaux)
      ? point.creneaux.map(creneau => ({
          id: creneau.id,
          date: creneau.date || '',
          debut: creneau.debut || creneau.heureDebut || '',
          fin: creneau.fin || creneau.heureFin || '',
          actif: creneau.actif !== false
        }))
      : []
  }
  showModal.value = true
}

async function handleDelete(id) {
  if (!confirm('Etes-vous sur de vouloir supprimer ce point de collecte ?')) {
    return
  }

  apiError.value = ''
  try {
    await pointsCollecteApi.remove(id)
    await loadPoints()
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  }
}

async function syncCreneaux(pointId, anciensCreneaux, nouveauxCreneaux) {
  const anciensIds = new Set((anciensCreneaux || []).map(creneau => creneau.id).filter(Boolean))
  const nouveauxIds = new Set(nouveauxCreneaux.map(creneau => creneau.id).filter(Boolean))

  const suppressions = [...anciensIds]
    .filter(id => !nouveauxIds.has(id))
    .map(id => creneauxApi.remove(id))
  await Promise.all(suppressions)

  for (const creneau of nouveauxCreneaux) {
    if (creneau.id) {
      await creneauxApi.update(pointId, creneau.id, creneau)
    } else {
      await creneauxApi.create(pointId, creneau)
    }
  }
}

async function handleSave() {
  const creneaux = (formData.value.creneaux || [])
    .filter(creneau => creneau.date && creneau.debut && creneau.fin)
    .map(creneau => ({
      id: creneau.id,
      date: creneau.date,
      debut: creneau.debut || '',
      fin: creneau.fin || '',
      actif: creneau.actif !== false
    }))

  apiError.value = ''

  if (creneaux.some(isInvalidCreneau)) {
    apiError.value = "L'heure de fin d'un creneau doit etre posterieure a l'heure de debut."
    return
  }

  try {
    let savedPoint
    if (editMode.value) {
      savedPoint = await pointsCollecteApi.update(currentPoint.value.id, formData.value)
      await syncCreneaux(savedPoint.id, currentPoint.value.creneaux || [], creneaux)
    } else {
      savedPoint = await pointsCollecteApi.create(formData.value)
      await syncCreneaux(savedPoint.id, [], creneaux)
    }

    await loadPoints()
    showModal.value = false
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  }
}
</script>

<style scoped>
.creneaux-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.creneau-item {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s ease;
}

.creneau-item:hover {
  border-color: #cbd5e0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.creneau-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr auto;
  gap: 16px;
  align-items: end;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #495057;
  margin: 0;
}

.date-input {
  padding: 10px 12px;
  border: 1px solid #ced4da;
  border-radius: 6px;
  font-size: 0.95rem;
  background-color: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.date-input:hover {
  border-color: #80bdff;
}

.date-input:focus {
  outline: none;
  border-color: #80bdff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.time-input {
  padding: 10px 12px;
  border: 1px solid #ced4da;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  color: #495057;
  background-color: white;
  transition: all 0.2s ease;
}

.time-input:hover {
  border-color: #80bdff;
}

.time-input:focus {
  outline: none;
  border-color: #80bdff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.time-input.error {
  border-color: #b95b5b;
  box-shadow: 0 0 0 3px rgba(185, 91, 91, 0.12);
}

.creneau-error {
  display: block;
  margin-top: 0.65rem;
  color: #8a3f3f;
  font-weight: 600;
}

.btn-icon-delete {
  background: #fff;
  border: 1px solid #dc3545;
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 44px;
}

.btn-icon-delete:hover {
  background: #dc3545;
  transform: scale(1.05);
}

.btn-icon-delete:hover .delete-icon {
  filter: brightness(0) invert(1);
}

.delete-icon {
  font-size: 1.1rem;
}

.btn-add-creneau {
  margin-top: 8px;
  width: 100%;
  padding: 12px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .creneau-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .btn-icon-delete {
    width: 100%;
  }
}
</style>
