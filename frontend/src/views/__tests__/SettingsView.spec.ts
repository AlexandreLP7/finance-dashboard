import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import SettingsView from '../SettingsView.vue'
import { useAuthStore } from '../../stores/auth'

vi.mock('qrcode', () => ({
  default: { toCanvas: vi.fn().mockResolvedValue(undefined) }
}))

function jsonResponse(body: unknown, status = 200) {
  return { ok: status < 300, status, text: () => Promise.resolve(JSON.stringify(body)) }
}

function buttonByText(wrapper: { findAll: (s: string) => Array<{ text: () => string, trigger: (e: string) => Promise<void> }> }, text: string) {
  const button = wrapper.findAll('button').find((b) => b.text() === text)
  if (!button) {
    throw new Error(`Bouton introuvable: ${text}`)
  }
  return button
}

function mockFetch(handlers: Record<string, unknown>) {
  vi.stubGlobal(
    'fetch',
    vi.fn((input: RequestInfo | URL) => {
      const url = String(input)
      for (const [fragment, body] of Object.entries(handlers)) {
        if (url.includes(fragment)) {
          return Promise.resolve(body)
        }
      }
      return Promise.resolve(jsonResponse({}))
    })
  )
}

describe('SettingsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    useAuthStore().accessToken = 'jeton'
  })

  it('change le mot de passe', async () => {
    mockFetch({ '/api/auth/me': jsonResponse({ username: 'alice', totpEnabled: false }) })
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValueOnce(jsonResponse({ username: 'alice', totpEnabled: false }))
    fetchMock.mockResolvedValueOnce(jsonResponse({}, 200))
    const wrapper = mount(SettingsView)
    await flushPromises()
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('ancien')
    await inputs[1].setValue('nouveau123')
    await inputs[2].setValue('nouveau123')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(wrapper.text()).toContain('Mot de passe modifié')
    expect(fetchMock.mock.calls.some((c) => String(c[0]) === '/api/auth/change-password')).toBe(true)
  })

  it('affiche une erreur si les mots de passe ne correspondent pas', async () => {
    mockFetch({ '/api/auth/me': jsonResponse({ username: 'alice', totpEnabled: false }) })
    const wrapper = mount(SettingsView)
    await flushPromises()
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('ancien')
    await inputs[1].setValue('nouveau123')
    await inputs[2].setValue('different123')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('ne correspondent pas')
  })

  it('active la 2FA via QR code et code', async () => {
    mockFetch({
      '/api/auth/me': jsonResponse({ username: 'alice', totpEnabled: false }),
      '/api/auth/2fa/setup': jsonResponse({ secret: 'SECRETBASE32', otpauthUri: 'otpauth://totp/Test:alice?secret=SECRETBASE32' }),
      '/api/auth/2fa/enable': jsonResponse({}, 200)
    })
    const wrapper = mount(SettingsView)
    await flushPromises()
    await buttonByText(wrapper, 'Activer la 2FA').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('SECRETBASE32')
    expect(wrapper.find('[data-test="qr-canvas"]').exists()).toBe(true)
    const inputs = wrapper.findAll('input')
    await inputs[inputs.length - 1].setValue('123456')
    await buttonByText(wrapper, "Confirmer l'activation").trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('2FA activée')
  })

  it('desactive la 2FA quand elle est active', async () => {
    mockFetch({
      '/api/auth/me': jsonResponse({ username: 'alice', totpEnabled: true }),
      '/api/auth/2fa/disable': jsonResponse({}, 200)
    })
    const wrapper = mount(SettingsView)
    await flushPromises()
    expect(wrapper.text()).toContain('La 2FA est activée')
    const inputs = wrapper.findAll('input')
    await inputs[inputs.length - 1].setValue('654321')
    await buttonByText(wrapper, 'Désactiver la 2FA').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('2FA désactivée')
  })
})
