<template>
  <div class="team-page">
    <!-- Header -->
    <header class="page-header">
      <div class="header-title">
        <h1>Team Workspace</h1>
        <p class="subtitle">Collaborate and manage tasks together</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary" @click="showJoinDialog = true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M13.8 12H3"/>
          </svg>
          Join Team
        </button>
        <button class="btn btn-primary" @click="showCreateDialog = true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          Create Team
        </button>
      </div>
    </header>

    <!-- Teams Grid -->
    <div v-if="teamStore.isLoading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading teams...</p>
    </div>

    <div v-else-if="teamStore.allTeams.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
          <circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
      </div>
      <h3>No teams yet</h3>
      <p>Create a team or join an existing one to start collaborating</p>
    </div>

    <div v-else class="teams-container">
      <!-- Owned Teams -->
      <section v-if="teamStore.ownedTeams.length > 0" class="teams-section">
        <h2 class="section-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
          </svg>
          My Teams (Owner)
        </h2>
        <div class="teams-grid">
          <TeamCard
            v-for="team in teamStore.ownedTeams"
            :key="team.id"
            :team="team"
            :is-owner="true"
            @click="goToTeam(team.id)"
          />
        </div>
      </section>

      <!-- Joined Teams -->
      <section v-if="teamStore.joinedTeams.length > 0" class="teams-section">
        <h2 class="section-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          Joined Teams
        </h2>
        <div class="teams-grid">
          <TeamCard
            v-for="team in teamStore.joinedTeams"
            :key="team.id"
            :team="team"
            :is-owner="false"
            @click="goToTeam(team.id)"
          />
        </div>
      </section>
    </div>

    <!-- Create Team Dialog -->
    <teleport to="body">
      <div v-if="showCreateDialog" class="modal-overlay" @click.self="showCreateDialog = false">
        <div class="modal-content">
          <div class="modal-header">
            <h2>Create New Team</h2>
            <button class="close-btn" @click="showCreateDialog = false">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <form class="modal-body" @submit.prevent="createTeam">
            <div class="form-group">
              <label>Team Name *</label>
              <input
                v-model="createForm.name"
                type="text"
                class="input"
                placeholder="Enter team name"
                required
                maxlength="100"
              />
            </div>

            <div class="form-group">
              <label>Description</label>
              <textarea
                v-model="createForm.description"
                class="input"
                placeholder="Describe your team's purpose"
                rows="3"
                maxlength="500"
              />
            </div>

            <div v-if="createError" class="error-message">
              {{ createError }}
            </div>
          </form>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="showCreateDialog = false">Cancel</button>
            <button
              class="btn btn-primary"
              :disabled="!createForm.name.trim() || teamStore.isLoading"
              @click="createTeam"
            >
              {{ teamStore.isLoading ? 'Creating...' : 'Create Team' }}
            </button>
          </div>
        </div>
      </div>
    </teleport>

    <!-- Join Team Dialog -->
    <teleport to="body">
      <div v-if="showJoinDialog" class="modal-overlay" @click.self="showJoinDialog = false">
        <div class="modal-content">
          <div class="modal-header">
            <h2>Join a Team</h2>
            <button class="close-btn" @click="showJoinDialog = false">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div class="modal-body">
            <p class="join-description">Enter the invite code to join a team</p>
            
            <div class="form-group">
              <label>Invite Code</label>
              <input
                v-model="joinForm.inviteCode"
                type="text"
                class="input"
                placeholder="XXXX-XXXX"
                required
                maxlength="20"
                style="text-transform: uppercase;"
              />
            </div>

            <div v-if="joinError" class="error-message">
              {{ joinError }}
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="showJoinDialog = false">Cancel</button>
            <button
              class="btn btn-primary"
              :disabled="!joinForm.inviteCode.trim() || teamStore.isLoading"
              @click="joinTeam"
            >
              {{ teamStore.isLoading ? 'Joining...' : 'Join Team' }}
            </button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTeamStore } from '../stores/teamStore'
import TeamCard from '../components/TeamCard.vue'

const router = useRouter()
const teamStore = useTeamStore()

const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const createError = ref('')
const joinError = ref('')

const createForm = ref({
  name: '',
  description: ''
})

const joinForm = ref({
  inviteCode: ''
})

async function createTeam() {
  createError.value = ''
  
  const result = await teamStore.createTeam({
    name: createForm.value.name,
    description: createForm.value.description
  })
  
  if (result.success) {
    showCreateDialog.value = false
    createForm.value = { name: '', description: '' }
    // Navigate to the new team
    router.push(`/team/${result.data.id}`)
  } else {
    createError.value = result.error
  }
}

async function joinTeam() {
  joinError.value = ''
  
  const result = await teamStore.joinTeam(joinForm.value.inviteCode)
  
  if (result.success) {
    showJoinDialog.value = false
    joinForm.value = { inviteCode: '' }
    // Navigate to the joined team
    router.push(`/team/${result.data.id}`)
  } else {
    joinError.value = result.error
  }
}

function goToTeam(teamId) {
  router.push(`/team/${teamId}`)
}

onMounted(() => {
  teamStore.fetchTeams()
})
</script>

<style scoped>
.team-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-xl);
}

.header-title h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
}

.subtitle {
  color: var(--text-tertiary);
  font-size: var(--font-size-sm);
}

.header-actions {
  display: flex;
  gap: var(--space-sm);
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

.empty-state {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--text-muted);
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--space-lg);
  background: var(--bg-tertiary);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
}

.teams-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.teams-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.teams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-lg);
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.modal-content {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  width: 90%;
  max-width: 480px;
  box-shadow: var(--shadow-xl);
  animation: slideIn 0.3s ease;
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
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: var(--border-light);
  color: var(--text-primary);
}

.modal-body {
  padding: var(--space-xl);
}

.join-description {
  color: var(--text-secondary);
  margin-bottom: var(--space-lg);
  font-size: var(--font-size-sm);
}

.form-group {
  margin-bottom: var(--space-lg);
}

.form-group label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin-bottom: var(--space-sm);
}

.error-message {
  padding: var(--space-md);
  background: var(--priority-high-bg);
  color: var(--priority-high);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  margin-top: var(--space-md);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
  padding: var(--space-lg) var(--space-xl);
  border-top: 1px solid var(--border-light);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from { transform: translateY(-20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: var(--space-md);
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1;
  }

  .teams-grid {
    grid-template-columns: 1fr;
  }
}
</style>
