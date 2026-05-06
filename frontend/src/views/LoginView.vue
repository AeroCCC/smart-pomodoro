<template>
  <div class="auth-page">
    <div class="auth-container">
      <!-- Left Side - Brand -->
      <div class="auth-brand">
        <div class="brand-content">
          <div class="brand-logo">
            <AppIcon name="Clock3" :size="40" :stroke-width="2" />
          </div>
          <h1>PomoFocus</h1>
          <p>Stay focused, achieve more. The smart way to manage your time and tasks.</p>
        </div>
      </div>

      <!-- Right Side - Login Form -->
      <div class="auth-form-container">
        <div class="auth-form-wrapper">
          <h2>Welcome Back</h2>
          <p class="form-subtitle">Sign in to continue to your workspace</p>

          <form class="auth-form" @submit.prevent="handleLogin">
            <div class="form-group">
              <label>Username or Email</label>
              <input
                v-model="form.usernameOrEmail"
                type="text"
                class="input"
                placeholder="Enter your username or email"
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
                  placeholder="Enter your password"
                  required
                />
                <button
                  type="button"
                  class="toggle-password"
                  @click="showPassword = !showPassword"
                >
                  <AppIcon v-if="showPassword" name="Eye" :size="20" />
                  <AppIcon v-else name="EyeOff" :size="20" />
                </button>
              </div>
            </div>

            <div class="form-options">
              <label class="remember-me">
                <input type="checkbox" v-model="form.rememberMe" />
                <span>Remember me</span>
              </label>
              <a href="#" class="forgot-password">Forgot password?</a>
            </div>

            <button
              type="submit"
              class="btn btn-primary submit-btn"
              :disabled="isLoading"
            >
              <span v-if="isLoading">Signing in...</span>
              <span v-else>Sign In</span>
            </button>
          </form>

          <div v-if="error" class="error-message">
            <AppIcon name="AlertCircle" :size="16" />
            {{ error }}
          </div>

          <div class="auth-divider">
            <span>or</span>
          </div>

          <p class="auth-footer">
            Don't have an account?
            <router-link to="/register">Create account</router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import AppIcon from '../components/AppIcon.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  usernameOrEmail: '',
  password: '',
  rememberMe: false
})

const showPassword = ref(false)
const isLoading = ref(false)
const error = ref('')

const handleLogin = async () => {
  isLoading.value = true
  error.value = ''

  try {
    const result = await authStore.login({
      usernameOrEmail: form.value.usernameOrEmail,
      password: form.value.password
    })

    if (result.success) {
      const redirectTarget = typeof route.query.redirect === 'string' ? route.query.redirect : '/tasks'
      router.push(redirectTarget)
    } else {
      error.value = result.message || 'Login failed. Please check your credentials.'
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-sm);
}

.remember-me {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--text-secondary);
  cursor: pointer;
}

.remember-me input {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
}

.forgot-password {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.forgot-password:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: var(--font-size-md);
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
