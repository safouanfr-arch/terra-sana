<template>
  <div class="admin-dashboard">
    <div class="admin-header">
      <div class="admin-welcome">
        <h1>Tableau de bord administrateur</h1>
        <p>Bienvenue, {{ user?.nom }} {{ user?.prenom }} ({{ user?.email }})</p>
      </div>

      <div class="admin-quick-stats">
        <div class="quick-stat">
          <span class="stat-label">Demandes en attente</span>
          <span class="stat-value">{{ dashboardStats.demandesEnAttente }}</span>
        </div>
        <div class="quick-stat">
          <span class="stat-label">Commandes du jour</span>
          <span class="stat-value">{{ dashboardStats.commandesJour }}</span>
        </div>
        <div class="quick-stat">
          <span class="stat-label">Alertes stock</span>
          <span class="stat-value warning">{{ dashboardStats.lowStock }}</span>
        </div>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div class="admin-sections-grid">
      <section
        v-for="section in adminSections"
        :key="section.id"
        class="admin-section"
        :style="{
          '--section-from': section.colors.from,
          '--section-to': section.colors.to,
          '--section-border': section.colors.border
        }"
      >
        <div class="admin-section-header">
          <span>{{ section.kicker }}</span>
          <h2>{{ section.title }}</h2>
          <p>{{ section.description }}</p>
        </div>

        <div class="admin-section-modules">
          <router-link
            v-for="module in section.modules"
            :key="module.id"
            :to="module.path"
            class="admin-module-card"
            :style="{ '--module-color': module.color }"
          >
            <div class="module-icon">{{ module.icon }}</div>
            <div class="module-content">
              <h3>{{ module.title }}</h3>
              <p>{{ module.description }}</p>

              <div v-if="module.stats" class="module-stats">
                <span v-for="(value, key) in module.stats" :key="key" class="stat-badge">
                  <template v-if="key === 'pending'">En attente: </template>
                  <template v-else-if="key === 'total'">Total: </template>
                  <template v-else-if="key === 'suspended'">Suspendus: </template>
                  <template v-else-if="key === 'lowStock'">Stock bas: </template>
                  <template v-else-if="key === 'active'">Actifs: </template>
                  <template v-else-if="key === 'confirmed'">Confirmees: </template>
                  <template v-else-if="key === 'inPrep'">Preparation: </template>
                  <template v-else-if="key === 'ready'">Pretes: </template>
                  <template v-else-if="key === 'distributed'">Distribuees: </template>
                  <template v-else-if="key === 'cancelled'">Annulees: </template>
                  <strong>{{ value }}</strong>
                </span>
              </div>
            </div>
            <div class="module-arrow">Consulter</div>
          </router-link>
        </div>
      </section>
    </div>

    <div class="admin-info-section">
      <div class="info-card">
        <h3>Informations</h3>
        <p>
          Ce tableau de bord affiche maintenant les donnees reelles du backend pour les domaines critiques.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminDashboardApi, getApiErrorMessage, handleApiAccessError } from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore'

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
})

const router = useRouter()
const authStore = useAuthStore()
const apiError = ref('')
const dashboardStats = ref({
  membresTotal: 0,
  membresSuspendus: 0,
  demandesTotal: 0,
  demandesEnAttente: 0,
  produitsTotal: 0,
  produitsActifs: 0,
  categoriesTotal: 0,
  pointsCollecteTotal: 0,
  commandesTotal: 0,
  commandesJour: 0,
  commandesConfirmed: 0,
  commandesInPrep: 0,
  commandesReady: 0,
  commandesDistributed: 0,
  commandesCancelled: 0,
  lowStock: 0
})

onMounted(() => {
  loadStats()
})

async function loadStats() {
  apiError.value = ''
  try {
    dashboardStats.value = await adminDashboardApi.getStats()
  } catch (error) {
    if (handleApiAccessError(error, authStore, router)) {
      return
    }
    apiError.value = getApiErrorMessage(error)
  }
}

