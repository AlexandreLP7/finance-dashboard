<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useTransactionsStore } from '../stores/transactions'
import { formatCurrency, formatDate } from '../utils/format'

const store = useTransactionsStore()

const selectedAccountId = ref<number | ''>('')
const fileInput = ref<HTMLInputElement | null>(null)
const busy = ref(false)
const message = ref<string | null>(null)

const currentAccountId = computed(() => {
  if (selectedAccountId.value !== '') return Number(selectedAccountId.value)
  return store.accounts[0]?.id ?? 0
})

async function reload() {
  if (currentAccountId.value) {
    await store.loadTransactions(currentAccountId.value)
  }
}

onMounted(async () => {
  await store.loadAccounts()
  await store.loadBankStatus()
  await reload()
})

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !currentAccountId.value) return
  busy.value = true
  message.value = null
  try {
    await store.importCsv(currentAccountId.value, file)
    message.value = `Import : ${store.lastImport?.imported ?? 0} importees, ${store.lastImport?.skipped ?? 0} ignorees (doublons).`
    await reload()
  } catch (err) {
    message.value = err instanceof Error ? err.message : 'Erreur import'
  } finally {
    busy.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

async function connectBank() {
  const url = await store.connectBank()
  window.location.href = url
}

async function sync() {
  busy.value = true
  message.value = null
  try {
    await store.syncBank()
    message.value = `Synchronisation ${store.lastSync?.provider ?? ''} : ${store.lastSync?.accounts ?? 0} compte(s), ${store.lastSync?.transactions ?? 0} transaction(s) importee(s).`
    await store.loadAccounts()
    await reload()
  } catch (err) {
    message.value = err instanceof Error ? err.message : 'Erreur de synchronisation (banque non connectee ? Utilisez Connecter ma banque.)'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section>
    <h1>Transactions</h1>

    <div class="toolbar">
      <label>
        Compte :
        <select
          v-model="selectedAccountId"
          @change="reload"
        >
          <option
            v-for="account in store.accounts"
            :key="account.id"
            :value="account.id"
          >
            {{ account.name }} ({{ account.ibanMasked ?? 'n/a' }})
          </option>
        </select>
      </label>
      <button
        :disabled="busy"
        @click="connectBank"
      >
        Connecter ma banque (PSD2)
      </button>
      <button
        :disabled="busy || !store.bankStatus?.connected"
        @click="sync"
      >
        Synchroniser
      </button>
      <label class="upload">
        Importer un CSV :
        <input
          ref="fileInput"
          type="file"
          accept=".csv,.txt"
          :disabled="busy || !currentAccountId"
          @change="onFileChange"
        >
      </label>
    </div>

    <p
      v-if="store.bankStatus && !store.bankStatus.connected"
      class="hint"
    >
      Banque non connectee : cliquez sur « Connecter ma banque (PSD2) » (consentement Arkéa), ou importez un CSV.
    </p>
    <p
      v-if="message"
      class="message"
    >
      {{ message }}
    </p>
    <p
      v-if="store.error"
      class="error"
    >
      {{ store.error }}
    </p>

    <table v-if="store.transactions.length">
      <thead>
        <tr>
          <th>Date</th>
          <th>Libellé</th>
          <th>Catégorie</th>
          <th class="num">
            Montant
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="tx in store.transactions"
          :key="tx.id"
        >
          <td>{{ formatDate(tx.date) }}</td>
          <td>{{ tx.label }}</td>
          <td>{{ tx.category ?? '—' }}</td>
          <td
            class="num"
            :class="{ negative: tx.amount < 0 }"
          >
            {{ formatCurrency(tx.amount, tx.currency) }}
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else-if="!store.loading">
      Aucune transaction. Importez un CSV ou synchronisez votre banque.
    </p>
    <p v-else>
      Chargement...
    </p>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th,
td {
  text-align: left;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #334155;
}
.num {
  text-align: right;
}
.negative {
  color: #f87171;
}
.hint {
  color: #fbbf24;
}
.message {
  color: #4ade80;
}
.error {
  color: #f87171;
}
button {
  background: #0ea5e9;
  border: none;
  border-radius: 0.375rem;
  color: #082f49;
  cursor: pointer;
  font-weight: 600;
  padding: 0.5rem 0.9rem;
}
button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
