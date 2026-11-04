<template>
  <div>
    <div class="list-toolbar">
      <div class="state-tabs">
        <button
          v-for="option in filters"
          :key="option.value"
          class="state-tab"
          :class="{ active: filter === option.value }"
          type="button"
          @click="setFilter(option.value)"
        >
          {{ option.label }}
          <span class="tab-count">{{ countOf(option.value) }}</span>
        </button>
      </div>
      <span class="spacer"></span>
      <router-link
        v-if="auth.user"
        class="btn btn-primary btn-sm"
        :to="`/${repo.owner}/${repo.name}/pulls/new`"
      >
        <Icon name="plus" :size="14" />发起合并
      </router-link>
      <router-link v-else class="btn btn-sm" :to="{ name: 'login', query: { redirect: route.fullPath } }">
        登录后发起合并
      </router-link>
    </div>

    <LoadingBlock v-if="loading" />

    <template v-else>
      <div v-if="pulls.length" class="item-list">
        <router-link
          v-for="item in pulls"
          :key="item.id"
          class="item-row"
          :to="`/${repo.owner}/${repo.name}/pulls/${item.number}`"
        >
          <span class="item-state-icon" :class="item.state.toLowerCase()">
            <Icon :name="item.state === 'MERGED' ? 'check' : 'pull'" :size="15" />
          </span>
          <div class="item-main">
            <span class="item-title">
              {{ item.title }}
              <span class="badge" :class="stateClass(item.state)">{{ stateLabel(item.state) }}</span>
            </span>
            <div class="item-meta">
              <span>#{{ item.number }}</span>
              <span class="branch-arrow">{{ item.baseBranch }} ← {{ item.headBranch }}</span>
              <span>由 {{ item.author.displayName }} 发起</span>
              <span>{{ timeAgo(item.createdAt) }}</span>
            </div>
          </div>
          <div class="item-side">
            <Icon name="message" :size="13" v-if="item.body" />{{ item.body ? '有说明' : '' }}
          </div>
        </router-link>
      </div>
      <EmptyState
        v-else
        icon="pull"
        title="还没有合并请求"
        description="把分支上的改动通过合并请求合入目标分支，留下完整记录。"
      >
        <router-link
          v-if="auth.user"
          class="btn btn-primary btn-sm"
          :to="`/${repo.owner}/${repo.name}/pulls/new`"
        >发起合并</router-link>
      </EmptyState>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { auth } from '../stores/auth'
import { timeAgo } from '../utils/format'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const props = defineProps({
  repo: { type: Object, required: true }
})

const route = useRoute()
const pulls = ref([])
const openCount = ref(0)
const closedCount = ref(0)
const mergedCount = ref(0)
const loading = ref(true)
const filter = ref('open')

const filters = [
  { value: 'open', label: '待合并' },
  { value: 'closed', label: '已结束' },
  { value: 'all', label: '全部' }
]

function countOf(value) {
  if (value === 'open') return openCount.value
  if (value === 'closed') return closedCount.value
  return openCount.value + closedCount.value
}

function stateLabel(state) {
  if (state === 'MERGED') return '已合并'
  if (state === 'CLOSED') return '已关闭'
  return '待合并'
}

function stateClass(state) {
  if (state === 'MERGED') return 'badge-merged'
  if (state === 'CLOSED') return 'badge-closed'
  return 'badge-open'
}

async function load() {
  loading.value = true
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/pulls`, {
      params: { state: filter.value }
    })
    pulls.value = data.items
    openCount.value = data.openCount
    closedCount.value = data.closedCount
    mergedCount.value = data.mergedCount
  } catch (error) {
    pulls.value = []
    notify(apiMessage(error, '合并请求加载失败'), 'error')
  } finally {
    loading.value = false
  }
}

function setFilter(value) {
  if (filter.value === value) return
  filter.value = value
  load()
}

watch(() => props.repo.name, load)
onMounted(load)
</script>
