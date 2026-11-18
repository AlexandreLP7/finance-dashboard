<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const router = useRouter()
const isAuthenticated = computed(() => auth.isAuthenticated)

async function logout() {
  await auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="layout">
    <nav v-if="isAuthenticated" class="nav">
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
      <RouterLink to="/settings">
        Paramètres
      </RouterLink>
      <button class="logout" type="button" @click="logout">
        Déconnexion
      </button>
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
.logout {
  margin-left: auto;
  background: #334155;
  color: #e2e8f0;
  border: 1px solid #475569;
  border-radius: 6px;
  padding: 0.35rem 0.9rem;
  cursor: pointer;
}
.logout:hover {
  background: #475569;
}
.content {
  flex: 1;
  padding: 1.5rem;
}
</style>
