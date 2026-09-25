<template>
  <div class="app">
    <Navbar
      :isConnected="isConnected"
      :user="user"
      @logout="handleLogout"
    />

    <main class="main-content">
      <router-view />
    </main>

    <footer class="footer">
      <p>&copy; 2026 Terra Sana</p>
      <span>Plateforme cooperative pour la commande et le retrait de produits locaux.</span>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import Navbar from './components/Navbar.vue'
import { useAuthStore } from './stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const isConnected = computed(() => authStore.isAuthenticated)
const user = computed(() => authStore.user)

const handleLogout = async () => {
  await authStore.logout()
  router.push('/')
}
</script>

<style>
@import './App.css';
</style>
