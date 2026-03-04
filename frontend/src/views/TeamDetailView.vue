<template>
  <div class="team-detail-page">
    <div v-if="!teamStore.currentTeam" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading team...</p>
    </div>

    <template v-else>
      <!-- Team Header -->
      <header class="team-header">
        <div class="header-main">
          <button class="btn btn-ghost back-btn" @click="$router.push('/team')">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            Back
          </button>
          
          <div class="team-title">
            <div class="team-avatar-large">
              {{ teamStore.currentTeam.name.charAt(0).toUpperCase() }}
            </div>
            <div class="team-info">
              <h1>{{ teamStore.currentTeam.name }}</h1>
              <p v-if="teamStore.currentTeam.description" class="description">
                {{ teamStore.currentTeam.description }}
              </p>
              <div class="team-meta">
                <span class="role-badge" :class="teamStore.currentTeam.myRole.toLowerCase()">
                  {{ teamStore.currentTeam.myRole }}
                </span>
                <span class="member-count">
                  {{ teamStore.currentTeam.memberCount }} members
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="header-actions">
          <button 
            v-if="teamStore.isCurrentTeamAdmin"
            class="btn btn-secondary"
            @click="openInviteDialog"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/>
              <line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
            Invite
          </button>
          <button 
            v-if="teamStore.isCurrentTeamOwner"
            class="btn btn-danger"
            @click="showDissolveDialog = true"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
            Dissolve Team
          </button>
        </div>
      </header>

      <!-- Tab Navigation -->
      <div class="tab-nav">
        <button 
          class="tab-btn"
          :class="{ active: activeTab === 'board' }"
          @click="activeTab = 'board'"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/>
            <rect x="14" y="3" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/>
            <rect x="3" y="14" width="7" height="7"/>
          </svg>
          Kanban Board
        </button>
        <button 
          class="tab-btn"
          :class="{ active: activeTab === 'members' }"
          @click="activeTab = 'members'"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          Members
        </button>
      </div>

      <!-- Kanban Board View -->
      <div v-if="activeTab === 'board'" class="kanban-board">
        <!-- TODO Column -->
        <div 
          class="kanban-column"
          :class="{ 'drag-over': dragOverColumn === 'TODO' }"
          @dragover.prevent
          @dragenter="handleDragEnter('TODO')"
          @dragleave="handleDragLeave"
          @drop="handleDrop('TODO')"
        >
          <div class="column-header">
            <h3>To Do</h3>
            <span class="task-count">{{ teamStore.todoTasks.length }}</span>
          </div>
          <div class="column-content">
            <TeamTaskCard
              v-for="task in teamStore.todoTasks"
              :key="task.id"
              :task="task"
              :can-edit="canEditTask(task)"
              @click="openTaskDetail(task)"
              @edit="openEditTask(task)"
              @delete="confirmDeleteTask(task)"
              @dragstart="handleCardDragStart"
            />
          </div>
          <button 
            v-if="teamStore.isCurrentTeamAdmin" 
            class="add-task-btn" 
            @click="openAddTask('TODO')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Add Task
          </button>
        </div>

        <!-- IN PROGRESS Column -->
        <div 
          class="kanban-column"
          :class="{ 'drag-over': dragOverColumn === 'IN_PROGRESS' }"
          @dragover.prevent
          @dragenter="handleDragEnter('IN_PROGRESS')"
          @dragleave="handleDragLeave"
          @drop="handleDrop('IN_PROGRESS')"
        >
          <div class="column-header">
            <h3>In Progress</h3>
            <span class="task-count">{{ teamStore.inProgressTasks.length }}</span>
          </div>
          <div class="column-content">
            <TeamTaskCard
              v-for="task in teamStore.inProgressTasks"
              :key="task.id"
              :task="task"
              :can-edit="canEditTask(task)"
              @click="openTaskDetail(task)"
              @edit="openEditTask(task)"
              @delete="confirmDeleteTask(task)"
              @dragstart="handleCardDragStart"
            />
          </div>
          <button 
            v-if="teamStore.isCurrentTeamAdmin" 
            class="add-task-btn" 
            @click="openAddTask('IN_PROGRESS')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Add Task
          </button>
        </div>

        <!-- DONE Column -->
        <div 
          class="kanban-column"
          :class="{ 'drag-over': dragOverColumn === 'DONE' }"
          @dragover.prevent
          @dragenter="handleDragEnter('DONE')"
          @dragleave="handleDragLeave"
          @drop="handleDrop('DONE')"
        >
          <div class="column-header">
            <h3>Done</h3>
            <span class="task-count">{{ teamStore.doneTasks.length }}</span>
          </div>
          <div class="column-content">
            <TeamTaskCard
              v-for="task in teamStore.doneTasks"
              :key="task.id"
              :task="task"
              :can-edit="canEditTask(task)"
              @click="openTaskDetail(task)"
              @edit="openEditTask(task)"
              @delete="confirmDeleteTask(task)"
              @dragstart="handleCardDragStart"
            />
          </div>
          <button 
            v-if="teamStore.isCurrentTeamAdmin" 
            class="add-task-btn" 
            @click="openAddTask('DONE')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Add Task
          </button>
        </div>
      </div>

      <!-- Members View -->
      <div v-else class="members-view">
        <div class="members-list">
          <div 
            v-for="member in teamStore.currentTeamMembers" 
            :key="member.id"
            class="member-card"
          >
            <div class="member-avatar">
              {{ member.username.charAt(0).toUpperCase() }}
            </div>
            <div class="member-info">
              <h4>{{ member.username }}</h4>
              <p class="member-email">{{ member.email }}</p>
            </div>
            <div class="member-role">
              <select 
                v-if="canManageRole(member)"
                v-model="member.role"
                class="role-select"
                @change="updateRole(member)"
              >
                <option value="ADMIN">Admin</option>
                <option value="MEMBER">Member</option>
              </select>
              <span v-else class="role-badge" :class="member.role.toLowerCase()">
                {{ member.role }}
              </span>
            </div>
            <button 
              v-if="canRemoveMember(member)"
              class="remove-btn"
              @click="removeMember(member)"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Invite Dialog -->
      <teleport to="body">
        <div v-if="showInviteDialog" class="modal-overlay" @click.self="showInviteDialog = false">
          <div class="modal-content">
            <div class="modal-header">
              <h2>Invite Members</h2>
              <button class="close-btn" @click="showInviteDialog = false">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <p class="invite-instruction">Share this invite link with your team members:</p>
              
              <!-- 完整邀请链接 -->
              <div class="invite-link-display">
                <span class="link-text">{{ inviteLink }}</span>
                <button class="copy-btn" @click="copyInviteLink">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                  </svg>
                  {{ copiedLink ? 'Copied!' : 'Copy Link' }}
                </button>
              </div>
              
              <div class="invite-divider">
                <span>OR</span>
              </div>
              
              <!-- 邀请码 -->
              <p class="invite-instruction">Use this invite code:</p>
              <div class="invite-code-display">
                <span class="code">{{ teamStore.currentTeam.inviteCode }}</span>
                <button class="copy-btn" @click="copyInviteCode">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                  </svg>
                  {{ copiedCode ? 'Copied!' : 'Copy Code' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </teleport>

      <!-- Task Dialog (Create/Edit) -->
      <teleport to="body">
        <div v-if="showTaskDialog" class="modal-overlay" @click.self="closeTaskDialog">
          <div class="modal-content task-modal">
            <div class="modal-header">
              <h2>{{ editingTask ? 'Edit Task' : 'New Task' }}</h2>
              <button class="close-btn" @click="closeTaskDialog">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <form @submit.prevent="saveTask">
                <div class="form-group">
                  <label>Task Title</label>
                  <input 
                    v-model="taskForm.text" 
                    type="text" 
                    placeholder="Enter task title"
                    required
                  />
                </div>
                
                <div class="form-row">
                  <div class="form-group">
                    <label>Priority</label>
                    <select v-model="taskForm.priority">
                      <option value="high">High</option>
                      <option value="medium">Medium</option>
                      <option value="low">Low</option>
                    </select>
                  </div>
                  
                  <div class="form-group">
                    <label>Deadline</label>
                    <input 
                      v-model="taskForm.deadline" 
                      type="datetime-local"
                    />
                  </div>
                </div>
                
                <div class="form-group">
                  <label>Assign To</label>
                  <select v-model="taskForm.assignedToId">
                    <option :value="null">Unassigned</option>
                    <option 
                      v-for="member in teamStore.currentTeamMembers" 
                      :key="member.id" 
                      :value="member.id"
                    >
                      {{ member.username }}
                    </option>
                  </select>
                </div>
                
                <div class="form-group">
                  <label>Status</label>
                  <select v-model="taskForm.status">
                    <option value="TODO">To Do</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="DONE">Done</option>
                  </select>
                </div>
                
                <div class="form-actions">
                  <button type="button" class="btn btn-secondary" @click="closeTaskDialog">
                    Cancel
                  </button>
                  <button type="submit" class="btn btn-primary" :disabled="teamStore.isLoading">
                    {{ teamStore.isLoading ? 'Saving...' : (editingTask ? 'Update' : 'Create') }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </teleport>

      <!-- Task Detail Dialog -->
      <teleport to="body">
        <div v-if="showTaskDetail" class="modal-overlay" @click.self="showTaskDetail = false">
          <div class="modal-content task-modal">
            <div class="modal-header">
              <h2>Task Details</h2>
              <button class="close-btn" @click="showTaskDetail = false">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
            <div class="modal-body" v-if="selectedTask">
              <div class="detail-item">
                <span class="label">Title:</span>
                <span class="value">{{ selectedTask.text }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Priority:</span>
                <span class="badge" :class="getPriorityClass(selectedTask.priority)">
                  {{ selectedTask.priority }}
                </span>
              </div>
              <div class="detail-item">
                <span class="label">Status:</span>
                <span class="status-badge" :class="selectedTask.status.toLowerCase()">
                  {{ formatStatus(selectedTask.status) }}
                </span>
              </div>
              <div class="detail-item">
                <span class="label">Created by:</span>
                <span class="value">{{ selectedTask.creatorName }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Assigned to:</span>
                <span class="value">{{ selectedTask.assignedToName || 'Unassigned' }}</span>
              </div>
              <div class="detail-item" v-if="selectedTask.deadline">
                <span class="label">Deadline:</span>
                <span class="value">{{ new Date(selectedTask.deadline).toLocaleString() }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Created:</span>
                <span class="value">{{ new Date(selectedTask.createdAt).toLocaleString() }}</span>
              </div>
            </div>
          </div>
        </div>
      </teleport>

      <!-- Dissolve Team Dialog -->
      <teleport to="body">
        <div v-if="showDissolveDialog" class="modal-overlay" @click.self="showDissolveDialog = false">
          <div class="modal-content">
            <div class="modal-header">
              <h2>Dissolve Team</h2>
              <button class="close-btn" @click="showDissolveDialog = false">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="warning-message">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="warning-icon">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
                <h3>Warning: This action cannot be undone!</h3>
                <p>Dissolving the team will:</p>
                <ul>
                  <li>Delete all team tasks permanently</li>
                  <li>Remove all team members</li>
                  <li>Delete all team data</li>
                </ul>
                <p class="confirm-text">Are you absolutely sure you want to dissolve <strong>{{ teamStore.currentTeam?.name }}</strong>?</p>
              </div>
              <div class="form-actions">
                <button class="btn btn-secondary" @click="showDissolveDialog = false">
                  Cancel
                </button>
                <button class="btn btn-danger" @click="dissolveTeam" :disabled="teamStore.isLoading">
                  {{ teamStore.isLoading ? 'Dissolving...' : 'Yes, Dissolve Team' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </teleport>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTeamStore } from '../stores/teamStore'
import TeamTaskCard from '../components/TeamTaskCard.vue'

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()

const activeTab = ref('board')
const showInviteDialog = ref(false)
const showTaskDialog = ref(false)
const showTaskDetail = ref(false)
const showDissolveDialog = ref(false)
const copiedCode = ref(false)
const copiedLink = ref(false)
const inviteLink = ref('')
const editingTask = ref(null)
const selectedTask = ref(null)
const dragOverColumn = ref(null)
const draggedTask = ref(null)

const taskForm = ref({
  text: '',
  priority: 'medium',
  deadline: '',
  assignedToId: null,
  status: 'TODO'
})

const teamId = computed(() => route.params.id)

function canEditTask(task) {
  if (teamStore.isCurrentTeamAdmin) return true
  // Assuming we have current user ID, check if user is the creator
  // For now, all members can edit
  return true
}

function canManageRole(member) {
  if (!teamStore.isCurrentTeamAdmin) return false
  if (member.role === 'OWNER') return false
  return true
}

function canRemoveMember(member) {
  if (member.role === 'OWNER') return false
  if (teamStore.isCurrentTeamOwner) return true
  if (teamStore.isCurrentTeamAdmin && member.role === 'MEMBER') return true
  return false
}

function getPriorityClass(priority) {
  const classes = {
    high: 'badge-high',
    medium: 'badge-medium',
    low: 'badge-low'
  }
  return classes[priority?.toLowerCase()] || 'badge-medium'
}

function formatStatus(status) {
  const statusMap = {
    'TODO': 'To Do',
    'IN_PROGRESS': 'In Progress',
    'DONE': 'Done'
  }
  return statusMap[status] || status
}

async function updateRole(member) {
  const result = await teamStore.updateMemberRole(teamId.value, member.id, member.role)
  if (!result.success) {
    await teamStore.fetchTeamMembers(teamId.value)
  }
}

async function removeMember(member) {
  if (!confirm(`Remove ${member.username} from the team?`)) return
  const result = await teamStore.removeMember(teamId.value, member.id)
  if (result.success) {
    // Member removed
  }
}

async function openInviteDialog() {
  showInviteDialog.value = true
  // 获取完整的邀请链接
  const result = await teamStore.getInviteLink(teamId.value)
  if (result.success) {
    inviteLink.value = result.data.inviteLink
  } else {
    // 如果API失败，使用默认生成的链接
    inviteLink.value = `${window.location.origin}/team/join?code=${teamStore.currentTeam.inviteCode}`
  }
}

function copyInviteCode() {
  navigator.clipboard.writeText(teamStore.currentTeam.inviteCode)
  copiedCode.value = true
  setTimeout(() => copiedCode.value = false, 2000)
}

function copyInviteLink() {
  navigator.clipboard.writeText(inviteLink.value)
  copiedLink.value = true
  setTimeout(() => copiedLink.value = false, 2000)
}

async function dissolveTeam() {
  if (!confirm('Are you sure you want to dissolve this team? All team data including tasks will be permanently deleted. This action cannot be undone.')) {
    return
  }
  
  const result = await teamStore.dissolveTeam(teamId.value)
  if (result.success) {
    alert('Team dissolved successfully')
    router.push('/team')
  } else {
    alert(result.error || 'Failed to dissolve team')
  }
}

// Task Management
function openAddTask(status = 'TODO') {
  editingTask.value = null
  taskForm.value = {
    text: '',
    priority: 'medium',
    deadline: '',
    assignedToId: null,
    status: status
  }
  showTaskDialog.value = true
}

function openEditTask(task) {
  editingTask.value = task
  taskForm.value = {
    text: task.text,
    priority: task.priority,
    deadline: task.deadline ? task.deadline.slice(0, 16) : '',
    assignedToId: task.assignedToId,
    status: task.status
  }
  showTaskDialog.value = true
}

function openTaskDetail(task) {
  selectedTask.value = task
  showTaskDetail.value = true
}

function closeTaskDialog() {
  showTaskDialog.value = false
  editingTask.value = null
}

async function saveTask() {
  const taskData = {
    text: taskForm.value.text,
    priority: taskForm.value.priority,
    status: taskForm.value.status,
    deadline: taskForm.value.deadline,
    assignedToId: taskForm.value.assignedToId
  }
  
  let result
  if (editingTask.value) {
    result = await teamStore.updateTeamTask(teamId.value, editingTask.value.id, taskData)
  } else {
    result = await teamStore.createTeamTask(teamId.value, taskData)
  }
  
  if (result.success) {
    closeTaskDialog()
  } else {
    alert(result.error)
  }
}

async function confirmDeleteTask(task) {
  if (!confirm(`Delete task "${task.text}"?`)) return
  const result = await teamStore.deleteTeamTask(teamId.value, task.id)
  if (!result.success) {
    alert(result.error)
  }
}

// Drag and Drop
function handleCardDragStart(task) {
  draggedTask.value = task
}

function handleDragEnter(status) {
  dragOverColumn.value = status
}

function handleDragLeave() {
  dragOverColumn.value = null
}

async function handleDrop(newStatus) {
  dragOverColumn.value = null
  if (!draggedTask.value || draggedTask.value.status === newStatus) {
    draggedTask.value = null
    return
  }
  
  const result = await teamStore.updateTaskStatus(teamId.value, draggedTask.value.id, newStatus)
  if (!result.success) {
    alert(result.error)
  }
  draggedTask.value = null
}

onMounted(async () => {
  await teamStore.fetchTeamDetail(teamId.value)
  await teamStore.fetchTeamMembers(teamId.value)
  await teamStore.fetchTeamTasks(teamId.value)
})
</script>

<style scoped>
.team-detail-page {
  max-width: 1400px;
  margin: 0 auto;
}

.loading-state {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--text-muted);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto var(--space-md);
  border: 3px solid var(--border-light);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Header */
.team-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--border-light);
}

.header-main {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.back-btn {
  align-self: flex-start;
}

.team-title {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.team-avatar-large {
  width: 72px;
  height: 72px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  flex-shrink: 0;
}

.team-info h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-xs) 0;
}

.description {
  color: var(--text-secondary);
  margin: 0 0 var(--space-sm) 0;
  max-width: 500px;
}

.team-meta {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.role-badge {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
}

.role-badge.owner {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.role-badge.admin {
  background: var(--priority-medium-bg);
  color: var(--priority-medium);
}

.role-badge.member {
  background: var(--priority-low-bg);
  color: var(--priority-low);
}

.member-count {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* Tab Navigation */
.tab-nav {
  display: flex;
  gap: var(--space-xs);
  margin-bottom: var(--space-xl);
  border-bottom: 1px solid var(--border-light);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all var(--transition-fast);
}

.tab-btn:hover {
  color: var(--text-primary);
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* Kanban Board */
.kanban-board {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-lg);
  min-height: 500px;
}

.kanban-column {
  background: var(--bg-tertiary);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  transition: background var(--transition-fast);
}

.kanban-column.drag-over {
  background: var(--color-primary-bg);
  border: 2px dashed var(--color-primary);
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) var(--space-md);
}

.column-header h3 {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.task-count {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  background: var(--bg-secondary);
  padding: 2px 10px;
  border-radius: var(--radius-full);
}

.column-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  min-height: 100px;
}

.add-task-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  padding: var(--space-md);
  background: transparent;
  border: 2px dashed var(--border-medium);
  border-radius: var(--radius-md);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.add-task-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Members View */
.members-view {
  max-width: 800px;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.member-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
}

.member-avatar {
  width: 48px;
  height: 48px;
  background: var(--color-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-lg);
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-info h4 {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
}

.member-email {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin: var(--space-xs) 0 0 0;
}

.role-select {
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  cursor: pointer;
}

.remove-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: var(--radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.remove-btn:hover {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  width: 90%;
  max-width: 500px;
  box-shadow: var(--shadow-xl);
}

.modal-content.task-modal {
  max-width: 600px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-lg) var(--space-xl);
  border-bottom: 1px solid var(--border-light);
}

.modal-header h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.close-btn:hover {
  background: var(--border-light);
  color: var(--text-primary);
}

.modal-body {
  padding: var(--space-xl);
}

.invite-instruction {
  color: var(--text-secondary);
  margin-bottom: var(--space-lg);
  font-size: var(--font-size-sm);
}

.invite-link-display {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--color-primary-bg);
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-md);
}

.invite-link-display .link-text {
  flex: 1;
  font-family: monospace;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  word-break: break-all;
}

.invite-divider {
  display: flex;
  align-items: center;
  margin: var(--space-md) 0;
  color: var(--text-muted);
}

.invite-divider::before,
.invite-divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--border-light);
}

.invite-divider span {
  padding: 0 var(--space-md);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
}

.invite-code-display {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.invite-code-display .code {
  flex: 1;
  font-family: monospace;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
  letter-spacing: 2px;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.copy-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Task Form */
.form-group {
  margin-bottom: var(--space-md);
}

.form-group label {
  display: block;
  margin-bottom: var(--space-xs);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.form-group input,
.form-group select {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--color-primary);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
  margin-top: var(--space-lg);
}

/* Task Detail */
.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--border-light);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item .label {
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.detail-item .value {
  color: var(--text-primary);
}

.status-badge {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.status-badge.todo {
  background: var(--priority-low-bg);
  color: var(--priority-low);
}

.status-badge.in_progress {
  background: var(--priority-medium-bg);
  color: var(--priority-medium);
}

.status-badge.done {
  background: var(--color-success-bg, #d1fae5);
  color: var(--color-success, #059669);
}

/* Danger Button */
.btn-danger {
  background: var(--priority-high);
  color: white;
  border: none;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  transition: all var(--transition-fast);
}

.btn-danger:hover {
  background: #dc2626;
  transform: translateY(-1px);
}

.btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Warning Message */
.warning-message {
  text-align: center;
  padding: var(--space-md);
}

.warning-icon {
  color: var(--priority-high);
  margin-bottom: var(--space-md);
}

.warning-message h3 {
  color: var(--priority-high);
  margin-bottom: var(--space-md);
  font-size: var(--font-size-lg);
}

.warning-message p {
  color: var(--text-secondary);
  margin-bottom: var(--space-sm);
}

.warning-message ul {
  text-align: left;
  display: inline-block;
  color: var(--text-secondary);
  margin: var(--space-md) 0;
  padding-left: var(--space-lg);
}

.warning-message li {
  margin-bottom: var(--space-xs);
}

.confirm-text {
  margin-top: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 1px solid var(--border-light);
  color: var(--text-primary);
  font-size: var(--font-size-md);
}

/* Responsive */
@media (max-width: 1024px) {
  .kanban-board {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .team-header {
    flex-direction: column;
    gap: var(--space-md);
  }

  .team-title {
    flex-direction: column;
    align-items: flex-start;
  }

  .kanban-board {
    grid-template-columns: 1fr;
  }

  .tab-nav {
    overflow-x: auto;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
