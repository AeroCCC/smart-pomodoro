import { defineStore } from 'pinia'
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import soundGenerator, { soundTypes, soundLabels } from '../utils/soundGenerator'

// Audio Manager for timer sounds
class AudioManager {
  constructor() {
    this.sounds = {}
    this.isEnabled = ref(true)
    this.volume = ref(0.5)
    
    // Initialize sounds
    this.initSounds()
  }
  
  initSounds() {
    // Create audio elements for different sounds
    this.sounds = {
      // Timer complete sound - pleasant chime
      complete: this.createAudio([
        // C5, E5, G5, C6 chord
        'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA='
      ]),
      // Work session start
      workStart: this.createAudio([
        'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA='
      ]),
      // Break start
      breakStart: this.createAudio([
        'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA='
      ]),
      // Click sound for button
      click: this.createAudio([
        'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA='
      ])
    }
  }
  
  createAudio(sources) {
    const audio = new Audio()
    // Use a simple beep sound as placeholder
    // In production, you would load actual audio files
    audio.src = this.generateBeepSound(440, 0.2) // 440Hz, 0.2s
    audio.volume = this.volume.value
    return audio
  }
  
  // Generate a simple beep sound using Web Audio API
  generateBeepSound(frequency, duration) {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const sampleRate = audioContext.sampleRate
    const numSamples = duration * sampleRate
    
    // Create WAV file
    const buffer = new ArrayBuffer(44 + numSamples * 2)
    const view = new DataView(buffer)
    
    // WAV header
    const writeString = (view, offset, string) => {
      for (let i = 0; i < string.length; i++) {
        view.setUint8(offset + i, string.charCodeAt(i))
      }
    }
    
    writeString(view, 0, 'RIFF')
    view.setUint32(4, 36 + numSamples * 2, true)
    writeString(view, 8, 'WAVE')
    writeString(view, 12, 'fmt ')
    view.setUint32(16, 16, true)
    view.setUint16(20, 1, true)
    view.setUint16(22, 1, true)
    view.setUint32(24, sampleRate, true)
    view.setUint32(28, sampleRate * 2, true)
    view.setUint16(32, 2, true)
    view.setUint16(34, 16, true)
    writeString(view, 36, 'data')
    view.setUint32(40, numSamples * 2, true)
    
    // Generate sine wave
    for (let i = 0; i < numSamples; i++) {
      const t = i / sampleRate
      const sample = Math.sin(2 * Math.PI * frequency * t) * 0.5
      view.setInt16(44 + i * 2, sample * 0x7FFF, true)
    }
    
    const blob = new Blob([buffer], { type: 'audio/wav' })
    return URL.createObjectURL(blob)
  }
  
  // Play complete sound with a nice melody
  playComplete() {
    if (!this.isEnabled.value) return
    
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const now = audioContext.currentTime
    
    // Play a nice C major chord: C4, E4, G4, C5
    const frequencies = [523.25, 659.25, 783.99, 1046.50] // C5, E5, G5, C6
    
    frequencies.forEach((freq, index) => {
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()
      
      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)
      
      oscillator.frequency.value = freq
      oscillator.type = 'sine'
      
      // Stagger the notes slightly for an arpeggio effect
      const startTime = now + index * 0.05
      const endTime = startTime + 0.8
      
      gainNode.gain.setValueAtTime(0, startTime)
      gainNode.gain.linearRampToValueAtTime(0.3 * this.volume.value, startTime + 0.05)
      gainNode.gain.exponentialRampToValueAtTime(0.01, endTime)
      
      oscillator.start(startTime)
      oscillator.stop(endTime)
    })
  }
  
  // Play work start sound
  playWorkStart() {
    if (!this.isEnabled.value) return
    
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const now = audioContext.currentTime
    
    // Rising tone
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    
    oscillator.frequency.setValueAtTime(440, now)
    oscillator.frequency.linearRampToValueAtTime(880, now + 0.3)
    oscillator.type = 'sine'
    
    gainNode.gain.setValueAtTime(0, now)
    gainNode.gain.linearRampToValueAtTime(0.3 * this.volume.value, now + 0.05)
    gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.5)
    
    oscillator.start(now)
    oscillator.stop(now + 0.5)
  }
  
  // Play break start sound
  playBreakStart() {
    if (!this.isEnabled.value) return
    
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const now = audioContext.currentTime
    
    // Gentle falling tone
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    
    oscillator.frequency.setValueAtTime(659.25, now) // E5
    oscillator.frequency.linearRampToValueAtTime(523.25, now + 0.5) // C5
    oscillator.type = 'sine'
    
    gainNode.gain.setValueAtTime(0, now)
    gainNode.gain.linearRampToValueAtTime(0.3 * this.volume.value, now + 0.1)
    gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.6)
    
    oscillator.start(now)
    oscillator.stop(now + 0.6)
  }
  
  // Play click sound
  playClick() {
    if (!this.isEnabled.value) return
    
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const now = audioContext.currentTime
    
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    
    oscillator.frequency.value = 800
    oscillator.type = 'sine'
    
    gainNode.gain.setValueAtTime(0.2 * this.volume.value, now)
    gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.1)
    
    oscillator.start(now)
    oscillator.stop(now + 0.1)
  }
  
  toggleSound() {
    this.isEnabled.value = !this.isEnabled.value
    return this.isEnabled.value
  }
  
  setVolume(value) {
    this.volume.value = Math.max(0, Math.min(1, value))
  }
}

// Create audio manager instance
const audioManager = new AudioManager()

