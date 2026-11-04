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
        :to="`/${repo.owner}/${repo.name}/issues/new`"
      >
        <Icon name="plus" :size="14" />新建议题
      </router-link>
      <router-link v-else class="btn btn-sm" :to="{ name: 'login', query: { redirect: route.fullPath } }">
        登录后提议题
      </router-link>
    </div>

    <LoadingBlock v-if="loading" />

    <template v-else>
      <div v-if="issues.length" class="item-list">
        <router-link
          v-for="item in issues"
          :key="item.id"
          class="item-row"
          :to="`/${repo.owner}/${repo.name}/issues/${item.number}`"
        >
          <span class="item-state-icon" :class="item.state.toLowerCase()">
            <Icon :name="item.state === 'OPEN' ? 'issue' : 'check'" :size="15" />
          </span>
          <div class="item-main">
            <span class="item-title">
              {{ item.title }}
              <span class="badge" :class="item.state === 'OPEN' ? 'badge-open' : 'badge-closed'">
                {{ item.state === 'OPEN' ? '待处理' : '已关闭' }}
              </span>
            </span>
            <div class="item-meta">
              <span>#{{ item.number }}</span>
              <span>由 {{ item.author.displayName }} 创建</span>
              <span>{{ timeAgo(item.createdAt) }}</span>
            </div>
          </div>
          <div class="item-side" v-if="item.commentCount">
            <Icon name="message" :size="13" />{{ item.commentCount }}
          </div>
        </router-link>
      </div>
      <EmptyState
        v-else
        icon="issue"
        :title="filter === 'closed' ? '没有已关闭的议题' : '还没有议题'"
        description="把遇到的问题或想法记下来，让讨论有迹可循。"
      >
        <router-link
          v-if="auth.user"
          class="btn btn-primary btn-sm"
          :to="`/${repo.owner}/${repo.name}/issues/new`"
        >新建议题</router-link>
      </EmptyState>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
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
const issues = ref([])
const openCount = ref(0)
const closedCount = ref(0)
const loading = ref(true)
const filter = ref('open')

const filters = [
  { value: 'open', label: '待处理' },
  { value: 'closed', label: '已关闭' },
  { value: 'all', label: '全部' }
]

const allCount = computed(() => openCount.value + closedCount.value)

function countOf(value) {
  if (value === 'open') return openCount.value
  if (value === 'closed') return closedCount.value
  return allCount.value
}

async function load() {
  loading.value = true
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/issues`, {
      params: { state: filter.value }
    })
    issues.value = data.items
    openCount.value = data.openCount
    closedCount.value = data.closedCount
  } catch (error) {
    issues.value = []
    notify(apiMessage(error, '议题加载失败'), 'error')
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
