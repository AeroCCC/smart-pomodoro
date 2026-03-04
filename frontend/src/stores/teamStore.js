import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

const API_URL = '/api/teams'
const TASK_API_URL = '/api/tasks'

export const useTeamStore = defineStore('team', () => {
  // State
  const ownedTeams = ref([])
  const joinedTeams = ref([])
  const currentTeam = ref(null)
  const currentTeamMembers = ref([])
  const currentTeamTasks = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  // Getters
  const allTeams = computed(() => [...ownedTeams.value, ...joinedTeams.value])
  const isCurrentTeamOwner = computed(() => {
    if (!currentTeam.value) return false
    return currentTeam.value.myRole === 'OWNER'
  })
  const isCurrentTeamAdmin = computed(() => {
    if (!currentTeam.value) return false
    return currentTeam.value.myRole === 'OWNER' || currentTeam.value.myRole === 'ADMIN'
  })

  // Actions
  async function fetchTeams() {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get(API_URL)
      ownedTeams.value = response.data.owned || []
      joinedTeams.value = response.data.joined || []
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to fetch teams'
      console.error('Failed to fetch teams:', err)
    } finally {
      isLoading.value = false
    }
  }

  async function createTeam(teamData) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.post(API_URL, teamData)
      ownedTeams.value.unshift(response.data)
      return { success: true, data: response.data }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to create team'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function joinTeam(inviteCode) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.post(`${API_URL}/join`, { inviteCode })
      joinedTeams.value.unshift(response.data)
      return { success: true, data: response.data }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to join team'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function fetchTeamDetail(teamId) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get(`${API_URL}/${teamId}`)
      currentTeam.value = response.data
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to fetch team detail'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function fetchTeamMembers(teamId) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get(`${API_URL}/${teamId}/members`)
      currentTeamMembers.value = response.data
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to fetch members'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function updateMemberRole(teamId, userId, role) {
    isLoading.value = true
    error.value = null
    try {
      await axios.put(`${API_URL}/${teamId}/members/${userId}/role`, { role })
      // Update local state
      const member = currentTeamMembers.value.find(m => m.id === userId)
      if (member) {
        member.role = role
      }
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to update role'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function removeMember(teamId, userId) {
    isLoading.value = true
    error.value = null
    try {
      await axios.delete(`${API_URL}/${teamId}/members/${userId}`)
      currentTeamMembers.value = currentTeamMembers.value.filter(m => m.id !== userId)
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to remove member'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  function clearCurrentTeam() {
    currentTeam.value = null
    currentTeamMembers.value = []
    currentTeamTasks.value = []
  }

  // Team Tasks Actions
  async function fetchTeamTasks(teamId) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get(`${TASK_API_URL}/team/${teamId}`)
      currentTeamTasks.value = response.data
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to fetch tasks'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function createTeamTask(teamId, taskData) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.post(`${TASK_API_URL}/team/${teamId}`, taskData)
      currentTeamTasks.value.unshift(response.data)
      return { success: true, data: response.data }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to create task'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function updateTeamTask(teamId, taskId, taskData) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.put(`${TASK_API_URL}/team/${teamId}/${taskId}`, taskData)
      const index = currentTeamTasks.value.findIndex(t => t.id === taskId)
      if (index !== -1) {
        currentTeamTasks.value[index] = response.data
      }
      return { success: true, data: response.data }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to update task'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function deleteTeamTask(teamId, taskId) {
    isLoading.value = true
    error.value = null
    try {
      await axios.delete(`${TASK_API_URL}/team/${teamId}/${taskId}`)
      currentTeamTasks.value = currentTeamTasks.value.filter(t => t.id !== taskId)
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to delete task'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function updateTaskStatus(teamId, taskId, status) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.put(`${TASK_API_URL}/team/${teamId}/${taskId}/status`, { status })
      const index = currentTeamTasks.value.findIndex(t => t.id === taskId)
      if (index !== -1) {
        currentTeamTasks.value[index] = response.data
      }
      return { success: true, data: response.data }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to update status'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  async function assignTask(teamId, taskId, assignedToId) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.put(`${TASK_API_URL}/team/${teamId}/${taskId}/assign`, { assignedToId })
      const index = currentTeamTasks.value.findIndex(t => t.id === taskId)
      if (index !== -1) {
        currentTeamTasks.value[index] = response.data
      }
      return { success: true, data: response.data }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to assign task'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  // Dissolve team (owner only)
  async function dissolveTeam(teamId) {
    isLoading.value = true
    error.value = null
    try {
      await axios.delete(`${API_URL}/${teamId}`)
      // Remove from owned teams
      ownedTeams.value = ownedTeams.value.filter(t => t.id !== teamId)
      // Clear current team if viewing
      if (currentTeam.value?.id === teamId) {
        clearCurrentTeam()
      }
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to dissolve team'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  // Get team invite link
  async function getInviteLink(teamId, baseUrl = window.location.origin) {
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get(`${API_URL}/${teamId}/invite-link`, {
        params: { baseUrl }
      })
      return { success: true, data: response.data }
    } catch (err) {
      error.value = err.response?.data?.error || 'Failed to get invite link'
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  // Getters for tasks by status
  const todoTasks = computed(() =>
    currentTeamTasks.value.filter(t => t.status === 'TODO')
  )
  const inProgressTasks = computed(() =>
    currentTeamTasks.value.filter(t => t.status === 'IN_PROGRESS')
  )
  const doneTasks = computed(() =>
    currentTeamTasks.value.filter(t => t.status === 'DONE')
  )

  return {
    // State
    ownedTeams,
    joinedTeams,
    currentTeam,
    currentTeamMembers,
    currentTeamTasks,
    isLoading,
    error,
    // Getters
    allTeams,
    isCurrentTeamOwner,
    isCurrentTeamAdmin,
    todoTasks,
    inProgressTasks,
    doneTasks,
    // Actions
    fetchTeams,
    createTeam,
    joinTeam,
    fetchTeamDetail,
    fetchTeamMembers,
    updateMemberRole,
    removeMember,
    clearCurrentTeam,
    fetchTeamTasks,
    createTeamTask,
    updateTeamTask,
    deleteTeamTask,
    updateTaskStatus,
    assignTask,
    dissolveTeam,
    getInviteLink
  }
})
