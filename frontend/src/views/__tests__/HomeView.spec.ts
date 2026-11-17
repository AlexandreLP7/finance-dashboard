import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import HomeView from '../HomeView.vue'
import { useDashboardStore } from '../../stores/dashboard'

describe('HomeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve([
        { id: 1, name: 'Compte courant', ibanMasked: 'FR76 **** 1234', currency: 'EUR', balance: 1500.25 },
        { id: 2, name: 'Livret A', ibanMasked: null, currency: 'EUR', balance: 4000 }
      ])
    }))
  })

  it('affiche le solde total apres chargement des comptes', async () => {
    const wrapper = mount(HomeView)
    await flushPromises()
    expect(wrapper.text()).toContain('Solde total')
    expect(wrapper.text()).toContain('5 500,25')
    expect(wrapper.text()).toContain('Compte courant')
    expect(wrapper.text()).toContain('Livret A')
  })

  it('affiche une erreur si l API est indisponible', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401 }))
    const wrapper = mount(HomeView)
    await flushPromises()
    expect(wrapper.find('.error').exists()).toBe(true)
  })

  it('le store calcule le solde total', async () => {
    const store = useDashboardStore()
    await store.loadAccounts()
    expect(store.totalBalance).toBeCloseTo(5500.25)
    expect(store.accounts).toHaveLength(2)
  })
})
