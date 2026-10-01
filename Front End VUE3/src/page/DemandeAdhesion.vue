<template>
  <div class="page-container">
    <div v-if="submitted" class="success-message">
      <div class="success-icon">OK</div>
      <h2>Demande d'adhésion enregistrée</h2>
      <p>Votre demande a été enregistrée.</p>
      <div class="success-details">
        <p>
          <strong>Prochaine étape :</strong> consultez votre boîte mail et confirmez votre adresse afin de transmettre votre demande à Terra Sana.
        </p>
        <p>La demande ne sera visible par notre équipe qu'après cette confirmation.</p>
      </div>
      <div class="actions">
        <router-link to="/" class="btn btn-primary">
          Retour à l'accueil
        </router-link>
      </div>
    </div>

    <template v-else>
      <div class="form-header">
        <h1>Demande d'adhésion</h1>
        <p>Remplissez ce formulaire pour devenir membre de notre communauté</p>
      </div>

      <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

      <form @submit.prevent="handleSubmit" class="adhesion-form">
        <div class="form-section">
          <h3>Informations personnelles</h3>

          <div class="form-row">
            <div class="form-group">
              <label for="nom">Nom *</label>
              <input
                id="nom"
                v-model="formData.nom"
                type="text"
                :class="{ error: errors.nom }"
                placeholder="Votre nom"
              />
              <span v-if="errors.nom" class="error-message">{{ errors.nom }}</span>
            </div>

            <div class="form-group">
              <label for="prenom">Prénom *</label>
              <input
                id="prenom"
                v-model="formData.prenom"
                type="text"
                :class="{ error: errors.prenom }"
                placeholder="Votre prénom"
              />
              <span v-if="errors.prenom" class="error-message">{{ errors.prenom }}</span>
            </div>
          </div>

          <div class="form-group">
            <label for="email">Email *</label>
            <input
              id="email"
              v-model="formData.email"
              type="email"
              :class="{ error: errors.email }"
              placeholder="votre.email@exemple.com"
            />
            <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
            <small class="field-help">
              Cet email servira d'identifiant si votre demande est approuvée.
            </small>
          </div>

          <div class="form-group">
            <label for="emailConfirm">Confirmer l'email *</label>
            <input
              id="emailConfirm"
              v-model="formData.emailConfirm"
              type="email"
              :class="{ error: errors.emailConfirm }"
              placeholder="Ressaisissez votre email"
            />
            <span v-if="errors.emailConfirm" class="error-message">{{ errors.emailConfirm }}</span>
          </div>

          <div class="form-group">
            <label for="telephone">Téléphone *</label>
            <input
              id="telephone"
              v-model="formData.telephone"
              type="tel"
              :class="{ error: errors.telephone }"
              placeholder="0470 00 00 00"
            />
            <span v-if="errors.telephone" class="error-message">{{ errors.telephone }}</span>
          </div>
        </div>

        <div class="form-section">
          <h3>Adresse</h3>

          <div class="form-group">
            <label for="rue">Rue *</label>
            <input
              id="rue"
              v-model="formData.rue"
              type="text"
              :class="{ error: errors.rue }"
              placeholder="Numéro et nom de rue"
            />
            <span v-if="errors.rue" class="error-message">{{ errors.rue }}</span>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="codePostal">Code postal *</label>
              <input
                id="codePostal"
                v-model="formData.codePostal"
                type="text"
                :class="{ error: errors.codePostal }"
                placeholder="1000"
              />
              <span v-if="errors.codePostal" class="error-message">{{ errors.codePostal }}</span>
            </div>

            <div class="form-group">
              <label for="ville">Ville *</label>
              <input
                id="ville"
                v-model="formData.ville"
                type="text"
                :class="{ error: errors.ville }"
                placeholder="Bruxelles"
              />
              <span v-if="errors.ville" class="error-message">{{ errors.ville }}</span>
            </div>
          </div>
        </div>

        <div class="form-section">
          <h3>Motivation</h3>

          <div class="form-group">
            <label for="justification">Pourquoi souhaitez-vous adhérer ?</label>
            <textarea
              id="justification"
              v-model="formData.justification"
              rows="4"
              placeholder="Expliquez brièvement vos motivations..."
            />
          </div>
        </div>

        <div class="form-section">
          <div class="form-group checkbox-group">
            <div class="conditions-consent">
              <input
                id="conditionsAdhesion"
                v-model="formData.accepteConditions"
                type="checkbox"
                required
                :class="{ error: errors.accepteConditions }"
              />
              <span class="conditions-consent-text">
                <label for="conditionsAdhesion">J'ai lu et j'accepte les</label>
                <router-link to="/conditions-adhesion" target="_blank" rel="noopener noreferrer">
                  conditions d'adhésion de Terra Sana
                </router-link><span>.</span>
              </span>
            </div>
            <span v-if="errors.accepteConditions" class="error-message">{{ errors.accepteConditions }}</span>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? 'Envoi en cours...' : 'Envoyer ma demande d\'adhésion' }}
          </button>
          <router-link to="/" class="btn btn-secondary">
            Annuler
          </router-link>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { demandesAdhesionApi, getApiErrorMessage } from '../services/api.js'

