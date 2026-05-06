<template>
  <div class="focus-page" :class="`mode-${focusStore.mode}`">
    <div class="focus-shell">
      <header class="focus-topbar glass-panel">
        <button class="topbar-back" type="button" @click="router.back()">
          <AppIcon name="ArrowLeft" :size="18" />
          <span>返回</span>
        </button>

        <div class="mode-switcher" role="tablist" aria-label="专注模式">
          <button
            v-for="option in modeOptions"
            :key="option.value"
            type="button"
            class="mode-btn"
            :class="{ active: focusStore.mode === option.value }"
            :aria-pressed="focusStore.mode === option.value"
            @click="setMode(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </header>

      <section class="focus-grid">
        <section class="focus-stage glass-panel">
          <div class="stage-header">
            <div class="stage-heading">
              <h1>{{ currentModeMeta.title }}</h1>
            </div>
          </div>

          <div class="task-line">
            {{ currentTask?.text || currentModeMeta.emptyTask }}
          </div>

          <div class="timer-zone">
            <div class="timer-aura"></div>

            <div class="timer-shell">
              <svg class="timer-ring" viewBox="0 0 220 220" aria-hidden="true">
                <defs>
                  <linearGradient id="focusRingGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" :stop-color="ringGradient.start" />
                    <stop offset="100%" :stop-color="ringGradient.end" />
                  </linearGradient>
                </defs>
                <circle class="timer-track outer" cx="110" cy="110" r="96" />
                <circle class="timer-track inner" cx="110" cy="110" r="82" />
                <circle
                  class="timer-progress"
                  cx="110"
                  cy="110"
                  r="96"
                  :stroke-dasharray="603.19"
                  :stroke-dashoffset="603.19 - (603.19 * focusStore.progress) / 100"
                />
              </svg>

              <div class="timer-center">
                <p class="timer-eyebrow">{{ currentModeMeta.next }}</p>
                <p class="timer-display">{{ focusStore.formatTime }}</p>
              </div>
            </div>
          </div>

          <div class="controls-row">
            <button class="control-btn secondary" type="button" aria-label="重置计时器" @click="handleReset">
              <AppIcon name="RotateCcw" :size="18" />
            </button>
            <button class="control-btn primary" type="button" @click="toggleTimer">
              <AppIcon :name="focusStore.isRunning ? 'Pause' : 'Play'" :size="20" />
              <span>{{ primaryActionText }}</span>
            </button>
            <button class="control-btn secondary" type="button" aria-label="跳过当前阶段" @click="handleSkip">
              <AppIcon name="SkipForward" :size="18" />
            </button>
            <button class="control-btn secondary" type="button" aria-label="切换提示音" @click="toggleSound">
              <AppIcon :name="focusStore.soundEnabled ? 'Volume2' : 'VolumeX'" :size="18" />
            </button>
            <button class="control-btn secondary" type="button" aria-label="切换全屏" @click="focusStore.toggleFullscreen()">
              <AppIcon :name="focusStore.isFullscreen ? 'Shrink' : 'Expand'" :size="18" />
            </button>
          </div>

          <div class="summary-grid">
            <article class="summary-card">
              <span class="summary-label">今日场次</span>
              <strong>{{ todaySessions }}</strong>
            </article>
            <article class="summary-card accent">
              <span class="summary-label">专注分钟</span>
              <strong>{{ totalFocusTime }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-label">节奏目标</span>
              <strong>{{ rhythmGoalText }}</strong>
            </article>
          </div>
        </section>

        <aside class="focus-sidebar">
          <section class="side-card glass-panel">
            <button class="panel-toggle" type="button" @click="showSettings = !showSettings">
              <h3>会话设置</h3>
              <AppIcon :name="showSettings ? 'ChevronLeft' : 'Settings'" :size="18" />
            </button>

            <div v-if="showSettings" class="settings-panel">
              <div class="duration-grid">
                <label v-for="option in modeOptions" :key="option.value" class="field-block">
                  <span>{{ option.settingLabel }}</span>
                  <select
                    :value="focusStore.selectedDurations[option.value]"
                    @change="focusStore.setDuration(option.value, Number($event.target.value))"
                  >
                    <option
                      v-for="preset in focusStore.durationPresets[option.value]"
                      :key="preset"
                      :value="preset"
                    >
                      {{ preset }} 分钟
                    </option>
                  </select>
                </label>
              </div>

              <label class="field-block range-block">
                <span>提醒音量</span>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  :value="focusStore.volume"
                  @input="focusStore.setVolume(Number($event.target.value))"
                />
              </label>

              <label class="toggle-row">
                <span>专注时自动播放白噪音</span>
                <input v-model="focusStore.autoPlayAmbient" type="checkbox" />
              </label>

              <div class="ambient-grid">
                <button
                  v-for="(label, key) in focusStore.soundLabels"
                  :key="key"
                  type="button"
                  class="ambient-pill"
                  :class="{ active: focusStore.ambientSounds[key]?.playing }"
                  @click="focusStore.toggleAmbientSound(key)"
                >
                  <span>{{ label }}</span>
                </button>
              </div>
            </div>
          </section>
        </aside>
      </section>
    </div>

    <Teleport to="body">
      <div v-if="completeDialog" class="focus-modal-overlay" @click.self="completeDialog = false">
        <div class="focus-modal glass-panel">
          <div class="focus-modal-icon">
            <AppIcon name="CircleCheck" :size="28" />
          </div>
          <h3>{{ completionMessage }}</h3>
          <button class="btn btn-primary modal-action" type="button" @click="completeDialog = false">
            继续
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import { useFocusStore } from '../stores/focusStore'
import { useTaskStore } from '../stores/taskStore'

const router = useRouter()
const route = useRoute()
const focusStore = useFocusStore()
const taskStore = useTaskStore()

const completeDialog = ref(false)
const showSettings = ref(true)

const modeOptions = [
  { value: 'work', label: '专注', settingLabel: '专注时长' },
  { value: 'shortBreak', label: '短休息', settingLabel: '短休息时长' },
  { value: 'longBreak', label: '长休息', settingLabel: '长休息时长' }
]

const modeMetaMap = {
  work: {
    title: '开始专注',
    next: '接下来：短休息',
    actionLabel: '开始专注',
    emptyTask: '选择一个任务开始本轮专注',
    ringStart: 'var(--focus-work)',
    ringEnd: '#FFB84D'
  },
  shortBreak: {
    title: '短休息',
    next: '接下来：回到专注',
    actionLabel: '开始休息',
    emptyTask: '放松一下，准备下一轮',
    ringStart: 'var(--focus-short-break)',
    ringEnd: '#86B7FF'
  },
  longBreak: {
    title: '长休息',
    next: '接下来：重新进入专注',
    actionLabel: '开始休息',
    emptyTask: '好好休息，恢复状态',
    ringStart: 'var(--focus-long-break)',
    ringEnd: '#8DE2C8'
  }
}

const currentModeMeta = computed(() => modeMetaMap[focusStore.mode] || modeMetaMap.work)
const ringGradient = computed(() => ({
  start: currentModeMeta.value.ringStart,
  end: currentModeMeta.value.ringEnd
}))
const currentTask = computed(() => taskStore.tasks.find((task) => task.id === focusStore.currentTaskId) || taskStore.pendingTasks[0] || null)
const todaySessions = computed(() => focusStore.todaySessions)
const totalFocusTime = computed(() => focusStore.totalFocusTime)
const rhythmGoalText = computed(() => `${Math.min(todaySessions.value, 4)}/4`)
const primaryActionText = computed(() => (focusStore.isRunning ? '暂停' : currentModeMeta.value.actionLabel))
const completionMessage = computed(() => {
  if (focusStore.mode === 'shortBreak' || focusStore.mode === 'longBreak') {
    return '休息完成，下一轮专注已就绪'
  }
  return '本轮专注完成'
})

const setMode = (mode) => {
  focusStore.setMode(mode)
}

const toggleTimer = () => {
  focusStore.toggle()
}

const handleReset = () => {
  focusStore.reset()
}

const handleSkip = () => {
  focusStore.skip()
}

const toggleSound = () => {
  focusStore.toggleSound()
}

watch(
  () => focusStore.timeLeft,
  (value, oldValue) => {
    if (oldValue === 1 && value > oldValue) {
      completeDialog.value = true
    }
  }
)

onMounted(async () => {
  focusStore.bindFullscreenListener()
  await taskStore.fetchTasks()

  const routeTaskId = route.params.taskId
    ? Number(route.params.taskId)
    : route.query.taskId
      ? Number(route.query.taskId)
      : null

  if (routeTaskId) {
    focusStore.setCurrentTask(routeTaskId)
  } else if (!focusStore.currentTaskId && taskStore.pendingTasks.length > 0) {
    focusStore.setCurrentTask(taskStore.pendingTasks[0].id)
  }
})

onUnmounted(() => {
  focusStore.unbindFullscreenListener()
  if (focusStore.isRunning) {
    focusStore.pause()
  }
})
</script>

<style scoped>
.focus-page {
  position: relative;
  min-height: 100vh;
  padding: clamp(20px, 3vw, 36px);
  background: var(--bg-focus-shell);
  overflow: hidden;
}

.focus-page::before,
.focus-page::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  filter: blur(18px);
  opacity: 0.72;
  pointer-events: none;
}

