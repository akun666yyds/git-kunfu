<template>
  <div class="container page">
    <div class="section-head">
      <div>
        <h1 class="section-title" style="font-size: 26px">探索公开仓库</h1>
        <p class="section-desc">
          <template v-if="keyword">关于“{{ keyword }}”的搜索结果</template>
          <template v-else>本机服务器上的所有公开项目</template>
          <span v-if="!loading"> · 共 {{ total }} 个</span>
        </p>
      </div>
      <form class="explore-search" @submit.prevent="submitSearch">
        <Icon name="search" :size="15" class="search-inline" />
        <input v-model="input" type="text" placeholder="按仓库名、简介或用户搜索" aria-label="搜索仓库" />
        <button class="btn btn-sm" type="submit">搜索</button>
      </form>
    </div>

    <LoadingBlock v-if="loading" />
    <template v-else>
      <div v-if="repos.length" class="repo-grid three">
        <RepoCard v-for="item in repos" :key="item.id" :repo="item" />
      </div>
      <EmptyState
        v-else
        icon="search"
        :title="keyword ? '没有找到匹配的仓库' : '还没有公开仓库'"
        :description="keyword ? '换个关键词试试，或者清空搜索看看全部项目。' : '公开仓库会集中展示在这里。'"
      >
        <button v-if="keyword" class="btn btn-sm" type="button" @click="clearSearch">清空搜索</button>
        <router-link v-else-if="auth.user" to="/new" class="btn btn-primary btn-sm">创建仓库</router-link>
        <router-link v-else to="/register" class="btn btn-primary btn-sm">注册账号</router-link>
      </EmptyState>

      <div v-if="total > size" class="pager">
        <button class="btn btn-sm" type="button" :disabled="page <= 1" @click="goPage(page - 1)">
          <Icon name="arrowLeft" :size="14" />上一页
        </button>
        <span class="small muted">第 {{ page }} / {{ pageCount }} 页</span>
        <button class="btn btn-sm" type="button" :disabled="page >= pageCount" @click="goPage(page + 1)">
          下一页<Icon name="arrowRight" :size="14" />
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { auth } from '../stores/auth'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import RepoCard from '../components/RepoCard.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const route = useRoute()
const router = useRouter()

const repos = ref([])
const total = ref(0)
const loading = ref(true)
const size = 20
const input = ref(route.query.q || '')

const keyword = computed(() => (route.query.q || '').toString().trim())
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size)))

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/repos', {
      params: { q: keyword.value || undefined, page: page.value, size }
    })
    repos.value = data.items
    total.value = data.total
  } catch (error) {
    notify(apiMessage(error, '仓库列表加载失败'), 'error')
    repos.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function submitSearch() {
  const q = input.value.trim()
  router.push({ path: '/explore', query: q ? { q } : {} })
}

function clearSearch() {
  input.value = ''
  router.push('/explore')
}

function goPage(target) {
  router.push({ path: '/explore', query: { ...route.query, page: target } })
}

watch(
  () => route.fullPath,
  () => {
    input.value = route.query.q || ''
    load()
  },
  { immediate: true }
)
</script>
