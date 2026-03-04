<template>
  <div class="focus-page">
    <div class="focus-container">
      <!-- Header -->
      <header class="focus-header">
        <button class="btn btn-ghost" @click="$router.push('/tasks')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          Back
        </button>
      </header>

      <!-- Mode Switcher -->
      <div class="mode-switcher">
        <button
          v-for="mode in ['work', 'shortBreak', 'longBreak']"
          :key="mode"
          class="mode-btn"
          :class="{ active: focusStore.mode === mode }"
          @click="setMode(mode)"
        >
          {{ mode === 'work' ? 'Focus Session' : mode === 'shortBreak' ? 'Short Break' : 'Long Break' }}
        </button>
      </div>

      <!-- Duration Settings -->
      <div class="settings-section">
        <button class="settings-toggle" @click="showSettings = !showSettings">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
          Settings
        </button>
        
        <div v-if="showSettings" class="settings-panel">
          <div class="setting-group">
            <label>Focus Duration</label>
            <div class="preset-buttons">
              <button
                v-for="preset in focusStore.durationPresets.work"
                :key="preset"
                class="preset-btn"
                :class="{ active: focusStore.selectedDurations.work === preset }"
                @click="focusStore.setDuration('work', preset)"
              >
                {{ preset }}m
              </button>
            </div>
          </div>
          
          <div class="setting-group">
            <label>Short Break</label>
            <div class="preset-buttons">
              <button
                v-for="preset in focusStore.durationPresets.shortBreak"
                :key="preset"
                class="preset-btn"
                :class="{ active: focusStore.selectedDurations.shortBreak === preset }"
                @click="focusStore.setDuration('shortBreak', preset)"
              >
                {{ preset }}m
              </button>
            </div>
          </div>
          
          <div class="setting-group">
            <label>Long Break</label>
            <div class="preset-buttons">
              <button
                v-for="preset in focusStore.durationPresets.longBreak"
                :key="preset"
                class="preset-btn"
                :class="{ active: focusStore.selectedDurations.longBreak === preset }"
                @click="focusStore.setDuration('longBreak', preset)"
              >
                {{ preset }}m
              </button>
            </div>
          </div>
          
          <div class="setting-group ambient-section">
            <label>Ambient Sounds</label>
            <div class="ambient-grid">
              <div
                v-for="(label, key) in focusStore.soundLabels"
                :key="key"
                class="ambient-item"
              >
                <button
                  class="ambient-toggle"
                  :class="{ active: focusStore.ambientSounds[key]?.playing }"
                  @click="focusStore.toggleAmbientSound(key)"
                >
                  {{ label }}
                </button>
                <input
                  v-if="focusStore.ambientSounds[key]?.playing"
                  type="range"
                  min="0"
                  max="100"
                  :value="(focusStore.ambientSounds[key]?.volume || 0) * 100"
                  @input="focusStore.setAmbientVolume(key, $event.target.value / 100)"
                  class="volume-slider"
                />
              </div>
            </div>
            
            <label class="checkbox-label">
              <input
                type="checkbox"
                v-model="focusStore.autoPlayAmbient"
              />
              Auto-play on focus start
            </label>
          </div>
        </div>
      </div>

      <!-- Timer Display -->
      <div class="timer-section">
        <div class="timer-ring">
          <svg class="timer-svg" viewBox="0 0 200 200">
            <!-- Background circle -->
            <circle
              class="timer-track"
              cx="100"
              cy="100"
              r="90"
            />
            <!-- Progress circle -->
            <circle
              class="timer-progress"
              cx="100"
              cy="100"
              r="90"
              :stroke-dasharray="565.48"
              :stroke-dashoffset="565.48 * (1 - focusStore.progress / 100)"
              :class="focusStore.mode"
            />
          </svg>
          <div class="timer-content">
            <div class="timer-time">{{ focusStore.formatTime }}</div>
            <div class="timer-label">
              <template v-if="currentTask">
                Focusing on: <span class="task-name">{{ currentTask.text }}</span>
              </template>
              <template v-else>
                Ready to focus
              </template>
            </div>
          </div>
        </div>
      </div>

      <!-- Controls -->
      <div class="timer-controls">
        <button class="control-btn secondary" @click="handleReset">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
          </svg>
        </button>

        <button
          class="control-btn primary"
          :class="{ running: focusStore.isRunning }"
          @click="toggleTimer"
        >
          <svg v-if="focusStore.isRunning" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <rect x="6" y="4" width="4" height="16"/>
            <rect x="14" y="4" width="4" height="16"/>
          </svg>
          <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
        </button>

        <button class="control-btn secondary" @click="handleSkip">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 4 15 12 5 20 5 4"/>
            <line x1="19" y1="5" x2="19" y2="19"/>
          </svg>
        </button>

        <!-- Sound Toggle -->
        <button
          class="control-btn secondary sound-btn"
          :class="{ muted: !focusStore.soundEnabled }"
          @click="toggleSound"
          title="Toggle sound"
        >
          <svg v-if="focusStore.soundEnabled" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
            <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
            <line x1="23" y1="9" x2="17" y2="15"/>
            <line x1="17" y1="9" x2="23" y2="15"/>
          </svg>
        </button>

        <!-- Fullscreen Toggle -->
        <button
          class="control-btn secondary"
          :class="{ active: focusStore.isFullscreen }"
          @click="focusStore.toggleFullscreen"
          title="Toggle fullscreen"
        >
          <svg v-if="!focusStore.isFullscreen" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"/>
          </svg>
        </button>
      </div>

      <!-- Stats -->
      <div class="focus-stats">
        <div class="stat-item">
          <span class="stat-value">{{ todaySessions }}</span>
          <span class="stat-label">Sessions Today</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ Math.round(totalFocusTime) }}</span>
          <span class="stat-label">Minutes Focused</span>
        </div>
      </div>

      <!-- Statistics Panel -->
      <div class="statistics-section">
        <button class="stats-toggle" @click="toggleStatsPanel">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 20V10M12 20V4M6 20v-6"/>
          </svg>
          Statistics
        </button>
        
        <div v-if="showStatsPanel" class="stats-panel">
          <!-- Weekly/Monthly Summary -->
          <div class="stats-summary">
            <div class="summary-card">
              <div class="summary-title">This Week</div>
              <div class="summary-value">{{ formatDuration(focusStore.weeklyStats?.totalMinutes) }}</div>
              <div class="summary-detail">{{ focusStore.weeklyStats?.sessions || 0 }} sessions</div>
            </div>
            <div class="summary-card">
              <div class="summary-title">This Month</div>
              <div class="summary-value">{{ formatDuration(focusStore.monthlyStats?.totalMinutes) }}</div>
              <div class="summary-detail">{{ focusStore.monthlyStats?.activeDays || 0 }} active days</div>
            </div>
          </div>
          
          <!-- Habits -->
          <div class="habits-section">
            <div class="habits-title">Focus Habits</div>
            <div class="habits-grid">
              <div class="habit-item">
                <span class="habit-label">Earliest</span>
                <span class="habit-value">{{ focusStore.habits?.earliestTime || 'N/A' }}</span>
              </div>
              <div class="habit-item">
                <span class="habit-label">Latest</span>
                <span class="habit-value">{{ focusStore.habits?.latestTime || 'N/A' }}</span>
              </div>
              <div class="habit-item">
                <span class="habit-label">Current Streak</span>
                <span class="habit-value streak">{{ focusStore.habits?.currentStreak || 0 }} days</span>
              </div>
              <div class="habit-item">
                <span class="habit-label">Longest Streak</span>
                <span class="habit-value">{{ focusStore.habits?.longestStreak || 0 }} days</span>
              </div>
            </div>
          </div>
          
          <!-- Daily Trend Chart -->
          <div class="chart-section">
            <div class="chart-title">Last 7 Days</div>
            <div class="chart-bars">
              <div 
                v-for="(day, index) in focusStore.dailyStats" 
                :key="day.date"
                class="chart-bar-container"
              >
                <div 
                  class="chart-bar" 
                  :style="{ height: getBarHeight(day.duration) + '%' }"
                  :title="`${day.date}: ${Math.round(day.duration / 60)} min`"
                ></div>
                <div class="chart-label">{{ formatDayLabel(day.date) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Completion Modal -->
    <teleport to="body">
      <div v-if="completeDialog" class="modal-overlay" @click.self="completeDialog = false">
        <div class="modal-content completion-modal">
          <div class="completion-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <h2>Great Job!</h2>
          <p>{{ completionMessage }}</p>
          <div class="modal-actions">
            <button class="btn btn-secondary" @click="completeDialog = false">
              Continue
            </button>
            <button class="btn btn-primary" @click="$router.push('/tasks')">
              Back to Tasks
            </button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useFocusStore } from '../stores/focusStore'
import { useTaskStore } from '../stores/taskStore'

const route = useRoute()
const router = useRouter()
const focusStore = useFocusStore()
const taskStore = useTaskStore()

const completeDialog = ref(false)
const showSettings = ref(false)
const showStatsPanel = ref(false)
const todaySessions = computed(() => focusStore.todaySessions)
const totalFocusTime = computed(() => focusStore.totalFocusTime)

const toggleStatsPanel = async () => {
  showStatsPanel.value = !showStatsPanel.value
  if (showStatsPanel.value) {
    await focusStore.fetchAllStats()
  }
}

const formatDuration = (minutes) => {
  if (!minutes) return '0m'
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (hours > 0) {
    return `${hours}h ${mins}m`
  }
  return `${mins}m`
}

const formatDayLabel = (dateStr) => {
  const date = new Date(dateStr)
  const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
  return days[date.getDay()]
}

const getBarHeight = (duration) => {
  const maxDuration = Math.max(...focusStore.dailyStats.map(d => d.duration || 0), 1)
  return Math.max((duration / maxDuration) * 100, 5)
}

const currentTask = computed(() => {
  if (!focusStore.currentTaskId) return null
  return taskStore.tasks.find(t => t.id === focusStore.currentTaskId)
})

const completionMessage = computed(() => {
  return focusStore.mode === 'work'
    ? 'Work session completed! Take a well-deserved break.'
    : 'Break is over! Ready to focus again?'
})

const setMode = (mode) => {
  if (focusStore.isRunning) {
    if (!confirm('Timer is running. Switch mode?')) return
    focusStore.pause()
  }
  focusStore.setMode(mode)
}

const toggleTimer = () => {
  focusStore.toggle()
}

const handleReset = () => {
  if (focusStore.isRunning) {
    if (!confirm('Timer is running. Reset?')) return
  }
  focusStore.reset()
}

const handleSkip = () => {
  if (focusStore.isRunning) {
    if (!confirm('Timer is running. Skip?')) return
    focusStore.pause()
  }
  focusStore.skip()
}

const toggleSound = () => {
  focusStore.toggleSound()
}

// Watch for timer completion
watch(() => focusStore.timeLeft, (newVal, oldVal) => {
  if (oldVal === 1 && newVal === 0) {
    completeDialog.value = true
  }
})

onMounted(async () => {
  await taskStore.fetchTasks()

  const taskId = route.params.taskId
  if (taskId) {
    const task = taskStore.tasks.find(t => t.id === parseInt(taskId))
    if (task) {
      focusStore.setCurrentTask(task.id)
    }
  }

  if (!focusStore.currentTaskId && taskStore.pendingTasks.length > 0) {
    focusStore.setCurrentTask(taskStore.pendingTasks[0].id)
  }
})

onUnmounted(() => {
  if (focusStore.isRunning) {
    focusStore.pause()
  }
})
</script>

<style scoped>
.focus-page {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--space-xl);
}

