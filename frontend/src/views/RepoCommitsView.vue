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
      <span class="small muted" v-if="!loading">当前分支的提交记录</span>
      <div class="clone-box">
        <router-link class="btn btn-sm" :to="{ path: basePath, query: { branch } }">
          <Icon name="code" :size="13" />浏览代码
        </router-link>
      </div>
    </div>

    <LoadingBlock v-if="loading" />

    <template v-else>
      <div v-if="commits.length" class="commit-list">
        <div v-for="item in commits" :key="item.sha" class="commit-item">
          <Icon name="commit" :size="17" class="file-icon" />
          <div class="commit-main">
            <router-link
              class="commit-title"
              :to="{ name: 'repo-commit', params: { owner: repo.owner, repo: repo.name, sha: item.sha } }"
            >
              {{ item.message }}
            </router-link>
            <div class="commit-sub">
              <span>{{ item.authorName }}</span>
              <span>·</span>
              <span>{{ timeAgo(item.authoredAt) }}</span>
              <span v-if="item.parents && item.parents.length > 1" class="tag">合并</span>
            </div>
          </div>
          <router-link
            class="commit-sha-btn"
            :to="{ name: 'repo-commit', params: { owner: repo.owner, repo: repo.name, sha: item.sha } }"
          >
            {{ item.shortSha }}
          </router-link>
        </div>
      </div>
      <EmptyState v-else icon="commit" title="还没有提交" description="这个分支上暂时没有提交记录。" />

      <div v-if="commits.length" class="pager">
        <button class="btn btn-sm" type="button" :disabled="page <= 1 || loading" @click="goPage(page - 1)">
          <Icon name="arrowLeft" :size="14" />上一页
        </button>
        <span class="small muted">第 {{ page }} 页</span>
        <button class="btn btn-sm" type="button" :disabled="!hasMore || loading" @click="goPage(page + 1)">
          下一页<Icon name="arrowRight" :size="14" />
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { timeAgo } from '../utils/format'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const props = defineProps({
  repo: { type: Object, required: true }
})

const route = useRoute()
const router = useRouter()
const branches = ref([])
const commits = ref([])
const page = ref(1)
const hasMore = ref(false)
const loading = ref(true)
const size = 20

const basePath = computed(() => `/${props.repo.owner}/${props.repo.name}`)
const branch = computed(() => (route.query.branch || props.repo.defaultBranch || 'main').toString())

async function loadBranches() {
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/branches`)
    branches.value = data
  } catch {
    branches.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/commits`, {
      params: { branch: branch.value, page: page.value, size }
    })
    commits.value = data.items
    hasMore.value = data.hasMore
  } catch (error) {
    commits.value = []
    hasMore.value = false
    notify(apiMessage(error, '提交记录加载失败'), 'error')
  } finally {
    loading.value = false
  }
}

function onBranchChange(event) {
  page.value = 1
  router.push({ path: `${basePath.value}/commits`, query: { branch: event.target.value } })
}

function goPage(target) {
  page.value = Math.max(1, target)
  load()
}

watch(branch, () => {
  page.value = 1
  load()
})

onMounted(() => {
  loadBranches()
  load()
})
</script>
