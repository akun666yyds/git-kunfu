<template>
  <div>
    <div class="repo-toolbar">
      <span class="small muted">
        <template v-if="!loading">共 {{ branches.length }} 个分支，默认分支为 <span class="mono">{{ repo.defaultBranch }}</span></template>
        <template v-else>正在读取分支…</template>
      </span>
      <div class="clone-box">
        <router-link class="btn btn-sm" :to="`${basePath}/commits`">
          <Icon name="commit" :size="13" />查看全部提交
        </router-link>
      </div>
    </div>

    <LoadingBlock v-if="loading" />
    <EmptyState v-else-if="!branches.length" icon="branch" title="还没有分支" description="仓库里出现第一个提交后，分支会显示在这里。" />
    <div v-else class="item-list">
      <router-link
        v-for="item in branches"
        :key="item.name"
        class="branch-row"
        :to="{ path: basePath, query: { branch: item.name } }"
        style="color: inherit"
      >
        <span class="branch-icon"><Icon name="branch" :size="14" /></span>
        <span style="min-width: 0; flex: 1">
          <div class="branch-name">{{ item.name }}</div>
          <div class="branch-sub">
            {{ item.message || '暂无提交说明' }}
            <span v-if="item.authorName"> · {{ item.authorName }}</span>
            <span v-if="item.updatedAt"> · {{ timeAgo(item.updatedAt) }}</span>
          </div>
        </span>
        <span v-if="item.isDefault" class="badge badge-public">默认</span>
        <span class="tag mono">{{ item.shortSha }}</span>
        <Icon name="arrowRight" :size="14" class="faint" />
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, apiMessage } from '../api/client'
import { timeAgo } from '../utils/format'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const props = defineProps({
  repo: { type: Object, required: true }
})

const branches = ref([])
const loading = ref(true)
const basePath = computed(() => `/${props.repo.owner}/${props.repo.name}`)

onMounted(async () => {
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/branches`)
    branches.value = data
  } catch (error) {
    notify(apiMessage(error, '分支加载失败'), 'error')
  } finally {
    loading.value = false
  }
})
</script>
