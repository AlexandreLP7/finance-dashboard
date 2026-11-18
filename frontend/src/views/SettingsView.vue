<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import QRCode from 'qrcode'
import { ApiError } from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordMessage = ref<string | null>(null)
const passwordError = ref<string | null>(null)

const totpEnabled = ref(false)
const setupSecret = ref<string | null>(null)
const setupUri = ref<string | null>(null)
const totpCode = ref('')
const totpMessage = ref<string | null>(null)
const totpError = ref<string | null>(null)
const qrCanvas = ref<HTMLCanvasElement | null>(null)

onMounted(async () => {
  try {
    const me = await auth.me()
    totpEnabled.value = me.totpEnabled
  } catch {
    totpError.value = 'Impossible de charger le statut 2FA'
  }
})

async function submitPassword() {
  passwordMessage.value = null
  passwordError.value = null
  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = 'Les mots de passe ne correspondent pas'
    return
  }
  try {
    await auth.changePassword(oldPassword.value, newPassword.value)
    passwordMessage.value = 'Mot de passe modifié'
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (err) {
    passwordError.value =
      err instanceof ApiError && err.status === 401 ? 'Ancien mot de passe invalide' : 'Erreur lors du changement'
  }
}

async function startSetup() {
  totpMessage.value = null
  totpError.value = null
  try {
    const setup = await auth.setup2fa()
    setupSecret.value = setup.secret
    setupUri.value = setup.otpauthUri
    await nextTick()
    if (qrCanvas.value && setupUri.value) {
      await QRCode.toCanvas(qrCanvas.value, setupUri.value, { width: 200 })
    }
  } catch {
    totpError.value = 'Impossible de démarrer la configuration 2FA'
  }
}

async function submitEnable() {
  totpMessage.value = null
  totpError.value = null
  try {
    await auth.enable2fa(totpCode.value)
    totpEnabled.value = true
    setupSecret.value = null
    setupUri.value = null
    totpCode.value = ''
    totpMessage.value = '2FA activée'
  } catch (err) {
    totpError.value = err instanceof ApiError && err.status === 401 ? 'Code invalide' : 'Erreur lors de l\'activation'
  }
}

async function submitDisable() {
  totpMessage.value = null
  totpError.value = null
  try {
    await auth.disable2fa(totpCode.value)
    totpEnabled.value = false
    totpCode.value = ''
    totpMessage.value = '2FA désactivée'
  } catch (err) {
    totpError.value = err instanceof ApiError && err.status === 401 ? 'Code invalide' : 'Erreur lors de la désactivation'
  }
}
</script>

<template>
  <div class="settings">
    <h1>Paramètres</h1>

    <section class="card">
      <h2>Mot de passe</h2>
      <form @submit.prevent="submitPassword">
        <label>
          Ancien mot de passe
          <input v-model="oldPassword" type="password" autocomplete="current-password" required>
        </label>
        <label>
          Nouveau mot de passe
          <input v-model="newPassword" type="password" autocomplete="new-password" minlength="8" required>
        </label>
        <label>
          Confirmer le nouveau mot de passe
          <input v-model="confirmPassword" type="password" autocomplete="new-password" minlength="8" required>
        </label>
        <p v-if="passwordError" class="error" role="alert">
          {{ passwordError }}
        </p>
        <p v-if="passwordMessage" class="ok">
          {{ passwordMessage }}
        </p>
        <button type="submit">
          Changer le mot de passe
        </button>
      </form>
    </section>

    <section class="card">
      <h2>Double authentification (TOTP)</h2>
      <p v-if="totpEnabled" class="ok">
        La 2FA est activée sur votre compte.
      </p>
      <div v-if="!totpEnabled && !setupSecret">
        <p>La 2FA n'est pas activée.</p>
        <button type="button" @click="startSetup">
          Activer la 2FA
        </button>
      </div>
      <div v-if="!totpEnabled && setupSecret" class="setup">
        <p>Scannez ce QR code avec votre application d'authentification, puis entrez le code affiché :</p>
        <canvas ref="qrCanvas" data-test="qr-canvas" />
        <p class="secret">
          Secret (saisie manuelle) : <code>{{ setupSecret }}</code>
        </p>
        <label>
          Code à 6 chiffres
          <input v-model="totpCode" type="text" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" required>
        </label>
        <button type="button" @click="submitEnable">
          Confirmer l'activation
        </button>
      </div>
      <div v-if="totpEnabled">
        <label>
          Code à 6 chiffres pour désactiver la 2FA
          <input v-model="totpCode" type="text" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" required>
        </label>
        <button type="button" @click="submitDisable">
          Désactiver la 2FA
        </button>
      </div>
      <p v-if="totpError" class="error" role="alert">
        {{ totpError }}
      </p>
      <p v-if="totpMessage" class="ok">
        {{ totpMessage }}
      </p>
    </section>
  </div>
</template>

<style scoped>
.settings {
  max-width: 560px;
}
.card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 8px;
}
form {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
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
.secret {
  font-size: 0.85rem;
  color: #94a3b8;
  word-break: break-all;
}
.error {
  color: #f87171;
  margin: 0;
}
.ok {
  color: #4ade80;
  margin: 0;
}
</style>
