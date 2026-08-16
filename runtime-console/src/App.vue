<script setup>
import { computed, onMounted, ref } from 'vue'

const tabs = [
  { id: 'tasks', label: 'Tasks' },
  { id: 'evaluations', label: 'Evaluations' },
  { id: 'skills', label: 'Skills' },
]

const activeTab = ref('tasks')
const tokenInput = ref(localStorage.getItem('evoagent.runtimeToken') || '')
const token = ref(localStorage.getItem('evoagent.runtimeToken') || '')
const tasks = ref([])
const selectedTask = ref(null)
const trace = ref([])
const loadingTasks = ref(false)
const loadingTrace = ref(false)
const retrying = ref(false)
const errorMessage = ref('')

const taskCountText = computed(() => {
  return `${tasks.value.length} runtime task${tasks.value.length === 1 ? '' : 's'}`
})

onMounted(() => {
  if (token.value) {
    loadTasks()
  }
})

function saveToken() {
  token.value = tokenInput.value.trim()
  if (!token.value) {
    localStorage.removeItem('evoagent.runtimeToken')
    tasks.value = []
    selectedTask.value = null
    trace.value = []
    return
  }

  localStorage.setItem('evoagent.runtimeToken', token.value)
  loadTasks()
}

async function loadTasks() {
  if (!ensureToken()) {
    return
  }

  loadingTasks.value = true
  errorMessage.value = ''
  try {
    const loadedTasks = await apiGet('/api/runtime/tasks')
    tasks.value = loadedTasks

    if (selectedTask.value) {
      selectedTask.value = loadedTasks.find((task) => task.id === selectedTask.value.id) || null
    }

    if (!selectedTask.value && loadedTasks.length > 0) {
      await selectTask(loadedTasks[0])
    } else if (selectedTask.value) {
      await loadTrace(selectedTask.value)
    }
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loadingTasks.value = false
  }
}

async function selectTask(task) {
  selectedTask.value = task
  await loadTrace(task)
}

async function loadTrace(task) {
  if (!task || !ensureToken()) {
    return
  }

  loadingTrace.value = true
  errorMessage.value = ''
  try {
    trace.value = await apiGet(`/api/runtime/tasks/ref/${taskRef(task)}/trace`)
  } catch (error) {
    trace.value = []
    errorMessage.value = error.message
  } finally {
    loadingTrace.value = false
  }
}

async function retryTask(task) {
  if (!task || !ensureToken()) {
    return
  }

  retrying.value = true
  errorMessage.value = ''
  try {
    selectedTask.value = await apiPost(`/api/runtime/tasks/${task.id}/retry`)
    await loadTasks()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    retrying.value = false
  }
}

async function apiGet(path) {
  const response = await fetch(path, {
    headers: authHeaders(),
  })
  return parseResponse(response)
}

async function apiPost(path) {
  const response = await fetch(path, {
    method: 'POST',
    headers: authHeaders(),
  })
  return parseResponse(response)
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.text()

  if (!response.ok) {
    const message =
      typeof payload === 'string'
        ? payload
        : payload.message || payload.error || `Request failed with status ${response.status}`
    throw new Error(message)
  }

  return payload
}

function authHeaders() {
  return {
    'X-Runtime-Token': token.value,
  }
}

function ensureToken() {
  if (token.value) {
    return true
  }
  errorMessage.value = 'Runtime token is required.'
  return false
}

function taskRef(task) {
  if (!task?.id) {
    return 'N/A'
  }
  return task.id.length <= 8 ? task.id : task.id.substring(0, 8)
}

function statusClass(status) {
  return (status || 'pending').toLowerCase()
}

function formatDate(value) {
  if (!value) {
    return 'N/A'
  }

  return new Intl.DateTimeFormat(undefined, {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value))
}

function githubPrUrl(task) {
  return `https://github.com/${task.owner}/${task.repo}/pull/${task.prNumber}`
}
</script>

