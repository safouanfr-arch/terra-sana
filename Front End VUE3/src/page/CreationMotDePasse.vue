<template>
  <div class="page-container">
    <div v-if="loading" class="loading-message">
      <div class="spinner"></div>
      <h2>Verification du lien</h2>
      <p>Nous verifions votre lien de creation de mot de passe.</p>
    </div>

    <div v-else-if="!tokenValid" class="error-message">
      <div class="error-icon">X</div>
      <h2>Lien invalide ou expire</h2>
      <p>Le lien utilise n'est plus valable.</p>
      <div class="actions">
        <router-link to="/reinitialisation-mot-de-passe" class="btn btn-primary">
          Demander un nouveau lien
        </router-link>
        <router-link to="/" class="btn btn-secondary">
          Retour a l'accueil
        </router-link>
      </div>
    </div>

    <div v-else-if="success" class="success-message">
      <div class="success-icon">OK</div>
      <h2>Mot de passe cree avec succes</h2>
      <p>Votre compte est pret. Vous pouvez maintenant vous connecter.</p>
      <div class="actions">
        <router-link to="/connexion" class="btn btn-primary">
          Se connecter
        </router-link>
      </div>
    </div>

    <template v-else>
      <div class="form-header">
        <h1>Creer votre mot de passe</h1>
        <p>{{ tokenEmail ? `Compte concerne : ${tokenEmail}` : 'Definissez votre mot de passe.' }}</p>
      </div>

      <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

      <form @submit.prevent="handleSubmit" class="password-form">
        <div class="form-section">
          <div class="form-group">
            <label for="motDePasse">Nouveau mot de passe *</label>
            <div class="password-field">
              <input
                id="motDePasse"
                v-model="formData.motDePasse"
                :type="showPassword ? 'text' : 'password'"
                :class="{ error: errors.motDePasse }"
              />
              <button type="button" class="password-toggle" @click="showPassword = !showPassword">
                {{ showPassword ? 'Masquer' : 'Afficher' }}
              </button>
            </div>
            <div v-if="errors.motDePasse" class="error-message">
              <template v-if="Array.isArray(errors.motDePasse)">
                <p>Le mot de passe doit contenir :</p>
                <ul>
                  <li v-for="(error, index) in errors.motDePasse" :key="index">{{ error }}</li>
                </ul>
              </template>
              <template v-else>{{ errors.motDePasse }}</template>
            </div>
          </div>

          <div class="password-requirements">
            <p><strong>Exigences du mot de passe :</strong></p>
            <div class="requirements-list">
              <div :class="['requirement', { met: formData.motDePasse.length >= 8 }]">Au moins 8 caracteres</div>
              <div :class="['requirement', { met: /[A-Z]/.test(formData.motDePasse) }]">Au moins une majuscule</div>
              <div :class="['requirement', { met: /[a-z]/.test(formData.motDePasse) }]">Au moins une minuscule</div>
              <div :class="['requirement', { met: /[0-9]/.test(formData.motDePasse) }]">Au moins un chiffre</div>
              <div :class="['requirement', { met: /[^A-Za-z0-9]/.test(formData.motDePasse) }]">Au moins un caractere special</div>
            </div>
          </div>

          <div class="form-group">
            <label for="confirmMotDePasse">Confirmer le mot de passe *</label>
            <div class="password-field">
              <input
                id="confirmMotDePasse"
                v-model="formData.confirmMotDePasse"
                :type="showConfirmPassword ? 'text' : 'password'"
                :class="{ error: errors.confirmMotDePasse }"
              />
              <button type="button" class="password-toggle" @click="showConfirmPassword = !showConfirmPassword">
                {{ showConfirmPassword ? 'Masquer' : 'Afficher' }}
              </button>
            </div>
            <span v-if="errors.confirmMotDePasse" class="error-message">{{ errors.confirmMotDePasse }}</span>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? 'Creation en cours...' : 'Creer mon mot de passe' }}
          </button>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi, getApiErrorMessage } from '../services/api.js'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const success = ref(false)
const tokenValid = ref(false)
const tokenEmail = ref('')
const apiError = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const formData = reactive({
  motDePasse: '',
  confirmMotDePasse: ''
})

const errors = reactive({})

onMounted(async () => {
  await validateToken()
})

async function validateToken() {
  loading.value = true
  apiError.value = ''
  try {
    const status = await authApi.validatePasswordToken(route.params.token)
    tokenValid.value = status.valid === true
    tokenEmail.value = status.email || ''
  } catch (error) {
    tokenValid.value = false
    apiError.value = getApiErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function validatePassword(password) {
  const validationErrors = []
  if (password.length < 8) validationErrors.push('Au moins 8 caracteres')
  if (!/[A-Z]/.test(password)) validationErrors.push('Au moins une majuscule')
  if (!/[a-z]/.test(password)) validationErrors.push('Au moins une minuscule')
  if (!/[0-9]/.test(password)) validationErrors.push('Au moins un chiffre')
  if (!/[^A-Za-z0-9]/.test(password)) validationErrors.push('Au moins un caractere special')
  return validationErrors
}

async function handleSubmit() {
  Object.keys(errors).forEach(key => delete errors[key])
  apiError.value = ''

  if (!tokenValid.value) {
    apiError.value = 'Le lien n est plus valide.'
    return
  }

  if (!formData.motDePasse) {
    errors.motDePasse = 'Le mot de passe est requis'
  } else {
    const passwordErrors = validatePassword(formData.motDePasse)
    if (passwordErrors.length > 0) {
      errors.motDePasse = passwordErrors
    }
  }

  if (!formData.confirmMotDePasse) {
    errors.confirmMotDePasse = 'La confirmation est requise'
  } else if (formData.motDePasse !== formData.confirmMotDePasse) {
    errors.confirmMotDePasse = 'Les mots de passe ne correspondent pas'
  }

  if (Object.keys(errors).length > 0) {
    return
  }

  submitting.value = true
  try {
    await authApi.completePasswordReset(route.params.token, formData.motDePasse)
    success.value = true
    window.setTimeout(() => {
      router.push('/connexion')
    }, 2000)
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* Les styles principaux viennent de App.css */
</style>
