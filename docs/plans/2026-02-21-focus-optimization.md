# 番茄钟功能优化实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 优化番茄钟功能，增加时长预设、全屏模式、白噪音系统和统计报表

**Architecture:** 前端使用 Vue 3 + Pinia 扩展现有 focusStore，后端新增统计 API，白噪音使用 Web Audio API 程序化生成

**Tech Stack:** Vue 3, Pinia, Web Audio API, ECharts, Spring Boot 3

---

## Phase 1: 时长预设 + 全屏模式

### Task 1.1: 扩展 focusStore 添加时长预设

**Files:**
- Modify: `frontend/src/stores/focusStore.js`

**Step 1: 添加时长预设配置和状态**

在 `focusStore.js` 的 `defineStore` 内部开头添加：

```javascript
const durationPresets = {
  work: [5, 15, 25, 45],
  shortBreak: [1, 3, 5],
  longBreak: [5, 10, 15]
}

const selectedDurations = ref({
  work: 25,
  shortBreak: 5,
  longBreak: 15
})

const autoSaveSettings = () => {
  localStorage.setItem('focusSettings', JSON.stringify(selectedDurations.value))
}

const loadSettings = () => {
  const saved = localStorage.getItem('focusSettings')
  if (saved) {
    const parsed = JSON.parse(saved)
    selectedDurations.value = { ...selectedDurations.value, ...parsed }
    durations.work = selectedDurations.value.work * 60
    durations.shortBreak = selectedDurations.value.shortBreak * 60
    durations.longBreak = selectedDurations.value.longBreak * 60
  }
}

loadSettings()
```

**Step 2: 更新 durations 引用为计算属性**

将原来的 `const durations = {...}` 改为：

```javascript
const durations = reactive({
  work: selectedDurations.value.work * 60,
  shortBreak: selectedDurations.value.shortBreak * 60,
  longBreak: selectedDurations.value.longBreak * 60
})
```

**Step 3: 添加设置时长的函数**

在 `defineStore` 内添加：

```javascript
function setDuration(mode, value) {
  selectedDurations.value[mode] = value
  durations[mode] = value * 60
  if (!isRunning.value) {
    timeLeft.value = durations[mode]
  }
  autoSaveSettings()
}
```

**Step 4: 导出新增的状态和函数**

在 return 语句中添加：

```javascript
return {
  durationPresets,
  selectedDurations,
  setDuration,
  // ... 其他已有导出
}
```

**Step 5: 验证**

Run: `cd frontend && npm run build`
Expected: Build successful, no errors

---

### Task 1.2: 添加全屏模式功能

**Files:**
- Modify: `frontend/src/stores/focusStore.js`
- Modify: `frontend/src/views/FocusView.vue`

**Step 1: 在 focusStore 添加全屏状态**

在 `defineStore` 内添加：

```javascript
const isFullscreen = ref(false)

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function handleFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
```

注意：需要在文件顶部添加 `onMounted, onUnmounted` 的导入：
```javascript
import { defineStore } from 'pinia'
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
```

**Step 2: 导出全屏相关**

在 return 中添加：
```javascript
isFullscreen,
toggleFullscreen,
```

**Step 3: 在 FocusView.vue 添加全屏按钮**

在控制栏中，音效按钮后添加（约第109行后）：

```html
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
```

**Step 4: 添加全屏模式样式**

在 `<style scoped>` 中添加：

```css
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
```

**Step 5: 验证**

Run: `cd frontend && npm run build`
Expected: Build successful

---

### Task 1.3: 添加时长设置面板 UI

**Files:**
- Modify: `frontend/src/views/FocusView.vue`

**Step 1: 添加设置面板状态**

在 `<script setup>` 中添加：

```javascript
const showSettings = ref(false)
```

**Step 2: 在模板中添加设置面板**

在模式切换器（mode-switcher）div 后添加：

```html
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
  </div>
</div>
```

