import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import TransactionsView from '../TransactionsView.vue'

const accounts = [{ id: 1, name: 'Compte courant', ibanMasked: 'FR76 **** 1234', currency: 'EUR', balance: 1500 }]
const transactions = [
  { id: 1, accountId: 1, amount: -42.5, currency: 'EUR', date: '2026-01-15', label: 'CARREFOUR', category: 'Alimentation', counterparty: 'CARREFOUR' },
  { id: 2, accountId: 1, amount: 3000, currency: 'EUR', date: '2026-01-16', label: 'SALAIRE', category: 'Revenus', counterparty: null }
]

function mockFetch(url: string) {
  if (url.includes('/api/bank/status')) {
    return { ok: true, status: 200, json: () => Promise.resolve({ connected: true, provider: 'ARKEA_STET' }) }
  }
  if (url.includes('/api/accounts/') && url.includes('/transactions')) {
    return { ok: true, status: 200, json: () => Promise.resolve(transactions) }
  }
  if (url === '/api/accounts') {
    return { ok: true, status: 200, json: () => Promise.resolve(accounts) }
  }
  return { ok: true, status: 200, json: () => Promise.resolve({}) }
}

describe('TransactionsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => Promise.resolve(mockFetch(String(input)))))
  })

  it('affiche le tableau des transactions apres chargement', async () => {
    const wrapper = mount(TransactionsView)
    await flushPromises()
    expect(wrapper.text()).toContain('CARREFOUR')
    expect(wrapper.text()).toContain('SALAIRE')
    expect(wrapper.text()).toContain('Alimentation')
    expect(wrapper.find('table').exists()).toBe(true)
  })

  it('desactive la synchro si la banque n est pas connectee', async () => {
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => {
      const url = String(input)
      if (url.includes('/api/bank/status')) {
        return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve({ connected: false, provider: 'ARKEA_STET' }) })
      }
      return Promise.resolve(mockFetch(url))
    }))
    const wrapper = mount(TransactionsView)
    await flushPromises()
    const buttons = wrapper.findAll('button')
    const syncButton = buttons.find((b) => b.text() === 'Synchroniser')
    expect(syncButton?.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Banque non connectee')
  })
})
