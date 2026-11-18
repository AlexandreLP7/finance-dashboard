import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    text: () => Promise.resolve(JSON.stringify(body))
  }
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('login stocke les tokens quand la 2FA est desactivee', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ accessToken: 'a1', refreshToken: 'r1', totpRequired: false, pre2faToken: null, username: 'alice' })
      )
    )
    const auth = useAuthStore()
    const res = await auth.login('alice', 'pass')
    expect(res.totpRequired).toBe(false)
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.accessToken).toBe('a1')
    expect(auth.username).toBe('alice')
    expect(localStorage.getItem('finance.refreshToken')).toBe('r1')
  })

  it('login avec 2FA active ne donne qu un jeton pre-2FA', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ accessToken: null, refreshToken: null, totpRequired: true, pre2faToken: 'pre', username: 'alice' })
      )
    )
    const auth = useAuthStore()
    await auth.login('alice', 'pass')
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.totpRequired).toBe(true)
    expect(auth.pre2faToken).toBe('pre')
  })

  it('verify2fa complete la connexion', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({ accessToken: null, refreshToken: null, totpRequired: true, pre2faToken: 'pre', username: 'alice' })
      )
      .mockResolvedValueOnce(
        jsonResponse({ accessToken: 'a2', refreshToken: 'r2', totpRequired: false, pre2faToken: null, username: 'alice' })
      )
    vi.stubGlobal('fetch', fetchMock)
    const auth = useAuthStore()
    await auth.login('alice', 'pass')
    await auth.verify2fa('123456')
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.accessToken).toBe('a2')
    expect(localStorage.getItem('finance.refreshToken')).toBe('r2')
    expect(fetchMock.mock.calls[1][1].body).toContain('"token":"pre"')
    expect(fetchMock.mock.calls[1][1].body).toContain('"code":"123456"')
  })

  it('refresh renouvelle les tokens et retourne false sans refresh token', async () => {
    const auth = useAuthStore()
    await expect(auth.refresh()).resolves.toBe(false)

    localStorage.setItem('finance.refreshToken', 'r1')
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ accessToken: 'a3', refreshToken: 'r3' }))
    )
    await expect(auth.refresh()).resolves.toBe(true)
    expect(auth.accessToken).toBe('a3')
    expect(localStorage.getItem('finance.refreshToken')).toBe('r3')
  })

  it('refresh en echec nettoie la session', async () => {
    localStorage.setItem('finance.refreshToken', 'r1')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ error: 'Unauthorized' }, 401)))
    const auth = useAuthStore()
    auth.accessToken = 'a1'
    await expect(auth.refresh()).resolves.toBe(false)
    expect(auth.accessToken).toBeNull()
    expect(localStorage.getItem('finance.refreshToken')).toBeNull()
  })

  it('logout appelle l API et nettoie la session', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}, 200)))
    const auth = useAuthStore()
    auth.accessToken = 'a1'
    auth.username = 'alice'
    localStorage.setItem('finance.refreshToken', 'r1')
    await auth.logout()
    expect(auth.isAuthenticated).toBe(false)
    expect(localStorage.getItem('finance.refreshToken')).toBeNull()
    expect(vi.mocked(fetch).mock.calls[0][0]).toBe('/api/auth/logout')
  })
})
