import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'
import axios from 'axios'
import soundGenerator, { soundLabels, soundTypes } from '../utils/soundGenerator'

class AudioManager {
  constructor() {
    this.sounds = {}
    this.audioContext = null
    this.objectUrls = new Set()
    this.isEnabled = ref(true)
    this.volume = ref(0.5)
    this.initSounds()
  }

  getAudioContext() {
    if (this.audioContext) return this.audioContext
    if (typeof window === 'undefined') return null
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) return null
    this.audioContext = new AudioContextClass()
    return this.audioContext
  }

  ensureAudioContext() {
    const audioContext = this.getAudioContext()
    if (!audioContext) return null
    if (audioContext.state === 'suspended') {
      audioContext.resume().catch(() => {})
    }
    return audioContext
  }

  initSounds() {
    this.sounds = {
      complete: this.createAudio(),
      workStart: this.createAudio(),
      breakStart: this.createAudio(),
      click: this.createAudio()
    }
  }

  createAudio() {
    const audio = new Audio()
    audio.src = this.generateBeepSound(440, 0.2)
    audio.volume = this.volume.value
    return audio
  }

  generateBeepSound(frequency, duration) {
    const audioContext = this.getAudioContext()
    if (!audioContext) return ''

    const sampleRate = audioContext.sampleRate
    const numSamples = Math.floor(duration * sampleRate)
    const buffer = new ArrayBuffer(44 + numSamples * 2)
    const view = new DataView(buffer)

    const writeString = (targetView, offset, value) => {
      for (let i = 0; i < value.length; i += 1) {
        targetView.setUint8(offset + i, value.charCodeAt(i))
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

    for (let i = 0; i < numSamples; i += 1) {
      const t = i / sampleRate
      const sample = Math.sin(2 * Math.PI * frequency * t) * 0.5
      view.setInt16(44 + i * 2, sample * 0x7fff, true)
    }

    const blob = new Blob([buffer], { type: 'audio/wav' })
    const objectUrl = URL.createObjectURL(blob)
    this.objectUrls.add(objectUrl)
    return objectUrl
  }

  playComplete() {
    if (!this.isEnabled.value) return
    const audioContext = this.ensureAudioContext()
    if (!audioContext) return

    const now = audioContext.currentTime
    const frequencies = [523.25, 659.25, 783.99, 1046.5]

    frequencies.forEach((freq, index) => {
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()

      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)
      oscillator.frequency.value = freq
      oscillator.type = 'sine'

      const startAt = now + index * 0.05
      const endAt = startAt + 0.8

      gainNode.gain.setValueAtTime(0, startAt)
      gainNode.gain.linearRampToValueAtTime(0.3 * this.volume.value, startAt + 0.05)
      gainNode.gain.exponentialRampToValueAtTime(0.01, endAt)

      oscillator.start(startAt)
      oscillator.stop(endAt)
    })
  }

  playWorkStart() {
    if (!this.isEnabled.value) return
    const audioContext = this.ensureAudioContext()
    if (!audioContext) return

    const now = audioContext.currentTime
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

  playBreakStart() {
    if (!this.isEnabled.value) return
    const audioContext = this.ensureAudioContext()
    if (!audioContext) return

    const now = audioContext.currentTime
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()

    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)

    oscillator.frequency.setValueAtTime(659.25, now)
    oscillator.frequency.linearRampToValueAtTime(523.25, now + 0.5)
    oscillator.type = 'sine'

    gainNode.gain.setValueAtTime(0, now)
    gainNode.gain.linearRampToValueAtTime(0.3 * this.volume.value, now + 0.1)
    gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.6)

    oscillator.start(now)
    oscillator.stop(now + 0.6)
  }

  playClick() {
    if (!this.isEnabled.value) return
    const audioContext = this.ensureAudioContext()
    if (!audioContext) return

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
    Object.values(this.sounds).forEach((audio) => {
      audio.volume = this.volume.value
    })
  }

  dispose() {
    Object.values(this.sounds).forEach((audio) => {
      audio.pause()
      audio.src = ''
    })
    this.sounds = {}

    this.objectUrls.forEach((url) => URL.revokeObjectURL(url))
    this.objectUrls.clear()

    if (this.audioContext && this.audioContext.state !== 'closed') {
      this.audioContext.close().catch(() => {})
    }
    this.audioContext = null
  }
}

