import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api',
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true,
  timeout: 10000
})

const EMAIL_WORKFLOW_TIMEOUT = 30000

api.interceptors.response.use(
  response => response,
  error => {
    const data = error.response?.data
    error.apiMessage =
      data?.message ||
      data?.detail ||
      data?.error ||
      (typeof data === 'string' ? data : null) ||
      error.message ||
      'Erreur API'
    return Promise.reject(error)
  }
)

let accessRedirectInterceptorId = null

export function getApiStatus(error) {
  return error?.response?.status
}

export function isApiAccessError(error) {
  return [401, 403].includes(getApiStatus(error))
}

function shouldSkipAccessRedirect(error) {
  const url = String(error?.config?.url || '')
  const method = String(error?.config?.method || '').toLowerCase()

  return Boolean(error?.config?.skipAccessRedirect)
    || url.includes('/auth/login')
    || url.includes('/auth/password-reset')
    || (method === 'get' && url.includes('/auth/me'))
}

function pushRoute(router, target) {
  if (!router) return
  Promise.resolve(router.push(target)).catch(() => {})
}

function redirectToConnexion(router) {
  const current = router?.currentRoute?.value
  if (!current || current.path === '/connexion') {
    return
  }

  pushRoute(router, {
    path: '/connexion',
    query: current.fullPath && current.fullPath !== '/connexion'
      ? { redirect: current.fullPath }
      : {}
  })
}

function redirectToForbidden(router) {
  const current = router?.currentRoute?.value
  if (!current || current.path === '/403') {
    return
  }

  pushRoute(router, {
    path: '/403',
    query: current.fullPath ? { from: current.fullPath } : {}
  })
}

export function handleApiAccessError(error, authStore, router) {
  const status = getApiStatus(error)

  if (status === 401) {
    authStore?.clear?.()
    redirectToConnexion(router)
    return true
  }

  if (status === 403) {
    redirectToForbidden(router)
    return true
  }

  return false
}

export function setupApiAccessHandling(router, getAuthStore) {
  if (accessRedirectInterceptorId !== null) {
    api.interceptors.response.eject(accessRedirectInterceptorId)
  }

  accessRedirectInterceptorId = api.interceptors.response.use(
    response => response,
    error => {
      if (!shouldSkipAccessRedirect(error)) {
        handleApiAccessError(error, getAuthStore?.(), router)
      }
      return Promise.reject(error)
    }
  )
}

export function getApiErrorMessage(error) {
  return error?.apiMessage || 'Une erreur est survenue pendant la communication avec le backend.'
}

export function getApiBaseUrl() {
  return String(api.defaults.baseURL || '').replace(/\/$/, '')
}

export function fromAuthDto(dto) {
  return {
    id: dto.id,
    email: dto.email || '',
    nom: dto.nom || '',
    prenom: dto.prenom || '',
    role: dto.role || '',
    actif: dto.actif !== false,
    telephone: dto.telephone || '',
    adresse: dto.adresse || '',
    rue: dto.adresse || '',
    ville: dto.ville || '',
    codePostal: dto.codePostal || '',
    pays: dto.pays || 'Belgique'
  }
}

function parseImagesMeta(images) {
  if (!images || typeof images !== 'string') {
    return {}
  }

  try {
    const parsed = JSON.parse(images)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return images.startsWith('http') || images.startsWith('/') ? { photos: [images] } : {}
  }
}

const FRONT_BADGE_TO_TAG = {
  bio: 'BIO',
  local: 'LOCAL',
  vegan: 'VEGAN',
  vegetarien: 'VEGETARIEN'
}

function tagsToBadges(tags = []) {
  const normalizedTags = new Set(
    (Array.isArray(tags) ? tags : [])
      .map(tag => String(tag || '').toUpperCase())
      .filter(Boolean)
  )

  return {
    bio: normalizedTags.has('BIO'),
    local: normalizedTags.has('LOCAL'),
    vegan: normalizedTags.has('VEGAN'),
    vegetarien: normalizedTags.has('VEGETARIEN')
  }
}

function badgesToTags(badges = {}) {
  return Object.entries(FRONT_BADGE_TO_TAG)
    .filter(([badgeKey]) => Boolean(badges?.[badgeKey]))
    .map(([, backendTag]) => backendTag)
}

