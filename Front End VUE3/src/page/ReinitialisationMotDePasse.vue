<template>
  <div class="page-container">
    <div v-if="step === 'success'" class="success-message">
      <div class="success-icon">OK</div>
      <h2>Lien de reinitialisation genere</h2>
      <p>Comme aucun email reel n'est encore envoye, le lien est affiche ici pour finaliser le flux.</p>

      <div class="email-display">
        <strong>{{ response.email }}</strong>
      </div>

      <div class="success-details">
        <p><strong>Lien :</strong></p>
        <p class="reset-link">{{ response.resetLink }}</p>
      </div>

      <div class="actions">
        <a :href="response.resetLink" class="btn btn-primary">Ouvrir le lien</a>
        <button @click="copyLink" class="btn btn-secondary">Copier le lien</button>
      </div>

      <div class="actions secondary-actions">
        <router-link to="/connexion" class="link">Retour a la connexion</router-link>
        <button @click="handleNewRequest" class="link-button">Nouvelle demande</button>
      </div>
    </div>

    <template v-else>
      <div class="form-header">
        <h1>Reinitialisation du mot de passe</h1>
        <p>Saisissez votre email pour generer un lien de reinitialisation.</p>
      </div>

      <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

      <form @submit.prevent="handleSubmit" class="reset-form">
        <div class="info-section">
          <div class="info-card">
            <h3>Comment ca marche ?</h3>
            <p>
              Le backend genere un lien de creation de mot de passe valable 24 heures.
              Pour ce TFE, le lien est affiche directement a l'ecran.
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
            {{ loading ? 'Generation en cours...' : 'Generer le lien de reinitialisation' }}
          </button>
        </div>

        <div class="auth-links">
          <router-link to="/connexion" class="link">
            Retour a la connexion
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
const formData = reactive({
  email: ''
})
const errors = reactive({})
const response = reactive({
  email: '',
  resetLink: ''
})

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
    response.email = result.email || formData.email
    response.resetLink = result.resetLink || ''
    step.value = 'success'
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(response.resetLink)
  } catch {
    apiError.value = 'Impossible de copier automatiquement le lien.'
  }
}

function handleNewRequest() {
  step.value = 'email'
  formData.email = ''
  response.email = ''
  response.resetLink = ''
  apiError.value = ''
  Object.keys(errors).forEach(key => delete errors[key])
}
</script>

<style scoped>
.reset-link {
  word-break: break-all;
}

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
