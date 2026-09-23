<template>
  <div class="access-denied-page">
    <section class="access-denied-panel">
      <div class="access-denied-code">403</div>

      <div class="access-denied-content">
        <p class="access-denied-kicker">Autorisation insuffisante</p>
        <h1>Acc&egrave;s refus&eacute;</h1>
        <p v-if="authStore.isAuthenticated">
          Votre compte est bien connect&eacute;, mais il ne poss&egrave;de pas les autorisations
          n&eacute;cessaires pour consulter cette ressource.
        </p>
        <p v-else>
          Cette ressource est prot&eacute;g&eacute;e. Connectez-vous avec un compte autoris&eacute;
          pour y acc&eacute;der.
        </p>

        <div class="access-denied-actions">
          <router-link :to="primaryTarget" class="btn btn-primary">
            {{ primaryLabel }}
          </router-link>
          <router-link to="/" class="btn btn-secondary">
            Retour &agrave; l'accueil
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()

const primaryTarget = computed(() => {
  if (authStore.isAdmin) return '/admin'
  if (authStore.isMembre) return '/membre'
  return '/connexion'
})

const primaryLabel = computed(() => {
  if (authStore.isAuthenticated) return 'Retour a mon espace'
  return 'Se connecter'
})
</script>

<style scoped>
.access-denied-page {
  min-height: min(720px, calc(100vh - 180px));
  display: grid;
  place-items: center;
  padding: 2rem 1rem;
}

.access-denied-panel {
  width: min(920px, 100%);
  display: grid;
  grid-template-columns: minmax(180px, 260px) 1fr;
  overflow: hidden;
  border: 1px solid rgba(66, 79, 74, 0.12);
  border-radius: 8px;
  background: #fffdf8;
  box-shadow: 0 18px 45px rgba(29, 39, 34, 0.08);
}

.access-denied-code {
  display: grid;
  place-items: center;
  min-height: 320px;
  background:
    linear-gradient(140deg, rgba(36, 72, 63, 0.98), rgba(74, 100, 73, 0.96)),
    #24483f;
  color: #fffdf8;
  font-size: clamp(4rem, 11vw, 8rem);
  font-weight: 800;
  letter-spacing: 0;
}

.access-denied-content {
  padding: clamp(2rem, 5vw, 4rem);
}

.access-denied-kicker {
  margin: 0 0 0.75rem;
  color: #6f7d70;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.access-denied-content h1 {
  margin: 0 0 1rem;
  color: #18231f;
  font-size: clamp(2rem, 5vw, 3.4rem);
  line-height: 1.05;
}

.access-denied-content p {
  max-width: 56ch;
  margin: 0;
  color: #4f5c56;
  font-size: 1.05rem;
}

.access-denied-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 2rem;
}

@media (max-width: 720px) {
  .access-denied-panel {
    grid-template-columns: 1fr;
  }

  .access-denied-code {
    min-height: 180px;
  }
}
</style>