function stringifyImagesMeta(produit) {
  const photos = Array.isArray(produit.photos)
    ? produit.photos
    : typeof produit.photos === 'string'
      ? produit.photos.split(',').map(url => url.trim()).filter(Boolean)
      : []

  const badges = produit.badges && typeof produit.badges === 'object'
    ? produit.badges
    : tagsToBadges(produit.tags || [])

  return JSON.stringify({
    image: produit.image || '',
    photos,
    badges,
    origine: produit.origine || '',
    producteur: produit.producteur || ''
  })
}

function toBackendUnite(value) {
  const normalized = String(value || '').trim().toLowerCase()
  const map = {
    kg: 'KG',
    kilogramme: 'KG',
    piece: 'PIECE',
    'pièce': 'PIECE',
    'piã¨ce': 'PIECE',
    pce: 'PIECE',
    barquette: 'BARQUETTE',
    litre: 'LITRE',
    liter: 'LITRE',
    l: 'LITRE'
  }
  return map[normalized] || 'PIECE'
}

function fromBackendUnite(value) {
  const map = {
    KG: 'kg',
    PIECE: 'pièce',
    BARQUETTE: 'barquette',
    LITRE: 'litre'
  }
  return map[value] || 'pièce'
}

function normalizeTime(value) {
  if (!value) return ''
  return String(value).slice(0, 5)
}

function formatDateLabel(dateString) {
  if (!dateString) return ''
  const date = new Date(`${dateString}T00:00:00`)
  if (Number.isNaN(date.getTime())) return dateString
  return date.toLocaleDateString('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long'
  })
}

export function fromCategorieDto(dto) {
  return {
    id: dto.id,
    nom: dto.nom,
    description: dto.description || '',
    actif: dto.actif !== false,
    compteur: dto.compteur || 0,
    nbProduits: dto.compteur || 0
  }
}

export function toCategorieDto(categorie) {
  return {
    nom: categorie.nom,
    description: categorie.description || '',
    actif: categorie.actif !== false
  }
}

export function fromProduitDto(dto, categories = []) {
  const meta = parseImagesMeta(dto.images)
  const categorie = categories.find(cat => cat.id === dto.categorieId)
  const tauxTVA = Number(dto.tauxTva ?? 6)
  const prixAvantPromotion = Number(dto.prixAvantPromotion ?? dto.prixUnitaire ?? 0)
  const prixPromotionnel = Number(dto.prixPromotionnel ?? dto.prixUnitaire ?? 0)
  const reductionMontant = Number(dto.reductionMontant ?? Math.max(0, prixAvantPromotion - prixPromotionnel))
  const promotionActive = Boolean(dto.promotionId) && reductionMontant > 0
  const fallbackBadges = {
    bio: Boolean(meta.badges?.bio),
    local: Boolean(meta.badges?.local),
    vegan: Boolean(meta.badges?.vegan),
    vegetarien: Boolean(meta.badges?.vegetarien)
  }
  const tags = Array.isArray(dto.tagsAlimentaires) && dto.tagsAlimentaires.length > 0
    ? dto.tagsAlimentaires
    : badgesToTags(fallbackBadges)
  const badges = tags.length > 0 ? tagsToBadges(tags) : fallbackBadges

  return {
    id: dto.id,
    nom: dto.nom,
    description: dto.description || '',
    categorieId: dto.categorieId,
    categorie: categorie?.nom || '',
    unite: fromBackendUnite(dto.unite),
    prix: prixAvantPromotion,
    prixUnitaire: prixAvantPromotion,
    prixAvantPromotion,
    prixOriginal: prixAvantPromotion,
    prixPromotionnel,
    prixEffectif: prixPromotionnel,
    reductionMontant,
    promotionActive,
    promotionId: dto.promotionId ?? null,
    promotionNom: dto.promotionNom || '',
    promotionTypeReduction: dto.promotionTypeReduction || '',
    promotionValeurReduction: Number(dto.promotionValeurReduction ?? 0),
    stock: Number(dto.stockActuel ?? 0),
    stockActuel: Number(dto.stockActuel ?? 0),
    tauxTVA,
    tauxTva: tauxTVA,
    tva: tauxTVA,
    image: meta.image || 'P',
    photos: Array.isArray(meta.photos) ? meta.photos : [],
    tags,
    badges,
    origine: meta.origine || '',
    producteur: meta.producteur || '',
    actif: dto.actif !== false
  }
}

