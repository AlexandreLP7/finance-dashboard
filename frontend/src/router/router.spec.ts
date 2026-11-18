import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { router } from './index'
import { useAuthStore } from '../stores/auth'

describe('guard du routeur', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })

  it('redirige vers /login quand non authentifie', async () => {
    await router.push('/transactions')
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/transactions')
  })

  it('laisse /login accessible sans authentification', async () => {
    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('accepte les routes protegees avec un jeton', async () => {
    useAuthStore().accessToken = 'jeton'
    await router.push('/analytics')
    expect(router.currentRoute.value.name).toBe('analytics')
  })
})
