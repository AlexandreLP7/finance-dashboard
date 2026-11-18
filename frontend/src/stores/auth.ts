import { defineStore } from 'pinia'
import { apiGet, apiPost, setAccessTokenProvider, setRefreshHandler } from '../api/client'

const REFRESH_KEY = 'finance.refreshToken'

export interface LoginResponse {
  accessToken: string | null
  refreshToken: string | null
  totpRequired: boolean
  pre2faToken: string | null
  username: string
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
}

export interface MeResponse {
  username: string
  totpEnabled: boolean
}

export interface TotpSetupResponse {
  secret: string
  otpauthUri: string
}

interface AuthState {
  accessToken: string | null
  username: string | null
  totpRequired: boolean
  pre2faToken: string | null
  initialized: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: null,
    username: null,
    totpRequired: false,
    pre2faToken: null,
    initialized: false
  }),
  getters: {
    isAuthenticated: (state): boolean => state.accessToken !== null
  },
  actions: {
    init(): void {
      setAccessTokenProvider(() => this.accessToken)
      setRefreshHandler(() => this.refresh())
      if (!this.initialized) {
        this.initialized = true
        if (localStorage.getItem(REFRESH_KEY)) {
          void this.refresh()
        }
      }
    },
    async login(username: string, password: string): Promise<LoginResponse> {
      const response = await apiPost<LoginResponse>('/api/auth/login', { username, password })
      this.applyLogin(response)
      return response
    },
    async verify2fa(code: string): Promise<LoginResponse> {
      const response = await apiPost<LoginResponse>('/api/auth/2fa/verify', {
        token: this.pre2faToken,
        code
      })
      this.applyLogin(response)
      return response
    },
    async refresh(): Promise<boolean> {
      const refreshToken = localStorage.getItem(REFRESH_KEY)
      if (!refreshToken) {
        return false
      }
      try {
        const pair = await apiPost<TokenPair>('/api/auth/refresh', { refreshToken })
        this.accessToken = pair.accessToken
        localStorage.setItem(REFRESH_KEY, pair.refreshToken)
        return true
      } catch {
        this.clearSession()
        return false
      }
    },
    async logout(): Promise<void> {
      try {
        await apiPost('/api/auth/logout', {})
      } catch {
        // la session locale est nettoyee quoi qu'il arrive
      }
      this.clearSession()
    },
    async me(): Promise<MeResponse> {
      return apiGet<MeResponse>('/api/auth/me')
    },
    async setup2fa(): Promise<TotpSetupResponse> {
      return apiPost<TotpSetupResponse>('/api/auth/2fa/setup', {})
    },
    async enable2fa(code: string): Promise<void> {
      await apiPost('/api/auth/2fa/enable', { code })
    },
    async disable2fa(code: string): Promise<void> {
      await apiPost('/api/auth/2fa/disable', { code })
    },
    async changePassword(oldPassword: string, newPassword: string): Promise<void> {
      await apiPost('/api/auth/change-password', { oldPassword, newPassword })
    },
    applyLogin(response: LoginResponse): void {
      if (response.totpRequired) {
        this.totpRequired = true
        this.pre2faToken = response.pre2faToken
        return
      }
      this.accessToken = response.accessToken
      this.username = response.username
      this.totpRequired = false
      this.pre2faToken = null
      if (response.refreshToken) {
        localStorage.setItem(REFRESH_KEY, response.refreshToken)
      }
    },
    clearSession(): void {
      this.accessToken = null
      this.username = null
      this.totpRequired = false
      this.pre2faToken = null
      localStorage.removeItem(REFRESH_KEY)
    }
  }
})
