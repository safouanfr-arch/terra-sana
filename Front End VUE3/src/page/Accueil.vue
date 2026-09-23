<template>
  <div class="page-container">
    <div class="hero-section">
      <img :src="siteLogo" alt="Terra Sana" class="hero-logo" />
      <p class="hero-kicker">Commerce local organise</p>
      <h1>Terra Sana</h1>
      <p class="hero-lead">
        Une application sobre pour commander des produits locaux, choisir un retrait et suivre chaque commande sans friction.
      </p>

      <div v-if="isConnected && user" class="welcome-message">
        <h2>Bonjour {{ user.email }}</h2>
        <p>Votre espace est pret. Reprenez votre parcours directement depuis l'application.</p>
        <div class="user-actions">
          <router-link :to="connectedTarget" class="btn btn-primary">
            {{ connectedLabel }}
          </router-link>
        </div>
      </div>

      <div v-else class="visitor-section">
        <p class="intro-text">
          Commandez des produits locaux, choisissez votre point de collecte et suivez vos commandes dans une interface simple et claire.
        </p>

        <div class="action-cards">
          <div class="card">
            <span class="card-kicker">Acces membre</span>
            <h3>Premiere adhesion</h3>
            <p>Soumettez votre demande d'adhesion pour acceder au catalogue et a la commande.</p>
            <router-link to="/demande-adhesion" class="btn btn-primary">
              Faire une demande d'adhesion
            </router-link>
          </div>

          <div class="card">
            <span class="card-kicker">Connexion</span>
            <h3>Compte existant</h3>
            <p>Connectez-vous a votre espace pour consulter le catalogue, le panier et vos commandes.</p>
            <router-link to="/connexion" class="btn btn-secondary">
              Se connecter
            </router-link>
          </div>
        </div>

        <div class="help-section">
          <p>
            Probleme de connexion ?
            <router-link to="/reinitialisation-mot-de-passe" class="link">
              Reinitialisez votre mot de passe
            </router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import siteLogo from '../assets/branding/site-logo.png'

const props = defineProps({
  isConnected: {
    type: Boolean,
    default: false
  },
  user: {
    type: Object,
    default: null
  }
})

const connectedTarget = computed(() => props.user?.role === 'admin' ? '/admin' : '/catalogue')
const connectedLabel = computed(() => props.user?.role === 'admin' ? 'Ouvrir l administration' : 'Ouvrir le catalogue')
</script>

<style scoped>
/* Les styles seront herites de App.css */
</style>
