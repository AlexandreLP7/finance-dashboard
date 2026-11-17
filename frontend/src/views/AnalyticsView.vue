<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import ChartCard from '../components/ChartCard.vue'
import { apiGet } from '../api/client'
import { useTransactionsStore } from '../stores/transactions'
import { formatCurrency, formatDate } from '../utils/format'

interface MonthlyFlow {
  month: string
  income: number
  expenses: number
}
interface CategorySpend {
  category: string
  total: number
}
interface Summary {
  monthly: MonthlyFlow[]
  byCategory: CategorySpend[]
}
interface Anomaly {
  transactionId: number
  type: string
  message: string
  amount: number
  date: string
  label: string
}
interface BudgetStatus {
  category: string
  limit: number
  spent: number
  remaining: number
}
interface Advice {
  code: string
  message: string
  severity: string
}

const store = useTransactionsStore()
const summary = ref<Summary | null>(null)
const anomalies = ref<Anomaly[]>([])
const budgets = ref<BudgetStatus[]>([])
const advices = ref<Advice[]>([])
const error = ref<string | null>(null)

const accountId = computed(() => store.accounts[0]?.id ?? 0)

const cashflowOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['Revenus', 'Dépenses'] },
  xAxis: { type: 'category', data: summary.value?.monthly.map((m) => m.month) ?? [] },
  yAxis: { type: 'value' },
  series: [
    { name: 'Revenus', type: 'bar', data: summary.value?.monthly.map((m) => m.income) ?? [], itemStyle: { color: '#4ade80' } },
    { name: 'Dépenses', type: 'bar', data: summary.value?.monthly.map((m) => -m.expenses) ?? [], itemStyle: { color: '#f87171' } }
  ]
}))

const categoryOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      name: 'Dépenses par catégorie (mois en cours)',
      type: 'pie',
      radius: ['30%', '65%'],
      data: summary.value?.byCategory.map((c) => ({ name: c.category, value: c.total })) ?? []
    }
  ]
}))

onMounted(async () => {
  try {
    await store.loadAccounts()
    if (!accountId.value) return
    const [s, a, b, adv] = await Promise.all([
      apiGet<Summary>(`/api/analytics/summary?accountId=${accountId.value}&months=6`),
      apiGet<Anomaly[]>(`/api/analytics/anomalies?accountId=${accountId.value}&months=6`),
      apiGet<BudgetStatus[]>(`/api/analytics/budgets?accountId=${accountId.value}`),
      apiGet<Advice[]>(`/api/analytics/advice?accountId=${accountId.value}`)
    ])
    summary.value = s
    anomalies.value = a
    budgets.value = b
    advices.value = adv
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Erreur de chargement des analyses'
  }
})
</script>

<template>
  <section>
    <h1>Analyses</h1>
    <p
      v-if="error"
      class="error"
    >
      {{ error }}
    </p>

    <div
      v-if="advices.length"
      class="advices"
    >
      <h2>Conseils</h2>
      <ul>
        <li
          v-for="advice in advices"
          :key="advice.code"
          :class="advice.severity"
        >
          {{ advice.message }}
        </li>
      </ul>
    </div>

    <div class="charts">
      <ChartCard
        title="Flux mensuels (6 mois)"
        :option="cashflowOption"
      />
      <ChartCard
        title="Dépenses par catégorie (mois en cours)"
        :option="categoryOption"
      />
    </div>

    <h2>Anomalies détectées</h2>
    <ul v-if="anomalies.length">
      <li
        v-for="anomaly in anomalies"
        :key="anomaly.transactionId + anomaly.type"
      >
        <strong>{{ anomaly.type }}</strong> — {{ formatDate(anomaly.date) }} :
        {{ anomaly.label }} ({{ formatCurrency(anomaly.amount) }}) — {{ anomaly.message }}
      </li>
    </ul>
    <p v-else>
      Aucune anomalie détectée.
    </p>

    <h2>Budgets du mois</h2>
    <table v-if="budgets.length">
      <thead>
        <tr>
          <th>Catégorie</th>
          <th class="num">
            Limite
          </th>
          <th class="num">
            Dépensé
          </th>
          <th class="num">
            Restant
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="budget in budgets"
          :key="budget.category"
        >
          <td>{{ budget.category }}</td>
          <td class="num">
            {{ formatCurrency(budget.limit) }}
          </td>
          <td class="num">
            {{ formatCurrency(budget.spent) }}
          </td>
          <td
            class="num"
            :class="{ negative: budget.remaining < 0 }"
          >
            {{ formatCurrency(budget.remaining) }}
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else>
      Aucun budget défini.
    </p>
  </section>
</template>

<style scoped>
.advices {
  margin-bottom: 1.5rem;
}
.advices li {
  margin-bottom: 0.4rem;
}
.advices .warning {
  color: #fbbf24;
}
.advices .danger {
  color: #f87171;
}
.advices .info {
  color: #38bdf8;
}
.charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}
table {
  border-collapse: collapse;
  width: 100%;
}
th,
td {
  border-bottom: 1px solid #334155;
  padding: 0.5rem 0.75rem;
  text-align: left;
}
.num {
  text-align: right;
}
.negative {
  color: #f87171;
}
.error {
  color: #f87171;
}
</style>
