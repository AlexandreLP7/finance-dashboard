import { defineStore } from 'pinia'
import { apiGet } from '../api/client'

export interface Account {
  id: number
  name: string
  ibanMasked: string | null
  currency: string
  balance: number
}

interface DashboardState {
  accounts: Account[]
  loading: boolean
  error: string | null
}

export const useDashboardStore = defineStore('dashboard', {
  state: (): DashboardState => ({
    accounts: [],
    loading: false,
    error: null
  }),
  getters: {
    totalBalance: (state): number =>
      state.accounts.reduce((sum, account) => sum + account.balance, 0)
  },
  actions: {
    async loadAccounts(): Promise<void> {
      this.loading = true
      this.error = null
      try {
        this.accounts = await apiGet<Account[]>('/api/accounts')
      } catch (err) {
        this.error = err instanceof Error ? err.message : 'Erreur inconnue'
      } finally {
        this.loading = false
      }
    }
  }
})
