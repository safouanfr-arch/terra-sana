<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <router-link to="/admin" class="back-link">Retour au dashboard</router-link>
        <h1>Parametres emails</h1>
        <p>Configuration backend des modeles d'emails et de l'identite Terra Sana.</p>
      </div>
    </div>

    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <div v-if="loading" class="empty-state">
      <div class="empty-icon">...</div>
      <h3>Chargement de la configuration email</h3>
    </div>

    <div v-else class="parametres-emails">
      <div class="card">
        <h2>Informations generales</h2>

        <div class="form-group">
          <label for="nomEntreprise">Nom de l'entreprise</label>
          <input id="nomEntreprise" v-model="config.nomEntreprise" type="text" class="form-control" />
        </div>

        <div class="form-group">
          <label for="adresseEmail">Adresse email</label>
          <input id="adresseEmail" v-model="config.adresseEmail" type="email" class="form-control" />
        </div>

        <div class="form-group">
          <label for="telephone">Telephone</label>
          <input id="telephone" v-model="config.telephone" type="tel" class="form-control" />
        </div>

        <div class="form-group">
          <label for="adressePostale">Adresse postale</label>
          <input id="adressePostale" v-model="config.adressePostale" type="text" class="form-control" />
        </div>

        <div class="form-group">
          <label for="mentionsLegales">Mentions legales</label>
          <input id="mentionsLegales" v-model="config.mentionsLegales" type="text" class="form-control" />
        </div>
      </div>

      <div v-for="template in orderedTemplates" :key="template.type" class="card">
        <div style="display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 12px;">
          <div>
            <h2>{{ getTemplateLabel(template.type) }}</h2>
            <p style="margin: 4px 0 0; color: var(--text-secondary); font-size: 0.95rem;">
              {{ getTemplateDescription(template.type) }}
            </p>
          </div>
          <label class="checkbox-label" style="white-space: nowrap;">
            <input v-model="template.active" type="checkbox" />
            <span>Template actif</span>
          </label>
        </div>

        <div class="form-group">
          <label>Objet de l'email</label>
          <input v-model="template.objet" type="text" class="form-control" />
        </div>

        <div class="form-group">
          <label>Contenu de l'email</label>
          <textarea
            v-model="template.contenu"
            class="form-control"
            rows="12"
            style="font-family: monospace"
          />
          <small class="form-hint">
            Variables possibles :
            {{ getVariablesHint(template.type) }}
          </small>
        </div>

        <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
          <button class="btn btn-outline" @click="previewTemplate(template)">
            Previsualiser
          </button>
          <small v-if="template.updatedAt" class="form-hint">
            Derniere mise a jour : {{ formatDateTime(template.updatedAt) }}
          </small>
        </div>
      </div>

      <div class="actions-bar">
        <button class="btn btn-primary btn-lg" @click="handleSave" :disabled="saving">
          {{ saving ? 'Enregistrement...' : 'Enregistrer la configuration' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  emailSettingsApi,
  emailTemplatesApi,
  getApiErrorMessage,
  handleApiAccessError,
  isApiAccessError
} from '../../services/api.js'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const CORE_TEMPLATE_KEYS = {
  ORDER_CONFIRMATION: 'confirmationCommande',
  ORDER_REMINDER_J1: 'rappelJ1',
  ORDER_CANCELLATION: 'annulationCommande',
  MEMBERSHIP_VALIDATION: 'validationAdhesion'
}

const TEMPLATE_ORDER = [
  'ORDER_CONFIRMATION',
  'ORDER_REMINDER_J1',
  'ORDER_CANCELLATION',
  'ORDER_READY',
  'MEMBERSHIP_REQUEST_RECEIVED',
  'MEMBERSHIP_VALIDATION',
  'MEMBERSHIP_REJECTION',
  'PASSWORD_SETUP',
  'LOW_STOCK_ALERT',
  'OUT_OF_STOCK_ALERT'
]

const TEMPLATE_LABELS = {
  ORDER_CONFIRMATION: 'Email de confirmation de commande',
  ORDER_REMINDER_J1: 'Email de rappel J-1',
  ORDER_CANCELLATION: "Email d'annulation de commande",
  ORDER_READY: 'Email commande prete',
  MEMBERSHIP_REQUEST_RECEIVED: "Email de reception d'une demande d'adhesion",
  MEMBERSHIP_VALIDATION: "Email de validation d'adhesion",
  MEMBERSHIP_REJECTION: "Email de refus d'adhesion",
  PASSWORD_SETUP: 'Email de definition ou reinitialisation du mot de passe',
  LOW_STOCK_ALERT: 'Email alerte stock faible',
  OUT_OF_STOCK_ALERT: 'Email rupture de stock'
}

const TEMPLATE_DESCRIPTIONS = {
  ORDER_CONFIRMATION: 'Envoye a la creation de la commande.',
  ORDER_REMINDER_J1: "Envoye manuellement par l'administrateur avant le retrait.",
  ORDER_CANCELLATION: "Envoye lorsqu'une commande est annulee.",
  ORDER_READY: 'Envoye quand une commande passe au statut prete.',
  MEMBERSHIP_REQUEST_RECEIVED: "Envoye a la reception d'une demande d'adhesion.",
  MEMBERSHIP_VALIDATION: "Envoye quand une demande d'adhesion est acceptee.",
  MEMBERSHIP_REJECTION: "Envoye quand une demande d'adhesion est refusee.",
  PASSWORD_SETUP: 'Envoye pour definir ou reinitialiser le mot de passe.',
  LOW_STOCK_ALERT: 'Envoye quand un produit passe sous le seuil de vigilance.',
  OUT_OF_STOCK_ALERT: 'Envoye quand un produit tombe en rupture de stock.'
}

const VARIABLE_HINTS = {
  common: ['{{prenom}}', '{{nom}}', '{{companyName}}', '{{companyEmail}}', '{{companyPhone}}', '{{companyAddress}}', '{{legalMentions}}'],
  order: ['{{numero}}', '{{montant}}', '{{codeRetrait}}', '{{pointCollecte}}', '{{dateRetrait}}', '{{creneauRetrait}}', '{{detailProduits}}'],
  password: ['{{lienMotDePasse}}'],
  stock: ['{{produitNom}}', '{{stockActuel}}', '{{seuilStock}}'],
  rejection: ['{{motifRefus}}']
}

const loading = ref(false)
const saving = ref(false)
const apiError = ref('')
const successMessage = ref('')
const emailTemplates = ref([])
const config = reactive({
  nomEntreprise: '',
  adresseEmail: '',
  telephone: '',
  adressePostale: '',
  mentionsLegales: ''
})

const orderedTemplates = computed(() => {
  return [...emailTemplates.value].sort((left, right) => {
    const leftIndex = TEMPLATE_ORDER.indexOf(left.type)
    const rightIndex = TEMPLATE_ORDER.indexOf(right.type)
    const safeLeft = leftIndex === -1 ? Number.MAX_SAFE_INTEGER : leftIndex
    const safeRight = rightIndex === -1 ? Number.MAX_SAFE_INTEGER : rightIndex
    return safeLeft - safeRight
  })
})

onMounted(() => {
  loadSettings()
})

async function loadSettings() {
  loading.value = true
  apiError.value = ''

  try {
    const [settings, templates] = await Promise.all([
      emailSettingsApi.get(),
      emailTemplatesApi.list()
    ])
    applySettings(settings)
    emailTemplates.value = templates
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
    }
  } finally {
    loading.value = false
  }
}

