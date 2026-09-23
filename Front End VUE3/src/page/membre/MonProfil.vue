<template>
  <div class="profil-page">
    <div class="profil-header">
      <div>
        <h1>Mon profil</h1>
        <p>Gerez vos informations personnelles et votre mot de passe.</p>
      </div>
      <router-link to="/catalogue" class="btn btn-secondary">
        Retour au catalogue
      </router-link>
    </div>

    <div class="profil-container">
      <div class="profil-tabs">
        <button :class="['tab-btn', { active: activeTab === 'profil' }]" @click="activeTab = 'profil'">
          Informations personnelles
        </button>
        <button :class="['tab-btn', { active: activeTab === 'password' }]" @click="activeTab = 'password'">
          Mot de passe
        </button>
      </div>

      <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

      <div v-if="activeTab === 'profil'" class="tab-content">
        <form class="profil-form" @submit.prevent="handleProfilSubmit">
          <div v-if="profilSaved" class="alert alert-success">
            Vos informations ont ete mises a jour.
          </div>

          <div class="form-section">
            <h3>Informations personnelles</h3>

            <div class="form-row">
              <div class="form-group">
                <label for="nom">Nom *</label>
                <input id="nom" v-model="profilData.nom" type="text" required />
              </div>

              <div class="form-group">
                <label for="prenom">Prenom *</label>
                <input id="prenom" v-model="profilData.prenom" type="text" required />
              </div>
            </div>

            <div class="form-group">
              <label for="email">Email *</label>
              <input id="email" v-model="profilData.email" type="email" required />
            </div>

            <div class="form-group">
              <label for="telephone">Telephone *</label>
              <input id="telephone" v-model="profilData.telephone" type="tel" required />
            </div>
          </div>

          <div class="form-section">
            <h3>Adresse</h3>

            <div class="form-group">
              <label for="rue">Rue *</label>
              <input id="rue" v-model="profilData.rue" type="text" required />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label for="codePostal">Code postal *</label>
                <input id="codePostal" v-model="profilData.codePostal" type="text" required />
              </div>

              <div class="form-group">
                <label for="ville">Ville *</label>
                <input id="ville" v-model="profilData.ville" type="text" required />
              </div>
            </div>

            <div class="form-group">
              <label for="pays">Pays *</label>
              <input id="pays" v-model="profilData.pays" type="text" required />
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="profilLoading">
              {{ profilLoading ? 'Enregistrement...' : 'Enregistrer les modifications' }}
            </button>
          </div>
        </form>
      </div>

      <div v-if="activeTab === 'password'" class="tab-content">
        <form class="password-form" @submit.prevent="handlePasswordSubmit">
          <div v-if="passwordSaved" class="alert alert-success">
            Votre mot de passe a ete modifie.
          </div>

          <div class="form-section">
            <h3>Changer de mot de passe</h3>

            <div class="form-group">
              <label for="currentPassword">Mot de passe actuel *</label>
              <div class="password-field">
                <input
                  id="currentPassword"
                  v-model="passwordData.currentPassword"
                  :type="showCurrentPassword ? 'text' : 'password'"
                  :class="{ error: passwordErrors.currentPassword }"
                />
                <button type="button" class="password-toggle" @click="showCurrentPassword = !showCurrentPassword">
                  {{ showCurrentPassword ? 'Masquer' : 'Afficher' }}
                </button>
              </div>
              <span v-if="passwordErrors.currentPassword" class="error-message">
                {{ passwordErrors.currentPassword }}
              </span>
            </div>

            <div class="form-group">
              <label for="newPassword">Nouveau mot de passe *</label>
              <div class="password-field">
                <input
                  id="newPassword"
                  v-model="passwordData.newPassword"
                  :type="showNewPassword ? 'text' : 'password'"
                  :class="{ error: passwordErrors.newPassword }"
                />
                <button type="button" class="password-toggle" @click="showNewPassword = !showNewPassword">
                  {{ showNewPassword ? 'Masquer' : 'Afficher' }}
                </button>
              </div>
              <div v-if="passwordErrors.newPassword" class="error-message">
                <div v-if="Array.isArray(passwordErrors.newPassword)">
                  <p>Le mot de passe doit contenir :</p>
                  <ul>
                    <li v-for="(error, index) in passwordErrors.newPassword" :key="index">{{ error }}</li>
                  </ul>
                </div>
                <span v-else>{{ passwordErrors.newPassword }}</span>
              </div>
            </div>

            <div class="password-requirements">
              <p><strong>Exigences du mot de passe :</strong></p>
              <div class="requirements-list">
                <div :class="['requirement', { met: passwordData.newPassword.length >= 8 }]">
                  Au moins 8 caracteres
                </div>
                <div :class="['requirement', { met: /[A-Z]/.test(passwordData.newPassword) }]">
                  Au moins une majuscule
                </div>
                <div :class="['requirement', { met: /[a-z]/.test(passwordData.newPassword) }]">
                  Au moins une minuscule
                </div>
                <div :class="['requirement', { met: /[0-9]/.test(passwordData.newPassword) }]">
                  Au moins un chiffre
                </div>
                <div :class="['requirement', { met: /[^A-Za-z0-9]/.test(passwordData.newPassword) }]">
                  Au moins un caractere special
                </div>
              </div>
            </div>

            <div class="form-group">
              <label for="confirmPassword">Confirmer le nouveau mot de passe *</label>
              <div class="password-field">
                <input
                  id="confirmPassword"
                  v-model="passwordData.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  :class="{ error: passwordErrors.confirmPassword }"
                />
                <button type="button" class="password-toggle" @click="showConfirmPassword = !showConfirmPassword">
                  {{ showConfirmPassword ? 'Masquer' : 'Afficher' }}
                </button>
              </div>
              <span v-if="passwordErrors.confirmPassword" class="error-message">
                {{ passwordErrors.confirmPassword }}
              </span>
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="passwordLoading">
              {{ passwordLoading ? 'Mise a jour...' : 'Modifier le mot de passe' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authApi, getApiErrorMessage, handleApiAccessError, isApiAccessError } from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore'

const props = defineProps({
  user: {
    type: Object,
    default: null
  }
})

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('profil')
const apiError = ref('')
const profilSaved = ref(false)
const passwordSaved = ref(false)
const profilLoading = ref(false)
const passwordLoading = ref(false)
const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

const profilData = reactive({
  nom: '',
  prenom: '',
  email: '',
  telephone: '',
  rue: '',
  ville: '',
  codePostal: '',
  pays: 'Belgique'
})

const passwordData = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordErrors = ref({})

watch(
  () => props.user,
  user => {
    if (!user) {
      return
    }
    profilData.nom = user.nom || ''
    profilData.prenom = user.prenom || ''
    profilData.email = user.email || ''
    profilData.telephone = user.telephone || ''
    profilData.rue = user.adresse || user.rue || ''
    profilData.ville = user.ville || ''
    profilData.codePostal = user.codePostal || ''
    profilData.pays = user.pays || 'Belgique'
  },
  { immediate: true }
)

async function handleProfilSubmit() {
  apiError.value = ''
  profilSaved.value = false
  profilLoading.value = true

  try {
    const updatedUser = await authApi.updateMe({
      nom: profilData.nom,
      prenom: profilData.prenom,
      email: profilData.email,
      telephone: profilData.telephone,
      rue: profilData.rue,
      adresse: profilData.rue,
      ville: profilData.ville,
      codePostal: profilData.codePostal,
      pays: profilData.pays
    })
    authStore.setUser(updatedUser)
    profilSaved.value = true
    window.setTimeout(() => {
      profilSaved.value = false
    }, 3000)
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
    }
  } finally {
    profilLoading.value = false
  }
}

