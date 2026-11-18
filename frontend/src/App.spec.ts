import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import { useAuthStore } from './stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', component: { template: '<div>home</div>' } }]
})

describe('App', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 200, text: () => Promise.resolve('') }))
  })

  it('affiche la navigation quand authentifie', () => {
    const auth = useAuthStore()
    auth.accessToken = 'token-de-test'
    const wrapper = mount(App, { global: { plugins: [router] } })
    expect(wrapper.text()).toContain('Finance Dashboard')
    expect(wrapper.text()).toContain('Tableau de bord')
    expect(wrapper.text()).toContain('Transactions')
    expect(wrapper.text()).toContain('Analyses')
    expect(wrapper.text()).toContain('Paramètres')
    expect(wrapper.text()).toContain('Déconnexion')
  })

  it('masque la navigation quand non authentifie', () => {
    const wrapper = mount(App, { global: { plugins: [router] } })
    expect(wrapper.text()).not.toContain('Tableau de bord')
    expect(wrapper.text()).not.toContain('Déconnexion')
  })
})
