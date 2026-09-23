<template>
  <div class="page-container">
    <div v-if="submitted" class="success-message">
      <div class="success-icon">OK</div>
      <h2>Demande d'adhesion envoyee</h2>
      <p>Merci, votre demande a bien ete enregistree dans l'application.</p>
      <div class="success-details">
        <p>
          <strong>Prochaine etape :</strong> un administrateur examinera votre demande depuis l'espace admin.
        </p>
        <p>Si elle est approuvee, un lien de creation de mot de passe sera genere.</p>
      </div>
      <div class="actions">
        <router-link to="/" class="btn btn-primary">
          Retour a l'accueil
        </router-link>
      </div>
    </div>

    <template v-else>
      <div class="form-header">
        <h1>Demande d'adhesion</h1>
        <p>Remplissez ce formulaire pour devenir membre de notre communaute</p>
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
              <label for="prenom">Prenom *</label>
              <input
                id="prenom"
                v-model="formData.prenom"
                type="text"
                :class="{ error: errors.prenom }"
                placeholder="Votre prenom"
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
              Cet email servira d'identifiant si votre demande est approuvee.
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
            <label for="telephone">Telephone *</label>
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
              placeholder="Numero et nom de rue"
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
            <label for="justification">Pourquoi souhaitez-vous adherer ?</label>
            <textarea
              id="justification"
              v-model="formData.justification"
              rows="4"
              placeholder="Expliquez brievement vos motivations..."
            />
          </div>
        </div>

        <div class="form-section">
          <div class="form-group checkbox-group">
            <label class="checkbox-label">
              <input
                v-model="formData.accepteConcept"
                type="checkbox"
                :class="{ error: errors.accepteConcept }"
              />
              <span class="checkmark"></span>
              J'accepte le concept de la plateforme et ses conditions d'utilisation *
            </label>
            <span v-if="errors.accepteConcept" class="error-message">{{ errors.accepteConcept }}</span>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? 'Envoi en cours...' : 'Envoyer ma demande d\'adhesion' }}
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
  accepteConcept: false,
  justification: ''
})

const errors = reactive({})
const submitted = ref(false)
const submitting = ref(false)
const apiError = ref('')

function validateForm() {
  const newErrors = {}

  if (!formData.nom.trim()) newErrors.nom = 'Le nom est requis'
  if (!formData.prenom.trim()) newErrors.prenom = 'Le prenom est requis'

  if (!formData.email.trim()) {
    newErrors.email = 'L email est requis'
  } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
    newErrors.email = 'Format d email invalide'
  }

  if (!formData.emailConfirm.trim()) {
    newErrors.emailConfirm = 'Merci de confirmer votre email'
  } else if (formData.email !== formData.emailConfirm) {
    newErrors.emailConfirm = 'Les emails ne correspondent pas'
  }

  if (!formData.telephone.trim()) {
    newErrors.telephone = 'Le numero de telephone est requis'
  } else if (!/^[\d\s+\-()]+$/.test(formData.telephone)) {
    newErrors.telephone = 'Format de telephone invalide'
  }

  if (!formData.rue.trim()) newErrors.rue = 'La rue est requise'

  if (!formData.codePostal.trim()) {
    newErrors.codePostal = 'Le code postal est requis'
  } else if (!/^\d{4,5}$/.test(formData.codePostal)) {
    newErrors.codePostal = 'Format de code postal invalide'
  }

  if (!formData.ville.trim()) newErrors.ville = 'La ville est requise'
  if (!formData.accepteConcept) newErrors.accepteConcept = 'Vous devez accepter le concept pour continuer'

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
      accepteConcept: formData.accepteConcept
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
</style>