function validatePassword(password) {
  const errors = []
  if (password.length < 8) errors.push('Au moins 8 caracteres')
  if (!/[A-Z]/.test(password)) errors.push('Au moins une majuscule')
  if (!/[a-z]/.test(password)) errors.push('Au moins une minuscule')
  if (!/[0-9]/.test(password)) errors.push('Au moins un chiffre')
  if (!/[^A-Za-z0-9]/.test(password)) errors.push('Au moins un caractere special')
  return errors
}

async function handlePasswordSubmit() {
  const errors = {}
  apiError.value = ''
  passwordSaved.value = false

  if (!passwordData.currentPassword) {
    errors.currentPassword = 'Le mot de passe actuel est requis'
  }
  if (!passwordData.newPassword) {
    errors.newPassword = 'Le nouveau mot de passe est requis'
  } else {
    const validationErrors = validatePassword(passwordData.newPassword)
    if (validationErrors.length > 0) {
      errors.newPassword = validationErrors
    }
  }
  if (!passwordData.confirmPassword) {
    errors.confirmPassword = 'La confirmation est requise'
  } else if (passwordData.newPassword !== passwordData.confirmPassword) {
    errors.confirmPassword = 'Les mots de passe ne correspondent pas'
  }

  if (Object.keys(errors).length > 0) {
    passwordErrors.value = errors
    return
  }

  passwordLoading.value = true
  try {
    await authApi.changePassword({
      currentPassword: passwordData.currentPassword,
      newPassword: passwordData.newPassword
    })
    passwordData.currentPassword = ''
    passwordData.newPassword = ''
    passwordData.confirmPassword = ''
    passwordErrors.value = {}
    passwordSaved.value = true
    window.setTimeout(() => {
      passwordSaved.value = false
    }, 3000)
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
    }
  } finally {
    passwordLoading.value = false
  }
}

function handleSessionError(error) {
  return handleApiAccessError(error, authStore, router)
}
</script>

<style scoped>
/* Les styles principaux restent ceux de App.css */
</style>