export function toProduitDto(produit, categories = []) {
  const categorie = categories.find(cat => cat.id === produit.categorieId || cat.nom === produit.categorie)
  if (!categorie) {
    throw new Error('Categorie introuvable pour ce produit.')
  }

  return {
    nom: produit.nom,
    description: produit.description || '',
    prixUnitaire: Number(produit.prix ?? produit.prixUnitaire ?? 0),
    unite: toBackendUnite(produit.unite),
    tauxTva: Number(produit.tauxTVA ?? produit.tauxTva ?? produit.tva ?? 6),
    stockActuel: Number(produit.stock ?? produit.stockActuel ?? 0),
    images: stringifyImagesMeta(produit),
    tagsAlimentaires: Array.from(
      new Set(
        produit.badges && typeof produit.badges === 'object'
          ? badgesToTags(produit.badges)
          : (Array.isArray(produit.tags) ? produit.tags : [])
      )
    ),
    actif: produit.actif !== false,
    categorieId: categorie.id
  }
}

export function toProduitStockAdjustmentDto(stockActuel) {
  return {
    stockActuel: Number(stockActuel)
  }
}

export function fromCreneauDto(dto) {
  const debut = normalizeTime(dto.heureDebut)
  const fin = normalizeTime(dto.heureFin)
  const date = dto.date || ''

  return {
    id: dto.id,
    pointCollecteId: dto.pointCollecteId,
    date,
    debut,
    fin,
    heureDebut: debut,
    heureFin: fin,
    actif: dto.actif !== false,
    horaire: `${formatDateLabel(date)} ${debut}-${fin}`.trim()
  }
}

export function toCreneauDto(pointCollecteId, creneau) {
  return {
    pointCollecteId,
    date: creneau.date,
    heureDebut: creneau.debut || creneau.heureDebut,
    heureFin: creneau.fin || creneau.heureFin,
    actif: creneau.actif !== false
  }
}

export function fromPointCollecteDto(dto) {
  const creneaux = (dto.creneaux || [])
    .map(fromCreneauDto)
    .filter(creneau => creneau.actif !== false)

  return {
    id: dto.id,
    nom: dto.nom,
    adresse: dto.adresse,
    actif: dto.actif !== false,
    creneaux
  }
}

export function toPointCollecteDto(point) {
  return {
    nom: point.nom,
    adresse: point.adresse,
    actif: point.actif !== false
  }
}

export function fromMembreDto(dto) {
  return {
    id: dto.id,
    nom: dto.nom || '',
    prenom: dto.prenom || '',
    email: dto.email || '',
    telephone: dto.telephone || '',
    rue: dto.adresse || '',
    adresse: dto.adresse || '',
    ville: dto.ville || '',
    codePostal: dto.codePostal || '',
    pays: dto.pays || 'Belgique',
    motDePasse: dto.motDePasse || '',
    actif: dto.actif !== false,
    statut: dto.actif === false ? 'suspendu' : 'actif',
    dateInscription: dto.dateCreation,
    dateCreation: dto.dateCreation,
    dateDerniereConnexion: dto.dateDerniereConnexion,
    nbCommandes: Number(dto.nbCommandes ?? 0)
  }
}

export function fromDemandeAdhesionDto(dto) {
  const statutMap = {
    EN_ATTENTE: 'en_attente',
    APPROUVEE: 'approuvee',
    REFUSEE: 'refusee'
  }

  return {
    id: dto.id,
    nom: dto.nom || '',
    prenom: dto.prenom || '',
    email: dto.email || '',
    telephone: dto.telephone || '',
    adresse: dto.adresse || '',
    rue: dto.adresse || '',
    ville: dto.ville || '',
    codePostal: dto.codePostal || '',
    pays: dto.pays || 'Belgique',
    message: dto.message || '',
    justification: dto.message || '',
    messageRefus: dto.messageRefus || '',
    statut: statutMap[dto.statut] || 'en_attente',
    statutCode: dto.statut || 'EN_ATTENTE',
    date: dto.dateSoumission,
    dateSoumission: dto.dateSoumission,
    dateTraitement: dto.dateTraitement,
    administrateurEmail: dto.administrateurEmail || '',
    lienActivation: dto.lienActivation || ''
  }
}

