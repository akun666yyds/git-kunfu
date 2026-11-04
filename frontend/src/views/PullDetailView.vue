<template>
  <div>
    <LoadingBlock v-if="loading" />

    <EmptyState
      v-else-if="error"
      icon="pull"
      title="找不到这个合并请求"
      description="请求可能不存在，或者链接有误。"
    >
      <router-link class="btn btn-sm" :to="`/${repo.owner}/${repo.name}/pulls`">返回合并请求</router-link>
    </EmptyState>

    <template v-else-if="detail">
      <div class="issue-hero">
        <span class="item-state-icon" :class="pull.state.toLowerCase()" style="margin-top: 4px">
          <Icon :name="pull.state === 'MERGED' ? 'check' : 'pull'" :size="16" />
        </span>
        <div style="flex: 1; min-width: 0">
          <h1>{{ pull.title }} <span class="num">#{{ pull.number }}</span></h1>
          <div class="item-meta" style="margin-top: 6px">
            <span class="badge" :class="stateClass(pull.state)">{{ stateLabel(pull.state) }}</span>
            <span class="branch-arrow">{{ pull.baseBranch }} ← {{ pull.headBranch }}</span>
            <span>{{ pull.author.displayName }} 发起于 {{ formatDateTime(pull.createdAt) }}</span>
          </div>
        </div>
        <router-link class="btn btn-sm" :to="`/${repo.owner}/${repo.name}/pulls`">
          <Icon name="arrowLeft" :size="13" />合并请求
        </router-link>
      </div>

      <div v-if="pull.state === 'MERGED'" class="merge-panel merged">
        <div class="merge-title"><Icon name="check" :size="16" />已合并</div>
        <div class="merge-desc">
          由 {{ pull.mergedByUsername || '仓库所有者' }} 合并于 {{ formatDateTime(pull.mergedAt) }}。
          <template v-if="pull.mergeCommitSha">
            合并后提交
            <router-link class="mono" :to="{ name: 'repo-commit', params: { owner: repo.owner, repo: repo.name, sha: pull.mergeCommitSha } }">{{ pull.mergeCommitSha.slice(0, 7) }}</router-link>。
          </template>
        </div>
      </div>

      <div v-else-if="pull.state === 'CLOSED'" class="merge-panel">
        <div class="merge-title"><Icon name="close" :size="16" />该合并请求已关闭</div>
        <div class="merge-desc">分支上的改动没有合入 {{ pull.baseBranch }}。可以重新发起一次合并请求。</div>
      </div>

      <div v-else class="merge-panel" :class="{ conflict: mergeConflict }">
        <template v-if="!detail.compare">
          <div class="merge-title"><Icon name="alert" :size="16" />暂时无法合并</div>
          <div class="merge-desc">来源分支或目标分支可能已被删除，合并请求本身仍可查看。</div>
        </template>
        <template v-else-if="detail.canMerge">
          <div class="merge-title">
            <Icon :name="mergeConflict ? 'alert' : 'pull'" :size="16" />
            <template v-if="mergeConflict">存在冲突，暂时无法合并</template>
            <template v-else>可以合并到 <span class="mono">{{ pull.baseBranch }}</span></template>
          </div>
          <div class="merge-desc">
            共 {{ detail.compare.aheadBy }} 个新提交、{{ detail.compare.files.length }} 个文件改动
            (<span class="additions">+{{ detail.compare.additions }}</span>
            <span class="deletions">−{{ detail.compare.deletions }}</span>)。
            {{ detail.compare.fastForward ? '这次会以快进方式合并。' : '这次会创建一个合并提交。' }}
          </div>
          <div v-if="mergeConflict" class="form-error" style="margin: 12px 0 0">
            {{ mergeResult.message }}
            <ul style="margin: 6px 0 0; padding-left: 18px">
              <li v-for="file in mergeResult.conflicts" :key="file" class="mono">{{ file }}</li>
            </ul>
            <div class="small" style="margin-top: 6px">请先在本地解决冲突并推送，再刷新本页合并。</div>
          </div>
          <div v-if="mergeError" class="form-error" style="margin: 12px 0 0">{{ mergeError }}</div>
          <template v-if="!mergeConflict">
            <div class="field" style="margin: 12px 0 0">
              <input
                v-model.trim="mergeMessage"
                type="text"
                maxlength="500"
                placeholder="合并说明（可选，留空将自动生成）"
              />
            </div>
            <div class="merge-actions">
              <button class="btn btn-primary btn-sm" type="button" :disabled="merging" @click="merge">
                <Icon name="check" :size="13" />{{ merging ? '合并中…' : '合并请求' }}
              </button>
              <button v-if="canClose" class="btn btn-sm" type="button" :disabled="closing" @click="closePull">
                <Icon name="close" :size="13" />关闭请求
              </button>
            </div>
          </template>
          <div v-else-if="canClose" class="merge-actions">
            <button class="btn btn-sm" type="button" :disabled="closing" @click="closePull">
              <Icon name="close" :size="13" />关闭请求
            </button>
          </div>
        </template>
        <template v-else>
          <div class="merge-title"><Icon name="clock" :size="16" />等待合并</div>
          <div class="merge-desc">
            只有仓库所有者可以执行合并。目前有 {{ detail.compare.aheadBy }} 个提交等待合入
            <span class="mono">{{ pull.baseBranch }}</span>。
          </div>
          <div v-if="mergeError" class="form-error" style="margin: 12px 0 0">{{ mergeError }}</div>
          <div v-if="canClose" class="merge-actions">
            <button class="btn btn-sm" type="button" :disabled="closing" @click="closePull">
              <Icon name="close" :size="13" />关闭请求
            </button>
          </div>
        </template>
      </div>

      <div class="issue-layout">
        <div>
          <div class="timeline-item">
            <div class="timeline-head">
              <Avatar :user="pull.author" :size="20" />
              <span class="strong" style="color: var(--ink-2)">{{ pull.author.displayName }}</span>
              <span>发起于 {{ timeAgo(pull.createdAt) }}</span>
            </div>
            <div class="timeline-body">
              <MarkdownView v-if="pull.body" :content="pull.body" />
              <p v-else class="muted small">没有填写说明。</p>
            </div>
          </div>

          <div v-if="detail.compare" class="section-head" style="margin: 22px 0 12px">
            <div>
              <h2 class="section-title" style="font-size: 17px">改动内容</h2>
              <p class="section-desc">
                从 <span class="mono">{{ pull.headBranch }}</span> 合入
                <span class="mono">{{ pull.baseBranch }}</span>
              </p>
            </div>
          </div>

          <template v-if="detail.compare">
            <div v-if="detail.compare.commits.length" class="commit-list" style="margin-bottom: 16px">
              <div v-for="item in detail.compare.commits" :key="item.sha" class="commit-item">
                <Icon name="commit" :size="16" class="file-icon" />
                <div class="commit-main">
                  <router-link
                    class="commit-title"
                    :to="{ name: 'repo-commit', params: { owner: repo.owner, repo: repo.name, sha: item.sha } }"
                  >{{ item.message }}</router-link>
                  <div class="commit-sub">
                    <span>{{ item.authorName }}</span><span>·</span><span>{{ timeAgo(item.authoredAt) }}</span>
                  </div>
                </div>
                <span class="tag mono">{{ item.shortSha }}</span>
              </div>
            </div>
            <DiffView :files="detail.compare.files" />
          </template>
        </div>

        <aside>
          <div class="side-panel">
            <h4>合并信息</h4>
            <div class="side-kv"><span>状态</span><span class="badge" :class="stateClass(pull.state)">{{ stateLabel(pull.state) }}</span></div>
            <div class="side-kv"><span>编号</span><span class="mono">#{{ pull.number }}</span></div>
            <div class="side-kv"><span>来源</span><span class="mono">{{ pull.headBranch }}</span></div>
            <div class="side-kv"><span>目标</span><span class="mono">{{ pull.baseBranch }}</span></div>
            <div class="side-kv"><span>发起人</span><span>{{ pull.author.displayName }}</span></div>
            <div v-if="detail.compare" class="side-kv"><span>新增提交</span><span>{{ detail.compare.aheadBy }}</span></div>
            <div v-if="pull.mergedAt" class="side-kv"><span>合并时间</span><span>{{ formatDate(pull.mergedAt) }}</span></div>
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
import DiffView from '../components/DiffView.vue'
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
const mergeMessage = ref('')
const mergeResult = ref(null)
const mergeError = ref('')
const merging = ref(false)
const closing = ref(false)

