import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true, layout: 'auth' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true, layout: 'auth' }
  },
  {
    path: '/',
    redirect: '/tasks'
  },
  {
    path: '/tasks',
    name: 'Tasks',
    component: () => import('../views/TaskListView.vue'),
    meta: { requiresAuth: true, layout: 'app' }
  },
  {
    path: '/focus/:taskId?',
    name: 'Focus',
    component: () => import('../views/FocusView.vue'),
    props: true,
    meta: { requiresAuth: true, layout: 'app' }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('../views/DashboardView.vue'),
    meta: { requiresAuth: true, layout: 'app' }
  },
  {
    path: '/team',
    name: 'TeamList',
    component: () => import('../views/TeamListView.vue'),
    meta: { requiresAuth: true, layout: 'app' }
  },
  {
    path: '/team/join',
    name: 'TeamJoin',
    component: () => import('../views/TeamListView.vue'),
    meta: { requiresAuth: true, layout: 'app' }
  },
  {
    path: '/team/:id',
    name: 'TeamDetail',
    component: () => import('../views/TeamDetailView.vue'),
    props: true,
    meta: { requiresAuth: true, layout: 'app' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/TaskListView.vue'),
    meta: { requiresAuth: true, layout: 'app' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  await authStore.initAuth()
  const isAuthenticated = authStore.isAuthenticated
  
  // If route requires auth and user is not authenticated
  if (to.meta.requiresAuth && !isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }
  
  // If route is public and user is authenticated, redirect to tasks
  if (to.meta.public && isAuthenticated) {
    return typeof to.query.redirect === 'string' ? to.query.redirect : '/tasks'
  }
})

export default router