const adminModules = computed(() => [
  {
    id: 1,
    title: "Demandes d'adhesion",
    icon: 'DA',
    description: "Valider ou refuser les nouvelles demandes d'adhesion",
    path: '/admin/demandes-adhesion',
    color: '#456e5d',
    stats: { pending: dashboardStats.value.demandesEnAttente, total: dashboardStats.value.demandesTotal }
  },
  {
    id: 2,
    title: 'Gestion des membres',
    icon: 'MB',
    description: 'Suspendre ou reactiver les comptes membres',
    path: '/admin/membres',
    color: '#24483f',
    stats: { total: dashboardStats.value.membresTotal, suspended: dashboardStats.value.membresSuspendus }
  },
  {
    id: 3,
    title: 'Produits',
    icon: 'PR',
    description: 'Creer, modifier et supprimer des produits',
    path: '/admin/produits',
    color: '#667044',
    stats: { total: dashboardStats.value.produitsTotal, active: dashboardStats.value.produitsActifs }
  },
  {
    id: 4,
    title: 'Categories',
    icon: 'CT',
    description: 'Gerer les categories de produits',
    path: '/admin/categories',
    color: '#58683d',
    stats: { total: dashboardStats.value.categoriesTotal }
  },
  {
    id: 5,
    title: 'Points de collecte',
    icon: 'PC',
    description: 'Gerer les lieux de retrait des commandes',
    path: '/admin/points-collecte',
    color: '#4e5f3a',
    stats: { total: dashboardStats.value.pointsCollecteTotal }
  },
  {
    id: 6,
    title: 'Commandes',
    icon: 'CM',
    description: 'Suivi, historique, details, statuts et export CSV',
    path: '/admin/commandes',
    color: '#365b67',
    stats: {
      total: dashboardStats.value.commandesTotal,
      confirmed: dashboardStats.value.commandesConfirmed,
      ready: dashboardStats.value.commandesReady
    }
  },
  {
    id: 7,
    title: 'Stocks',
    icon: 'ST',
    description: 'Consulter et mettre a jour les niveaux de stock',
    path: '/admin/stocks',
    color: '#5c6a3d',
    stats: { lowStock: dashboardStats.value.lowStock, total: dashboardStats.value.produitsTotal }
  },
  {
    id: 8,
    title: 'Promotions',
    icon: 'PM',
    description: 'Creer et gerer les offres promotionnelles',
    path: '/admin/promotions',
    color: '#6d7042',
    stats: null
  },
  {
    id: 9,
    title: 'Export des ventes',
    icon: 'EX',
    description: 'Exporter les donnees de vente en CSV',
    path: '/admin/export-ventes',
    color: '#6f5b4d',
    stats: null
  },
  {
    id: 10,
    title: 'Tableaux de preparation',
    icon: 'TP',
    description: 'Pick-lists, totalisations par produit et impression',
    path: '/admin/tableaux-preparation',
    color: '#2f4f5a',
    stats: null
  },
  {
    id: 12,
    title: 'Parametres emails',
    icon: 'EM',
    description: 'Configuration des modeles d emails',
    path: '/admin/parametres-emails',
    color: '#735b50',
    stats: null
  },
  {
    id: 13,
    title: 'Emails envoyes',
    icon: 'HI',
    description: 'Historique des emails envoyes par les evenements metier',
    path: '/admin/emails-generes',
    color: '#654e47',
    stats: null
  }
])

const adminSections = computed(() => {
  const modules = adminModules.value
  const byId = (...ids) => ids
    .map(id => modules.find(module => module.id === id))
    .filter(Boolean)

  return [
    {
      id: 'membres',
      kicker: 'Comptes',
      title: 'Membres et adhesions',
      description: 'Traitement des demandes, comptes actifs et suspensions.',
      colors: { from: '#eef5ef', to: '#dcebe3', border: '#2f5d4f' },
      modules: byId(1, 2)
    },
    {
      id: 'catalogue',
      kicker: 'Catalogue',
      title: 'Produits, stocks et offres',
      description: 'Gestion des articles visibles, categories, stocks et promotions.',
      colors: { from: '#f3f5ea', to: '#e4ead3', border: '#667044' },
      modules: byId(3, 4, 7, 8)
    },
    {
      id: 'commandes',
      kicker: 'Operations',
      title: 'Commandes et retraits',
      description: 'Suivi des commandes, historique, points de collecte et preparation.',
      colors: { from: '#edf3f4', to: '#dbe8eb', border: '#365b67' },
      modules: byId(5, 6, 10)
    },
    {
      id: 'communication',
      kicker: 'Suivi',
      title: 'Emails et exports',
      description: 'Configuration des emails, historique des envois et exports CSV.',
      colors: { from: '#f5f0ec', to: '#eaded6', border: '#735b50' },
      modules: byId(12, 13, 9)
    }
  ]
})
</script>

<style scoped>
.admin-sections-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  align-items: start;
}

.admin-section {
  min-width: 0;
  min-height: 100%;
  border: 1px solid color-mix(in srgb, var(--section-border) 28%, transparent);
  border-radius: 12px;
  background: linear-gradient(135deg, var(--section-from), var(--section-to));
  box-shadow: var(--shadow-sm);
  padding: 1rem;
}

.admin-section-header {
  margin-bottom: 0.9rem;
}

.admin-section-header span {
  color: var(--section-border);
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-section-header h2 {
  margin: 0.2rem 0 0.25rem;
  font-size: 1.15rem;
}

.admin-section-header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.92rem;
  line-height: 1.45;
}

.admin-section-modules {
  display: grid;
  gap: 0.75rem;
}

.admin-section .admin-module-card {
  min-height: 0;
  background: rgba(255, 253, 248, 0.84);
  border-color: color-mix(in srgb, var(--section-border) 18%, transparent);
}

@media (max-width: 760px) {
  .admin-sections-grid {
    grid-template-columns: 1fr;
  }
}
</style>