export function toDemandeAdhesionCreateDto(demande) {
  return {
    nom: demande.nom,
    prenom: demande.prenom,
    email: demande.email,
    telephone: demande.telephone,
    adresse: demande.adresse || demande.rue,
    ville: demande.ville,
    codePostal: demande.codePostal,
    pays: demande.pays || 'Belgique',
    message: demande.message || demande.justification || '',
    accepteConditions: demande.accepteConditions === true || demande.accepteConcept === true
  }
}

export function toMembreDto(membre, actif = membre.actif !== false) {
  const payload = {
    id: membre.id,
    nom: membre.nom,
    prenom: membre.prenom,
    email: membre.email,
    telephone: membre.telephone,
    adresse: membre.rue || membre.adresse,
    ville: membre.ville,
    codePostal: membre.codePostal,
    pays: membre.pays || 'Belgique',
    actif
  }

  if (membre.motDePasse) {
    payload.motDePasse = membre.motDePasse
  }

  return payload
}

export function fromCommandeDto(dto) {
  const pointCollecte = dto.pointCollecte
    ? {
        id: dto.pointCollecte.id,
        nom: dto.pointCollecte.nom,
        adresse: dto.pointCollecte.adresse
      }
    : null

  const creneauRetrait = dto.creneauRetrait
    ? {
        id: dto.creneauRetrait.id,
        pointCollecteId: dto.creneauRetrait.pointCollecteId,
        date: dto.creneauRetrait.date,
        debut: normalizeTime(dto.creneauRetrait.heureDebut),
        fin: normalizeTime(dto.creneauRetrait.heureFin),
        heureDebut: normalizeTime(dto.creneauRetrait.heureDebut),
        heureFin: normalizeTime(dto.creneauRetrait.heureFin),
        horaire: dto.creneauRetrait.horaire || `${normalizeTime(dto.creneauRetrait.heureDebut)}-${normalizeTime(dto.creneauRetrait.heureFin)}`
      }
    : null

  const produits = (dto.produits || []).map(item => ({
    id: item.produitId,
    ligneId: item.id,
    produitId: item.produitId,
    nom: item.nom,
    image: '📦',
    unite: fromBackendUnite(item.unite),
    quantite: Number(item.quantite ?? 0),
    prix: Number(item.prixUnitaire ?? 0),
    prixUnitaire: Number(item.prixUnitaire ?? 0),
    prixAvantPromotion: Number(item.prixUnitaireOriginal ?? item.prixUnitaire ?? 0),
    prixOriginal: Number(item.prixUnitaireOriginal ?? item.prixUnitaire ?? 0),
    reductionMontant: Number(item.reductionUnitaire ?? 0),
    promotionActive: Boolean(item.promotionId) && Number(item.reductionUnitaire ?? 0) > 0,
    promotionId: item.promotionId ?? null,
    promotionNom: item.promotionNom || '',
    tauxTVA: Number(item.tauxTVA ?? 6),
    tauxTva: Number(item.tauxTVA ?? 6),
    tva: Number(item.tauxTVA ?? 6),
    totalHT: Number(item.montantHT ?? 0),
    totalTTC: Number(item.montantTTC ?? 0)
  }))

  return {
    id: dto.id,
    numero: dto.numero,
    codeRetrait: dto.codeRetrait,
    statut: dto.statut,
    date: dto.dateCommande,
    dateCommande: dto.dateCommande,
    dateRetrait: dto.dateRetrait,
    dateAnnulation: dto.dateAnnulation,
    commentaire: dto.commentaire || '',
    paye: dto.paye === true,
    datePaiement: dto.datePaiement,
    membre: dto.membre
      ? {
          id: dto.membre.id,
          nom: dto.membre.nom,
          prenom: dto.membre.prenom,
          email: dto.membre.email
        }
      : null,
    pointCollecte,
    creneauRetrait,
    produits,
    articles: produits,
    totalHT: Number(dto.montantHT ?? 0),
    totalTVA: Number(dto.montantTVA ?? 0),
    totalTVAC: Number(dto.montantTTC ?? 0),
    montant: Number(dto.montantTTC ?? 0),
    montantTTC: Number(dto.montantTTC ?? 0)
  }
}

