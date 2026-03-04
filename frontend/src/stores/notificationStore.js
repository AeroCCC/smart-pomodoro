import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

const API_URL = '/api/notifications'

export const useNotificationStore = defineStore('notification', () => {
  // State
  const isSupported = ref('serviceWorker' in navigator && 'PushManager' in window)
  const isSubscribed = ref(false)
  const subscriptionCount = ref(0)
  const vapidPublicKey = ref('')
  const isEnabled = ref(false)
  const isLoading = ref(false)
  const error = ref(null)
  
  // Subscription object
  let pushSubscription = null
  
  // Getters
  const canSubscribe = computed(() => {
    return isSupported.value && vapidPublicKey.value && !isSubscribed.value
  })
  
  const status = computed(() => {
    if (!isSupported.value) return 'not-supported'
    if (isSubscribed.value) return 'subscribed'
    if (!vapidPublicKey.value) return 'not-configured'
    return 'available'
  })
  
  // Register Service Worker
  async function registerServiceWorker() {
    if (!isSupported.value) {
      console.log('Push notifications not supported')
      return null
    }
    
    try {
      const registration = await navigator.serviceWorker.register('/sw.js')
      console.log('Service Worker registered:', registration.scope)
      return registration
    } catch (err) {
      console.error('Service Worker registration failed:', err)
      error.value = 'Failed to register service worker'
      return null
    }
  }
  
  // Get VAPID public key from server
  async function getVapidPublicKey() {
    try {
      const response = await axios.get(`${API_URL}/vapid-public-key`)
      vapidPublicKey.value = response.data.publicKey
      isEnabled.value = response.data.enabled === 'true'
      return vapidPublicKey.value
    } catch (err) {
      console.error('Failed to get VAPID key:', err)
      return null
    }
  }
  
  // Check subscription status
  async function checkSubscriptionStatus() {
    isLoading.value = true
    error.value = null
    
    try {
      // Check with server
      const response = await axios.get(`${API_URL}/status`)
      isSubscribed.value = response.data.subscribed
      subscriptionCount.value = response.data.subscriptionCount
      isEnabled.value = response.data.vapidEnabled
      
      // Also check local subscription
      if (isSupported.value) {
        const registration = await navigator.serviceWorker.ready
        const subscription = await registration.pushManager.getSubscription()
        
        if (subscription && !isSubscribed.value) {
          // Local subscription exists but server doesn't know about it
          // Try to re-subscribe
          pushSubscription = subscription
        } else if (!subscription && isSubscribed.value) {
          // Server thinks we're subscribed but local is not
          isSubscribed.value = false
        }
      }
    } catch (err) {
      console.error('Failed to check subscription status:', err)
      error.value = 'Failed to check subscription status'
    } finally {
      isLoading.value = false
    }
  }
  
  // Subscribe to push notifications
  async function subscribe() {
    if (!isSupported.value) {
      error.value = 'Push notifications not supported by your browser'
      return false
    }
    
    isLoading.value = true
    error.value = null
    
    try {
      // Request permission first
      const permission = await Notification.requestPermission()
      
      if (permission !== 'granted') {
        error.value = 'Notification permission denied'
        isLoading.value = false
        return false
      }
      
      // Get VAPID key if not already loaded
      if (!vapidPublicKey.value) {
        await getVapidPublicKey()
      }
      
      if (!vapidPublicKey.value) {
        error.value = 'Push notifications not configured on server'
        isLoading.value = false
        return false
      }
      
      // Register service worker if not already done
      let registration = await navigator.serviceWorker.ready
      if (!registration) {
        registration = await registerServiceWorker()
      }
      
      // Subscribe to push
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey.value)
      })
      
      pushSubscription = subscription
      
      // Send subscription to server
      await axios.post(`${API_URL}/subscribe`, {
        endpoint: subscription.endpoint,
        keys: {
          p256dh: arrayBufferToBase64(subscription.getKey('p256dh')),
          auth: arrayBufferToBase64(subscription.getKey('auth'))
        },
        userAgent: navigator.userAgent
      })
      
      isSubscribed.value = true
      subscriptionCount.value = 1
      
      console.log('Push notification subscription successful')
      return true
      
    } catch (err) {
      console.error('Failed to subscribe:', err)
      error.value = err.response?.data?.error || 'Failed to subscribe to notifications'
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  // Unsubscribe from push notifications
  async function unsubscribe() {
    isLoading.value = true
    error.value = null
    
    try {
      // Get current subscription
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.getSubscription()
      
      if (subscription) {
        // Unsubscribe locally
        await subscription.unsubscribe()
        
        // Notify server
        await axios.post(`${API_URL}/unsubscribe`, {
          endpoint: subscription.endpoint
        })
      }
      
      isSubscribed.value = false
      subscriptionCount.value = 0
      pushSubscription = null
      
      console.log('Push notification unsubscription successful')
      return true
      
    } catch (err) {
      console.error('Failed to unsubscribe:', err)
      error.value = err.response?.data?.error || 'Failed to unsubscribe'
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  // Send test notification
  async function sendTestNotification() {
    try {
      await axios.post(`${API_URL}/test`)
      return true
    } catch (err) {
      console.error('Failed to send test notification:', err)
      error.value = err.response?.data?.error || 'Failed to send test notification'
      return false
    }
  }
  
  // Initialize - check support and load status
  async function initialize() {
    if (!isSupported.value) {
      console.log('Push notifications not supported in this browser')
      return
    }
    
    // Register service worker
    await registerServiceWorker()
    
    // Get VAPID key
    await getVapidPublicKey()
    
    // Check subscription status
    await checkSubscriptionStatus()
  }
  
  // Helper: Convert URL-safe base64 to Uint8Array
  function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4)
    const base64 = (base64String + padding)
      .replace(/\-/g, '+')
      .replace(/_/g, '/')
    
    const rawData = window.atob(base64)
    const outputArray = new Uint8Array(rawData.length)
    
    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i)
    }
    return outputArray
  }
  
  // Helper: Convert ArrayBuffer to Base64
  function arrayBufferToBase64(buffer) {
    const binary = String.fromCharCode(...new Uint8Array(buffer))
    return window.btoa(binary)
  }
  
  return {
    // State
    isSupported,
    isSubscribed,
    subscriptionCount,
    vapidPublicKey,
    isEnabled,
    isLoading,
    error,
    
    // Getters
    canSubscribe,
    status,
    
    // Actions
    initialize,
    subscribe,
    unsubscribe,
    sendTestNotification,
    checkSubscriptionStatus,
    registerServiceWorker,
    getVapidPublicKey
  }
})
