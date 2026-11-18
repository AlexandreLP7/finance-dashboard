import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiGet, apiPost, ApiError, setAccessTokenProvider, setRefreshHandler } from './client'

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessTokenProvider(() => null)
  setRefreshHandler(null)
})

describe('apiGet', () => {
  it('retourne le JSON quand la reponse est OK', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve([{ id: 1 }])
    }))
    const data = await apiGet<Array<{ id: number }>>('/api/accounts')
    expect(data).toEqual([{ id: 1 }])
  })

  it('leve ApiError quand la reponse est en erreur', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({})
    }))
    await expect(apiGet('/api/accounts')).rejects.toBeInstanceOf(ApiError)
    await expect(apiGet('/api/accounts')).rejects.toMatchObject({ status: 401 })
  })
})

describe('client avec jeton', () => {
  it('ajoute l en-tete Authorization', async () => {
    setAccessTokenProvider(() => 'mon-jeton')
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({})
    })
    vi.stubGlobal('fetch', fetchMock)
    await apiGet('/api/accounts')
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer mon-jeton')
  })

  it('tente un refresh puis rejoue la requete sur un 401', async () => {
    let calls = 0
    setAccessTokenProvider(() => (calls > 0 ? 'jeton-neuf' : 'jeton-vieux'))
    setRefreshHandler(async () => {
      calls++
      return true
    })
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({ ok: false, status: 401, text: () => Promise.resolve('') })
      .mockResolvedValueOnce({ ok: true, status: 200, text: () => Promise.resolve('{"ok":true}') })
    vi.stubGlobal('fetch', fetchMock)
    const data = await apiGet<{ ok: boolean }>('/api/accounts')
    expect(data).toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe('Bearer jeton-neuf')
  })

  it('propage le 401 si le refresh echoue', async () => {
    setRefreshHandler(async () => false)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, text: () => Promise.resolve('') }))
    await expect(apiGet('/api/accounts')).rejects.toMatchObject({ status: 401 })
  })

  it('ne tente pas de refresh sur les endpoints /api/auth/', async () => {
    const refresh = vi.fn().mockResolvedValue(true)
    setRefreshHandler(refresh)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, text: () => Promise.resolve('') }))
    await expect(apiPost('/api/auth/login', {})).rejects.toMatchObject({ status: 401 })
    expect(refresh).not.toHaveBeenCalled()
  })
})
