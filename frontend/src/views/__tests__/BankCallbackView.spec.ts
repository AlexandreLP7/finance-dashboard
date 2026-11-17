import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import BankCallbackView from '../BankCallbackView.vue'

function makeRouter(initialUrl: string) {
  const router = createRouter({
    history: createWebHistory(),
    routes: [{ path: '/bank-callback', component: BankCallbackView }]
  })
  return router.push(initialUrl).then(() => router)
}

describe('BankCallbackView', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 200, text: () => Promise.resolve('') }))
  })

  it('poste le code au backend puis redirige', async () => {
    const router = await makeRouter('/bank-callback?code=abc123&state=s1')
    const wrapper = mount(BankCallbackView, { global: { plugins: [router] } })
    await flushPromises()
    expect(fetch).toHaveBeenCalledWith(
      '/api/bank/callback',
      expect.objectContaining({ method: 'POST' })
    )
    const body = JSON.parse((vi.mocked(fetch).mock.calls[0][1] as RequestInit).body as string)
    expect(body.code).toBe('abc123')
    expect(body.state).toBe('s1')
    expect(wrapper.text()).toContain('connecté')
  })

  it('affiche une erreur si le code est absent', async () => {
    const router = await makeRouter('/bank-callback')
    const wrapper = mount(BankCallbackView, { global: { plugins: [router] } })
    await flushPromises()
    expect(wrapper.text()).toContain("'code' manquant")
    expect(fetch).not.toHaveBeenCalled()
  })
})