<template>
  <div class="page-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">EvoAgent</p>
        <h1>Runtime Console</h1>
        <p class="subtitle">Agent runtime, evaluation harness, and skill evolution workspace.</p>
      </div>

      <div class="token-panel">
        <label for="runtime-token">Runtime Token</label>
        <div class="token-row">
          <input
            id="runtime-token"
            v-model="tokenInput"
            type="password"
            placeholder="X-Runtime-Token"
            @keyup.enter="saveToken"
          >
          <button class="btn primary" type="button" @click="saveToken">Save</button>
        </div>
      </div>
    </header>

    <nav class="tabs" aria-label="Console sections">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        :class="['tab', { active: activeTab === tab.id }]"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </nav>

    <main>
      <section v-if="activeTab === 'tasks'" class="workspace">
        <aside class="task-list-panel">
          <div class="panel-header">
            <div>
              <h2>Tasks</h2>
              <p>{{ taskCountText }}</p>
            </div>
            <button class="btn" type="button" :disabled="loadingTasks" @click="loadTasks">
              {{ loadingTasks ? 'Refreshing' : 'Refresh' }}
            </button>
          </div>

          <div v-if="errorMessage" class="notice error">{{ errorMessage }}</div>
          <div v-if="!token" class="notice">Enter your runtime token to load protected runtime data.</div>
          <div v-if="tasks.length === 0 && !loadingTasks" class="empty-state">No runtime tasks yet.</div>

          <button
            v-for="task in tasks"
            :key="task.id"
            type="button"
            :class="['task-row', { selected: selectedTask && selectedTask.id === task.id }]"
            @click="selectTask(task)"
          >
            <span class="task-main">
              <span class="task-title">{{ task.owner }}/{{ task.repo }} #{{ task.prNumber }}</span>
              <span class="task-meta">{{ taskRef(task) }} · {{ task.eventType || 'manual' }}</span>
            </span>
            <span :class="['status-badge', statusClass(task.status)]">{{ task.status }}</span>
          </button>
        </aside>

        <section class="detail-panel">
          <div v-if="!selectedTask" class="empty-detail">
            <h2>Select a task</h2>
            <p>Choose a runtime task to inspect node trace, retry failures, and connect PR review results to execution history.</p>
          </div>

          <template v-else>
            <div class="detail-header">
              <div>
                <p class="eyebrow">Task Ref {{ taskRef(selectedTask) }}</p>
                <h2>{{ selectedTask.owner }}/{{ selectedTask.repo }} PR #{{ selectedTask.prNumber }}</h2>
                <p class="subtitle">Task ID {{ selectedTask.id }}</p>
              </div>

              <div class="detail-actions">
                <a class="btn" :href="githubPrUrl(selectedTask)" target="_blank" rel="noreferrer">Open PR</a>
                <button class="btn" type="button" :disabled="loadingTrace" @click="loadTrace(selectedTask)">
                  {{ loadingTrace ? 'Loading' : 'Reload Trace' }}
                </button>
                <button
                  v-if="selectedTask.status === 'FAILED'"
                  class="btn danger"
                  type="button"
                  :disabled="retrying"
                  @click="retryTask(selectedTask)"
                >
                  {{ retrying ? 'Retrying' : 'Retry' }}
                </button>
              </div>
            </div>

            <div class="summary-grid">
              <div class="summary-item">
                <span>Status</span>
                <strong :class="['status-text', statusClass(selectedTask.status)]">{{ selectedTask.status }}</strong>
              </div>
              <div class="summary-item">
                <span>Current Node</span>
                <strong>{{ selectedTask.currentNode || 'N/A' }}</strong>
              </div>
              <div class="summary-item">
                <span>Created</span>
                <strong>{{ formatDate(selectedTask.createdAt) }}</strong>
              </div>
              <div class="summary-item">
                <span>Updated</span>
                <strong>{{ formatDate(selectedTask.updatedAt) }}</strong>
              </div>
            </div>

            <div v-if="selectedTask.errorMessage" class="notice error">{{ selectedTask.errorMessage }}</div>

            <section class="trace-panel">
              <div class="panel-header compact">
                <div>
                  <h3>Node Trace</h3>
                  <p>{{ trace.length }} execution record{{ trace.length === 1 ? '' : 's' }}</p>
                </div>
              </div>

              <div v-if="trace.length === 0 && !loadingTrace" class="empty-state">
                No trace records found for this task.
              </div>

              <div v-for="execution in trace" :key="execution.id" class="trace-row">
                <div class="trace-left">
                  <span :class="['trace-dot', statusClass(execution.status)]"></span>
                  <div>
                    <h4>{{ execution.nodeName }}</h4>
                    <p>{{ execution.input || 'No input summary' }}</p>
                  </div>
                </div>

                <div class="trace-right">
                  <span :class="['status-badge', statusClass(execution.status)]">{{ execution.status }}</span>
                  <span>{{ execution.durationMs === null || execution.durationMs === undefined ? '-' : execution.durationMs + 'ms' }}</span>
                  <span>retry {{ execution.retryCount }}</span>
                </div>

                <pre v-if="execution.output" class="trace-output">{{ execution.output }}</pre>
                <pre v-if="execution.errorMessage" class="trace-output error-text">{{ execution.errorMessage }}</pre>
              </div>
            </section>
          </template>
        </section>
      </section>

      <section v-if="activeTab === 'evaluations'" class="placeholder-panel">
        <p class="eyebrow">Week 5</p>
        <h2>Evaluation Harness</h2>
        <p>Reserved for evaluation runs, datasets, baseline score, candidate score, regression cases, and improvement cases.</p>
        <div class="placeholder-grid">
          <span>Evaluation Runs</span>
          <span>Dataset</span>
          <span>Baseline Score</span>
          <span>Candidate Score</span>
          <span>Regression Cases</span>
          <span>Improvement Cases</span>
        </div>
      </section>

      <section v-if="activeTab === 'skills'" class="placeholder-panel">
        <p class="eyebrow">Week 6</p>
        <h2>Skill Evolution</h2>
        <p>Reserved for failure cases, candidate skills, evaluation gates, active skill versions, and rollback controls.</p>
        <div class="placeholder-grid">
          <span>Skill Registry</span>
          <span>Skill Version</span>
          <span>DRAFT</span>
          <span>EVALUATING</span>
          <span>ACTIVE</span>
          <span>REJECTED</span>
        </div>
      </section>
    </main>
  </div>
</template>
