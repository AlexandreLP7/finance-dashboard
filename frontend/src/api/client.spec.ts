import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiGet, ApiError } from './client'

afterEach(() => {
  vi.unstubAllGlobals()
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
