import { defineStore } from 'pinia'
import { apiGet, apiPost, apiUpload } from '../api/client'
import type { Account } from './dashboard'

export interface Transaction {
  id: number
  accountId: number
  amount: number
  currency: string
  date: string
  label: string
  category: string | null
  counterparty: string | null
}

export interface BankStatus {
  connected: boolean
  provider: string
}

export interface SyncResult {
  provider: string
  accounts: number
  transactions: number
}

export interface ImportResult {
  imported: number
  skipped: number
  errors: string[]
}

interface TransactionsState {
  accounts: Account[]
  transactions: Transaction[]
  bankStatus: BankStatus | null
  loading: boolean
  error: string | null
  lastSync: SyncResult | null
  lastImport: ImportResult | null
}

export const useTransactionsStore = defineStore('transactions', {
  state: (): TransactionsState => ({
    accounts: [],
    transactions: [],
    bankStatus: null,
    loading: false,
    error: null,
    lastSync: null,
    lastImport: null
  }),
  actions: {
    async loadAccounts(): Promise<void> {
      this.accounts = await apiGet<Account[]>('/api/accounts')
    },
    async loadBankStatus(): Promise<void> {
      this.bankStatus = await apiGet<BankStatus>('/api/bank/status')
    },
    async loadTransactions(accountId: number, from?: string, to?: string): Promise<void> {
      this.loading = true
      this.error = null
      try {
        const params = new URLSearchParams()
        if (from) params.set('from', from)
        if (to) params.set('to', to)
        const qs = params.toString()
        this.transactions = await apiGet<Transaction[]>(
          `/api/accounts/${accountId}/transactions${qs ? `?${qs}` : ''}`
        )
      } catch (err) {
        this.error = err instanceof Error ? err.message : 'Erreur inconnue'
      } finally {
        this.loading = false
      }
    },
    async connectBank(): Promise<string> {
      const res = await apiGet<{ url: string }>('/api/bank/connect')
      return res.url
    },
    async syncBank(): Promise<void> {
      this.lastSync = await apiPost<SyncResult>('/api/bank/sync', {})
    },
    async importCsv(accountId: number, file: File): Promise<void> {
      this.lastImport = await apiUpload<ImportResult>(`/api/bank/accounts/${accountId}/import/csv`, file)
    }
  }
})