const audioManager = new AudioManager()
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => audioManager.dispose(), { once: true })
}

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

  const durations = reactive({
    work: 25 * 60,
    shortBreak: 5 * 60,
    longBreak: 15 * 60
  })

  const timeLeft = ref(25 * 60)
  const isRunning = ref(false)
  const mode = ref('work')
  const currentTaskId = ref(null)
  const todaySessions = ref(0)
  const totalFocusTime = ref(0)

  let timerInterval = null
  let startTime = null

  const soundEnabled = computed(() => audioManager.isEnabled.value)
  const volume = computed(() => audioManager.volume.value)

  const progress = computed(() => {
    const total = durations[mode.value] || durations.work
    return ((total - timeLeft.value) / total) * 100
  })

  const formatTime = computed(() => {
    const mins = Math.floor(timeLeft.value / 60)
    const secs = timeLeft.value % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  })

  const autoSaveSettings = () => {
    if (typeof window === 'undefined') return
    localStorage.setItem('focusSettings', JSON.stringify(selectedDurations.value))
  }

  const loadSettings = () => {
    if (typeof window === 'undefined') return
    const saved = localStorage.getItem('focusSettings')
    if (!saved) return

    const parsed = JSON.parse(saved)
    selectedDurations.value = { ...selectedDurations.value, ...parsed }
    durations.work = selectedDurations.value.work * 60
    durations.shortBreak = selectedDurations.value.shortBreak * 60
    durations.longBreak = selectedDurations.value.longBreak * 60
    timeLeft.value = durations[mode.value]
  }

  loadSettings()

  function setMode(newMode) {
    if (isRunning.value) return
    mode.value = newMode
    timeLeft.value = durations[newMode] || durations.work
  }

  function setDuration(modeType, value) {
    selectedDurations.value[modeType] = value
    durations[modeType] = value * 60
    if (!isRunning.value && mode.value === modeType) {
      timeLeft.value = durations[modeType]
    }
    autoSaveSettings()
  }

  function start() {
    if (isRunning.value) return

    if (mode.value === 'work') {
      audioManager.playWorkStart()
    } else {
      audioManager.playBreakStart()
    }

    if (mode.value === 'work' && autoPlayAmbient.value) {
      const activeSound = Object.values(ambientSounds.value).find((sound) => sound.playing)
      if (!activeSound) {
        const firstSound = Object.keys(soundTypes)[0]
        setAmbientVolume(firstSound, 0.5)
      }
    }

    isRunning.value = true
    startTime = Date.now()

    timerInterval = setInterval(() => {
      if (timeLeft.value > 0) {
        timeLeft.value -= 1
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

  async function saveFocusLog(duration) {
    try {
      await axios.post('/api/focus', {
        duration,
        startTime: new Date(Date.now() - duration * 1000).toISOString()
      })
    } catch (error) {
      console.error('Failed to save focus log:', error)
    }
  }

  async function complete() {
    pause()
    audioManager.playComplete()

    if (mode.value === 'work' && startTime) {
      const duration = Math.floor((Date.now() - startTime) / 1000)
      await saveFocusLog(duration)
      todaySessions.value += 1
      totalFocusTime.value += Math.floor(duration / 60)
    }

    if (mode.value === 'work') {
      if (todaySessions.value > 0 && todaySessions.value % 4 === 0) {
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

    startTime = null
  }

  function reset() {
    pause()
    timeLeft.value = durations[mode.value] || durations.work
    audioManager.playClick()
  }

  function skip() {
    pause()
    const modes = ['work', 'shortBreak', 'longBreak']
    const currentIndex = modes.indexOf(mode.value)
    const nextIndex = (currentIndex + 1) % modes.length
    mode.value = modes[nextIndex]
    timeLeft.value = durations[mode.value]
    audioManager.playClick()
  }

  function toggle() {
    if (isRunning.value) {
      pause()
    } else {
      start()
    }
  }

  function setCurrentTask(taskId) {
    currentTaskId.value = taskId
  }

  function toggleSound() {
    return audioManager.toggleSound()
  }

  function setVolume(value) {
    audioManager.setVolume(value)
  }

  function playTestSound() {
    audioManager.playComplete()
  }

  const ambientSounds = ref({})
  const autoPlayAmbient = ref(false)

  const initAmbientSound = (name) => {
    if (!ambientSounds.value[name]) {
      soundGenerator.createSound(name, soundTypes[name])
      ambientSounds.value[name] = { volume: 0, playing: false }
    }
  }

  const setAmbientVolume = (name, nextVolume) => {
    initAmbientSound(name)
    soundGenerator.setVolume(name, nextVolume)
    ambientSounds.value[name].volume = nextVolume
    ambientSounds.value[name].playing = nextVolume > 0
  }

  const toggleAmbientSound = (name) => {
    initAmbientSound(name)
    const current = ambientSounds.value[name]
    if (current.playing) {
      soundGenerator.setVolume(name, 0)
      current.volume = 0
      current.playing = false
      return
    }

    soundGenerator.setVolume(name, 0.5)
    current.volume = 0.5
    current.playing = true
  }

  const stopAllAmbient = () => {
    Object.keys(ambientSounds.value).forEach((name) => {
      soundGenerator.setVolume(name, 0)
      ambientSounds.value[name].volume = 0
      ambientSounds.value[name].playing = false
    })
  }

  const dailyStats = ref([])
  const weeklyStats = ref(null)
  const monthlyStats = ref(null)
  const habits = ref(null)

  const fetchDailyStats = async (days = 7) => {
    try {
      const res = await axios.get(`/api/focus/daily?days=${days}`)
      dailyStats.value = res.data.data || res.data
    } catch (error) {
      console.error('Failed to fetch daily stats:', error)
    }
  }

  const fetchWeeklyStats = async () => {
    try {
      const res = await axios.get('/api/focus/weekly')
      weeklyStats.value = res.data
    } catch (error) {
      console.error('Failed to fetch weekly stats:', error)
    }
  }

  const fetchMonthlyStats = async () => {
    try {
      const res = await axios.get('/api/focus/monthly')
      monthlyStats.value = res.data
    } catch (error) {
      console.error('Failed to fetch monthly stats:', error)
    }
  }

  const fetchHabits = async () => {
    try {
      const res = await axios.get('/api/focus/habits')
      habits.value = res.data
    } catch (error) {
      console.error('Failed to fetch habits:', error)
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

  const isFullscreen = ref(false)

  const syncFullscreenState = () => {
    if (typeof document === 'undefined') {
      isFullscreen.value = false
      return
    }
    isFullscreen.value = !!document.fullscreenElement
  }

  const toggleFullscreen = async () => {
    if (typeof document === 'undefined') return
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen()
      } else {
        await document.exitFullscreen()
      }
    } catch (error) {
      console.error('Failed to toggle fullscreen:', error)
    } finally {
      syncFullscreenState()
    }
  }

  const bindFullscreenListener = () => {
    if (typeof document === 'undefined') return
    document.addEventListener('fullscreenchange', syncFullscreenState)
    syncFullscreenState()
  }

  const unbindFullscreenListener = () => {
    if (typeof document === 'undefined') return
    document.removeEventListener('fullscreenchange', syncFullscreenState)
    syncFullscreenState()
  }

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
    bindFullscreenListener,
    unbindFullscreenListener,
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
