<template>
  <div class="planning-retraits-page">
    <div class="catalogue-header">
      <div>
        <router-link to="/catalogue" class="back-link">Retour au catalogue</router-link>
        <h1>Prochains retraits</h1>
        <p>
          Consultez les points de collecte et les creneaux disponibles avant de preparer votre panier.
          Le choix definitif se fera au moment de valider la commande.
        </p>
      </div>
      <router-link to="/catalogue" class="btn btn-primary">
        Ouvrir le catalogue
      </router-link>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div v-if="loading" class="empty-state">
      <h3>Chargement des retraits...</h3>
      <p>Recuperation des prochains creneaux de collecte.</p>
    </div>

    <div v-else-if="pointsAvecCreneaux.length === 0" class="empty-state">
      <h3>Aucun creneau disponible</h3>
      <p>Les prochains points de retrait seront affiches ici des qu'ils seront planifies.</p>
    </div>

    <div v-else class="planning-grid">
      <article v-for="point in pointsAvecCreneaux" :key="point.id" class="planning-card">
        <div class="planning-card-header">
          <span class="planning-kicker">Point de collecte</span>
          <h2>{{ point.nom }}</h2>
          <p>{{ point.adresse }}</p>
        </div>

        <div class="creneaux-list-info">
          <div v-for="creneau in point.creneaux" :key="creneau.id" class="creneau-info-row">
            <div>
              <span class="creneau-date">{{ formatDate(creneau.date) }}</span>
              <span class="creneau-day">{{ formatWeekDay(creneau.date) }}</span>
            </div>
            <strong>{{ formatHoraire(creneau) }}</strong>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { pointsCollecteApi, getApiErrorMessage } from '../../services/api.js'

const pointsCollecte = ref([])
const loading = ref(true)
const apiError = ref('')

onMounted(async () => {
  apiError.value = ''
  loading.value = true

  try {
    pointsCollecte.value = await pointsCollecteApi.listActive()
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
    pointsCollecte.value = []
  } finally {
    loading.value = false
  }
})

const pointsAvecCreneaux = computed(() => {
  return pointsCollecte.value
    .map(point => ({
      ...point,
      creneaux: (point.creneaux || [])
        .filter(creneau => creneau.actif !== false && isFutureOrToday(creneau.date))
        .sort(compareCreneaux)
    }))
    .filter(point => point.creneaux.length > 0)
    .sort((a, b) => compareCreneaux(a.creneaux[0], b.creneaux[0]))
})

function isFutureOrToday(dateValue) {
  if (!dateValue) return false
  const creneauDate = new Date(`${String(dateValue).slice(0, 10)}T00:00:00`)
  const today = new Date()
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  return creneauDate >= todayStart
}

function compareCreneaux(a, b) {
  const dateCompare = String(a.date || '').localeCompare(String(b.date || ''))
  if (dateCompare !== 0) return dateCompare
  return String(a.debut || a.heureDebut || '').localeCompare(String(b.debut || b.heureDebut || ''))
}

function formatDate(dateValue) {
  if (!dateValue) return 'Date a confirmer'
  const date = new Date(`${String(dateValue).slice(0, 10)}T00:00:00`)
  if (Number.isNaN(date.getTime())) return dateValue
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })
}

function formatWeekDay(dateValue) {
  if (!dateValue) return ''
  const date = new Date(`${String(dateValue).slice(0, 10)}T00:00:00`)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleDateString('fr-FR', { weekday: 'long' })
}

function formatHoraire(creneau) {
  const debut = creneau.debut || creneau.heureDebut || ''
  const fin = creneau.fin || creneau.heureFin || ''
  return `${String(debut).slice(0, 5)} - ${String(fin).slice(0, 5)}`
}
</script>

<style scoped>
.planning-retraits-page {
  display: grid;
  gap: 1.5rem;
}

.planning-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}

.planning-card {
  border: 1px solid rgba(61, 73, 68, 0.1);
  border-radius: 10px;
  background: rgba(255, 253, 248, 0.96);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.planning-card-header {
  padding: 1.2rem;
  border-bottom: 1px solid rgba(61, 73, 68, 0.1);
  background: rgba(246, 242, 234, 0.7);
}

.planning-kicker {
  display: block;
  margin-bottom: 0.45rem;
  color: var(--text-secondary);
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.planning-card h2 {
  margin: 0;
  font-size: 1.35rem;
}

.planning-card p {
  margin: 0.45rem 0 0;
  color: var(--text-secondary);
}

.creneaux-list-info {
  display: grid;
}

.creneau-info-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.2rem;
  border-bottom: 1px solid rgba(61, 73, 68, 0.08);
}

.creneau-info-row:last-child {
  border-bottom: 0;
}

.creneau-date,
.creneau-day {
  display: block;
}

.creneau-date {
  color: var(--text-primary);
  font-weight: 700;
}

.creneau-day {
  margin-top: 0.18rem;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.creneau-info-row strong {
  white-space: nowrap;
  color: #28483e;
}

@media (max-width: 640px) {
  .creneau-info-row {
    display: grid;
  }
}
</style>
