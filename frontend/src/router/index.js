import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    component: Dashboard,
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'docs', name: 'Documents', component: () => import('../views/Documents.vue') },
      { path: 'convs', name: 'Conversations', component: () => import('../views/Conversations.vue') },
      { path: 'settings', name: 'Settings', component: () => import('../views/Settings.vue') },
      { path: 'account', name: 'Account', component: () => import('../views/Account.vue') },
      { path: 'profile', redirect: '/account' },
      { path: 'change-password', redirect: '/account' },
      { path: 'chat/:docId', name: 'Chat', component: () => import('../views/Chat.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userId = sessionStorage.getItem('userId')
  if (to.meta.requiresAuth && !userId) {
    next('/login')
  } else {
    next()
  }
})

export default router