.focus-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-xl);
}

/* Header */
.focus-header {
  width: 100%;
  display: flex;
  justify-content: flex-start;
}

/* Mode Switcher */
.mode-switcher {
  display: flex;
  gap: var(--space-sm);
  background: var(--bg-tertiary);
  padding: 4px;
  border-radius: var(--radius-lg);
}

.mode-btn {
  padding: var(--space-sm) var(--space-md);
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.mode-btn:hover {
  color: var(--text-primary);
}

.mode-btn.active {
  background: var(--bg-secondary);
  color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

/* Settings Section */
.settings-section {
  width: 100%;
  max-width: 400px;
}

.settings-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: transparent;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin: 0 auto;
}

.settings-toggle:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.settings-panel {
  margin-top: var(--space-md);
  padding: var(--space-lg);
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.setting-group {
  margin-bottom: var(--space-md);
}

.setting-group:last-child {
  margin-bottom: 0;
}

.setting-group label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin-bottom: var(--space-sm);
}

.preset-buttons {
  display: flex;
  gap: var(--space-xs);
  flex-wrap: wrap;
}

.preset-btn {
  padding: var(--space-xs) var(--space-md);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.preset-btn:hover {
  background: var(--border-light);
  color: var(--text-primary);
}

.preset-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.ambient-section {
  border-top: 1px solid var(--border-light);
  padding-top: var(--space-md);
  margin-top: var(--space-md);
}

.ambient-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.ambient-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.ambient-toggle {
  padding: var(--space-sm);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: center;
}

.ambient-toggle:hover {
  background: var(--border-light);
}

.ambient-toggle.active {
  background: var(--color-info);
  border-color: var(--color-info);
  color: white;
}

.volume-slider {
  width: 100%;
  height: 4px;
  -webkit-appearance: none;
  background: var(--border-light);
  border-radius: 2px;
  outline: none;
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  background: var(--color-primary);
  border-radius: 50%;
  cursor: pointer;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  cursor: pointer;
}

.checkbox-label input {
  accent-color: var(--color-primary);
}

/* Timer Section */
.timer-section {
  position: relative;
}

.timer-ring {
  position: relative;
  width: 320px;
  height: 320px;
}

.timer-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.timer-track {
  fill: none;
  stroke: var(--border-light);
  stroke-width: 8;
}

.timer-progress {
  fill: none;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.5s ease;
}

.timer-progress.work {
  stroke: var(--color-primary);
}

.timer-progress.shortBreak,
.timer-progress.longBreak {
  stroke: var(--color-info);
}

.timer-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.timer-time {
  font-size: 64px;
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: -2px;
}

.timer-label {
  margin-top: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  max-width: 200px;
}

.task-name {
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

/* Controls */
.timer-controls {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.control-btn.primary {
  width: 72px;
  height: 72px;
  background: var(--color-primary);
  color: white;
  border-radius: 50%;
  box-shadow: 0 8px 24px rgba(255, 107, 53, 0.3);
}

.control-btn.primary:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 32px rgba(255, 107, 53, 0.4);
}

.control-btn.primary.running {
  background: var(--color-secondary);
  box-shadow: 0 8px 24px rgba(74, 85, 104, 0.3);
}

.control-btn.secondary {
  width: 48px;
  height: 48px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border-radius: 50%;
}

.control-btn.secondary:hover {
  background: var(--border-light);
  color: var(--text-primary);
}

.control-btn.sound-btn {
  position: relative;
}

.control-btn.sound-btn.muted {
  color: var(--text-muted);
}

.control-btn.sound-btn.muted::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 2px;
  height: 24px;
  background: currentColor;
  transform: translate(-50%, -50%) rotate(45deg);
}

/* Stats */
.focus-stats {
  display: flex;
  align-items: center;
  gap: var(--space-xl);
  padding: var(--space-md) var(--space-xl);
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--border-light);
}

/* Completion Modal */
.completion-modal {
  text-align: center;
  padding: var(--space-2xl);
}

.completion-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--space-lg);
  background: var(--priority-low-bg);
  color: var(--priority-low);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.completion-modal h2 {
  font-size: var(--font-size-2xl);
  margin-bottom: var(--space-sm);
}

