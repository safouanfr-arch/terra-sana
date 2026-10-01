<template>
  <main class="page-container conditions-page">
    <header class="conditions-header">
      <p class="conditions-kicker">Terra Sana</p>
      <h1>Conditions d'adhésion Terra Sana</h1>
      <p class="conditions-version">Version v1.0</p>
      <p>Cette charte présente les règles essentielles d'utilisation du prototype.</p>
    </header>

    <section>
      <h2>Objet de la plateforme</h2>
      <p>Terra Sana permet à ses membres de consulter un catalogue, de commander des produits disponibles et de les retirer dans un point de collecte.</p>
    </section>

    <section>
      <h2>Adhésion et accès</h2>
      <p>L'accès au catalogue et aux commandes est réservé aux membres dont la demande a été acceptée. Terra Sana peut accepter ou refuser une demande d'adhésion.</p>
    </section>

    <section>
      <h2>Informations fournies</h2>
      <p>Le candidat s'engage à communiquer des informations exactes et à maintenir ses coordonnées à jour.</p>
    </section>

    <section>
      <h2>Commandes et disponibilité</h2>
      <p>Le panier ne réserve aucun produit. La disponibilité et le prix sont confirmés lors de la validation de la commande, moment auquel le stock est réservé.</p>
    </section>

    <section>
      <h2>Collecte et paiement</h2>
      <p>Le membre choisit un point et un créneau actifs dans la fenêtre autorisée. Il se présente au retrait avec son code de retrait. Le paiement est enregistré au moment du retrait ; aucune livraison à domicile ni aucun paiement en ligne ne sont proposés.</p>
    </section>

    <section>
      <h2>Annulation</h2>
      <p>Une commande peut être annulée avant son retrait, selon son état et les actions disponibles dans l'espace membre. Les produits réservés sont alors remis en stock.</p>
    </section>

    <section>
      <h2>Communications</h2>
      <p>Le candidat et le membre acceptent de recevoir les emails nécessaires au traitement de l'adhésion, à la sécurité du compte et au suivi des commandes.</p>
    </section>

    <section>
      <h2>Comportement attendu</h2>
      <p>Chaque membre utilise la plateforme de bonne foi, protège ses accès et respecte les horaires ainsi que l'organisation des points de collecte.</p>
    </section>

    <div class="conditions-actions">
      <router-link to="/demande-adhesion" class="btn btn-primary">Retour à la demande d'adhésion</router-link>
    </div>
  </main>
</template>

<style scoped>
.conditions-page {
  width: min(100%, 860px);
  margin-inline: auto;
}

.conditions-header {
  margin-bottom: 2rem;
}

.conditions-kicker {
  color: var(--color-primary, #315d4b);
  font-weight: 700;
  text-transform: uppercase;
}

.conditions-version {
  margin: 0.25rem 0 0.75rem;
  color: #52615a;
  font-weight: 600;
}

.conditions-page section {
  padding: 1rem 0;
  border-top: 1px solid #d8e0dc;
}

.conditions-page h2 {
  margin: 0 0 0.45rem;
  font-size: 1.15rem;
}

.conditions-page p {
  margin: 0;
  line-height: 1.65;
}

.conditions-actions {
  margin-top: 2rem;
}
</style>
