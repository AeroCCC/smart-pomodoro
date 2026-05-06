<template>
  <router-view v-if="isAuthLayout" />
  <div
    v-else
    class="app-layout"
    :style="{ '--app-sidebar-width': sidebarWidth }"
  >
    <Sidebar />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import { useSidebarState } from './lib/useSidebarState'
import { useThemeStore } from './stores/themeStore'

const route = useRoute()
const themeStore = useThemeStore()
const { sidebarWidth, initSidebarState } = useSidebarState()
const isAuthLayout = computed(() => route.meta.layout === 'auth')

initSidebarState()

onMounted(() => {
  themeStore.initTheme()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: var(--font-family);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  width: 100%;
  min-height: 100vh;
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  margin-left: var(--app-sidebar-width, var(--sidebar-width-expanded));
  padding: var(--space-xl);
  background: var(--bg-primary);
  min-height: 100vh;
  transition: margin-left var(--sidebar-transition);
}

@media (max-width: 768px) {
  .main-content {
    margin-left: var(--sidebar-width-expanded);
  }
}
</style>
