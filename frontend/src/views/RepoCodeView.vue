<template>
  <div>
    <div v-if="repoDetail?.empty" class="empty-repo">
      <h3>快速初始化仓库</h3>
      <p>仓库还是空的。选一种方式把代码推上来，刷新后这里就会显示文件与提交。</p>

      <div class="clone-box" style="margin: 0 0 18px">
        <div class="clone-input" style="max-width: 520px">
          <Icon name="link" :size="13" />
          <span>{{ repo.cloneUrl }}</span>
        </div>
        <button class="btn btn-sm" type="button" @click="copyClone">
          <Icon name="copy" :size="13" /><span class="copy-label">复制</span>
        </button>
      </div>

      <div v-if="isOwner" class="setup-list">
        <section v-for="(guide, index) in setupGuides" :key="guide.key" class="setup-item">
          <div class="setup-head">
            <span class="setup-step">{{ index + 1 }}</span>
            <div>
              <h4>{{ guide.title }}</h4>
              <p class="setup-intro">{{ guide.intro }}</p>
            </div>
          </div>
          <div class="cmd-wrap">
            <pre class="code-block"><code>{{ guide.code }}</code></pre>
            <button class="btn btn-sm cmd-copy" type="button" @click="copyGuide(guide)">
              <Icon name="copy" :size="12" /><span class="copy-label">复制</span>
            </button>
          </div>
          <p class="setup-note">{{ guide.note }}</p>
        </section>
      </div>

      <p v-else class="hint">仓库所有者还没有推送第一个提交，稍后再来看看。</p>

      <button class="btn btn-sm" type="button" style="margin-top: 16px" @click="refresh">
        <Icon name="refresh" :size="13" />已推送，刷新看看
      </button>
    </div>

    <template v-else>
      <div class="repo-toolbar">
        <div class="branch-select">
          <select :value="branch" aria-label="选择分支" @change="onBranchChange">
            <option v-for="item in branches" :key="item.name" :value="item.name">{{ item.name }}</option>
            <option v-if="!branches.length" :value="branch">{{ branch }}</option>
          </select>
          <span class="chev"><Icon name="chevronDown" :size="14" /></span>
        </div>

        <nav v-if="path" class="breadcrumb">
          <router-link class="crumb" :to="{ path: basePath, query: { branch } }">{{ repo.name }}</router-link>
          <template v-for="(part, index) in pathParts" :key="index">
            <span class="sep">/</span>
            <span v-if="index === pathParts.length - 1" class="current">{{ part }}</span>
            <router-link v-else class="crumb" :to="{ path: basePath, query: { branch, path: pathParts.slice(0, index + 1).join('/') } }">{{ part }}</router-link>
          </template>
        </nav>

        <div v-if="latest" class="latest-commit">
          <span class="tag mono">{{ latest.shortSha }}</span>
          <span class="msg">{{ latest.message }}</span>
          <span>· {{ latest.authorName }} {{ timeAgo(latest.authoredAt) }}</span>
        </div>
        <div class="clone-box">
          <div class="clone-input">
            <Icon name="link" :size="13" />
            <span>{{ repo.cloneUrl }}</span>
          </div>
          <button class="btn btn-sm" type="button" @click="copyClone">
            <Icon name="copy" :size="13" /><span class="copy-label">复制</span>
          </button>
        </div>
      </div>

      <LoadingBlock v-if="loading" />

      <template v-else>
        <div v-if="tree.length" class="file-panel">
          <router-link
            v-for="entry in tree"
            :key="entry.path"
            class="file-row"
            :to="entry.type === 'dir'
              ? { path: basePath, query: { branch, path: entry.path } }
              : { name: 'repo-blob', params: { owner: repo.owner, repo: repo.name }, query: { branch, path: entry.path } }"
          >
            <span class="file-main">
              <Icon :name="entry.type === 'dir' ? 'folder' : 'file'" :size="15" :class="['file-icon', { folder: entry.type === 'dir' }]" />
              <span>{{ entry.name }}</span>
            </span>
            <span class="file-size">{{ entry.type === 'dir' ? '' : formatSize(entry.size) }}</span>
          </router-link>
        </div>

        <EmptyState
          v-else
          icon="folder"
          title="这个目录是空的"
          description="换一个分支或目录看看，也可以先在本地提交一些文件。"
        />

        <div v-if="readme && !path" class="readme-card" style="margin-top: 16px">
          <div class="readme-head">
            <Icon name="book" :size="13" />
            <span>{{ readme.name }}</span>
          </div>
          <div class="readme-body">
            <MarkdownView v-if="readme.markdown" :content="readme.content" />
            <pre v-else class="code-block" style="border: 0; background: transparent; padding: 0">{{ readme.content }}</pre>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { timeAgo, formatSize } from '../utils/format'
