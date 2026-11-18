<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const code = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

async function submit() {
  loading.value = true
  error.value = null
  try {
    if (auth.totpRequired) {
      await auth.verify2fa(code.value)
    } else {
      await auth.login(username.value, password.value)
      if (auth.totpRequired) {
        return
      }
    }
    const target = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(target)
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      error.value = 'Identifiants ou code invalides'
      if (auth.totpRequired) {
        code.value = ''
      }
    } else {
      error.value = err instanceof Error ? err.message : 'Erreur inconnue'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <h1>Connexion</h1>
    <form class="card" @submit.prevent="submit">
      <label>
        Nom d'utilisateur
        <input v-model="username" type="text" name="username" autocomplete="username" required :disabled="auth.totpRequired">
      </label>
      <label v-if="!auth.totpRequired">
        Mot de passe
        <input v-model="password" type="password" name="password" autocomplete="current-password" required>
      </label>
      <label v-else>
        Code à 6 chiffres (application d'authentification)
        <input v-model="code" type="text" name="code" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" autocomplete="one-time-code" required autofocus>
      </label>
      <p v-if="error" class="error" role="alert">
        {{ error }}
      </p>
      <button type="submit" :disabled="loading">
        {{ auth.totpRequired ? 'Vérifier le code' : 'Se connecter' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.login {
  max-width: 360px;
  margin: 4rem auto;
}
.card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 8px;
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.9rem;
  color: #cbd5f5;
}
input {
  padding: 0.5rem;
  border-radius: 6px;
  border: 1px solid #475569;
  background: #0f172a;
  color: #e2e8f0;
}
button {
  padding: 0.6rem;
  border: none;
  border-radius: 6px;
  background: #38bdf8;
  color: #082f49;
  font-weight: 600;
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
}
.error {
  color: #f87171;
  margin: 0;
}
</style>