.focus-page::before {
  top: 72px;
  right: 8%;
  width: 280px;
  height: 280px;
  background: rgba(255, 184, 77, 0.18);
}

.focus-page::after {
  bottom: 64px;
  left: 2%;
  width: 240px;
  height: 240px;
  background: rgba(76, 141, 255, 0.16);
}

.focus-shell {
  position: relative;
  z-index: 1;
  max-width: 1320px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.glass-panel {
  background: var(--bg-focus-panel);
  border: 1px solid var(--border-glass);
  box-shadow: var(--shadow-glass);
  backdrop-filter: var(--panel-blur);
}

.focus-topbar {
  display: flex;
  align-items: center;
  gap: 18px;
  justify-content: space-between;
  padding: 16px 18px;
  border-radius: var(--radius-xl);
}

.topbar-back,
.mode-btn,
.control-btn,
.panel-toggle,
.ambient-pill {
  border: none;
  cursor: pointer;
}

.topbar-back {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.72);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
}

.mode-switcher {
  display: inline-grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 10px;
  padding: 6px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.52);
}

.mode-btn {
  min-height: 42px;
  padding: 0 16px;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  transition: transform var(--transition-fast), background var(--transition-fast), color var(--transition-fast), box-shadow var(--transition-fast);
}

.mode-btn.active {
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-primary);
  box-shadow: var(--shadow-soft);
}