**Step 3: 添加设置面板样式**

在 `<style scoped>` 中添加：

```css
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
```

**Step 4: 验证**

Run: `cd frontend && npm run build`
Expected: Build successful

---

### Task 1.4: Phase 1 提交

**Step 1: 提交代码**

Run:
```bash
git add frontend/src/stores/focusStore.js frontend/src/views/FocusView.vue docs/plans/
git commit -m "feat(focus): add duration presets and fullscreen mode"
```

Expected: Commit successful

---

## Phase 2: 白噪音系统

### Task 2.1: 创建音效生成器工具

**Files:**
- Create: `frontend/src/utils/soundGenerator.js`

**Step 1: 创建音效生成器文件**

```javascript
class SoundGenerator {
  constructor() {
    this.audioContext = null
    this.activeSounds = new Map()
    this.volumes = new Map()
  }

  initContext() {
    if (!this.audioContext) {
      this.audioContext = new (window.AudioContext || window.webkitAudioContext)()
    }
    if (this.audioContext.state === 'suspended') {
      this.audioContext.resume()
    }
  }

  createNoiseBuffer(type = 'white') {
    const bufferSize = 2 * this.audioContext.sampleRate
    const buffer = this.audioContext.createBuffer(1, bufferSize, this.audioContext.sampleRate)
    const output = buffer.getChannelData(0)

    let lastOut = 0
    for (let i = 0; i < bufferSize; i++) {
      const white = Math.random() * 2 - 1
      
      if (type === 'white') {
        output[i] = white
      } else if (type === 'pink') {
        output[i] = (lastOut + (0.02 * white)) / 1.02
        lastOut = output[i]
        output[i] *= 3.5
      } else if (type === 'brown') {
        output[i] = (lastOut + (0.02 * white)) / 1.02
        lastOut = output[i]
        output[i] *= 2
      }
    }
    return buffer
  }

  createSound(name, type) {
    this.initContext()
    
    const sound = {
      source: null,
      gainNode: null,
      filter: null
    }

    sound.source = this.audioContext.createBufferSource()
    sound.source.buffer = this.createNoiseBuffer(type.noise || 'white')
    sound.source.loop = true

    sound.filter = this.audioContext.createBiquadFilter()
    sound.filter.type = type.filterType || 'lowpass'
    sound.filter.frequency.value = type.frequency || 1000

    sound.gainNode = this.audioContext.createGain()
    sound.gainNode.gain.value = 0

    sound.source.connect(sound.filter)
    sound.filter.connect(sound.gainNode)
    sound.gainNode.connect(this.audioContext.destination)

    sound.source.start()
    
    this.activeSounds.set(name, sound)
    this.volumes.set(name, 0)
    
    return sound
  }

  setVolume(name, volume) {
    const sound = this.activeSounds.get(name)
    if (sound) {
      sound.gainNode.gain.setTargetAtTime(volume * 0.5, this.audioContext.currentTime, 0.1)
      this.volumes.set(name, volume)
    }
  }

  getVolume(name) {
    return this.volumes.get(name) || 0
  }

  stopSound(name) {
    const sound = this.activeSounds.get(name)
    if (sound) {
      sound.source.stop()
      this.activeSounds.delete(name)
      this.volumes.delete(name)
    }
  }

  stopAll() {
    this.activeSounds.forEach((sound, name) => {
      this.stopSound(name)
    })
  }
}

export const soundTypes = {
  rain: { noise: 'brown', filterType: 'lowpass', frequency: 400 },
  forest: { noise: 'pink', filterType: 'bandpass', frequency: 800 },
  ocean: { noise: 'brown', filterType: 'lowpass', frequency: 200 },
  fire: { noise: 'pink', filterType: 'highpass', frequency: 300 },
  cafe: { noise: 'white', filterType: 'lowpass', frequency: 1500 },
  library: { noise: 'white', filterType: 'lowpass', frequency: 500 },
  fan: { noise: 'white', filterType: 'lowpass', frequency: 800 }
}

export const soundLabels = {
  rain: '🌧️ Rain',
  forest: '🌲 Forest',
  ocean: '🌊 Ocean',
  fire: '🔥 Fireplace',
  cafe: '☕ Cafe',
  library: '📚 Library',
  fan: '🌀 Fan'
}

export default new SoundGenerator()
```

