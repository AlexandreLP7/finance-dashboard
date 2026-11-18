import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import TransactionsView from '../views/TransactionsView.vue'
import AnalyticsView from '../views/AnalyticsView.vue'
import BankCallbackView from '../views/BankCallbackView.vue'
import LoginView from '../views/LoginView.vue'
import SettingsView from '../views/SettingsView.vue'
import { useAuthStore } from '../stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/', name: 'home', component: HomeView },
    { path: '/transactions', name: 'transactions', component: TransactionsView },
    { path: '/analytics', name: 'analytics', component: AnalyticsView },
    { path: '/settings', name: 'settings', component: SettingsView },
    { path: '/bank-callback', name: 'bank-callback', component: BankCallbackView }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  auth.init()
  if (!to.meta.public && !auth.isAuthenticated) {
    const refreshed = await auth.refresh()
    if (!refreshed) {
      return to.fullPath === '/'
        ? { name: 'login' }
        : { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'home' }
  }
  return true
})