const formData = reactive({
  nom: '',
  prenom: '',
  email: '',
  emailConfirm: '',
  telephone: '',
  rue: '',
  codePostal: '',
  ville: '',
  accepteConditions: false,
  justification: ''
})

const errors = reactive({})
const submitted = ref(false)
const submitting = ref(false)
const apiError = ref('')

function validateForm() {
  const newErrors = {}

  if (!formData.nom.trim()) newErrors.nom = 'Le nom est requis'
  if (!formData.prenom.trim()) newErrors.prenom = 'Le prénom est requis'

  if (!formData.email.trim()) {
    newErrors.email = "L'email est requis"
  } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
    newErrors.email = "Format d'email invalide"
  }

  if (!formData.emailConfirm.trim()) {
    newErrors.emailConfirm = 'Merci de confirmer votre email'
  } else if (formData.email !== formData.emailConfirm) {
    newErrors.emailConfirm = 'Les emails ne correspondent pas'
  }

  if (!formData.telephone.trim()) {
    newErrors.telephone = 'Le numéro de téléphone est requis'
  } else if (!/^[\d\s+\-()]+$/.test(formData.telephone)) {
    newErrors.telephone = 'Format de téléphone invalide'
  }

  if (!formData.rue.trim()) newErrors.rue = 'La rue est requise'

  if (!formData.codePostal.trim()) {
    newErrors.codePostal = 'Le code postal est requis'
  } else if (!/^\d{4,5}$/.test(formData.codePostal)) {
    newErrors.codePostal = 'Format de code postal invalide'
  }

  if (!formData.ville.trim()) newErrors.ville = 'La ville est requise'
  if (!formData.accepteConditions) newErrors.accepteConditions = 'Vous devez accepter les conditions pour continuer'

  return newErrors
}

async function handleSubmit() {
  Object.keys(errors).forEach(key => delete errors[key])
  apiError.value = ''

  const newErrors = validateForm()
  if (Object.keys(newErrors).length > 0) {
    Object.assign(errors, newErrors)
    return
  }

  submitting.value = true
  try {
    await demandesAdhesionApi.create({
      nom: formData.nom,
      prenom: formData.prenom,
      email: formData.email,
      telephone: formData.telephone,
      rue: formData.rue,
      codePostal: formData.codePostal,
      ville: formData.ville,
      justification: formData.justification,
      accepteConditions: formData.accepteConditions === true
    })
    submitted.value = true
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

</script>

<style scoped>
/* Les styles seront herites de App.css */

.conditions-consent {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  align-items: start;
  gap: 0.55rem;
}

.conditions-consent input[type="checkbox"] {
  width: 18px;
  height: 18px;
  padding: 0;
  margin: 0.15rem 0 0;
}

.conditions-consent-text {
  min-width: 0;
  line-height: 1.5;
}

.conditions-consent-text label {
  display: inline;
  margin: 0 0.25rem 0 0;
}
</style>
