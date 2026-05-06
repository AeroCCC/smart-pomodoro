import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from '../lib/api'

export const useTaskStore = defineStore('task', () => {
  const tasks = ref([])
  const loading = ref(false)

  const completedTasks = computed(() => tasks.value.filter(t => t.completed))
  const pendingTasks = computed(() => tasks.value.filter(t => !t.completed))
  const completionRate = computed(() => {
    if (tasks.value.length === 0) return 0
    return Math.round((completedTasks.value.length / tasks.value.length) * 100)
  })

  async function fetchTasks() {
    loading.value = true
    try {
      const { data } = await axios.get('/api/tasks')
      tasks.value = data
    } finally {
      loading.value = false
    }
  }

  async function addTask(text, priority = 'medium', deadline = null, options = {}) {
    const { completionDefinition = null, estimatedPomodoros = null } = options
    const { data } = await axios.post('/api/tasks', {
      text,
      priority,
      deadline,
      completionDefinition,
      estimatedPomodoros
    })
    tasks.value.unshift(data)
    return data
  }

  async function updateTask(id, updates) {
    const { data } = await axios.put(`/api/tasks/${id}`, updates)
    const index = tasks.value.findIndex(t => t.id === id)
    if (index !== -1) tasks.value[index] = data
  }

  async function deleteTask(id) {
    await axios.delete(`/api/tasks/${id}`)
    tasks.value = tasks.value.filter(t => t.id !== id)
  }

  return { tasks, loading, completedTasks, pendingTasks, completionRate, fetchTasks, addTask, updateTask, deleteTask }
})
