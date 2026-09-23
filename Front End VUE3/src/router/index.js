import { createRouter, createWebHistory } from 'vue-router'
import { pinia } from '../stores'
import { useAuthStore } from '../stores/authStore'

// Pages publiques
import Accueil from '../page/Accueil.vue'
import Connexion from '../page/Connexion.vue'
import DemandeAdhesion from '../page/DemandeAdhesion.vue'
import CreationMotDePasse from '../page/CreationMotDePasse.vue'
import ReinitialisationMotDePasse from '../page/ReinitialisationMotDePasse.vue'
import AccesRefuse from '../page/AccesRefuse.vue'

// Pages Admin
import AdminDashboard from '../page/admin/AdminDashboard.vue'
import GestionDemandesAdhesion from '../page/admin/GestionDemandesAdhesion.vue'
import GestionMembres from '../page/admin/GestionMembres.vue'
import GestionProduits from '../page/admin/GestionProduits.vue'
import GestionCategories from '../page/admin/GestionCategories.vue'
import GestionPointsCollecte from '../page/admin/GestionPointsCollecte.vue'
import GestionCommandes from '../page/admin/GestionCommandes.vue'
import GestionStocks from '../page/admin/GestionStocks.vue'
import GestionPromotions from '../page/admin/GestionPromotions.vue'
import ExportVentes from '../page/admin/ExportVentes.vue'
import ParametresEmails from '../page/admin/ParametresEmails.vue'
import TableauxPreparation from '../page/admin/TableauxPreparation.vue'
import EmailsGeneres from '../page/admin/EmailsGeneres.vue'

// Pages Membre
import MembreDashboard from '../page/membre/MembreDashboard.vue'
import Catalogue from '../page/membre/Catalogue.vue'
import PlanningRetraits from '../page/membre/PlanningRetraits.vue'
import FicheProduit from '../page/membre/FicheProduit.vue'
import Panier from '../page/membre/Panier.vue'
import Commande from '../page/membre/Commande.vue'
import MesCommandes from '../page/membre/MesCommandes.vue'
import MonProfil from '../page/membre/MonProfil.vue'

// Composants partages
import Archives from '../page/shared/Archives.vue'

const routes = [
  {
    path: '/',
    name: 'Accueil',
    component: Accueil
  },
  {
    path: '/demande-adhesion',
    name: 'DemandeAdhesion',
    component: DemandeAdhesion
  },
  {
    path: '/creation-mot-de-passe/:token',
    name: 'CreationMotDePasse',
    component: CreationMotDePasse
  },
  {
    path: '/connexion',
    name: 'Connexion',
    component: Connexion,
    meta: { publicOnly: true }
  },
  {
    path: '/reinitialisation-mot-de-passe',
    name: 'ReinitialisationMotDePasse',
    component: ReinitialisationMotDePasse
  },
  {
    path: '/403',
    name: 'AccesRefuse',
    component: AccesRefuse
  },

  // Routes Admin
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: AdminDashboard,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/demandes-adhesion',
    name: 'GestionDemandesAdhesion',
    component: GestionDemandesAdhesion,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/membres',
    name: 'GestionMembres',
    component: GestionMembres,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/produits',
    name: 'GestionProduits',
    component: GestionProduits,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/categories',
    name: 'GestionCategories',
    component: GestionCategories,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/points-collecte',
    name: 'GestionPointsCollecte',
    component: GestionPointsCollecte,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/commandes',
    name: 'GestionCommandes',
    component: GestionCommandes,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/stocks',
    name: 'GestionStocks',
    component: GestionStocks,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/promotions',
    name: 'GestionPromotions',
    component: GestionPromotions,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/export-ventes',
    name: 'ExportVentes',
    component: ExportVentes,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/parametres-emails',
    name: 'ParametresEmails',
    component: ParametresEmails,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/emails-generes',
    name: 'EmailsGeneres',
    component: EmailsGeneres,
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/tableaux-preparation',
    name: 'TableauxPreparation',
    component: TableauxPreparation,
    meta: { requiresAuth: true, role: 'admin' }
  },
  // Routes Membre
  {
    path: '/membre',
    name: 'MembreDashboard',
    component: MembreDashboard,
    meta: { requiresAuth: true, role: 'membre' }
  },
  {
    path: '/catalogue',
    name: 'Catalogue',
    component: Catalogue,
    meta: { requiresAuth: true, role: 'membre' }
  },
  {
    path: '/points-retrait',
    name: 'PlanningRetraits',
    component: PlanningRetraits,
    meta: { requiresAuth: true, role: 'membre' }
  },
  {
    path: '/produit/:id',
    name: 'FicheProduit',
    component: FicheProduit,
    meta: { requiresAuth: true, role: 'membre' }
  },
  {
    path: '/panier',
    name: 'Panier',
    component: Panier,
    meta: { requiresAuth: true, role: 'membre' }
  },
  {
    path: '/commande',
    name: 'Commande',
    component: Commande,
    meta: { requiresAuth: true, role: 'membre' }
  },
  {
    path: '/mes-commandes',
    name: 'MesCommandes',
    component: MesCommandes,
    meta: { requiresAuth: true, role: 'membre' }
  },
  {
    path: '/mon-profil',
    name: 'MonProfil',
    component: MonProfil,
    meta: { requiresAuth: true, role: 'membre' }
  },
  {
    path: '/mes-archives',
    name: 'MembreArchives',
    component: Archives,
    meta: { requiresAuth: true, role: 'membre' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async to => {
  const authStore = useAuthStore(pinia)
  await authStore.restoreSession()

  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const publicOnly = to.matched.some(record => record.meta.publicOnly)
  const requiredRole = to.meta.role

  if (publicOnly && authStore.isAuthenticated) {
    return authStore.isAdmin ? '/admin' : '/membre'
  }

  if (requiresAuth && !authStore.isAuthenticated) {
    return {
      path: '/connexion',
      query: to.fullPath !== '/connexion' ? { redirect: to.fullPath } : {}
    }
  }

  if (requiresAuth && requiredRole && authStore.user?.role !== requiredRole) {
    return {
      path: '/403',
      query: to.fullPath !== '/403' ? { from: to.fullPath } : {}
    }
  }

  return true
})

export default router