export function toCommandeCreateDto(commande) {
  return {
    creneauCollecteId: commande.creneauCollecteId,
    commentaire: commande.commentaire || '',
    produits: (commande.produits || []).map(item => ({
      produitId: item.produitId,
      quantite: Number(item.quantite)
    }))
  }
}

export function fromPromotionDto(dto) {
  return {
    id: dto.id,
    nom: dto.nom || '',
    description: dto.description || '',
    type: dto.typeReduction === 'POURCENTAGE' ? 'pourcentage' : 'montant',
    reduction: Number(dto.valeurReduction ?? 0),
    dateDebut: dto.dateDebut || '',
    dateFin: dto.dateFin || '',
    actif: dto.actif !== false,
    produits: Array.isArray(dto.produitIds) ? dto.produitIds : [],
    categories: Array.isArray(dto.categorieIds) ? dto.categorieIds : []
  }
}

export function toPromotionDto(promotion) {
  return {
    nom: promotion.nom,
    description: promotion.description || '',
    typeReduction: promotion.type === 'pourcentage' ? 'POURCENTAGE' : 'MONTANT_FIXE',
    valeurReduction: Number(promotion.reduction ?? 0),
    dateDebut: promotion.dateDebut,
    dateFin: promotion.dateFin,
    actif: promotion.actif !== false,
    produitIds: promotion.produits || [],
    categorieIds: promotion.categories || []
  }
}

function normalizeEmailTemplate(template = {}) {
  return {
    type: template.type || '',
    objet: template.objet || '',
    contenu: template.contenu || '',
    active: template.active !== false,
    updatedAt: template.updatedAt || null
  }
}

export function fromEmailSettingsDto(dto = {}) {
  return {
    nomEntreprise: dto.nomEntreprise || 'Terra Sana',
    adresseEmail: dto.adresseEmail || 'contact@terrasana.test',
    telephone: dto.telephone || '+32 470 00 00 00',
    adressePostale: dto.adressePostale || 'Rue du Marche 12, 1000 Bruxelles',
    mentionsLegales: dto.mentionsLegales || 'TVA BE0000000000',
    confirmationCommande: normalizeEmailTemplate(dto.confirmationCommande),
    rappelJ1: normalizeEmailTemplate(dto.rappelJ1),
    annulationCommande: normalizeEmailTemplate(dto.annulationCommande),
    validationAdhesion: normalizeEmailTemplate(dto.validationAdhesion)
  }
}

export function toEmailSettingsDto(settings) {
  return {
    nomEntreprise: settings.nomEntreprise || '',
    adresseEmail: settings.adresseEmail || '',
    telephone: settings.telephone || '',
    adressePostale: settings.adressePostale || '',
    mentionsLegales: settings.mentionsLegales || '',
    confirmationCommande: normalizeEmailTemplate(settings.confirmationCommande),
    rappelJ1: normalizeEmailTemplate(settings.rappelJ1),
    annulationCommande: normalizeEmailTemplate(settings.annulationCommande),
    validationAdhesion: normalizeEmailTemplate(settings.validationAdhesion)
  }
}

export function fromEmailTemplateDto(dto = {}) {
  return {
    type: dto.type || '',
    objet: dto.objet || '',
    contenu: dto.contenu || '',
    active: dto.active !== false,
    updatedAt: dto.updatedAt || null
  }
}

export function toEmailTemplateDto(template = {}) {
  return {
    type: template.type || '',
    objet: template.objet || '',
    contenu: template.contenu || '',
    active: template.active !== false
  }
}

export function fromGeneratedEmailDto(dto = {}) {
  return {
    id: dto.id,
    type: dto.type || '',
    destinataire: dto.destinataire || '',
    sujet: dto.sujet || '',
    contenuHtml: dto.contenuHtml || '',
    createdAt: dto.createdAt || null,
    statutEnvoi: dto.statutEnvoi || 'PENDING',
    dateTentativeEnvoi: dto.dateTentativeEnvoi || null,
    dateEnvoi: dto.dateEnvoi || null,
    erreurEnvoi: dto.erreurEnvoi || '',
    commandeId: dto.commandeId || null,
    membreId: dto.membreId || null,
    produitId: dto.produitId || null,
    demandeAdhesionId: dto.demandeAdhesionId || null
  }
}

