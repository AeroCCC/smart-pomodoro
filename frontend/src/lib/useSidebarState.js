import { computed, ref } from 'vue'

const SIDEBAR_COLLAPSED_KEY = 'sidebarCollapsed'
const DESKTOP_BREAKPOINT = 768
const SIDEBAR_WIDTH_EXPANDED = '240px'
const SIDEBAR_WIDTH_COLLAPSED = '60px'

const collapsedPreference = ref(false)
const isDesktopViewport = ref(true)

let hasInitialized = false

const updateViewportState = () => {
  if (typeof window === 'undefined') {
    return
  }

  isDesktopViewport.value = window.innerWidth > DESKTOP_BREAKPOINT
}

const persistSidebarState = () => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(collapsedPreference.value))
}

export const useSidebarState = () => {
  const isCollapsed = computed(() => isDesktopViewport.value && collapsedPreference.value)
  const sidebarWidth = computed(() => (
    isCollapsed.value ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH_EXPANDED
  ))

  const initSidebarState = () => {
    if (typeof window === 'undefined') {
      return
    }

    if (!hasInitialized) {
      const savedValue = window.localStorage.getItem(SIDEBAR_COLLAPSED_KEY)
      collapsedPreference.value = savedValue === 'true'
      updateViewportState()
      window.addEventListener('resize', updateViewportState)
      hasInitialized = true
      return
    }

    updateViewportState()
  }

  const toggleSidebar = () => {
    if (!isDesktopViewport.value) {
      return
    }

    collapsedPreference.value = !collapsedPreference.value
    persistSidebarState()
  }

  return {
    isCollapsed,
    isDesktopViewport,
    sidebarWidth,
    toggleSidebar,
    initSidebarState
  }
}
