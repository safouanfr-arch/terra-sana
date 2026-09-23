<template>
  <div v-if="panier.length === 0" class="page-container">
    <div class="empty-state">
      <div v-if="infoMessage" class="alert alert-info">{{ infoMessage }}</div>
      <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>
      <div class="empty-icon">0</div>
      <h2>Votre panier est vide</h2>
      <p>Decouvrez nos produits et commencez vos achats.</p>
      <router-link to="/catalogue" class="btn btn-primary">
        Parcourir le catalogue
      </router-link>
    </div>
  </div>

  <div v-else class="panier-page">
    <div v-if="infoMessage" class="alert alert-info">{{ infoMessage }}</div>
    <div v-if="apiError" class="alert alert-error">{{ apiError }}</div>

    <div class="panier-header">
      <h1>Mon panier</h1>
      <p>{{ panier.length }} article(s)</p>
    </div>

    <div class="panier-container">
      <div class="panier-items">
        <div class="panier-actions-top">
          <router-link to="/catalogue" class="link">Continuer mes achats</router-link>
          <button class="btn-text btn-danger clear-cart-btn" @click="handleViderPanier">
            Vider le panier
          </button>
        </div>

        <div v-for="item in panier" :key="item.id" class="panier-item">
          <img class="item-image" :src="getImageSrc(item)" :alt="`Image de ${item.nom}`" @error="onImgError($event, item)" />

          <div class="item-info">
            <router-link :to="`/produit/${item.id}`" class="item-nom">{{ item.nom }}</router-link>
            <span class="item-categorie">{{ item.categorie }}</span>
            <span v-if="hasPromotion(item)" class="item-prix-original">{{ prixTTCOriginal(item).toFixed(2) }} € / {{ item.unite }}</span>
            <span class="item-prix-unitaire">{{ prixTTC(item).toFixed(2) }} € / {{ item.unite }}</span>
            <span v-if="hasPromotion(item)" class="item-promo">{{ item.promotionNom }}</span>
          </div>

          <div class="item-quantite">
            <button
              class="qty-btn"
              @click="modifierQuantite(item.id, item.quantite - 1)"
            >
              -
            </button>
            <input
              type="number"
              :value="item.quantite"
              @input="modifierQuantite(item.id, parseInt($event.target.value) || 1)"
              min="1"
              :max="item.stock"
            />
            <button
              class="qty-btn"
              @click="modifierQuantite(item.id, item.quantite + 1)"
              :disabled="item.quantite >= item.stock"
            >
              +
            </button>
          </div>

          <div class="item-total">
            <span class="item-prix-total">{{ (prixTTC(item) * item.quantite).toFixed(2) }} €</span>
          </div>

          <button
            class="item-remove"
            @click="retirerDuPanier(item.id)"
            title="Retirer du panier"
          >
            Retirer
          </button>
        </div>
      </div>

      <div class="panier-resume">
        <div class="resume-card">
          <h2>Recapitulatif</h2>

          <div class="resume-ligne resume-total">
            <span><strong>Total a payer</strong></span>
            <span><strong>{{ totaux.totalTVAC.toFixed(2) }} €</strong></span>
          </div>

          <button
            class="btn btn-primary btn-full"
            @click="handleValiderCommande"
          >
            Valider ma commande
          </button>

          <div class="resume-info">
            <p>Paiement au retrait</p>
            <p>Retrait en point de collecte</p>
            <p>Commande securisee</p>
          </div>
        </div>

        <div class="resume-help">
          <h3>Besoin d'aide ?</h3>
          <p>
            Vous pouvez modifier les quantites ou retirer des articles avant de valider votre commande.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePanierStore } from '../../stores/panierStore'
import { getApiErrorMessage, produitsApi } from '../../services/api.js'
import { getProductImageSrc, productPlaceholder } from '../../utils/imageHelper.js'

const route = useRoute()
const router = useRouter()
const panierStore = usePanierStore()

const infoMessage = ref('')
const apiError = ref('')
const panier = computed(() => panierStore.panier)

onMounted(async () => {
  await synchroniserPanier()
})

function getImageSrc(produit) {
  return getProductImageSrc(produit)
}

function onImgError(event, produit) {
  event.target.src = productPlaceholder(produit?.nom, produit?.categorie)
}

const totaux = computed(() => {
  let totalHT = 0
  let totalTVA = 0

  panier.value.forEach(item => {
    const prixUnitaireHT = prixEffectifHT(item)
    const tauxTVAProduit = (item.tauxTVA || 6) / 100
    const montantHT = prixUnitaireHT * item.quantite
    const montantTVA = montantHT * tauxTVAProduit

    totalHT += montantHT
    totalTVA += montantTVA
  })

  return {
    totalHT,
    totalTVA,
    totalTVAC: totalHT + totalTVA
  }
})

const modifierQuantite = (id, quantite) => {
  panierStore.modifierQuantite(id, quantite)
}

function prixTTC(item) {
  const prixHT = prixEffectifHT(item)
  const tauxTVA = Number(item.tauxTVA ?? item.tauxTva ?? item.tva ?? 6) / 100
  return prixHT * (1 + tauxTVA)
}

