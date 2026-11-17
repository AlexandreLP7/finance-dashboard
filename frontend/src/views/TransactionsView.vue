<script setup lang="ts">
import { formatDate } from '../utils/format'
</script>

<template>
  <section>
    <h1>Transactions</h1>
    <p>Liste des transactions importees (connecteur bancaire ou CSV).</p>
    <p class="muted">
      Exemple de rendu de date : {{ formatDate('2026-01-15') }}
    </p>
  </section>
</template>

<style scoped>
.muted {
  color: #94a3b8;
}
</style>
