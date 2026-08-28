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
const skills = ref([])
const loadingSkills = ref(false)
const runningEvolution = ref(false)
const includeUnexpectedFindings = ref(false)
const skillErrorMessage = ref('')
const pipelineRun = ref(null)
const pipelineRuns = ref([])
const loadingPipelineRuns = ref(false)
const evaluationRuns = ref([])
const selectedEvaluationRun = ref(null)
const loadingEvaluationRuns = ref(false)
const runningEvaluation = ref(false)
const evaluationErrorMessage = ref('')

const taskCountText = computed(() => {
  return `${tasks.value.length} runtime task${tasks.value.length === 1 ? '' : 's'}`
})

const skillCounts = computed(() => {
  return skills.value.reduce(
    (counts, skill) => {
      const status = skill.status || 'UNKNOWN'
      counts[status] = (counts[status] || 0) + 1
      counts.total += 1
      return counts
    },
    { total: 0 },
  )
})

onMounted(() => {
  if (token.value) {
    loadTasks()
  }
  loadSkills()
})

function switchTab(tabId) {
  activeTab.value = tabId
  if (tabId === 'evaluations') {
    loadEvaluationRuns()
  }
  if (tabId === 'skills') {
    loadSkills()
    loadPipelineRuns()
  }
}

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

async function loadSkills() {
  loadingSkills.value = true
  skillErrorMessage.value = ''
  try {
    skills.value = await apiGet('/api/skills')
  } catch (error) {
    skills.value = []
    skillErrorMessage.value = error.message
  } finally {
    loadingSkills.value = false
  }
}

async function loadPipelineRuns() {
  loadingPipelineRuns.value = true
  skillErrorMessage.value = ''
  try {
    pipelineRuns.value = await apiGet('/api/skills/evolution/runs')
    if (!pipelineRun.value && pipelineRuns.value.length > 0) {
      pipelineRun.value = pipelineRuns.value[0]
    }
  } catch (error) {
    pipelineRuns.value = []
    skillErrorMessage.value = error.message
  } finally {
    loadingPipelineRuns.value = false
  }
}

async function runSkillEvolution() {
  runningEvolution.value = true
  skillErrorMessage.value = ''
  try {
    pipelineRun.value = await apiPost(
      `/api/skills/evolution/run?includeUnexpectedFindings=${includeUnexpectedFindings.value}`,
    )
    await loadSkills()
    await loadPipelineRuns()
  } catch (error) {
    skillErrorMessage.value = error.message
  } finally {
    runningEvolution.value = false
  }
}

function selectPipelineRun(run) {
  pipelineRun.value = run
}

async function loadEvaluationRuns() {
  loadingEvaluationRuns.value = true
  evaluationErrorMessage.value = ''
  try {
    evaluationRuns.value = await apiGet('/api/evaluation/runs')
    if (selectedEvaluationRun.value) {
      selectedEvaluationRun.value =
        evaluationRuns.value.find((run) => run.id === selectedEvaluationRun.value.id) || null
    }
    if (!selectedEvaluationRun.value && evaluationRuns.value.length > 0) {
      selectedEvaluationRun.value = evaluationRuns.value[0]
    }
  } catch (error) {
    evaluationRuns.value = []
    evaluationErrorMessage.value = error.message
  } finally {
    loadingEvaluationRuns.value = false
  }
}

async function runEvaluation() {
  runningEvaluation.value = true
  evaluationErrorMessage.value = ''
  try {
    selectedEvaluationRun.value = await apiPost('/api/evaluation/runs')
    await loadEvaluationRuns()
  } catch (error) {
    evaluationErrorMessage.value = error.message
  } finally {
    runningEvaluation.value = false
  }
}