export const useFocusStore = defineStore('focus', () => {
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

  // 番茄钟状态
  const timeLeft = ref(25 * 60) // 秒
  const isRunning = ref(false)
  const mode = ref('work') // 'work' | 'shortBreak' | 'longBreak'
  const currentTaskId = ref(null)
  const todaySessions = ref(0)
  const totalFocusTime = ref(0)
  
  // 音效设置
  const soundEnabled = computed(() => audioManager.isEnabled.value)
  const volume = computed(() => audioManager.volume.value)
  
  // 配置
  const durations = reactive({
    work: 25 * 60,
    shortBreak: 5 * 60,
    longBreak: 15 * 60
  })

  loadSettings()
  
  // 计时器
  let timerInterval = null
  let startTime = null
  
  // 计算属性
  const progress = computed(() => {
    const total = durations[mode.value] || durations.work
    return ((total - timeLeft.value) / total) * 100
  })
  
  // 格式化时间
  const formatTime = computed(() => {
    const mins = Math.floor(timeLeft.value / 60)
    const secs = timeLeft.value % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  })
  
  // 设置模式
  function setMode(newMode) {
    if (isRunning.value) return
    mode.value = newMode
    timeLeft.value = durations[newMode] || durations.work
  }

  function setDuration(modeType, value) {
    selectedDurations.value[modeType] = value
    durations[modeType] = value * 60
    if (!isRunning.value) {
      timeLeft.value = durations[modeType]
    }
    autoSaveSettings()
  }
  
  // 开始/暂停
  function toggle() {
    if (isRunning.value) {
      pause()
    } else {
      start()
    }
  }
  
  function start() {
    if (isRunning.value) return
    
    // Play appropriate start sound
    if (mode.value === 'work') {
      audioManager.playWorkStart()
    } else {
      audioManager.playBreakStart()
    }

    // Auto-play ambient sound if enabled
    if (mode.value === 'work' && autoPlayAmbient.value) {
      const lastPlayed = Object.entries(ambientSounds.value).find(([_, s]) => s.playing)
      if (!lastPlayed) {
        const firstSound = Object.keys(soundTypes)[0]
        setAmbientVolume(firstSound, 0.5)
      }
    }
    
    isRunning.value = true
    startTime = Date.now()
    
    timerInterval = setInterval(() => {
      if (timeLeft.value > 0) {
        timeLeft.value--
      } else {
        complete()
      }
    }, 1000)
  }
  
  function pause() {
    if (timerInterval) {
      clearInterval(timerInterval)
      timerInterval = null
    }
    isRunning.value = false
    audioManager.playClick()
  }
  
  // 完成当前周期
  async function complete() {
    pause()
    
    // Play completion sound
    audioManager.playComplete()
    
    // Save focus log if it was a work session
    if (mode.value === 'work' && startTime) {
      const duration = Math.floor((Date.now() - startTime) / 1000)
      await saveFocusLog(duration)
      todaySessions.value++
      totalFocusTime.value += Math.floor(duration / 60)
    }
    
    // Auto-switch mode
    if (mode.value === 'work') {
      // After 4 work sessions, take a long break
      if (todaySessions.value % 4 === 0) {
        mode.value = 'longBreak'
        timeLeft.value = durations.longBreak
      } else {
        mode.value = 'shortBreak'
        timeLeft.value = durations.shortBreak
      }
    } else {
      mode.value = 'work'
      timeLeft.value = durations.work
    }
  }
  
  // Save focus log to backend
  async function saveFocusLog(duration) {
    try {
      await axios.post('/api/focus', {
        duration: duration,
        startTime: new Date(Date.now() - duration * 1000).toISOString()
      })
    } catch (error) {
      console.error('Failed to save focus log:', error)
    }
  }
  
  // 重置
  function reset() {
    pause()
    timeLeft.value = durations[mode.value] || durations.work
    audioManager.playClick()
  }
  
  // 跳过
  function skip() {
    pause()
    const modes = ['work', 'shortBreak', 'longBreak']
    const currentIndex = modes.indexOf(mode.value)
    const nextIndex = (currentIndex + 1) % modes.length
    mode.value = modes[nextIndex]
    timeLeft.value = durations[mode.value]
    audioManager.playClick()
  }
  
  // 设置当前任务
  function setCurrentTask(taskId) {
    currentTaskId.value = taskId
  }
  
  // 音效控制
  function toggleSound() {
    return audioManager.toggleSound()
  }
  
  function setVolume(value) {
    audioManager.setVolume(value)
  }
  
  // 播放测试音效
  function playTestSound() {
    audioManager.playComplete()
  }
  
  // 白噪音状态
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

  // Statistics
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

  const fetchAllStats = async () => {
    await Promise.all([
      fetchDailyStats(7),
      fetchWeeklyStats(),
      fetchMonthlyStats(),
      fetchHabits()
    ])
  }

  // 全屏状态
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
  
  return {
    timeLeft,
    isRunning,
    mode,
    currentTaskId,
    todaySessions,
    totalFocusTime,
    progress,
    formatTime,
    soundEnabled,
    volume,
    durationPresets,
    selectedDurations,
    setMode,
    setDuration,
    toggle,
    start,
    pause,
    reset,
    skip,
    complete,
    setCurrentTask,
    toggleSound,
    setVolume,
    playTestSound,
    saveFocusLog,
    isFullscreen,
    toggleFullscreen,
    soundTypes,
    soundLabels,
    ambientSounds,
    autoPlayAmbient,
    initAmbientSound,
    setAmbientVolume,
    toggleAmbientSound,
    stopAllAmbient,
    dailyStats,
    weeklyStats,
    monthlyStats,
    habits,
    fetchDailyStats,
    fetchWeeklyStats,
    fetchMonthlyStats,
    fetchHabits,
    fetchAllStats
  }
})