.focus-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.36fr) minmax(300px, 0.74fr);
  gap: 24px;
  align-items: start;
}

.focus-stage {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: clamp(24px, 3vw, 32px);
  border-radius: 32px;
}

.stage-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.stage-heading {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stage-heading h1,
.side-card h3,
.focus-modal h3 {
  margin: 0;
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.08;
}

.task-line {
  margin: -6px 0 2px;
  text-align: center;
  font-size: 28px;
  line-height: 1.3;
  font-weight: 700;
  color: var(--text-primary);
}

.timer-eyebrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 8px 14px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.76);
  color: var(--text-accent);
  font-size: 13px;
  font-weight: var(--font-weight-semibold);
}

.timer-zone {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 500px;
}

.timer-aura {
  position: absolute;
  width: min(440px, 76%);
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 107, 53, 0.16), rgba(255, 255, 255, 0));
  filter: blur(14px);
}

.timer-shell {
  position: relative;
  width: min(100%, 540px);
  aspect-ratio: 1;
  display: grid;
  place-items: center;
}

.timer-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.timer-track {
  fill: none;
}

.timer-track.outer {
  stroke: var(--focus-ring-track);
  stroke-width: 14;
}

.timer-track.inner {
  stroke: var(--focus-ring-inner);
  stroke-width: 22;
}

