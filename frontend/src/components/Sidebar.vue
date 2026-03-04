<template>
  <aside class="sidebar">
    <div class="sidebar-brand">
      <div class="brand-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
          <path d="M12 6V12L16 14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
      <span class="brand-text">PomoFocus</span>
    </div>
    
    <nav class="sidebar-nav">
      <router-link 
        v-for="item in navItems" 
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <component :is="item.icon" class="nav-icon" />
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </nav>
    
    <div class="sidebar-footer">
      <!-- User Info -->
      <div v-if="authStore.currentUser" class="user-info">
        <div class="user-avatar">
          {{ authStore.currentUser.username?.charAt(0).toUpperCase() }}
        </div>
        <div class="user-details">
          <span class="user-name">{{ authStore.currentUser.username }}</span>
          <span class="user-email">{{ authStore.currentUser.email }}</span>
        </div>
      </div>

      <button class="footer-item" @click="toggleDarkMode">
        <ThemeIcon class="nav-icon" />
        <span class="nav-label">{{ themeStore.themeLabel }}</span>
      </button>

      <button class="footer-item logout" @click="handleLogout">
        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
          <polyline points="16 17 21 12 16 7"/>
          <line x1="21" y1="12" x2="9" y2="12"/>
        </svg>
        <span class="nav-label">Logout</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { h, computed } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { useThemeStore } from '../stores/themeStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

// Dynamic theme icon
const ThemeIcon = computed(() => {
  return themeStore.isDark 
    ? () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
        h('circle', { cx: '12', cy: '12', r: '5' }),
        h('line', { x1: '12', y1: '1', x2: '12', y2: '3' }),
        h('line', { x1: '12', y1: '21', x2: '12', y2: '23' }),
        h('line', { x1: '4.22', y1: '4.22', x2: '5.64', y2: '5.64' }),
        h('line', { x1: '18.36', y1: '18.36', x2: '19.78', y2: '19.78' }),
        h('line', { x1: '1', y1: '12', x2: '3', y2: '12' }),
        h('line', { x1: '21', y1: '12', x2: '23', y2: '12' }),
        h('line', { x1: '4.22', y1: '19.78', x2: '5.64', y2: '18.36' }),
        h('line', { x1: '18.36', y1: '5.64', x2: '19.78', y2: '4.22' })
      ])
    : () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
        h('path', { d: 'M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z' })
      ])
})

// SVG Icons as components
const TaskIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { d: 'M9 11l3 3L22 4' }),
  h('path', { d: 'M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11' })
])

const FocusIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('circle', { cx: '12', cy: '12', r: '10' }),
  h('path', { d: 'M12 6v6l4 2' })
])

const TeamIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { d: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2' }),
  h('circle', { cx: '9', cy: '7', r: '4' }),
  h('path', { d: 'M23 21v-2a4 4 0 00-3-3.87' }),
  h('path', { d: 'M16 3.13a4 4 0 010 7.75' })
])

const StatsIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { d: 'M18 20V10' }),
  h('path', { d: 'M12 20V4' }),
  h('path', { d: 'M6 20v-6' })
])

const ProfileIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
  h('path', { d: 'M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2' }),
  h('circle', { cx: '12', cy: '7', r: '4' })
])

const navItems = [
  { path: '/tasks', label: 'Tasks', icon: TaskIcon },
  { path: '/focus', label: 'Focus', icon: FocusIcon },
  { path: '/team', label: 'Team', icon: TeamIcon },
  { path: '/stats', label: 'Stats', icon: StatsIcon },
  { path: '/profile', label: 'Profile', icon: ProfileIcon }
]

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const toggleDarkMode = () => {
  themeStore.toggleTheme()
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  padding: var(--space-lg);
  z-index: 100;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--border-light);
}

.brand-icon {
  width: 36px;
  height: 36px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-icon svg {
  width: 20px;
  height: 20px;
}

.brand-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  transition: all var(--transition-fast);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.nav-item:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--color-primary);
  color: white;
  box-shadow: var(--shadow-md);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.nav-label {
  flex: 1;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--border-light);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  margin-bottom: var(--space-sm);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: var(--color-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-sm);
}

.user-details {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.user-name {
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-email {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  width: 100%;
  text-align: left;
}

.footer-item:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.footer-item.logout:hover {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}
</style>
