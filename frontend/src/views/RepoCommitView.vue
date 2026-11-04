<template>
  <div>
    <div class="repo-toolbar">
      <router-link class="btn btn-sm" :to="{ name: 'repo-commits', params: { owner: repo.owner, repo: repo.name }, query: { branch } }">
        <Icon name="arrowLeft" :size="14" />返回提交列表
      </router-link>
      <span class="spacer"></span>
      <span v-if="detail" class="mono small muted">{{ detail.commit.sha }}</span>
    </div>

    <LoadingBlock v-if="loading" />

    <EmptyState
      v-else-if="error"
      icon="commit"
      title="找不到这次提交"
      description="提交可能不存在，或者仓库历史已被重写。"
    />

    <template v-else-if="detail">
      <div class="commit-detail-head">
        <div class="commit-detail-title">{{ detail.commit.message }}</div>
        <pre v-if="detail.commit.body" class="commit-body">{{ detail.commit.body }}</pre>
        <div class="commit-detail-meta">
          <Avatar :user="{ displayName: detail.commit.authorName, avatarColor: '#8a7a68' }" :size="22" />
          <span class="strong">{{ detail.commit.authorName }}</span>
          <span>&lt;{{ detail.commit.authorEmail }}&gt;</span>
          <span>提交于 {{ formatDateTime(detail.commit.authoredAt) }}</span>
          <span class="tag mono">{{ detail.commit.shortSha }}</span>
          <span class="diff-stats">
            <span class="additions">+{{ detail.additions }}</span>
            <span class="deletions">−{{ detail.deletions }}</span>
            <span class="faint">{{ detail.files.length }} 个文件</span>
          </span>
        </div>
        <div v-if="detail.commit.parents && detail.commit.parents.length" class="commit-detail-meta" style="margin-top: 6px">
          <span>父提交</span>
          <router-link
            v-for="parent in detail.commit.parents"
            :key="parent"
            class="tag mono"
            :to="{ name: 'repo-commit', params: { owner: repo.owner, repo: repo.name, sha: parent } }"
          >{{ parent.slice(0, 7) }}</router-link>
        </div>
      </div>

      <DiffView :files="detail.files" />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { formatDateTime } from '../utils/format'
import Icon from '../components/Icon.vue'
import Avatar from '../components/Avatar.vue'
import DiffView from '../components/DiffView.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const props = defineProps({
  repo: { type: Object, required: true }
})

const route = useRoute()
const detail = ref(null)
const loading = ref(true)
const error = ref('')

const branch = computed(() => (route.query.branch || props.repo.defaultBranch || 'main').toString())

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get(
      `/repos/${props.repo.owner}/${props.repo.name}/commits/${route.params.sha}`
    )
    detail.value = data
  } catch (ex) {
    detail.value = null
    error.value = apiMessage(ex, '提交加载失败')
  } finally {
    loading.value = false
  }
}

watch(() => route.params.sha, load)

onMounted(load)
</script>