.timer-progress {
  fill: none;
  stroke: url(#focusRingGradient);
  stroke-width: 14;
  stroke-linecap: round;
  transition: stroke-dashoffset 300ms ease;
  filter: drop-shadow(0 10px 18px rgba(255, 107, 53, 0.22));
}

.timer-center {
  position: absolute;
  left: 50%;
  top: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  transform: translate(-50%, -50%);
  text-align: center;
}

.timer-display {
  margin: 0;
  font-size: clamp(72px, 12vw, 104px);
  line-height: 0.92;
  letter-spacing: -0.06em;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}

.controls-row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) repeat(3, 56px);
  gap: 12px;
}

.control-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 56px;
  border-radius: 22px;
  background: none;
  color: var(--text-primary);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-fast), color var(--transition-fast);
}

.control-btn:hover,
.topbar-back:hover,
.panel-toggle:hover,
.ambient-pill:hover,
.mode-btn:hover {
  transform: translateY(-1px);
}

.control-btn.secondary {
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.4);
}

.control-btn.primary {
  gap: 10px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-inverse);
  background: linear-gradient(135deg, var(--focus-work), #FFB84D);
  box-shadow: var(--shadow-primary-glow);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.56);
}

.summary-card.accent {
  background: var(--surface-accent);
}

.summary-label {
  font-size: 15px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-secondary);
}

.summary-card strong {
  font-size: 38px;
  line-height: 1;
  color: var(--text-primary);
}

.focus-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-card {
  padding: 24px;
  border-radius: 28px;
}

.panel-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0;
  background: none;
  color: var(--text-primary);
}

.settings-panel {
  margin-top: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.duration-grid {
  display: grid;
  gap: 12px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.field-block span,
.toggle-row span {
  font-weight: var(--font-weight-semibold);
}

.field-block select,
.field-block input[type="range"] {
  width: 100%;
}

.field-block select {
  min-height: 46px;
  padding: 0 14px;
  border-radius: 16px;
  border: 1px solid var(--border-light);
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-primary);
}

.range-block input[type="range"] {
  accent-color: var(--focus-work);
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--text-secondary);
}

.ambient-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ambient-pill {
  min-height: 54px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(255, 255, 255, 0.52);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  transition: transform var(--transition-fast), background var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.ambient-pill.active {
  background: rgba(255, 107, 53, 0.12);
  border-color: rgba(255, 107, 53, 0.28);
  box-shadow: var(--shadow-soft);
}

.focus-modal-overlay {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(24, 33, 49, 0.36);
  backdrop-filter: blur(10px);
  z-index: 2000;
}

.focus-modal {
  width: min(100%, 400px);
  padding: 28px;
  border-radius: 30px;
  text-align: center;
}

.focus-modal-icon {
  width: 68px;
  height: 68px;
  margin: 0 auto 16px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.18), rgba(255, 184, 77, 0.22));
  color: var(--focus-work);
}

.modal-action {
  width: 100%;
  margin-top: 20px;
}

.mode-shortBreak .control-btn.primary {
  background: linear-gradient(135deg, var(--focus-short-break), #86B7FF);
}

.mode-longBreak .control-btn.primary {
  background: linear-gradient(135deg, var(--focus-long-break), #8DE2C8);
}

.mode-shortBreak .timer-aura {
  background: radial-gradient(circle, rgba(76, 141, 255, 0.16), rgba(255, 255, 255, 0));
}

.mode-longBreak .timer-aura {
  background: radial-gradient(circle, rgba(78, 201, 165, 0.18), rgba(255, 255, 255, 0));
}

@media (max-width: 1120px) {
  .focus-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .focus-topbar,
  .stage-header {
    flex-direction: column;
    align-items: stretch;
  }

  .mode-switcher,
  .summary-grid,
  .controls-row,
  .ambient-grid {
    grid-template-columns: 1fr;
  }

  .controls-row {
    display: flex;
    flex-wrap: wrap;
  }

  .control-btn.primary {
    order: -1;
    width: 100%;
  }
}

@media (max-width: 640px) {
  .focus-page {
    padding: 16px;
  }

  .focus-topbar,
  .focus-stage,
  .side-card,
  .focus-modal {
    padding: 18px;
  }

  .timer-zone {
    min-height: 400px;
  }

  .timer-shell {
    width: min(100%, 420px);
  }

  .task-line {
    font-size: 22px;
  }
}
</style>
