import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import TransactionsView from '../views/TransactionsView.vue'
import AnalyticsView from '../views/AnalyticsView.vue'
import BankCallbackView from '../views/BankCallbackView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/transactions', name: 'transactions', component: TransactionsView },
    { path: '/analytics', name: 'analytics', component: AnalyticsView },
    { path: '/bank-callback', name: 'bank-callback', component: BankCallbackView }
  ]
})
