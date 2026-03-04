import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    redirect: '/tasks'
  },
  {
    path: '/tasks',
    name: 'Tasks',
    component: () => import('../views/TaskListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/focus/:taskId?',
    name: 'Focus',
    component: () => import('../views/FocusView.vue'),
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('../views/DashboardView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/team',
    name: 'TeamList',
    component: () => import('../views/TeamListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/team/:id',
    name: 'TeamDetail',
    component: () => import('../views/TeamDetailView.vue'),
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/TaskListView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // Initialize auth state
  authStore.initAuth()
  
  const isAuthenticated = authStore.isAuthenticated
  
  // If route requires auth and user is not authenticated
  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
    return
  }
  
  // If route is public and user is authenticated, redirect to tasks
  if (to.meta.public && isAuthenticated) {
    next('/tasks')
    return
  }
  
  next()
})

export default router
