<template>
  <div class="container page">
    <LoadingBlock v-if="loading" />

    <EmptyState
      v-else-if="error"
      icon="lock"
      :title="errorTitle"
      :description="errorDescription"
    >
      <router-link to="/dashboard" class="btn btn-sm">回到我的仓库</router-link>
      <router-link to="/explore" class="btn btn-sm" style="margin-left: 8px">探索公开仓库</router-link>
    </EmptyState>

    <div v-else class="repo-shell">
      <div class="repo-head">
        <div class="repo-title-row">
          <router-link :to="`/u/${repo.owner}`" class="repo-owner">{{ repo.owner }}</router-link>
          <span class="faint" style="font-size: 20px">/</span>
          <span class="repo-name">{{ repo.name }}</span>
          <span class="badge" :class="repo.visibility === 'PRIVATE' ? 'badge-private' : 'badge-public'">
            <Icon :name="repo.visibility === 'PRIVATE' ? 'lock' : 'globe'" :size="11" />
            {{ repo.visibility === 'PRIVATE' ? '私有' : '公开' }}
          </span>
          <span v-if="isOwner" class="spacer"></span>
          <router-link v-if="isOwner" :to="`/${repo.owner}/${repo.name}/settings`" class="btn btn-sm">
            <Icon name="settings" :size="13" />仓库设置
          </router-link>
        </div>
        <p class="repo-desc">{{ repo.description || '这个仓库还没有写简介。' }}</p>
        <div class="repo-meta-row">
          <span><Icon name="commit" :size="12" />{{ detail.commitCount }} 次提交</span>
          <span><Icon name="branch" :size="12" />{{ detail.branchCount }} 个分支</span>
          <span><Icon name="clock" :size="12" />{{ timeAgo(repo.pushedAt || repo.updatedAt) }}更新</span>
          <span><Icon name="issue" :size="12" />{{ repo.openIssues }} 个待办议题</span>
        </div>
      </div>

      <div class="repo-tabs">
        <router-link :to="base" custom v-slot="{ href, navigate }">
          <a :href="href" class="repo-tab" :class="{ 'is-active': tab === 'code' }" @click.prevent="navigate">
            <Icon name="code" :size="14" />代码
          </a>
        </router-link>
        <router-link :to="`${base}/issues`" custom v-slot="{ href, navigate }">
          <a :href="href" class="repo-tab" :class="{ 'is-active': tab === 'issues' }" @click.prevent="navigate">
            <Icon name="issue" :size="14" />议题
            <span v-if="repo.openIssues" class="tab-count">{{ repo.openIssues }}</span>
          </a>
        </router-link>
        <router-link :to="`${base}/pulls`" custom v-slot="{ href, navigate }">
          <a :href="href" class="repo-tab" :class="{ 'is-active': tab === 'pulls' }" @click.prevent="navigate">
            <Icon name="pull" :size="14" />合并请求
            <span v-if="repo.openPulls" class="tab-count">{{ repo.openPulls }}</span>
          </a>
        </router-link>
        <router-link :to="`${base}/commits`" custom v-slot="{ href, navigate }">
          <a :href="href" class="repo-tab" :class="{ 'is-active': tab === 'commits' }" @click.prevent="navigate">
            <Icon name="commit" :size="14" />提交
          </a>
        </router-link>
        <router-link :to="`${base}/branches`" custom v-slot="{ href, navigate }">
          <a :href="href" class="repo-tab" :class="{ 'is-active': tab === 'branches' }" @click.prevent="navigate">
            <Icon name="branch" :size="14" />分支
          </a>
        </router-link>
        <router-link v-if="isOwner" :to="`${base}/settings`" custom v-slot="{ href, navigate }">
          <a :href="href" class="repo-tab" :class="{ 'is-active': tab === 'settings' }" @click.prevent="navigate">
            <Icon name="settings" :size="14" />设置
          </a>
        </router-link>
      </div>

      <div class="repo-body">
        <router-view v-slot="{ Component }">
          <component :is="Component" :repo="repo" :repo-detail="detail" :refresh-repo="loadRepo" />
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { auth } from '../stores/auth'
import { timeAgo } from '../utils/format'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const route = useRoute()
const repo = ref(null)
const detail = ref(null)
const loading = ref(true)
const error = ref('')
const errorTitle = ref('仓库不存在')
const errorDescription = ref('地址可能不正确，或者这个仓库属于私有项目。')

const base = computed(() => (repo.value ? `/${repo.value.owner}/${repo.value.name}` : ''))
const isOwner = computed(() => Boolean(auth.user && repo.value && auth.user.username === repo.value.owner))
const tab = computed(() => {
  const name = route.name || ''
  if (name.startsWith('repo-issue')) return 'issues'
  if (name.startsWith('repo-pull')) return 'pulls'
  if (name === 'repo-commits' || name === 'repo-commit') return 'commits'
  if (name === 'repo-branches') return 'branches'
  if (name === 'repo-settings') return 'settings'
  return 'code'
})

async function loadRepo() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get(`/repos/${route.params.owner}/${route.params.repo}`)
    detail.value = data
    repo.value = data.repo
  } catch (ex) {
    error.value = apiMessage(ex, '仓库加载失败')
    const status = ex?.response?.status
    errorTitle.value = status === 404 ? '仓库不存在或无权访问' : '仓库暂时打不开'
    errorDescription.value = status === 404
      ? '地址可能不正确，也可能这是一个私有仓库。'
      : '服务可能正在重启，稍后再刷新试试。'
  } finally {
    loading.value = false
  }
}

watch(
  () => [route.params.owner, route.params.repo],
  () => {
    repo.value = null
    detail.value = null
    loadRepo()
  },
  { immediate: true }
)
</script>