function applySettings(settings) {
  config.nomEntreprise = settings.nomEntreprise || ''
  config.adresseEmail = settings.adresseEmail || ''
  config.telephone = settings.telephone || ''
  config.adressePostale = settings.adressePostale || ''
  config.mentionsLegales = settings.mentionsLegales || ''
}

function buildSettingsPayload() {
  const payload = {
    nomEntreprise: config.nomEntreprise || '',
    adresseEmail: config.adresseEmail || '',
    telephone: config.telephone || '',
    adressePostale: config.adressePostale || '',
    mentionsLegales: config.mentionsLegales || ''
  }

  Object.entries(CORE_TEMPLATE_KEYS).forEach(([type, key]) => {
    const template = emailTemplates.value.find(item => item.type === type)
    payload[key] = template
      ? {
          type: template.type,
          objet: template.objet,
          contenu: template.contenu,
          active: template.active !== false,
          updatedAt: template.updatedAt || null
        }
      : { type, objet: '', contenu: '', active: true, updatedAt: null }
  })

  return payload
}

async function handleSave() {
  saving.value = true
  apiError.value = ''
  successMessage.value = ''

  try {
    await emailSettingsApi.update(buildSettingsPayload())

    const extraTemplates = emailTemplates.value.filter(template => !(template.type in CORE_TEMPLATE_KEYS))
    for (const template of extraTemplates) {
      await emailTemplatesApi.update(template.type, template)
    }

    const [settings, templates] = await Promise.all([
      emailSettingsApi.get(),
      emailTemplatesApi.list()
    ])
    applySettings(settings)
    emailTemplates.value = templates
    successMessage.value = 'Configuration email enregistree.'
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
    }
  } finally {
    saving.value = false
  }
}

