<script setup lang="ts">
import { RouterView, RouterLink } from 'vue-router'
</script>

<template>
  <div class="layout">
    <nav class="nav">
      <span class="brand">Finance Dashboard</span>
      <RouterLink to="/">
        Tableau de bord
      </RouterLink>
      <RouterLink to="/transactions">
        Transactions
      </RouterLink>
      <RouterLink to="/analytics">
        Analyses
      </RouterLink>
    </nav>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.nav {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 0.75rem 1.5rem;
  background: #1e293b;
  border-bottom: 1px solid #334155;
}
.brand {
  font-weight: 700;
  margin-right: 1rem;
}
.nav a {
  color: #94a3b8;
  text-decoration: none;
}
.nav a.router-link-active {
  color: #38bdf8;
}
.content {
  flex: 1;
  padding: 1.5rem;
}
</style>
