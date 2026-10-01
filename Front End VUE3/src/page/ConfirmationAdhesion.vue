<template>
  <div class="page-container confirmation-page">
    <div class="confirmation-panel" role="status" aria-live="polite">
      <div v-if="state === 'loading'">
        <h1>Confirmation en cours</h1>
        <p>Vérification de votre lien de confirmation...</p>
      </div>

      <div v-else-if="state === 'confirmed'" class="success-message">
        <h1>Adresse email confirmée</h1>
        <p>Votre adresse email a été confirmée. Votre demande a été transmise à Terra Sana.</p>
      </div>

      <div v-else-if="state === 'already-confirmed'" class="success-message">
        <h1>Adresse déjà confirmée</h1>
        <p>Votre adresse email a déjà été confirmée. Votre demande est en attente de traitement.</p>
      </div>

      <div v-else-if="state === 'expired'" class="alert alert-error">
        <h1>Lien expiré</h1>
        <p>Ce lien de confirmation a expiré.</p>
      </div>

      <div v-else class="alert alert-error">
        <h1>Lien invalide</h1>
        <p>Ce lien de confirmation est invalide.</p>
      </div>

      <form
        v-if="state === 'expired' || state === 'invalid'"
        class="resend-form"
        @submit.prevent="resendConfirmation"
      >
        <h2>Renvoyer un lien de confirmation</h2>
        <label for="confirmationEmail">Adresse email utilisée lors de la demande</label>
        <input
          id="confirmationEmail"
          v-model.trim="email"
          type="email"
          autocomplete="email"
          required
        />
        <button type="submit" class="btn btn-secondary" :disabled="resending">
          {{ resending ? 'Envoi en cours...' : 'Renvoyer le lien' }}
        </button>
        <p v-if="resendMessage" class="resend-message">{{ resendMessage }}</p>
        <p v-if="resendError" class="error-message">{{ resendError }}</p>
      </form>

      <router-link to="/" class="btn btn-primary">Retour à l'accueil</router-link>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { demandesAdhesionApi } from '../services/api.js'

const route = useRoute()
const state = ref('loading')
const email = ref('')
const resending = ref(false)
const resendMessage = ref('')
const resendError = ref('')

async function resendConfirmation() {
  resending.value = true
  resendMessage.value = ''
  resendError.value = ''
  try {
    const response = await demandesAdhesionApi.resendConfirmation(email.value)
    resendMessage.value = response.message
  } catch (error) {
    resendError.value = error?.response?.status === 429
      ? 'Trop de demandes. Veuillez patienter quelques minutes avant de réessayer.'
      : 'Le renvoi est momentanément indisponible. Veuillez réessayer plus tard.'
  } finally {
    resending.value = false
  }
}

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token.trim() : ''
  if (!token) {
    state.value = 'invalid'
    return
  }

  try {
    const response = await demandesAdhesionApi.confirmEmail(token)
    state.value = {
      CONFIRMED: 'confirmed',
      ALREADY_CONFIRMED: 'already-confirmed',
      EXPIRED: 'expired',
      INVALID: 'invalid'
    }[response.statut] || 'invalid'
  } catch {
    state.value = 'invalid'
  }
})
</script>

<style scoped>
.confirmation-page {
  display: grid;
  place-items: center;
  min-height: 65vh;
}

.confirmation-panel {
  width: min(100%, 620px);
  text-align: center;
}

.confirmation-panel .btn {
  margin-top: 1.25rem;
}

.resend-form {
  display: grid;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #d8e0dc;
  text-align: left;
}

.resend-form h2 {
  margin: 0;
  font-size: 1.1rem;
}

.resend-form .btn {
  justify-self: start;
  margin-top: 0;
}

.resend-message {
  color: #315d4b;
}
</style>
