<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiPost } from '../api/client'

const route = useRoute()
const router = useRouter()

const status = ref<'pending' | 'ok' | 'error'>('pending')
const message = ref('')

onMounted(async () => {
  const code = route.query.code
  const state = route.query.state
  if (!code || typeof code !== 'string') {
    status.value = 'error'
    message.value = "Paramètre 'code' manquant dans le callback bancaire."
    return
  }
  try {
    await apiPost('/api/bank/callback', { code, state: typeof state === 'string' ? state : null })
    status.value = 'ok'
    message.value = 'Compte bancaire connecté (consentement enregistré). Redirection vers les transactions...'
    setTimeout(() => void router.push('/transactions'), 1500)
  } catch (err) {
    status.value = 'error'
    message.value = err instanceof Error ? err.message : 'Erreur lors du callback bancaire.'
  }
})
</script>

<template>
  <section class="callback">
    <h1>Connexion bancaire</h1>
    <p :class="status">
      {{ message || 'Finalisation de la connexion...' }}
    </p>
  </section>
</template>

<style scoped>
.callback {
  max-width: 40rem;
  margin: 2rem auto;
}
.ok {
  color: #4ade80;
}
.error {
  color: #f87171;
}
.pending {
  color: #94a3b8;
}
</style>