**Step 2: 验证**

Run: `cd frontend && npm run build`
Expected: Build successful

---

### Task 2.2: 扩展 focusStore 添加白噪音管理

**Files:**
- Modify: `frontend/src/stores/focusStore.js`

**Step 1: 导入音效生成器**

在文件顶部添加：

```javascript
import soundGenerator, { soundTypes, soundLabels } from '../utils/soundGenerator'
```

**Step 2: 添加白噪音状态和管理函数**

在 `defineStore` 内添加：

```javascript
const ambientSounds = ref({})
const autoPlayAmbient = ref(false)

const initAmbientSound = (name) => {
  if (!ambientSounds.value[name]) {
    soundGenerator.createSound(name, soundTypes[name])
    ambientSounds.value[name] = { volume: 0, playing: false }
  }
}

const setAmbientVolume = (name, volume) => {
  initAmbientSound(name)
  soundGenerator.setVolume(name, volume)
  ambientSounds.value[name].volume = volume
  ambientSounds.value[name].playing = volume > 0
}

const toggleAmbientSound = (name) => {
  initAmbientSound(name)
  const current = ambientSounds.value[name]
  if (current.playing) {
    soundGenerator.setVolume(name, 0)
    current.volume = 0
    current.playing = false
  } else {
    soundGenerator.setVolume(name, 0.5)
    current.volume = 0.5
    current.playing = true
  }
}

const stopAllAmbient = () => {
  Object.keys(ambientSounds.value).forEach(name => {
    soundGenerator.setVolume(name, 0)
    ambientSounds.value[name].volume = 0
    ambientSounds.value[name].playing = false
  })
}
```

**Step 3: 修改 start 函数添加自动播放**

在 `start()` 函数开头，`isRunning.value = true` 之前添加：

```javascript
if (mode.value === 'work' && autoPlayAmbient.value) {
  const lastPlayed = Object.entries(ambientSounds.value).find(([_, s]) => s.playing)
  if (!lastPlayed) {
    const firstSound = Object.keys(soundTypes)[0]
    setAmbientVolume(firstSound, 0.5)
  }
}
```

**Step 4: 导出新增内容**

在 return 中添加：

```javascript
soundTypes,
soundLabels,
ambientSounds,
autoPlayAmbient,
initAmbientSound,
setAmbientVolume,
toggleAmbientSound,
stopAllAmbient,
```

**Step 5: 验证**

Run: `cd frontend && npm run build`
Expected: Build successful

---

### Task 2.3: 添加白噪音 UI

**Files:**
- Modify: `frontend/src/views/FocusView.vue`

**Step 1: 在设置面板中添加白噪音选项**

在 settings-panel 的最后，`</div>` 之前添加：

```html
<div class="setting-group ambient-section">
  <label>🎵 Ambient Sounds</label>
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
```

**Step 2: 添加白噪音样式**

在 `<style scoped>` 中添加：

```css
.ambient-section {
  border-top: 1px solid var(--border-light);
  padding-top: var(--space-md);
  margin-top: var(--space-md);
}

.ambient-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
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
```

**Step 3: 验证**

Run: `cd frontend && npm run build`
Expected: Build successful

---

### Task 2.4: Phase 2 提交

**Step 1: 提交代码**

Run:
```bash
git add frontend/src/utils/soundGenerator.js frontend/src/stores/focusStore.js frontend/src/views/FocusView.vue
git commit -m "feat(focus): add ambient sounds system"
```

