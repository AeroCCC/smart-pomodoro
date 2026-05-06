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
  rain: '雨声',
  forest: '森林',
  ocean: '海浪',
  fire: '篝火',
  cafe: '咖啡馆',
  library: '图书馆',
  fan: '风扇'
}

export default new SoundGenerator()