export const categoriesApi = {
  async list() {
    const { data } = await api.get('/categories')
    return data.map(fromCategorieDto)
  },
  async listActive() {
    const { data } = await api.get('/categories/actives')
    return data.map(fromCategorieDto)
  },
  async create(categorie) {
    const { data } = await api.post('/categories', toCategorieDto(categorie))
    return fromCategorieDto(data)
  },
  async update(id, categorie) {
    const { data } = await api.put(`/categories/${id}`, toCategorieDto(categorie))
    return fromCategorieDto(data)
  },
  remove(id) {
    return api.delete(`/categories/${id}`)
  }
}

export const produitsApi = {
  async list(categories = []) {
    const { data } = await api.get('/produits')
    return data.map(dto => fromProduitDto(dto, categories))
  },
  async listActive(categories = []) {
    const { data } = await api.get('/produits/actives')
    return data.map(dto => fromProduitDto(dto, categories))
  },
  async get(id, categories = []) {
    const { data } = await api.get(`/produits/${id}`)
    return fromProduitDto(data, categories)
  },
  async create(produit, categories = []) {
    const { data } = await api.post('/produits', toProduitDto(produit, categories))
    return fromProduitDto(data, categories)
  },
  async update(id, produit, categories = []) {
    const { data } = await api.put(`/produits/${id}`, toProduitDto(produit, categories))
    return fromProduitDto(data, categories)
  },
  async adjustStock(id, stockActuel, categories = []) {
    const { data } = await api.put(`/produits/${id}/stock`, toProduitStockAdjustmentDto(stockActuel))
    return fromProduitDto(data, categories)
  },
  remove(id) {
    return api.delete(`/produits/${id}`)
  }
}

export const pointsCollecteApi = {
  async list() {
    const { data } = await api.get('/points-collecte')
    return data.map(fromPointCollecteDto)
  },
  async listActive() {
    const { data } = await api.get('/points-collecte/actifs')
    return data.map(fromPointCollecteDto)
  },
  async create(point) {
    const { data } = await api.post('/points-collecte', toPointCollecteDto(point))
    return fromPointCollecteDto(data)
  },
  async update(id, point) {
    const { data } = await api.put(`/points-collecte/${id}`, toPointCollecteDto(point))
    return fromPointCollecteDto(data)
  },
  remove(id) {
    return api.delete(`/points-collecte/${id}`)
  }
}

export const creneauxApi = {
  async create(pointCollecteId, creneau) {
    const { data } = await api.post('/creneaux-collecte', toCreneauDto(pointCollecteId, creneau))
    return fromCreneauDto(data)
  },
  async update(pointCollecteId, id, creneau) {
    const { data } = await api.put(`/creneaux-collecte/${id}`, toCreneauDto(pointCollecteId, creneau))
    return fromCreneauDto(data)
  },
  remove(id) {
    return api.delete(`/creneaux-collecte/${id}`)
  }
}

export const membresApi = {
  async list(statut = '') {
    const params = statut ? { statut } : {}
    const { data } = await api.get('/membres', { params })
    return data.map(fromMembreDto)
  },
  async update(id, membre, actif = membre.actif !== false) {
    const { data } = await api.put(`/membres/${id}`, toMembreDto(membre, actif))
    return fromMembreDto(data)
  }
}

export const demandesAdhesionApi = {
  async create(demande) {
    const { data } = await api.post('/demandes-adhesion', toDemandeAdhesionCreateDto(demande), {
      timeout: EMAIL_WORKFLOW_TIMEOUT
    })
    return fromDemandeAdhesionDto(data)
  },
  async list() {
    const { data } = await api.get('/demandes-adhesion')
    return data.map(fromDemandeAdhesionDto)
  },
  async decide(id, statut, messageRefus = '') {
    const { data } = await api.put(`/demandes-adhesion/${id}/decision`, {
      statut,
      messageRefus
    }, {
      timeout: EMAIL_WORKFLOW_TIMEOUT
    })
    return fromDemandeAdhesionDto(data)
  }
}

