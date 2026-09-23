<template>
  <div class="page-container">
    <div class="form-header">
      <h1>Connexion</h1>
      <p>Accedez a votre espace membre</p>
    </div>

    <div class="auth-container">
      <form @submit.prevent="handleSubmit" class="login-form">
        <div v-if="errors.general" class="alert alert-error">
          <span class="alert-icon">!</span>
          {{ errors.general }}
        </div>

        <div class="form-section">
          <div class="form-group">
            <label for="email">Email</label>
            <input
              id="email"
              v-model="formData.email"
              type="email"
              :class="{ error: errors.email }"
              placeholder="votre.email@exemple.com"
              :disabled="loading"
            />
            <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
          </div>

          <div class="form-group">
            <label for="motDePasse">Mot de passe</label>
            <div class="password-field">
              <input
                id="motDePasse"
                v-model="formData.motDePasse"
                :type="showPassword ? 'text' : 'password'"
                :class="{ error: errors.motDePasse }"
                placeholder="Votre mot de passe"
                :disabled="loading"
              />
              <button
                type="button"
                class="password-toggle"
                :disabled="loading"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? 'Masquer' : 'Afficher' }}
              </button>
            </div>
            <span v-if="errors.motDePasse" class="error-message">{{ errors.motDePasse }}</span>
          </div>

        </div>

        <div class="form-actions">
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="loading"
          >
            <template v-if="loading">
              <span class="spinner small"></span>
              Connexion en cours...
            </template>
            <template v-else>
              Se connecter
            </template>
          </button>
        </div>

        <div class="auth-links">
          <router-link to="/reinitialisation-mot-de-passe" class="link">
            Mot de passe oublie ?
          </router-link>
        </div>
      </form>

      <div class="auth-divider">
        <span>ou</span>
      </div>

      <div class="auth-alternative">
        <h3>Pas encore membre ?</h3>
        <p>Rejoignez notre communaute en soumettant une demande d'adhesion.</p>
        <router-link to="/demande-adhesion" class="btn btn-secondary">
          Demande d'adhesion
        </router-link>
      </div>
    </div>

    <div class="demo-info">
      <div class="info-card">
        <h4>Information de connexion</h4>
        <p>
          Utilisez un compte administrateur ou membre existant en base.
          Les comptes de demonstration actuels sont
          <strong>admin.demo@terrasana.test</strong> et
          <strong>membre.demo@terrasana.test</strong> avec le mot de passe
          <strong>demo-password</strong>.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formData = reactive({
  email: '',
  motDePasse: ''
})

const errors = reactive({})
const loading = ref(false)
const showPassword = ref(false)

const validateForm = () => {
  const newErrors = {}

  if (!formData.email.trim()) {
    newErrors.email = "L'email est requis"
  } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
    newErrors.email = "Format d'email invalide"
  }

  if (!formData.motDePasse) {
    newErrors.motDePasse = 'Le mot de passe est requis'
  }

  return newErrors
}

const defaultRouteForRole = role => role === 'admin' ? '/admin' : '/membre'

const isRedirectAllowedForRole = (target, role) => {
  if (!target || target === '/connexion' || target === '/403') {
    return false
  }

  const resolved = router.resolve(target)
  if (!resolved.matched.length) {
    return false
  }

  const requiredRole = resolved.matched.find(record => record.meta?.role)?.meta.role
  return !requiredRole || requiredRole === role
}

const resolvePostLoginRoute = user => {
  const fallback = defaultRouteForRole(user.role)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  return isRedirectAllowedForRole(redirect, user.role) ? redirect : fallback
}

const handleSubmit = async () => {
  const newErrors = validateForm()
  Object.keys(errors).forEach(key => delete errors[key])

  if (Object.keys(newErrors).length > 0) {
    Object.assign(errors, newErrors)
    return
  }

  loading.value = true

  try {
    const user = await authStore.login({
      email: formData.email,
      motDePasse: formData.motDePasse
    })

    router.push(resolvePostLoginRoute(user))
  } catch (error) {
    errors.general = error.message || 'Email ou mot de passe incorrect.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Les styles seront herites de App.css */
</style>
