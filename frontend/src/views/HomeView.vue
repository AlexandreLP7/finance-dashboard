<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useDashboardStore } from '../stores/dashboard'
import { formatCurrency } from '../utils/format'

const store = useDashboardStore()

const totalFormatted = computed(() => formatCurrency(store.totalBalance))

onMounted(() => {
  void store.loadAccounts()
})
</script>

<template>
  <section>
    <h1>Tableau de bord</h1>
    <p v-if="store.loading">
      Chargement...
    </p>
    <p
      v-else-if="store.error"
      class="error"
    >
      {{ store.error }}
    </p>
    <div
      v-else
      class="cards"
    >
      <div class="card">
        <h2>Solde total</h2>
        <p class="amount">
          {{ totalFormatted }}
        </p>
      </div>
      <div class="card">
        <h2>Comptes</h2>
        <p class="amount">
          {{ store.accounts.length }}
        </p>
      </div>
    </div>
    <ul>
      <li
        v-for="account in store.accounts"
        :key="account.id"
      >
        {{ account.name }} ({{ account.ibanMasked ?? 'IBAN non renseigne' }}) :
        {{ formatCurrency(account.balance, account.currency) }}
      </li>
    </ul>
  </section>
</template>

<style scoped>
.cards {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}
.card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 0.5rem;
  padding: 1rem 1.5rem;
  min-width: 12rem;
}
.amount {
  font-size: 1.5rem;
  font-weight: 700;
}
.error {
  color: #f87171;
}
</style>