.completion-modal p {
  color: var(--text-secondary);
  margin-bottom: var(--space-xl);
}

.modal-actions {
  display: flex;
  gap: var(--space-md);
  justify-content: center;
}

.focus-page:fullscreen,
.focus-page:-webkit-full-screen {
  background: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.focus-page:fullscreen .focus-header,
.focus-page:-webkit-full-screen .focus-header {
  position: absolute;
  top: 20px;
  left: 20px;
}

.focus-page:fullscreen .timer-ring,
.focus-page:-webkit-full-screen .timer-ring {
  width: 400px;
  height: 400px;
}

.focus-page:fullscreen .timer-time,
.focus-page:-webkit-full-screen .timer-time {
  font-size: 80px;
}

.control-btn.active {
  background: var(--color-primary);
  color: white;
}

/* Statistics Section */
.statistics-section {
  width: 100%;
  max-width: 500px;
}

.stats-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: transparent;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin: 0 auto;
}

.stats-toggle:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.stats-panel {
  margin-top: var(--space-md);
  padding: var(--space-lg);
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
}

.stats-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.summary-card {
  padding: var(--space-md);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  text-align: center;
}

.summary-title {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin-bottom: var(--space-xs);
}

.summary-value {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.summary-detail {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-top: var(--space-xs);
}

.habits-section {
  margin-bottom: var(--space-lg);
}

.habits-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin-bottom: var(--space-sm);
}

.habits-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}

.habit-item {
  display: flex;
  justify-content: space-between;
  padding: var(--space-sm);
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
}

.habit-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.habit-value {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.habit-value.streak {
  color: var(--color-primary);
}

.chart-section {
  margin-top: var(--space-md);
}

.chart-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin-bottom: var(--space-sm);
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: var(--space-xs);
  height: 80px;
  padding: var(--space-sm);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.chart-bar-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.chart-bar {
  width: 100%;
  max-width: 24px;
  background: var(--color-primary);
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  margin-top: auto;
  transition: height 0.3s ease;
  cursor: pointer;
}

.chart-bar:hover {
  background: var(--color-secondary);
}

.chart-label {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 4px;
}
</style>
