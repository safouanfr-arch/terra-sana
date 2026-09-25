<template>
  <div class="membre-dashboard">
    <section class="membre-hero">
      <div class="membre-hero-copy">
        <p class="membre-eyebrow">Espace membre</p>
        <h1>Bonjour {{ user?.prenom || 'membre' }}</h1>
        <p>
          Retrouvez votre catalogue, votre panier et le suivi de vos commandes dans un espace plus clair et plus direct.
        </p>
      </div>

      <div class="membre-summary">
        <div class="summary-label">Acces rapide</div>
        <div class="summary-value">{{ panierStore.getTotalArticles }}</div>
        <div class="summary-text">article(s) actuellement dans le panier</div>
      </div>
    </section>

    <div class="quick-actions-grid">
      <router-link
        v-for="action in quickActions"
        :key="action.id"
        :to="action.path"
        class="quick-action-card"
      >
        <div class="action-kicker">{{ action.kicker }}</div>
        <div class="action-content">
          <h3>{{ action.title }}</h3>
          <p>{{ action.description }}</p>
        </div>
        <div class="action-arrow">Ouvrir</div>
      </router-link>
    </div>

    <div class="dashboard-info">
      <div class="info-card">
        <h3>Commander simplement</h3>
        <p>
          Consultez les prochains retraits, preparez votre panier, puis choisissez le point de collecte et le creneau au moment de valider.
        </p>
        <router-link to="/catalogue" class="btn btn-primary">
          Ouvrir le catalogue
        </router-link>
      </div>

      <div class="info-card">
        <h3>Votre parcours</h3>
        <ol class="dashboard-steps">
          <li>Selection des produits</li>
          <li>Validation du panier</li>
          <li>Choix du point de collecte</li>
          <li>Confirmation de la commande</li>
          <li>Suivi dans vos commandes et archives</li>
        </ol>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '../../stores/authStore'
import { usePanierStore } from '../../stores/panierStore'

const authStore = useAuthStore()
const panierStore = usePanierStore()
const { user } = storeToRefs(authStore)

const quickActions = computed(() => [
  { id: 1, kicker: 'Catalogue', title: 'Produits disponibles', description: 'Parcourir les produits et leurs categories', path: '/catalogue' },
  { id: 2, kicker: 'Retraits', title: 'Prochains creneaux', description: 'Voir les lieux et horaires de collecte disponibles', path: '/points-retrait' },
  { id: 3, kicker: 'Panier', title: 'Preparation commande', description: `${panierStore.getTotalArticles} article(s) actuellement selectionne(s)`, path: '/panier' },
  { id: 4, kicker: 'Commandes', title: 'Suivi et historique', description: 'Consulter vos commandes en cours et passees', path: '/mes-commandes' },
  { id: 5, kicker: 'Profil', title: 'Informations personnelles', description: 'Verifier vos coordonnees et votre compte', path: '/mon-profil' }
])
</script>

<style scoped>
/* Les styles seront herites de App.css */
</style>
