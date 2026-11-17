import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AnalyticsView from '../AnalyticsView.vue'

vi.mock('echarts', () => ({
  init: () => ({ setOption: vi.fn(), dispose: vi.fn() })
}))

const accounts = [{ id: 1, name: 'Compte courant', ibanMasked: 'FR76 **** 1234', currency: 'EUR', balance: 1500 }]

function mockFetch(url: string) {
  if (url === '/api/accounts') {
    return { ok: true, status: 200, json: () => Promise.resolve(accounts) }
  }
  if (url.includes('/api/analytics/summary')) {
    return {
      ok: true, status: 200,
      json: () => Promise.resolve({
        monthly: [{ month: '2026-11', income: 3000, expenses: 1600 }],
        byCategory: [{ category: 'Logement', total: 1200 }, { category: 'Alimentation', total: 400 }]
      })
    }
  }
  if (url.includes('/api/analytics/anomalies')) {
    return {
      ok: true, status: 200,
      json: () => Promise.resolve([{ transactionId: 9, type: 'DOUBLON', message: '2 operations identiques', amount: -85.2, date: '2026-11-10', label: 'EDF' }])
    }
  }
  if (url.includes('/api/analytics/budgets')) {
    return { ok: true, status: 200, json: () => Promise.resolve([{ category: 'Alimentation', limit: 500, spent: 400, remaining: 100 }]) }
  }
  if (url.includes('/api/analytics/advice')) {
    return { ok: true, status: 200, json: () => Promise.resolve([{ code: 'TAUX_EPARGNE_OK', message: "Bon taux d'epargne", severity: 'info' }]) }
  }
  return { ok: true, status: 200, json: () => Promise.resolve({}) }
}

describe('AnalyticsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => Promise.resolve(mockFetch(String(input)))))
  })

  it('affiche conseils, anomalies, budgets et graphiques', async () => {
    const wrapper = mount(AnalyticsView)
    await flushPromises()
    expect(wrapper.text()).toContain("Bon taux d'epargne")
    expect(wrapper.text()).toContain('DOUBLON')
    expect(wrapper.text()).toContain('EDF')
    expect(wrapper.text()).toContain('Alimentation')
    expect(wrapper.text()).toContain('Flux mensuels')
    expect(wrapper.findAll('.chart-card')).toHaveLength(2)
  })
})
