<template>
  <div>
    <LoadingBlock v-if="loading" />

    <EmptyState
      v-else-if="error"
      icon="issue"
      title="找不到这个议题"
      description="议题可能不存在，或者链接有误。"
    >
      <router-link class="btn btn-sm" :to="`/${repo.owner}/${repo.name}/issues`">返回议题列表</router-link>
    </EmptyState>

    <template v-else-if="detail">
      <div class="issue-hero">
        <span class="item-state-icon" :class="issue.state.toLowerCase()" style="margin-top: 4px">
          <Icon :name="issue.state === 'OPEN' ? 'issue' : 'check'" :size="16" />
        </span>
        <div style="flex: 1; min-width: 0">
          <h1>{{ issue.title }} <span class="num">#{{ issue.number }}</span></h1>
          <div class="item-meta" style="margin-top: 6px">
            <span class="badge" :class="issue.state === 'OPEN' ? 'badge-open' : 'badge-closed'">
              {{ issue.state === 'OPEN' ? '待处理' : '已关闭' }}
            </span>
            <span>{{ issue.author.displayName }} 创建于 {{ formatDateTime(issue.createdAt) }}</span>
            <span>· {{ issue.commentCount }} 条评论</span>
          </div>
        </div>
        <div style="display: flex; gap: 8px; flex-wrap: wrap">
          <button v-if="canEdit" class="btn btn-sm" type="button" :disabled="updating" @click="toggleState">
            <Icon :name="issue.state === 'OPEN' ? 'check' : 'refresh'" :size="13" />
            {{ issue.state === 'OPEN' ? '关闭议题' : '重新打开' }}
          </button>
          <router-link class="btn btn-sm" :to="`/${repo.owner}/${repo.name}/issues`">
            <Icon name="arrowLeft" :size="13" />议题列表
          </router-link>
        </div>
      </div>

      <div class="issue-layout">
        <div>
          <div class="timeline-item">
            <div class="timeline-head">
              <Avatar :user="issue.author" :size="20" />
              <span class="strong" style="color: var(--ink-2)">{{ issue.author.displayName }}</span>
              <span>打开于 {{ timeAgo(issue.createdAt) }}</span>
            </div>
            <div class="timeline-body">
              <MarkdownView v-if="issue.body" :content="issue.body" />
              <p v-else class="muted small">没有填写描述。</p>
            </div>
          </div>

          <div v-for="comment in detail.comments" :key="comment.id" class="timeline-item">
            <div class="timeline-head">
              <Avatar :user="comment.author" :size="20" />
              <span class="strong" style="color: var(--ink-2)">{{ comment.author.displayName }}</span>
              <span>评论于 {{ timeAgo(comment.createdAt) }}</span>
            </div>
            <div class="timeline-body comment">
              <MarkdownView :content="comment.body" />
            </div>
          </div>

          <div v-if="auth.user" class="comment-box">
            <textarea
              v-model="commentBody"
              placeholder="写下你的想法，支持 Markdown…"
              maxlength="20000"
            ></textarea>
            <div class="comment-actions">
              <button class="btn btn-primary btn-sm" type="button" :disabled="commenting || !commentBody.trim()" @click="submitComment">
                <Icon name="message" :size="13" />{{ commenting ? '发送中…' : '发表评论' }}
              </button>
            </div>
          </div>
          <div v-else class="comment-box" style="text-align: center">
            <p class="small muted" style="margin-bottom: 10px">登录后可以参与讨论。</p>
            <router-link class="btn btn-sm" :to="{ name: 'login', query: { redirect: route.fullPath } }">登录</router-link>
          </div>
        </div>

        <aside>
          <div class="side-panel">
            <h4>议题状态</h4>
            <div class="side-kv">
              <span>状态</span>
              <span class="badge" :class="issue.state === 'OPEN' ? 'badge-open' : 'badge-closed'">
                {{ issue.state === 'OPEN' ? '待处理' : '已关闭' }}
              </span>
            </div>
            <div class="side-kv"><span>编号</span><span class="mono">#{{ issue.number }}</span></div>
            <div class="side-kv"><span>作者</span><span>{{ issue.author.displayName }}</span></div>
            <div class="side-kv"><span>创建</span><span>{{ formatDate(issue.createdAt) }}</span></div>
            <div v-if="issue.closedAt" class="side-kv"><span>关闭</span><span>{{ formatDate(issue.closedAt) }}</span></div>
            <div class="side-kv"><span>评论</span><span>{{ issue.commentCount }}</span></div>
          </div>
          <div class="side-panel">
            <h4>所属仓库</h4>
            <router-link class="side-link" :to="`/${repo.owner}/${repo.name}`" style="padding-left: 0">
              <Icon name="repo" :size="14" />{{ repo.owner }}/{{ repo.name }}
            </router-link>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { auth } from '../stores/auth'
import { formatDate, formatDateTime, timeAgo } from '../utils/format'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import Avatar from '../components/Avatar.vue'
import MarkdownView from '../components/MarkdownView.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const props = defineProps({
  repo: { type: Object, required: true },
  refreshRepo: { type: Function, default: null }
})

const route = useRoute()
const detail = ref(null)
const loading = ref(true)
const error = ref('')
const commentBody = ref('')
const commenting = ref(false)
const updating = ref(false)

const issue = computed(() => detail.value?.issue)
const canEdit = computed(() => {
  if (!auth.user || !issue.value) return false
  return auth.user.username === issue.value.author.username || auth.user.username === props.repo.owner
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get(
      `/repos/${props.repo.owner}/${props.repo.name}/issues/${route.params.number}`
    )
    detail.value = data
  } catch (ex) {
    detail.value = null
    error.value = apiMessage(ex, '议题加载失败')
  } finally {
    loading.value = false
  }
}

async function toggleState() {
  if (!issue.value) return
  updating.value = true
  const next = issue.value.state === 'OPEN' ? 'CLOSED' : 'OPEN'
  try {
    const { data } = await api.patch(
      `/repos/${props.repo.owner}/${props.repo.name}/issues/${issue.value.number}`,
      { state: next }
    )
    detail.value = data
    notify(next === 'CLOSED' ? '议题已关闭' : '议题已重新打开', 'success')
    if (props.refreshRepo) props.refreshRepo()
  } catch (ex) {
    notify(apiMessage(ex, '状态更新失败'), 'error')
  } finally {
    updating.value = false
  }
}

async function submitComment() {
  const body = commentBody.value.trim()
  if (!body || !issue.value) return
  commenting.value = true
  try {
    const { data } = await api.post(
      `/repos/${props.repo.owner}/${props.repo.name}/issues/${issue.value.number}/comments`,
      { body }
    )
    detail.value = data
    commentBody.value = ''
    notify('评论已发表', 'success')
  } catch (ex) {
    notify(apiMessage(ex, '评论发送失败'), 'error')
  } finally {
    commenting.value = false
  }
}

watch(() => route.params.number, load)
onMounted(load)
</script>
