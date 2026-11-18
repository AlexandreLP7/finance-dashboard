import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../LoginView.vue'
import { useAuthStore } from '../../stores/auth'

function jsonResponse(body: unknown, status = 200) {
  return { ok: status < 300, status, text: () => Promise.resolve(JSON.stringify(body)) }
}

async function makeRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', name: 'login', component: LoginView },
      { path: '/', name: 'home', component: { template: '<div>home</div>' } }
    ]
  })
  await router.push('/login')
  await router.isReady()
  return router
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('se connecte et navigue vers /', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ accessToken: 'a1', refreshToken: 'r1', totpRequired: false, pre2faToken: null, username: 'alice' })
      )
    )
    const router = await makeRouter()
    const wrapper = mount(LoginView, { global: { plugins: [router] } })
    await wrapper.find('input[name="username"]').setValue('alice')
    await wrapper.find('input[name="password"]').setValue('pass')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/')
    expect(useAuthStore().isAuthenticated).toBe(true)
  })

  it('affiche le champ code quand la 2FA est requise puis verifie', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ accessToken: null, refreshToken: null, totpRequired: true, pre2faToken: 'pre', username: 'alice' })
      )
      .mockResolvedValueOnce(
        jsonResponse({ accessToken: 'a2', refreshToken: 'r2', totpRequired: false, pre2faToken: null, username: 'alice' })
      )
    vi.stubGlobal('fetch', fetchMock)
    const router = await makeRouter()
    const wrapper = mount(LoginView, { global: { plugins: [router] } })
    await wrapper.find('input[name="username"]').setValue('alice')
    await wrapper.find('input[name="password"]').setValue('pass')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(wrapper.find('input[name="code"]').exists()).toBe(true)
    await wrapper.find('input[name="code"]').setValue('123456')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/')
    expect(useAuthStore().accessToken).toBe('a2')
  })

  it('affiche une erreur sur mauvais identifiants', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ error: 'Unauthorized' }, 401)))
    const router = await makeRouter()
    const wrapper = mount(LoginView, { global: { plugins: [router] } })
    await wrapper.find('input[name="username"]').setValue('alice')
    await wrapper.find('input[name="password"]').setValue('wrong')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('Identifiants ou code invalides')
    expect(router.currentRoute.value.path).toBe('/login')
  })
})
