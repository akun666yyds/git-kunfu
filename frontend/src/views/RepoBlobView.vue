<template>
  <div>
    <div class="repo-toolbar">
      <div class="branch-select">
        <select :value="branch" aria-label="选择分支" @change="onBranchChange">
          <option v-for="item in branches" :key="item.name" :value="item.name">{{ item.name }}</option>
          <option v-if="!branches.length" :value="branch">{{ branch }}</option>
        </select>
        <span class="chev"><Icon name="chevronDown" :size="14" /></span>
      </div>
      <nav class="breadcrumb">
        <router-link class="crumb" :to="{ path: basePath, query: { branch } }">{{ repo.name }}</router-link>
        <template v-for="(part, index) in pathParts" :key="index">
          <span class="sep">/</span>
          <span v-if="index === pathParts.length - 1" class="current">{{ part }}</span>
          <router-link v-else class="crumb" :to="{ path: basePath, query: { branch, path: pathParts.slice(0, index + 1).join('/') } }">{{ part }}</router-link>
        </template>
      </nav>
      <div class="clone-box">
        <button class="btn btn-sm" type="button" :disabled="downloading" @click="download">
          <Icon name="download" :size="13" />{{ downloading ? '准备中…' : '下载文件' }}
        </button>
      </div>
    </div>

    <LoadingBlock v-if="loading" />

    <EmptyState
      v-else-if="error"
      icon="file"
      :title="errorTitle"
      description="文件可能已被移动或删除，回到目录里找找看。"
    >
      <router-link class="btn btn-sm" :to="{ path: basePath, query: { branch } }">返回代码</router-link>
    </EmptyState>

    <div v-else-if="blob" class="file-viewer">
      <div class="file-viewer-head">
        <Icon name="file" :size="14" class="file-icon" />
        <span class="mono small strong">{{ blob.path }}</span>
        <span class="tag">{{ formatSize(blob.size) }}</span>
        <span class="badge">{{ branch }}</span>
        <span class="spacer"></span>
        <router-link
          class="btn btn-sm"
          :to="{ name: 'repo-commits', params: { owner: repo.owner, repo: repo.name }, query: { branch } }"
        >
          <Icon name="clock" :size="13" />历史
        </router-link>
      </div>

      <div v-if="blob.binary || blob.truncated" class="binary-state">
        <Icon name="file" :size="26" style="margin: 0 auto 10px" />
        <div v-if="blob.truncated">这个文件太大，无法在页面中直接预览，可以下载后查看。</div>
        <div v-else>这是二进制文件，不适合直接预览，可以下载后查看。</div>
      </div>

      <div v-else class="file-viewer-body">
        <div class="code-table">
          <div class="line-numbers" aria-hidden="true">
            <div v-for="line in lineCount" :key="line">{{ line }}</div>
          </div>
          <pre class="code-pre"><code class="hljs" v-html="highlighted"></code></pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import hljs from 'highlight.js/lib/common'
import { api, apiMessage } from '../api/client'
import { formatSize } from '../utils/format'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const props = defineProps({
  repo: { type: Object, required: true }
})

const route = useRoute()
const router = useRouter()

const blob = ref(null)
const branches = ref([])
const loading = ref(true)
const error = ref('')
const errorTitle = ref('文件不存在')
const downloading = ref(false)

const basePath = computed(() => `/${props.repo.owner}/${props.repo.name}`)
const branch = computed(() => (route.query.branch || props.repo.defaultBranch || 'main').toString())
const path = computed(() => (route.query.path || '').toString())
const pathParts = computed(() => (path.value ? path.value.split('/') : []))
const lineCount = computed(() => (blob.value?.content ? blob.value.content.split('\n').length : 0))

const LANGUAGE_MAP = {
  js: 'javascript', mjs: 'javascript', cjs: 'javascript', jsx: 'javascript',
  ts: 'typescript', tsx: 'typescript', vue: 'xml', html: 'xml', htm: 'xml', svg: 'xml',
  css: 'css', scss: 'scss', less: 'less', json: 'json', yml: 'yaml', yaml: 'yaml',
  java: 'java', kt: 'kotlin', c: 'c', h: 'c', cpp: 'cpp', cc: 'cpp', hpp: 'cpp',
  py: 'python', go: 'go', rs: 'rust', rb: 'ruby', php: 'php', sh: 'bash', bash: 'bash',
  sql: 'sql', xml: 'xml', md: 'markdown', ini: 'ini', toml: 'ini', dockerfile: 'dockerfile'
}

function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const highlighted = computed(() => {
  const content = blob.value?.content || ''
  const name = (blob.value?.name || '').toLowerCase()
  const ext = name.includes('.') ? name.split('.').pop() : name
  const language = LANGUAGE_MAP[ext]
  if (language) {
    try {
      return hljs.highlight(content, { language }).value
    } catch {
      // 语言不支持时回退到纯文本
    }
  }
  return escapeHtml(content)
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/blob`, {
      params: { path: path.value, branch: branch.value }
    })
    blob.value = data
  } catch (ex) {
    blob.value = null
    error.value = apiMessage(ex, '文件加载失败')
    errorTitle.value = ex?.response?.status === 404 ? '文件不存在' : '文件暂时打不开'
  } finally {
    loading.value = false
  }
}

async function loadBranches() {
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/branches`)
    branches.value = data
  } catch {
    branches.value = []
  }
}

function onBranchChange(event) {
  router.replace({
    name: 'repo-blob',
    params: { owner: props.repo.owner, repo: props.repo.name },
    query: { branch: event.target.value, path: path.value }
  })
}

async function download() {
  if (!blob.value) return
  downloading.value = true
  try {
    const response = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/raw`, {
      params: { path: path.value, branch: branch.value },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = blob.value.name || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (ex) {
    notify(apiMessage(ex, '下载失败'), 'error')
  } finally {
    downloading.value = false
  }
}

onMounted(() => {
  loadBranches()
  if (!path.value) {
    router.replace({ path: basePath.value, query: { branch: branch.value } })
    return
  }
  load()
})

watch(
  () => [branch.value, path.value],
  () => {
    if (path.value) load()
  }
)
</script>
