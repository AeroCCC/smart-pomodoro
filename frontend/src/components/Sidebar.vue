<template>
  <aside
    class="sidebar"
    :class="{ collapsed: isCollapsed }"
    :style="{ '--current-sidebar-width': sidebarWidth }"
  >
    <div class="sidebar-brand">
      <router-link
        to="/tasks"
        class="brand-main"
        :title="isCollapsed ? 'PomoFocus' : ''"
      >
        <div class="brand-icon">
          <AppIcon name="Clock3" :size="20" :stroke-width="1.9" />
        </div>
        <span class="brand-text">PomoFocus</span>
      </router-link>

      <button
        v-if="isDesktopViewport"
        type="button"
        class="sidebar-toggle"
        :title="isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'"
        @click="toggleSidebar"
      >
        <AppIcon :name="isCollapsed ? 'ChevronRight' : 'ChevronLeft'" :size="18" />
      </button>
    </div>

    <nav class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
        :title="isCollapsed ? item.label : ''"
      >
        <AppIcon :name="item.icon" :size="20" :stroke-width="1.75" custom-class="nav-icon" />
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="sidebar-footer">
      <div
        v-if="authStore.currentUser"
        class="user-info"
        :title="isCollapsed ? authStore.currentUser.username : ''"
      >
        <div class="user-avatar">
          {{ authStore.currentUser.username?.charAt(0).toUpperCase() }}
        </div>
        <div class="user-details">
          <span class="user-name">{{ authStore.currentUser.username }}</span>
          <span class="user-email">{{ authStore.currentUser.email }}</span>
        </div>
      </div>

      <button
        class="footer-item"
        :title="isCollapsed ? themeStore.themeLabel : ''"
        @click="toggleDarkMode"
      >
        <AppIcon :name="themeIconName" :size="20" :stroke-width="1.75" custom-class="nav-icon" />
        <span class="nav-label">{{ themeStore.themeLabel }}</span>
      </button>

      <button
        class="footer-item logout"
        :title="isCollapsed ? 'Logout' : ''"
        @click="handleLogout"
      >
        <AppIcon name="LogOut" :size="20" :stroke-width="1.75" custom-class="nav-icon" />
        <span class="nav-label">Logout</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSidebarState } from '../lib/useSidebarState'
import { useAuthStore } from '../stores/authStore'
import { useThemeStore } from '../stores/themeStore'
import AppIcon from './AppIcon.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const { isCollapsed, isDesktopViewport, sidebarWidth, toggleSidebar } = useSidebarState()

const themeIconName = computed(() => (themeStore.isDark ? 'Sun' : 'Moon'))

const navItems = [
  { path: '/tasks', label: 'Tasks', icon: 'ListChecks' },
  { path: '/focus', label: 'Focus', icon: 'Clock3' },
  { path: '/team', label: 'Team', icon: 'Users' },
  { path: '/stats', label: 'Stats', icon: 'BarChart3' },
  { path: '/profile', label: 'Profile', icon: 'UserRound' }
]

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')

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
  width: var(--current-sidebar-width, var(--sidebar-width-expanded));
  height: 100vh;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  padding: var(--space-lg);
  z-index: 100;
  transition:
    width var(--sidebar-transition),
    padding var(--sidebar-transition),
    background-color var(--transition-normal),
    border-color var(--transition-normal);
}

.sidebar.collapsed {
  padding-left: 10px;
  padding-right: 10px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--border-light);
}

.brand-main {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  color: inherit;
  text-decoration: none;
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
  flex-shrink: 0;
}

.brand-text,
.nav-label,
.user-details {
  transition:
    opacity var(--sidebar-transition),
    max-width var(--sidebar-transition),
    margin var(--sidebar-transition);
}

.brand-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-toggle {
  width: var(--sidebar-toggle-size);
  height: var(--sidebar-toggle-size);
  border-radius: var(--radius-full);
  border: 1px solid var(--border-light);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  color: var(--text-primary);
  background: var(--bg-tertiary);
  box-shadow: var(--shadow-sm);
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.nav-item,
.footer-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  min-height: 44px;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  transition: all var(--transition-fast);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.nav-item:hover,
.footer-item:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--color-primary);
  color: white;
  box-shadow: var(--shadow-md);
}

.nav-icon {
  flex-shrink: 0;
}

.nav-label {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
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
  transition: all var(--transition-fast);
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
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  min-width: 0;
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
  background: transparent;
  border: none;
  cursor: pointer;
  width: 100%;
  text-align: left;
}

.footer-item.logout:hover {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}

.sidebar.collapsed .sidebar-brand {
  justify-content: center;
  flex-direction: column;
  gap: var(--space-sm);
}

.sidebar.collapsed .brand-main,
.sidebar.collapsed .nav-item,
.sidebar.collapsed .footer-item,
.sidebar.collapsed .user-info {
  justify-content: center;
}

.sidebar.collapsed .nav-item,
.sidebar.collapsed .footer-item {
  width: 40px;
  min-height: 40px;
  padding-left: 0;
  padding-right: 0;
  align-self: center;
}

.sidebar.collapsed .brand-main {
  flex: none;
}

.sidebar.collapsed .brand-text,
.sidebar.collapsed .nav-label,
.sidebar.collapsed .user-details {
  opacity: 0;
  max-width: 0;
  overflow: hidden;
  pointer-events: none;
}

.sidebar.collapsed .nav-item.active {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  box-shadow: none;
}

.sidebar.collapsed .nav-item.active .nav-icon,
.sidebar.collapsed .footer-item:hover .nav-icon {
  filter: none;
}

.sidebar.collapsed .user-info {
  width: 40px;
  padding-left: 0;
  padding-right: 0;
  align-self: center;
}

@media (max-width: 768px) {
  .sidebar {
    width: var(--sidebar-width-expanded);
    padding: var(--space-lg);
  }

  .sidebar.collapsed {
    padding-left: var(--space-lg);
    padding-right: var(--space-lg);
  }

  .sidebar-toggle {
    display: none;
  }

  .sidebar.collapsed .sidebar-brand {
    justify-content: space-between;
    flex-direction: row;
  }

  .sidebar.collapsed .brand-main,
  .sidebar.collapsed .nav-item,
  .sidebar.collapsed .footer-item,
  .sidebar.collapsed .user-info {
    justify-content: flex-start;
  }

  .sidebar.collapsed .nav-item,
  .sidebar.collapsed .footer-item {
    width: auto;
    padding-left: var(--space-md);
    padding-right: var(--space-md);
    align-self: stretch;
  }

  .sidebar.collapsed .brand-text,
  .sidebar.collapsed .nav-label,
  .sidebar.collapsed .user-details {
    opacity: 1;
    max-width: 200px;
    pointer-events: auto;
  }

  .sidebar.collapsed .nav-item.active {
    background: var(--color-primary);
    color: white;
    box-shadow: var(--shadow-md);
  }

  .sidebar.collapsed .user-info {
    width: auto;
    padding-left: var(--space-md);
    padding-right: var(--space-md);
    align-self: stretch;
  }
}
</style>
