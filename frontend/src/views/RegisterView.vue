<template>
  <div class="auth-page">
    <div class="auth-container">
      <!-- Left Side - Brand -->
      <div class="auth-brand">
        <div class="brand-content">
          <div class="brand-logo">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </div>
          <h1>PomoFocus</h1>
          <p>Join thousands of people who use PomoFocus to stay focused and achieve their goals.</p>
        </div>
      </div>

      <!-- Right Side - Register Form -->
      <div class="auth-form-container">
        <div class="auth-form-wrapper">
          <h2>Create Account</h2>
          <p class="form-subtitle">Start your productivity journey today</p>

          <form class="auth-form" @submit.prevent="handleRegister">
            <div class="form-group">
              <label>Username</label>
              <input
                v-model="form.username"
                type="text"
                class="input"
                placeholder="Choose a username"
                required
                minlength="3"
              />
              <span class="input-hint">At least 3 characters</span>
            </div>

            <div class="form-group">
              <label>Email</label>
              <input
                v-model="form.email"
                type="email"
                class="input"
                placeholder="Enter your email"
                required
              />
            </div>

            <div class="form-group">
              <label>Password</label>
              <div class="password-input">
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="input"
                  placeholder="Create a password"
                  required
                  minlength="6"
                />
                <button
                  type="button"
                  class="toggle-password"
                  @click="showPassword = !showPassword"
                >
                  <svg v-if="showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                  <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                    <line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
              <span class="input-hint">At least 6 characters</span>
            </div>

            <div class="form-group">
              <label>Confirm Password</label>
              <input
                v-model="form.confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                class="input"
                placeholder="Confirm your password"
                required
              />
            </div>

            <div class="form-options">
              <label class="terms-agreement">
                <input type="checkbox" v-model="form.agreeTerms" required />
                <span>I agree to the <a href="#">Terms of Service</a> and <a href="#">Privacy Policy</a></span>
              </label>
            </div>

            <button
              type="submit"
              class="btn btn-primary submit-btn"
              :disabled="isLoading || !isFormValid"
            >
              <span v-if="isLoading">Creating account...</span>
              <span v-else>Create Account</span>
            </button>
          </form>

          <div v-if="error" class="error-message">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {{ error }}
          </div>

          <div class="auth-divider">
            <span>or</span>
          </div>

          <p class="auth-footer">
            Already have an account?
            <router-link to="/login">Sign in</router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreeTerms: false
})

const showPassword = ref(false)
const isLoading = ref(false)
const error = ref('')

const isFormValid = computed(() => {
  return form.value.username.length >= 3 &&
         form.value.email.includes('@') &&
         form.value.password.length >= 6 &&
         form.value.password === form.value.confirmPassword &&
         form.value.agreeTerms
})

const handleRegister = async () => {
  if (!isFormValid.value) {
    if (form.value.password !== form.value.confirmPassword) {
      error.value = 'Passwords do not match'
    }
    return
  }

  isLoading.value = true
  error.value = ''

  try {
    const result = await authStore.register({
      username: form.value.username,
      email: form.value.email,
      password: form.value.password
    })

    if (result.success) {
      const redirectTarget = typeof route.query.redirect === 'string' ? route.query.redirect : '/tasks'
      router.push(redirectTarget)
    } else {
      error.value = result.message || 'Registration failed. Please try again.'
    }
  } catch (err) {
    error.value = 'An error occurred. Please try again.'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
}

.auth-container {
  display: flex;
  width: 100%;
  min-height: 100vh;
}

/* Brand Section */
.auth-brand {
  flex: 1;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-2xl);
  color: white;
}

.brand-content {
  max-width: 400px;
  text-align: center;
}

.brand-logo {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-lg);
  backdrop-filter: blur(10px);
}

.brand-logo svg {
  width: 40px;
  height: 40px;
}

.brand-content h1 {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--space-md);
}

.brand-content p {
  font-size: var(--font-size-lg);
  opacity: 0.9;
  line-height: 1.6;
}

/* Form Section */
.auth-form-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-2xl);
  background: var(--bg-primary);
  overflow-y: auto;
}

.auth-form-wrapper {
  width: 100%;
  max-width: 400px;
}

.auth-form-wrapper h2 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--space-xs);
  color: var(--text-primary);
}

.form-subtitle {
  color: var(--text-tertiary);
  margin-bottom: var(--space-xl);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.form-group label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin-bottom: var(--space-sm);
}

.input-hint {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-top: var(--space-xs);
}

.password-input {
  position: relative;
}

.password-input .input {
  padding-right: 44px;
}

.toggle-password {
  position: absolute;
  right: var(--space-sm);
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.toggle-password:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.form-options {
  font-size: var(--font-size-sm);
}

.terms-agreement {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  color: var(--text-secondary);
  cursor: pointer;
  line-height: 1.5;
}

.terms-agreement input {
  width: 16px;
  height: 16px;
  margin-top: 2px;
  accent-color: var(--color-primary);
  flex-shrink: 0;
}

.terms-agreement a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.terms-agreement a:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: var(--font-size-md);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--priority-high-bg);
  color: var(--priority-high);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  margin-top: var(--space-md);
}

.auth-divider {
  display: flex;
  align-items: center;
  margin: var(--space-xl) 0;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.auth-divider::before,
.auth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-light);
}

.auth-divider span {
  padding: 0 var(--space-md);
}

.auth-footer {
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.auth-footer a {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
}

.auth-footer a:hover {
  text-decoration: underline;
}

/* Responsive */
@media (max-width: 768px) {
  .auth-brand {
    display: none;
  }

  .auth-form-container {
    padding: var(--space-lg);
  }
}
</style>
