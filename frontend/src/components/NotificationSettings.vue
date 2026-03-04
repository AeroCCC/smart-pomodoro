<template>
  <div class="notification-settings">
    <h3>Push Notifications</h3>
    <p class="description">
      Receive notifications for task deadlines, Pomodoro timer completion, and daily summaries.
    </p>
    
    <div v-if="!notificationStore.isSupported" class="not-supported">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <line x1="15" y1="9" x2="9" y2="15"/>
        <line x1="9" y1="9" x2="15" y2="15"/>
      </svg>
      <span>Push notifications are not supported by your browser.</span>
    </div>
    
    <div v-else-if="!notificationStore.isEnabled" class="not-configured">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <span>Push notifications are not configured on the server.</span>
    </div>
    
    <div v-else class="subscription-controls">
      <div class="status-badge" :class="notificationStore.status">
        <span v-if="notificationStore.isSubscribed" class="subscribed">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          Notifications Enabled
        </span>
        <span v-else class="not-subscribed">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          Notifications Disabled
        </span>
      </div>
      
      <div v-if="notificationStore.error" class="error-message">
        {{ notificationStore.error }}
      </div>
      
      <div class="actions">
        <button 
          v-if="!notificationStore.isSubscribed"
          class="btn btn-primary"
          :disabled="notificationStore.isLoading"
          @click="subscribe"
        >
          <span v-if="notificationStore.isLoading">Subscribing...</span>
          <span v-else>Enable Notifications</span>
        </button>
        
        <button 
          v-else
          class="btn btn-secondary"
          :disabled="notificationStore.isLoading"
          @click="unsubscribe"
        >
          <span v-if="notificationStore.isLoading">Unsubscribing...</span>
          <span v-else>Disable Notifications</span>
        </button>
        
        <button 
          v-if="notificationStore.isSubscribed"
          class="btn btn-ghost"
          @click="sendTest"
        >
          Send Test
        </button>
      </div>
    </div>
    
    <div class="notification-types">
      <h4>You will receive notifications for:</h4>
      <ul>
        <li>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
          Task deadline reminders
        </li>
        <li>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 6v6l4 2"/>
          </svg>
          Pomodoro completion
        </li>
        <li>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          Team task assignments
        </li>
        <li>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/>
            <path d="M22 12A10 10 0 0 0 12 2v10z"/>
          </svg>
          Daily summary at 6 PM
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useNotificationStore } from '../stores/notificationStore'

const notificationStore = useNotificationStore()

onMounted(() => {
  notificationStore.initialize()
})

async function subscribe() {
  const success = await notificationStore.subscribe()
  if (success) {
    // Optionally show a success message
  }
}

async function unsubscribe() {
  const success = await notificationStore.unsubscribe()
  if (success) {
    // Optionally show a success message
  }
}

async function sendTest() {
  await notificationStore.sendTestNotification()
}
</script>

<style scoped>
.notification-settings {
  max-width: 600px;
  padding: var(--space-xl);
}

.notification-settings h3 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--space-sm);
}

.description {
  color: var(--text-secondary);
  margin-bottom: var(--space-lg);
  line-height: 1.6;
}

.not-supported,
.not-configured {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  color: var(--text-muted);
}

.not-supported svg,
.not-configured svg {
  flex-shrink: 0;
}

.subscription-controls {
  margin-bottom: var(--space-xl);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  margin-bottom: var(--space-md);
}

.status-badge.subscribed {
  background: var(--priority-low-bg);
  color: var(--priority-low);
}

.status-badge.not-subscribed {
  background: var(--priority-medium-bg);
  color: var(--priority-medium);
}

.status-badge span {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.error-message {
  padding: var(--space-md);
  background: var(--priority-high-bg);
  color: var(--priority-high);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-md);
  font-size: var(--font-size-sm);
}

.actions {
  display: flex;
  gap: var(--space-md);
}

.notification-types {
  padding-top: var(--space-lg);
  border-top: 1px solid var(--border-light);
}

.notification-types h4 {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--space-md);
  color: var(--text-primary);
}

.notification-types ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.notification-types li {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.notification-types svg {
  color: var(--color-primary);
  flex-shrink: 0;
}
</style>
