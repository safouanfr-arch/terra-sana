<template>
  <nav class="navbar">
    <div class="nav-brand">
      <router-link to="/" class="brand-link">
        <img :src="siteLogo" alt="Terra Sana" class="brand-logo" />
        <div class="brand-copy">
          <strong>Terra Sana</strong>
          <span>Cooperative e-commerce locale</span>
        </div>
      </router-link>
    </div>

    <div class="nav-links">
      <template v-if="!isConnected">
        <router-link
          to="/demande-adhesion"
          class="nav-link"
          active-class="active"
        >
          Demande d'adhesion
        </router-link>
        <router-link
          to="/connexion"
          class="nav-link"
          active-class="active"
        >
          Se connecter
        </router-link>
      </template>

      <template v-else>
        <template v-if="user && user.role === 'admin'">
          <router-link
            to="/admin"
            class="nav-link"
            active-class="active"
          >
            Administration
          </router-link>
        </template>

        <template v-else>
          <router-link
            to="/catalogue"
            class="nav-link"
            active-class="active"
          >
            Catalogue
          </router-link>
          <router-link
            to="/points-retrait"
            class="nav-link"
            active-class="active"
          >
            Retraits
          </router-link>
          <router-link
            to="/panier"
            class="nav-link"
            active-class="active"
          >
            Panier
          </router-link>
          <router-link
            to="/mes-commandes"
            class="nav-link"
            active-class="active"
          >
            Mes commandes
          </router-link>
          <router-link
            to="/mes-archives"
            class="nav-link"
            active-class="active"
          >
            Archives
          </router-link>
          <router-link
            to="/mon-profil"
            class="nav-link"
            active-class="active"
          >
            Mon profil
          </router-link>
        </template>

        <span class="user-info">
          <span class="user-role">{{ user?.role === 'admin' ? 'Administration' : 'Espace membre' }}</span>
          <span class="user-email">{{ user?.email }}</span>
        </span>
        <button @click="$emit('logout')" class="logout-btn">
          Deconnexion
        </button>
      </template>
    </div>
  </nav>
</template>

<script setup>
import siteLogo from '../assets/branding/site-logo.png'

defineProps({
  isConnected: {
    type: Boolean,
    required: true
  },
  user: {
    type: Object,
    default: null
  }
})

defineEmits(['logout'])
</script>

<style scoped>
/* Les styles seront herites de App.css */
</style>