function selectEvaluationRun(run) {
  selectedEvaluationRun.value = run
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

function formatMetric(value) {
  if (value === null || value === undefined) {
    return 'N/A'
  }
  return Number(value).toFixed(3)
}

function metricDelta(beforeValue, afterValue) {
  if (beforeValue === null || beforeValue === undefined || afterValue === null || afterValue === undefined) {
    return 'N/A'
  }

  const delta = afterValue - beforeValue
  const sign = delta > 0 ? '+' : ''
  return `${sign}${delta.toFixed(3)}`
}

function metricDeltaClass(beforeValue, afterValue) {
  if (beforeValue === null || beforeValue === undefined || afterValue === null || afterValue === undefined) {
    return 'neutral'
  }

  const delta = afterValue - beforeValue
  if (delta > 0) {
    return 'positive'
  }
  if (delta < 0) {
    return 'negative'
  }
  return 'neutral'
}

function truncateId(value) {
  if (!value) {
    return 'N/A'
  }
  return value.length <= 8 ? value : value.substring(0, 8)
}

function findingTitle(finding) {
  return finding?.title || finding?.id || 'Untitled finding'
}

function findingMeta(finding) {
  if (!finding) {
    return 'N/A'
  }
  return `${finding.level || 'N/A'} · ${finding.type || 'N/A'} · ${finding.file || 'N/A'}`
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
        @click="switchTab(tab.id)"
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

      <section v-if="activeTab === 'evaluations'" class="workspace">
        <aside class="task-list-panel">
          <div class="panel-header">
            <div>
              <h2>Evaluation Runs</h2>
              <p>{{ evaluationRuns.length }} saved run{{ evaluationRuns.length === 1 ? '' : 's' }}</p>
            </div>
            <div class="detail-actions">
              <button class="btn" type="button" :disabled="loadingEvaluationRuns" @click="loadEvaluationRuns">
                {{ loadingEvaluationRuns ? 'Loading' : 'Refresh' }}
              </button>
              <button class="btn primary" type="button" :disabled="runningEvaluation" @click="runEvaluation">
                {{ runningEvaluation ? 'Running' : 'Run Evaluation' }}
              </button>
            </div>
          </div>

          <div v-if="evaluationErrorMessage" class="notice error">{{ evaluationErrorMessage }}</div>
          <div v-if="runningEvaluation" class="notice">
            Evaluation is running. It may take a few minutes because every case calls the review agent.
          </div>
          <div v-if="evaluationRuns.length === 0 && !loadingEvaluationRuns" class="empty-state">
            No evaluation runs yet.
          </div>

          <button
            v-for="run in evaluationRuns"
            :key="run.id"
            type="button"
            :class="['task-row', { selected: selectedEvaluationRun && selectedEvaluationRun.id === run.id }]"
            @click="selectEvaluationRun(run)"
          >
            <span class="task-main">
              <span class="task-title">{{ truncateId(run.id) }} · {{ run.datasetName }}</span>
              <span class="task-meta">
                F1 {{ formatMetric(run.metrics?.f1) }} · P {{ formatMetric(run.metrics?.precision) }} · R {{ formatMetric(run.metrics?.recall) }}
              </span>
            </span>
            <span :class="['status-badge', statusClass(run.status)]">{{ run.status }}</span>
          </button>
        </aside>

        <section class="detail-panel">
          <div v-if="!selectedEvaluationRun" class="empty-detail">
            <h2>Select an evaluation run</h2>
            <p>Choose a run to inspect score, passed cases, missed findings, unexpected findings, and execution errors.</p>
          </div>

          <template v-else>
            <div class="detail-header">
              <div>
                <p class="eyebrow">Run {{ truncateId(selectedEvaluationRun.id) }}</p>
                <h2>{{ selectedEvaluationRun.datasetName }}</h2>
                <p class="subtitle">{{ selectedEvaluationRun.agentName }} · {{ formatDate(selectedEvaluationRun.finishedAt) }}</p>
              </div>

              <span :class="['status-badge', statusClass(selectedEvaluationRun.status)]">
                {{ selectedEvaluationRun.status }}
              </span>
            </div>

            <div class="summary-grid">
              <div class="summary-item">
                <span>Total Cases</span>
                <strong>{{ selectedEvaluationRun.metrics?.totalCases ?? 0 }}</strong>
              </div>
              <div class="summary-item">
                <span>Passed</span>
                <strong>{{ selectedEvaluationRun.metrics?.passedCases ?? 0 }}</strong>
              </div>
              <div class="summary-item">
                <span>Failed</span>
                <strong>{{ selectedEvaluationRun.metrics?.failedCases ?? 0 }}</strong>
              </div>
              <div class="summary-item">
                <span>Errors</span>
                <strong>{{ selectedEvaluationRun.metrics?.errorCases ?? 0 }}</strong>
              </div>
            </div>

            <section class="metrics-panel">
              <div class="panel-header compact">
                <div>
                  <h3>Metrics</h3>
                  <p>Expected {{ selectedEvaluationRun.metrics?.expectedFindingCount ?? 0 }} · Actual {{ selectedEvaluationRun.metrics?.actualFindingCount ?? 0 }} · Matched {{ selectedEvaluationRun.metrics?.matchedFindingCount ?? 0 }}</p>
                </div>
              </div>

              <div class="metric-grid">
                <div class="metric-card">
                  <span>F1</span>
                  <strong>{{ formatMetric(selectedEvaluationRun.metrics?.f1) }}</strong>
                </div>
                <div class="metric-card">
                  <span>Precision</span>
                  <strong>{{ formatMetric(selectedEvaluationRun.metrics?.precision) }}</strong>
                </div>
                <div class="metric-card">
                  <span>Recall</span>
                  <strong>{{ formatMetric(selectedEvaluationRun.metrics?.recall) }}</strong>
                </div>
                <div class="metric-card">
                  <span>High Risk Recall</span>
                  <strong>{{ formatMetric(selectedEvaluationRun.metrics?.highRiskRecall) }}</strong>
                </div>
              </div>
            </section>

            <section class="trace-panel">
              <div class="panel-header compact">
                <div>
                  <h3>Case Results</h3>
                  <p>{{ selectedEvaluationRun.caseResults?.length || 0 }} case result{{ selectedEvaluationRun.caseResults?.length === 1 ? '' : 's' }}</p>
                </div>
              </div>

              <div
                v-for="caseResult in selectedEvaluationRun.caseResults"
                :key="caseResult.caseId"
                class="case-row"
              >
                <div class="case-header">
                  <div>
                    <h4>{{ caseResult.caseId }}</h4>
                    <p>{{ caseResult.title }}</p>
                  </div>
                  <span :class="['status-badge', statusClass(caseResult.status)]">{{ caseResult.status }}</span>
                </div>

                <div class="case-counts">
                  <span>Expected {{ caseResult.expectedFindings?.length || 0 }}</span>
                  <span>Actual {{ caseResult.actualFindings?.length || 0 }}</span>
                  <span>Matched {{ caseResult.matchedFindings?.length || 0 }}</span>
                  <span>Missed {{ caseResult.missedFindings?.length || 0 }}</span>
                  <span>Unexpected {{ caseResult.unexpectedFindings?.length || 0 }}</span>
                </div>

                <div v-if="caseResult.errorMessage" class="notice error">{{ caseResult.errorMessage }}</div>

                <div class="finding-columns">
                  <div class="finding-column">
                    <h4>Missed Findings</h4>
                    <p v-if="!caseResult.missedFindings?.length" class="muted-text">None</p>
                    <div v-for="finding in caseResult.missedFindings" :key="finding.id || finding.title" class="finding-item">
                      <strong>{{ findingTitle(finding) }}</strong>
                      <span>{{ findingMeta(finding) }}</span>
                    </div>
                  </div>

                  <div class="finding-column">
                    <h4>Unexpected Findings</h4>
                    <p v-if="!caseResult.unexpectedFindings?.length" class="muted-text">None</p>
                    <div v-for="finding in caseResult.unexpectedFindings" :key="finding.title + finding.file" class="finding-item">
                      <strong>{{ findingTitle(finding) }}</strong>
                      <span>{{ findingMeta(finding) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </template>
        </section>
      </section>

      <section v-if="activeTab === 'skills'" class="skills-workspace">
        <aside class="task-list-panel">
          <div class="panel-header">
            <div>
              <h2>Skill Registry</h2>
              <p>{{ skillCounts.total }} skill{{ skillCounts.total === 1 ? '' : 's' }}</p>
            </div>
            <button class="btn" type="button" :disabled="loadingSkills" @click="loadSkills">
              {{ loadingSkills ? 'Refreshing' : 'Refresh' }}
            </button>
          </div>

          <div v-if="skillErrorMessage" class="notice error">{{ skillErrorMessage }}</div>

          <div class="skill-counts">
            <div class="summary-item">
              <span>Active</span>
              <strong>{{ skillCounts.ACTIVE || 0 }}</strong>
            </div>
            <div class="summary-item">
              <span>Candidate</span>
              <strong>{{ skillCounts.CANDIDATE || 0 }}</strong>
            </div>
            <div class="summary-item">
              <span>Rejected</span>
              <strong>{{ skillCounts.REJECTED || 0 }}</strong>
            </div>
          </div>

          <div v-if="skills.length === 0 && !loadingSkills" class="empty-state">No skills generated yet.</div>

          <div v-for="skill in skills" :key="skill.id" class="skill-row">
            <div class="skill-main">
              <h4>{{ skill.name }}</h4>
              <p>{{ skill.description || 'No description' }}</p>
              <span class="task-meta">{{ truncateId(skill.id) }} · v{{ skill.version }} · {{ skill.category || 'GENERAL' }}</span>
            </div>
            <span :class="['status-badge', statusClass(skill.status)]">{{ skill.status }}</span>
          </div>

          <section class="history-panel">
            <div class="panel-header">
              <div>
                <h3>Evolution Runs</h3>
                <p>{{ pipelineRuns.length }} saved run{{ pipelineRuns.length === 1 ? '' : 's' }}</p>
              </div>
              <button class="btn" type="button" :disabled="loadingPipelineRuns" @click="loadPipelineRuns">
                {{ loadingPipelineRuns ? 'Loading' : 'Reload' }}
              </button>
            </div>

            <div v-if="pipelineRuns.length === 0 && !loadingPipelineRuns" class="empty-state">
              No pipeline runs yet.
            </div>

            <button
              v-for="run in pipelineRuns"
              :key="run.id"
              type="button"
              :class="['history-row', { selected: pipelineRun && pipelineRun.id === run.id }]"
              @click="selectPipelineRun(run)"
            >
              <span class="task-main">
                <span class="task-title">{{ truncateId(run.id) }} · {{ run.status }}</span>
                <span class="task-meta">
                  F1 {{ formatMetric(run.beforeMetrics?.f1) }} → {{ formatMetric(run.afterMetrics?.f1) }}
                </span>
              </span>
              <span>{{ formatDate(run.finishedAt) }}</span>
            </button>
          </section>
        </aside>

        <section class="detail-panel">
          <div class="detail-header">
            <div>
              <p class="eyebrow">Skill Evolution</p>
              <h2>Automated Evolution Pipeline</h2>
              <p class="subtitle">Run evaluation, analyze failures, generate candidate skills, gate them, and compare final metrics.</p>
            </div>

            <div class="detail-actions">
              <label class="switch-row">
                <input v-model="includeUnexpectedFindings" type="checkbox">
                <span>Include unexpected findings</span>
              </label>
              <button class="btn primary" type="button" :disabled="runningEvolution" @click="runSkillEvolution">
                {{ runningEvolution ? 'Running' : 'Run Skill Evolution' }}
              </button>
            </div>
          </div>

          <div v-if="runningEvolution" class="notice">
            Pipeline is running. Full evaluation can take several minutes because it calls DeepSeek many times.
          </div>

          <div v-if="!pipelineRun && !runningEvolution" class="empty-state">
            Run the pipeline to generate a before/after report for Skill Evolution.
          </div>

          <template v-if="pipelineRun">
            <div class="summary-grid">
              <div class="summary-item">
                <span>Status</span>
                <strong :class="['status-text', statusClass(pipelineRun.status)]">{{ pipelineRun.status }}</strong>
              </div>
              <div class="summary-item">
                <span>Generated</span>
                <strong>{{ pipelineRun.generatedSkillCount }}</strong>
              </div>
              <div class="summary-item">
                <span>Activated</span>
                <strong>{{ pipelineRun.activatedSkillCount }}</strong>
              </div>
              <div class="summary-item">
                <span>Rejected</span>
                <strong>{{ pipelineRun.rejectedSkillCount }}</strong>
              </div>
            </div>

            <section class="metrics-panel">
              <div class="panel-header compact">
                <div>
                  <h3>Metrics</h3>
                  <p>Baseline {{ truncateId(pipelineRun.baselineRunId) }} · Final {{ truncateId(pipelineRun.finalRunId) }}</p>
                </div>
              </div>

              <div class="metric-grid">
                <div class="metric-card">
                  <span>F1</span>
                  <strong>{{ formatMetric(pipelineRun.beforeMetrics?.f1) }} → {{ formatMetric(pipelineRun.afterMetrics?.f1) }}</strong>
                  <small :class="metricDeltaClass(pipelineRun.beforeMetrics?.f1, pipelineRun.afterMetrics?.f1)">
                    {{ metricDelta(pipelineRun.beforeMetrics?.f1, pipelineRun.afterMetrics?.f1) }}
                  </small>
                </div>
                <div class="metric-card">
                  <span>Precision</span>
                  <strong>{{ formatMetric(pipelineRun.beforeMetrics?.precision) }} → {{ formatMetric(pipelineRun.afterMetrics?.precision) }}</strong>
                  <small :class="metricDeltaClass(pipelineRun.beforeMetrics?.precision, pipelineRun.afterMetrics?.precision)">
                    {{ metricDelta(pipelineRun.beforeMetrics?.precision, pipelineRun.afterMetrics?.precision) }}
                  </small>
                </div>
                <div class="metric-card">
                  <span>Recall</span>
                  <strong>{{ formatMetric(pipelineRun.beforeMetrics?.recall) }} → {{ formatMetric(pipelineRun.afterMetrics?.recall) }}</strong>
                  <small :class="metricDeltaClass(pipelineRun.beforeMetrics?.recall, pipelineRun.afterMetrics?.recall)">
                    {{ metricDelta(pipelineRun.beforeMetrics?.recall, pipelineRun.afterMetrics?.recall) }}
                  </small>
                </div>
                <div class="metric-card">
                  <span>High Risk Recall</span>
                  <strong>{{ formatMetric(pipelineRun.beforeMetrics?.highRiskRecall) }} → {{ formatMetric(pipelineRun.afterMetrics?.highRiskRecall) }}</strong>
                  <small :class="metricDeltaClass(pipelineRun.beforeMetrics?.highRiskRecall, pipelineRun.afterMetrics?.highRiskRecall)">
                    {{ metricDelta(pipelineRun.beforeMetrics?.highRiskRecall, pipelineRun.afterMetrics?.highRiskRecall) }}
                  </small>
                </div>
              </div>
            </section>

            <section class="trace-panel">
              <div class="panel-header compact">
                <div>
                  <h3>Activation Decisions</h3>
                  <p>{{ pipelineRun.activationDecisions?.length || 0 }} candidate decision{{ pipelineRun.activationDecisions?.length === 1 ? '' : 's' }}</p>
                </div>
              </div>

              <div
                v-for="decision in pipelineRun.activationDecisions"
                :key="decision.skillId"
                class="decision-row"
              >
                <div>
                  <h4>{{ decision.skillName }}</h4>
                  <p>{{ decision.reason }}</p>
                  <span class="task-meta">
                    baseline {{ truncateId(decision.baselineRunId) }} · candidate {{ truncateId(decision.candidateRunId) }}
                  </span>
                </div>
                <span :class="['status-badge', decision.activated ? 'succeeded' : 'failed']">
                  {{ decision.activated ? 'ACTIVATED' : 'REJECTED' }}
                </span>
              </div>
            </section>
          </template>
        </section>
      </section>
    </main>
  </div>
</template>