function prixTTCOriginal(item) {
  const prixHT = Number(item.prixAvantPromotion ?? item.prixOriginal ?? item.prixUnitaire ?? item.prix ?? 0)
  const tauxTVA = Number(item.tauxTVA ?? item.tauxTva ?? item.tva ?? 6) / 100
  return prixHT * (1 + tauxTVA)
}

function hasPromotion(item) {
  return Boolean(item?.promotionActive && Number(item?.reductionMontant ?? 0) > 0)
}

function prixEffectifHT(item) {
  return Number(item.prixEffectif ?? item.prixPromotionnel ?? item.prixUnitaire ?? item.prix ?? 0)
}

const retirerDuPanier = (id) => {
  panierStore.retirerDuPanier(id)
}

const handleViderPanier = () => {
  if (confirm('Etes-vous sur de vouloir vider votre panier ?')) {
    panierStore.viderPanier()
  }
}

const handleValiderCommande = () => {
  router.push('/commande')
}

async function synchroniserPanier() {
  if (panier.value.length === 0) {
    return
  }

  infoMessage.value = ''
  apiError.value = ''

  try {
    const produits = await produitsApi.listActive()
    const resultat = panierStore.synchroniserAvecProduits(produits)
    const stockChanged = route.query.stockChanged === '1'

    if (stockChanged || resultat.quantitesAjustees > 0 || resultat.produitsRetires > 0) {
      infoMessage.value = buildStockMessage(resultat)
    }
  } catch (error) {
    apiError.value = getApiErrorMessage(error)
  }
}

function buildStockMessage(resultat) {
  if (resultat.panierVide) {
    return 'Le stock a change. Votre panier ne contient plus d article encore disponible.'
  }

  const morceaux = ['Le stock a evolue depuis votre derniere action.']
  if (resultat.quantitesAjustees > 0) {
    morceaux.push(`${resultat.quantitesAjustees} quantite(s) ont ete ajustee(s).`)
  }
  if (resultat.produitsRetires > 0) {
    morceaux.push(`${resultat.produitsRetires} produit(s) indisponible(s) ont ete retires.`)
  }
  return morceaux.join(' ')
}
</script>

<style scoped>
.panier-actions-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(61, 73, 68, 0.12);
}

.panier-actions-top .link {
  font-weight: 700;
  color: var(--primary-color);
}

.clear-cart-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0.55rem 0.85rem;
  border-radius: 8px;
  border: 1px solid rgba(138, 67, 67, 0.16);
  background: rgba(138, 67, 67, 0.07);
  color: var(--error-color);
  font-weight: 700;
  cursor: pointer;
}

.clear-cart-btn:hover {
  background: rgba(138, 67, 67, 0.12);
}

.panier-page .panier-item {
  grid-template-columns: 82px minmax(0, 1fr) 148px 92px auto;
  gap: 1.1rem;
  align-items: center;
}

.item-image {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  background: #f2f4f7;
}

.item-info {
  display: grid;
  gap: 0.24rem;
  min-width: 0;
}

.item-nom {
  width: fit-content;
  color: var(--text-primary);
  font-size: 1.05rem;
  font-weight: 800;
  line-height: 1.2;
  text-decoration: none;
}

.item-nom:hover {
  color: var(--primary-color);
  text-decoration: underline;
}

.item-categorie,
.item-prix-unitaire {
  display: block;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.35;
}

.item-prix-unitaire {
  color: var(--text-primary);
  font-weight: 700;
}

.item-prix-original {
  color: var(--text-muted);
  font-size: 0.82rem;
  text-decoration: line-through;
}

.item-promo {
  width: fit-content;
  padding: 0.16rem 0.5rem;
  border-radius: 999px;
  background: rgba(184, 121, 63, 0.12);
  color: #8a5a2b;
  font-size: 0.74rem;
  font-weight: 700;
}

.item-quantite {
  justify-content: center;
}

.item-quantite input {
  width: 54px;
  height: 34px;
  border: 1px solid rgba(61, 73, 68, 0.16);
  border-radius: 8px;
  background: #fff;
  color: var(--text-primary);
  font-weight: 700;
  text-align: center;
}

.item-total {
  min-width: 78px;
  text-align: right;
}

.item-prix-total {
  color: var(--primary-color);
  font-weight: 800;
  white-space: nowrap;
}

.item-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: auto;
  min-width: 72px;
  height: 36px;
  padding: 0 0.8rem;
  font-weight: 700;
}

@media (max-width: 820px) {
  .panier-page .panier-item {
    grid-template-columns: 74px minmax(0, 1fr);
    gap: 0.85rem 1rem;
  }

  .item-image {
    width: 68px;
    height: 68px;
  }

  .item-quantite,
  .item-total,
  .item-remove {
    grid-column: 2;
    justify-self: start;
  }

  .item-total {
    text-align: left;
  }
}

@media (max-width: 560px) {
  .panier-actions-top {
    align-items: stretch;
    flex-direction: column;
  }

  .clear-cart-btn,
  .panier-actions-top .link {
    width: 100%;
    justify-content: center;
    text-align: center;
  }
}
</style>