Expected: Commit successful

---

## Phase 3: 统计报表增强

### Task 3.1: 后端 - 添加统计 API

**Files:**
- Modify: `backend/src/main/java/com/pomotodo/controller/FocusController.java`
- Modify: `backend/src/main/java/com/pomotodo/service/FocusService.java`

**Step 1: 在 FocusController 添加统计接口**

在 `FocusController.java` 中添加：

```java
@GetMapping("/daily")
public ResponseEntity<?> getDailyStats(@RequestParam(defaultValue = "7") int days) {
    return ResponseEntity.ok(focusService.getDailyStats(days));
}

@GetMapping("/weekly")
public ResponseEntity<?> getWeeklyStats() {
    return ResponseEntity.ok(focusService.getWeeklyStats());
}

@GetMapping("/monthly")
public ResponseEntity<?> getMonthlyStats() {
    return ResponseEntity.ok(focusService.getMonthlyStats());
}

@GetMapping("/habits")
public ResponseEntity<?> getHabits() {
    return ResponseEntity.ok(focusService.getHabits());
}
```

**Step 2: 在 FocusService 添加统计方法**

需要先查看现有的 FocusService 结构，然后添加相应方法。

**Step 3: 验证后端编译**

Run: `cd backend && mvn compile -q`
Expected: Build successful

---

### Task 3.2: 前端 - 添加统计面板

**Files:**
- Modify: `frontend/src/views/FocusView.vue`
- Modify: `frontend/src/stores/focusStore.js`

**Step 1: 在 focusStore 添加统计相关**

```javascript
const dailyStats = ref([])
const weeklyStats = ref(null)
const monthlyStats = ref(null)
const habits = ref(null)

const fetchDailyStats = async (days = 7) => {
  try {
    const res = await axios.get(`/api/focus/daily?days=${days}`)
    dailyStats.value = res.data.data || res.data
  } catch (e) {
    console.error('Failed to fetch daily stats:', e)
  }
}

const fetchWeeklyStats = async () => {
  try {
    const res = await axios.get('/api/focus/weekly')
    weeklyStats.value = res.data
  } catch (e) {
    console.error('Failed to fetch weekly stats:', e)
  }
}

const fetchMonthlyStats = async () => {
  try {
    const res = await axios.get('/api/focus/monthly')
    monthlyStats.value = res.data
  } catch (e) {
    console.error('Failed to fetch monthly stats:', e)
  }
}

const fetchHabits = async () => {
  try {
    const res = await axios.get('/api/focus/habits')
    habits.value = res.data
  } catch (e) {
    console.error('Failed to fetch habits:', e)
  }
}
```

**Step 2: 在 FocusView 添加统计面板 UI**

添加统计面板组件，显示趋势图和汇总数据。

**Step 3: 验证**

Run: `cd frontend && npm run build`
Expected: Build successful

---

### Task 3.3: Phase 3 提交

**Step 1: 提交代码**

Run:
```bash
git add backend/src/main/java/com/pomotodo/controller/FocusController.java backend/src/main/java/com/pomotodo/service/FocusService.java frontend/
git commit -m "feat(focus): add enhanced statistics and reports"
```

Expected: Commit successful

---

## 验收清单

### Phase 1
- [ ] 可选择 5/15/25/45 分钟专注时长
- [ ] 可选择 1/3/5 分钟短休息
- [ ] 可选择 5/10/15 分钟长休息
- [ ] 设置自动保存到 localStorage
- [ ] 全屏按钮正常工作
- [ ] ESC 退出全屏

### Phase 2
- [ ] 至少 5 种白噪音可选
- [ ] 每种可独立调节音量
- [ ] 支持多音效混合
- [ ] 自动播放选项生效

### Phase 3
- [ ] 每日趋势图显示
- [ ] 周/月统计数据准确
- [ ] 专注习惯数据正确
