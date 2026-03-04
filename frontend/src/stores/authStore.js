import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

const API_URL = '/api'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const isLoading = ref(false)
  const error = ref(null)

  // Getters
  const isAuthenticated = computed(() => !!token.value)
  const currentUser = computed(() => user.value)

  // Actions
  const setAuthData = (authData) => {
    token.value = authData.accessToken
    user.value = {
      id: authData.userId,
      username: authData.username,
      email: authData.email,
      avatar: authData.avatar
    }
    
    // Save to localStorage
    localStorage.setItem('token', authData.accessToken)
    localStorage.setItem('user', JSON.stringify(user.value))
    
    // Set axios default header
    axios.defaults.headers.common['Authorization'] = `Bearer ${authData.accessToken}`
  }

  const clearAuthData = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    delete axios.defaults.headers.common['Authorization']
  }

  const login = async (credentials) => {
    isLoading.value = true
    error.value = null

    try {
      const response = await axios.post(`${API_URL}/auth/login`, credentials)
      
      if (response.data.accessToken) {
        setAuthData(response.data)
        return { success: true }
      }
    } catch (err) {
      error.value = err.response?.data?.message || 'Login failed'
      return { 
        success: false, 
        message: err.response?.data?.message || 'Login failed. Please check your credentials.'
      }
    } finally {
      isLoading.value = false
    }
  }

  const register = async (userData) => {
    isLoading.value = true
    error.value = null

    try {
      const response = await axios.post(`${API_URL}/auth/register`, userData)
      
      if (response.data.accessToken) {
        setAuthData(response.data)
        return { success: true }
      }
    } catch (err) {
      error.value = err.response?.data?.message || 'Registration failed'
      return { 
        success: false, 
        message: err.response?.data?.message || 'Registration failed. Please try again.'
      }
    } finally {
      isLoading.value = false
    }
  }

  const logout = () => {
    clearAuthData()
  }

  const fetchCurrentUser = async () => {
    if (!token.value) return

    try {
      const response = await axios.get(`${API_URL}/auth/me`)
      user.value = response.data
      localStorage.setItem('user', JSON.stringify(user.value))
    } catch (err) {
      console.error('Failed to fetch user:', err)
      if (err.response?.status === 401) {
        clearAuthData()
      }
    }
  }

  const initAuth = () => {
    const savedToken = localStorage.getItem('token')
    if (savedToken) {
      token.value = savedToken
      axios.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`
      fetchCurrentUser()
    }
  }

  return {
    token,
    user,
    isLoading,
    error,
    isAuthenticated,
    currentUser,
    login,
    register,
    logout,
    initAuth
  }
})
