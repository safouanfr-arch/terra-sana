<template>
  <div class="page-container">
    <div v-if="step === 'success'" class="success-message">
      <div class="success-icon">OK</div>
      <h2>Demande prise en compte</h2>
      <p>{{ responseMessage }}</p>

      <div class="actions secondary-actions">
        <router-link to="/connexion" class="link">Retour à la connexion</router-link>
        <button @click="handleNewRequest" class="link-button">Nouvelle demande</button>
      </div>
    </div>

    <template v-else>
      <div class="form-header">
        <h1>Réinitialisation du mot de passe</h1>
        <p>Saisissez votre email pour recevoir un lien de réinitialisation.</p>
      </div>

      <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

      <form @submit.prevent="handleSubmit" class="reset-form">
        <div class="info-section">
          <div class="info-card">
            <h3>Comment ça marche ?</h3>
            <p>
              Si un compte correspond à cette adresse, un lien valable 24 heures sera envoyé par email.
            </p>
          </div>
        </div>

        <div class="form-section">
          <div class="form-group">
            <label for="email">Votre adresse email</label>
            <input
              id="email"
              v-model="formData.email"
              type="email"
              :class="{ error: errors.email }"
              :disabled="loading"
            />
            <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="loading">
            {{ loading ? 'Envoi en cours...' : 'Envoyer la demande' }}
          </button>
        </div>

        <div class="auth-links">
          <router-link to="/connexion" class="link">
            Retour à la connexion
          </router-link>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { authApi, getApiErrorMessage } from '../services/api.js'

const step = ref('email')
const loading = ref(false)
const apiError = ref('')
const genericMessage = 'Si un compte correspond à cette adresse, un email de réinitialisation a été envoyé.'
const responseMessage = ref('')
const formData = reactive({
  email: ''
})
const errors = reactive({})

function validateEmail() {
  const newErrors = {}
  if (!formData.email.trim()) {
    newErrors.email = "L'email est requis"
  } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
    newErrors.email = "Format d'email invalide"
  }
  return newErrors
}

async function handleSubmit() {
  Object.keys(errors).forEach(key => delete errors[key])
  apiError.value = ''

  const newErrors = validateEmail()
  if (Object.keys(newErrors).length > 0) {
    Object.assign(errors, newErrors)
    return
  }

  loading.value = true
  try {
    const result = await authApi.requestPasswordReset(formData.email)
    responseMessage.value = result.message || genericMessage
    step.value = 'success'
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function handleNewRequest() {
  step.value = 'email'
  formData.email = ''
  responseMessage.value = ''
  apiError.value = ''
  Object.keys(errors).forEach(key => delete errors[key])
}
</script>

<style scoped>
.secondary-actions {
  gap: 1rem;
}

.link-button {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-decoration: underline;
}
</style>