import { copyText } from '../utils/clipboard'
import { notify } from '../utils/toast'
import { auth } from '../stores/auth'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'
import MarkdownView from '../components/MarkdownView.vue'

const props = defineProps({
  repo: { type: Object, required: true },
  repoDetail: { type: Object, default: null },
  refreshRepo: { type: Function, default: null }
})

const route = useRoute()
const router = useRouter()

const branches = ref([])
const tree = ref([])
const readme = ref(null)
const latest = ref(null)
const loading = ref(true)
const refreshing = ref(false)

const basePath = computed(() => `/${props.repo.owner}/${props.repo.name}`)
const branch = computed(() => (route.query.branch || props.repo.defaultBranch || 'main').toString())
const path = computed(() => (route.query.path || '').toString().replace(/^\/+|\/+$/g, ''))
const pathParts = computed(() => (path.value ? path.value.split('/') : []))
const isOwner = computed(() => Boolean(auth.user && auth.user.username === props.repo.owner))

const setupGuides = computed(() => {
  const url = props.repo.cloneUrl
  const main = props.repo.defaultBranch || 'main'
  return [
    {
      key: 'new',
      title: '在本地新建仓库',
      intro: '还没有代码目录，从零开始创建。',
      code: [
        'git init',
        'git add .',
        'git commit -m "第一次提交"',
        `git branch -M ${main}`,
        `git remote add origin ${url}`,
        `git push -u origin ${main}`
      ].join('\n'),
      note: `推送时输入功夫吉特的用户名和密码，默认分支为 ${main}。`
    },
    {
      key: 'existing',
      title: '推送已有的本地仓库',
      intro: '代码已经在本地用 Git 管理，只差一个远程地址。',
      code: [
        'cd 你的项目目录',
        `git remote add origin ${url}`,
        `git branch -M ${main}`,
        `git push -u origin ${main}`
      ].join('\n'),
      note: `如果项目里已经有 origin，把第二行换成 git remote set-url origin ${url}。`
    },
    {
      key: 'mirror',
      title: '从其他平台迁移仓库',
      intro: '把 GitHub、Gitee 等平台上的仓库连同分支、标签一起搬过来。',
      code: [
        'git clone --mirror https://github.com/用户名/仓库.git',
        'cd 仓库.git',
        `git remote set-url origin ${url}`,
        'git push --mirror'
      ].join('\n'),
      note: '--mirror 会同步全部分支、标签与引用。源仓库需要登录时，克隆阶段按提示输入原平台凭据；迁移完成后本地这份裸仓库可以直接删除。'
    }
  ]
})

async function loadBranches() {
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/branches`)
    branches.value = data
  } catch {
    branches.value = []
  }
}

async function loadTree() {
  loading.value = true
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/tree`, {
      params: { branch: branch.value, path: path.value || undefined }
    })
    tree.value = data
  } catch (error) {
    tree.value = []
    if (error?.response?.status !== 404) {
      notify(apiMessage(error, '目录加载失败'), 'error')
    }
  } finally {
    loading.value = false
  }
}

async function loadReadme() {
  readme.value = null
  if (path.value) return
  try {
    const response = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/readme`, {
      params: { branch: branch.value }
    })
    readme.value = response.status === 200 ? response.data : null
  } catch {
    readme.value = null
  }
}

async function loadLatest() {
  latest.value = null
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/commits`, {
      params: { branch: branch.value, page: 1, size: 1 }
    })
    latest.value = data.items?.[0] || null
  } catch {
    latest.value = null
  }
}

function onBranchChange(event) {
  const value = event.target.value
  router.push({ path: basePath.value, query: { branch: value, path: path.value || undefined } })
}

async function copyClone() {
  const ok = await copyText(props.repo.cloneUrl)
  notify(ok ? '克隆地址已复制' : '复制失败，请手动选择地址', ok ? 'success' : 'error')
}

async function copyGuide(guide) {
  const ok = await copyText(guide.code)
  notify(ok ? '命令已复制' : '复制失败，请手动选择命令', ok ? 'success' : 'error')
}

async function refresh() {
  refreshing.value = true
  try {
    if (props.refreshRepo) await props.refreshRepo()
    await Promise.all([loadBranches(), loadTree(), loadReadme(), loadLatest()])
  } finally {
    refreshing.value = false
  }
}

async function loadAll() {
  await Promise.all([loadTree(), loadReadme(), loadLatest()])
}

onMounted(() => {
  loadBranches()
  if (!props.repoDetail?.empty) loadAll()
})

watch(
  () => [branch.value, path.value],
  () => {
    if (!props.repoDetail?.empty) loadAll()
  }
)
</script>
