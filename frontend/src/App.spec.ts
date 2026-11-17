import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', component: { template: '<div>home</div>' } }]
})

describe('App', () => {
  it('affiche la navigation', () => {
    const wrapper = mount(App, { global: { plugins: [router] } })
    expect(wrapper.text()).toContain('Finance Dashboard')
    expect(wrapper.text()).toContain('Tableau de bord')
    expect(wrapper.text()).toContain('Transactions')
    expect(wrapper.text()).toContain('Analyses')
  })
})