function handleSessionError(error) {
  return handleApiAccessError(error, authStore, router)
}

async function previewTemplate(template) {
  apiError.value = ''
  try {
    const html = await emailTemplatesApi.preview(template.type, template, buildPreviewVariables(template.type))
    openHtmlPreview(html)
  } catch (error) {
    if (handleSessionError(error)) return
    if (!isApiAccessError(error)) {
      apiError.value = getApiErrorMessage(error)
    }
  }
}

function buildPreviewVariables(type) {
  const base = {
    prenom: 'Marie',
    nom: 'Dupont',
    numero: 'CMD-2026-001',
    montant: '42,50 EUR',
    codeRetrait: 'TS-4821',
    pointCollecte: 'Point de collecte Bruxelles',
    dateRetrait: '15/05/2026',
    creneauRetrait: '17h00 - 19h00',
    detailProduits: '<ul><li>Panier legumes bio x1</li><li>Jus de pomme artisanal x2</li></ul>',
    lienMotDePasse: 'http://localhost:5173/creation-mot-de-passe/demo',
    produitNom: 'Panier legumes bio',
    stockActuel: '3',
    seuilStock: '5',
    motifRefus: 'Informations insuffisantes'
  }

  if (type === 'LOW_STOCK_ALERT' || type === 'OUT_OF_STOCK_ALERT') {
    return {
      ...base,
      prenom: 'Admin',
      nom: 'Terra Sana'
    }
  }

  return base
}

function openHtmlPreview(html) {
  const previewWindow = window.open('', '_blank')
  if (!previewWindow) {
    apiError.value = 'La previsualisation a ete bloquee par le navigateur.'
    return
  }
  previewWindow.document.open()
  previewWindow.document.write(html)
  previewWindow.document.close()
}

function getTemplateLabel(type) {
  return TEMPLATE_LABELS[type] || type
}

function getTemplateDescription(type) {
  return TEMPLATE_DESCRIPTIONS[type] || 'Template email du systeme.'
}

function getVariablesHint(type) {
  const variables = [...VARIABLE_HINTS.common]

  if (String(type).startsWith('ORDER_')) {
    variables.push(...VARIABLE_HINTS.order)
  }
  if (type === 'PASSWORD_SETUP' || type === 'MEMBERSHIP_VALIDATION') {
    variables.push(...VARIABLE_HINTS.password)
  }
  if (type === 'LOW_STOCK_ALERT' || type === 'OUT_OF_STOCK_ALERT') {
    variables.push(...VARIABLE_HINTS.stock)
  }
  if (type === 'MEMBERSHIP_REJECTION') {
    variables.push(...VARIABLE_HINTS.rejection)
  }

  return Array.from(new Set(variables)).join(', ')
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('fr-FR')
}
</script>

<style scoped>
/* Les styles generaux viennent de App.css */
</style>