const pull = computed(() => detail.value?.pull)
const mergeConflict = computed(() => mergeResult.value?.status === 'CONFLICT')
const canClose = computed(() => {
  if (!auth.user || !pull.value || pull.value.state !== 'OPEN') return false
  return auth.user.id === pull.value.author.id || auth.user.username === props.repo.owner
})

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
  error.value = ''
  try {
    const { data } = await api.get(
      `/repos/${props.repo.owner}/${props.repo.name}/pulls/${route.params.number}`
    )
    detail.value = data
  } catch (ex) {
    detail.value = null
    error.value = apiMessage(ex, '合并请求加载失败')
  } finally {
    loading.value = false
  }
}

async function merge() {
  if (!pull.value) return
  merging.value = true
  mergeError.value = ''
  mergeResult.value = null
  try {
    const { data } = await api.post(
      `/repos/${props.repo.owner}/${props.repo.name}/pulls/${pull.value.number}/merge`,
      { message: mergeMessage.value || null }
    )
    mergeResult.value = data
    if (data.status === 'CONFLICT') {
      notify('分支存在冲突，暂时无法合并', 'error')
      return
    }
    notify(data.status === 'FAST_FORWARD' ? '已快进合并' : data.status === 'UP_TO_DATE' ? '两个分支已经一致' : '已创建合并提交', 'success')
    await load()
    if (props.refreshRepo) props.refreshRepo()
  } catch (ex) {
    mergeError.value = apiMessage(ex, '合并失败，请稍后再试')
  } finally {
    merging.value = false
  }
}

async function closePull() {
  if (!pull.value) return
  closing.value = true
  mergeError.value = ''
  try {
    const { data } = await api.post(
      `/repos/${props.repo.owner}/${props.repo.name}/pulls/${pull.value.number}/close`
    )
    detail.value = data
    notify('合并请求已关闭', 'success')
    if (props.refreshRepo) props.refreshRepo()
  } catch (ex) {
    mergeError.value = apiMessage(ex, '关闭失败，请稍后再试')
  } finally {
    closing.value = false
  }
}

watch(() => route.params.number, load)
onMounted(load)
</script>
