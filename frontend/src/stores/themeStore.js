import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // State
  const isDark = ref(false)
  const isInitialized = ref(false)
  
  // Getters
  const theme = computed(() => isDark.value ? 'dark' : 'light')
  const themeIcon = computed(() => isDark.value ? 'sun' : 'moon')
  const themeLabel = computed(() => isDark.value ? 'Light Mode' : 'Dark Mode')
  
  // Initialize theme from localStorage or system preference
  function initTheme() {
    if (isInitialized.value) return
    
    // Check localStorage first
    const savedTheme = localStorage.getItem('theme')
    
    if (savedTheme) {
      isDark.value = savedTheme === 'dark'
    } else {
      // Check system preference
      isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
    }
    
    // Apply theme
    applyTheme()
    isInitialized.value = true
    
    // Listen for system theme changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (!localStorage.getItem('theme')) {
        isDark.value = e.matches
        applyTheme()
      }
    })
  }
  
  // Apply theme to document
  function applyTheme() {
    const html = document.documentElement
    
    // Add no-transition class to prevent flash
    html.classList.add('no-transition')
    
    if (isDark.value) {
      html.setAttribute('data-theme', 'dark')
      document.body.style.backgroundColor = '#1A202C'
      document.body.style.color = '#F7FAFC'
    } else {
      html.removeAttribute('data-theme')
      document.body.style.backgroundColor = '#F7FAFC'
      document.body.style.color = '#1A202C'
    }
    
    // Remove no-transition after a short delay
    setTimeout(() => {
      html.classList.remove('no-transition')
    }, 50)
  }
  
  // Toggle theme
  function toggleTheme() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    applyTheme()
  }
  
  // Set specific theme
  function setTheme(dark) {
    isDark.value = dark
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    applyTheme()
  }
  
  return {
    isDark,
    theme,
    themeIcon,
    themeLabel,
    initTheme,
    toggleTheme,
    setTheme
  }
})