export const authApi = {
  async login(credentials) {
    const { data } = await api.post('/auth/login', credentials)
    return fromAuthDto(data)
  },
  async me() {
    const { data } = await api.get('/auth/me', { skipAccessRedirect: true })
    return fromAuthDto(data)
  },
  async updateMe(profile) {
    const { data } = await api.put('/auth/me', {
      nom: profile.nom,
      prenom: profile.prenom,
      email: profile.email,
      telephone: profile.telephone,
      adresse: profile.adresse || profile.rue,
      ville: profile.ville,
      codePostal: profile.codePostal,
      pays: profile.pays || 'Belgique'
    })
    return fromAuthDto(data)
  },
  changePassword(passwords) {
    return api.put('/auth/me/password', passwords)
  },
  async requestPasswordReset(email) {
    const { data } = await api.post('/auth/password-reset/request', { email }, {
      timeout: EMAIL_WORKFLOW_TIMEOUT
    })
    return data
  },
  async validatePasswordToken(token) {
    const { data } = await api.get(`/auth/password-reset/${token}`)
    return data
  },
  completePasswordReset(token, motDePasse) {
    return api.post(`/auth/password-reset/${token}`, { motDePasse })
  },
  logout() {
    return api.post('/auth/logout', null, { skipAccessRedirect: true })
  }
}

export const adminDashboardApi = {
  async getStats() {
    const { data } = await api.get('/admin/dashboard/stats')
    return data
  }
}

export const commandesApi = {
  async create(commande) {
    const { data } = await api.post('/commandes', toCommandeCreateDto(commande), {
      timeout: EMAIL_WORKFLOW_TIMEOUT
    })
    return fromCommandeDto(data)
  },
  async listAdmin() {
    const { data } = await api.get('/commandes')
    return data.map(fromCommandeDto)
  },
  async listMy() {
    const { data } = await api.get('/commandes/moi')
    return data.map(fromCommandeDto)
  },
  async cancel(id) {
    const { data } = await api.put(`/commandes/${id}/annuler`, null, {
      timeout: EMAIL_WORKFLOW_TIMEOUT
    })
    return fromCommandeDto(data)
  },
  async updateStatus(id, statut) {
    const { data } = await api.put(`/commandes/${id}/statut`, { statut }, {
      timeout: EMAIL_WORKFLOW_TIMEOUT
    })
    return fromCommandeDto(data)
  }
}

export const promotionsApi = {
  async list() {
    const { data } = await api.get('/promotions')
    return data.map(fromPromotionDto)
  },
  async create(promotion) {
    const { data } = await api.post('/promotions', toPromotionDto(promotion))
    return fromPromotionDto(data)
  },
  async update(id, promotion) {
    const { data } = await api.put(`/promotions/${id}`, toPromotionDto(promotion))
    return fromPromotionDto(data)
  },
  remove(id) {
    return api.delete(`/promotions/${id}`)
  }
}

export const emailSettingsApi = {
  async get() {
    const { data } = await api.get('/admin/email-settings')
    return fromEmailSettingsDto(data)
  },
  async update(settings) {
    const { data } = await api.put('/admin/email-settings', toEmailSettingsDto(settings))
    return fromEmailSettingsDto(data)
  }
}

export const emailTemplatesApi = {
  async list() {
    const { data } = await api.get('/admin/email-templates')
    return data.map(fromEmailTemplateDto)
  },
  async update(type, template) {
    const { data } = await api.put(`/admin/email-templates/${type}`, toEmailTemplateDto(template))
    return fromEmailTemplateDto(data)
  },
  async preview(type, template, variables = {}) {
    const { data } = await api.post(
      `/admin/email-templates/${type}/preview`,
      {
        objet: template?.objet || '',
        contenu: template?.contenu || '',
        active: template?.active ?? true,
        variables
      },
      {
        responseType: 'text'
      }
    )
    return data
  }
}

export const emailNotificationsApi = {
  async list() {
    const { data } = await api.get('/admin/email-notifications')
    return data.map(fromGeneratedEmailDto)
  },
  previewUrl(id) {
    return `${getApiBaseUrl()}/admin/email-notifications/${id}/preview`
  },
  async sendOrderReminder(commandeId) {
    const { data } = await api.post(`/admin/email-notifications/send/order-reminder/${commandeId}`)
    return fromGeneratedEmailDto(data)
  }
}
