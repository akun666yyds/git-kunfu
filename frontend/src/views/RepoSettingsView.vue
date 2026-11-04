<template>
  <div>
    <EmptyState
      v-if="!isOwner"
      icon="lock"
      title="只有仓库所有者可以进入设置"
      description="这里可以修改仓库信息与默认分支，并管理仓库的删除。"
    >
      <router-link class="btn btn-sm" :to="`/${repo.owner}/${repo.name}`">返回仓库</router-link>
    </EmptyState>

    <div v-else style="max-width: 760px">
      <div class="section-head">
        <div>
          <h1 class="section-title" style="font-size: 21px">仓库设置</h1>
          <p class="section-desc">修改会立即生效，已克隆的仓库不受影响。</p>
        </div>
      </div>

      <form class="form-card" @submit.prevent="save" style="margin-bottom: 18px">
        <div v-if="error" class="form-error">{{ error }}</div>
        <div v-if="saved" class="form-success">设置已保存。</div>

        <div class="field">
          <label>仓库地址</label>
          <div class="clone-input" style="max-width: 100%; height: 38px">
            <Icon name="link" :size="13" />
            <span>{{ repo.cloneUrl }}</span>
            <button type="button" class="icon-btn" style="width: 26px; height: 26px" @click="copyClone">
              <Icon name="copy" :size="13" />
            </button>
          </div>
        </div>

        <div class="field">
          <div class="label-row">
            <label for="description">简介</label>
            <span class="hint">{{ form.description.length }} / 500</span>
          </div>
          <textarea id="description" v-model="form.description" maxlength="500" style="min-height: 92px"></textarea>
        </div>

        <div class="field">
          <label>可见性</label>
          <div class="choice-grid">
            <div class="choice" :class="{ active: form.visibility === 'PUBLIC' }" @click="form.visibility = 'PUBLIC'">
              <div class="choice-title"><Icon name="globe" :size="15" />公开</div>
              <div class="choice-desc">任何人都能浏览与克隆这个仓库。</div>
            </div>
            <div class="choice" :class="{ active: form.visibility === 'PRIVATE' }" @click="form.visibility = 'PRIVATE'">
              <div class="choice-title"><Icon name="lock" :size="15" />私有</div>
              <div class="choice-desc">只有自己可以访问和推送。</div>
            </div>
          </div>
        </div>

        <div class="field">
          <label for="defaultBranch">默认分支</label>
          <select id="defaultBranch" v-model="form.defaultBranch">
            <option v-for="item in branches" :key="item.name" :value="item.name">{{ item.name }}</option>
            <option v-if="!branches.length" :value="repo.defaultBranch">{{ repo.defaultBranch }}</option>
          </select>
          <span class="hint">打开仓库时默认展示这个分支。</span>
        </div>

        <button class="btn btn-primary" type="submit" :disabled="saving">
          {{ saving ? '保存中…' : '保存设置' }}
        </button>
      </form>

      <div class="danger-zone">
        <h3>删除仓库</h3>
        <p>会同时删除 Git 数据、议题与合并请求，操作不可恢复。建议先在本地保留一份克隆。</p>
        <div v-if="!confirming">
          <button class="btn btn-danger btn-sm" type="button" @click="confirming = true">
            <Icon name="trash" :size="13" />删除这个仓库
          </button>
        </div>
        <div v-else>
          <div class="field" style="margin-bottom: 10px">
            <label for="confirmName">输入 <span class="mono">{{ repo.name }}</span> 确认删除</label>
            <input id="confirmName" v-model.trim="confirmName" type="text" placeholder="仓库名称" />
          </div>
          <div style="display: flex; gap: 8px">
            <button class="btn btn-danger btn-sm" type="button" :disabled="deleting || confirmName !== repo.name" @click="remove">
              {{ deleting ? '删除中…' : '确认删除' }}
            </button>
            <button class="btn btn-sm" type="button" :disabled="deleting" @click="cancelDelete">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { auth } from '../stores/auth'
import { copyText } from '../utils/clipboard'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'

const props = defineProps({
  repo: { type: Object, required: true },
  repoDetail: { type: Object, default: null },
  refreshRepo: { type: Function, default: null }
})

const router = useRouter()
const branches = ref([])
const form = reactive({
  description: props.repo.description || '',
  visibility: props.repo.visibility,
  defaultBranch: props.repo.defaultBranch
})
const error = ref('')
const saved = ref(false)
const saving = ref(false)
const confirming = ref(false)
const confirmName = ref('')
const deleting = ref(false)

const isOwner = computed(() => Boolean(auth.user && auth.user.username === props.repo.owner))

async function loadBranches() {
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/branches`)
    branches.value = data
  } catch {
    branches.value = []
  }
}

async function save() {
  error.value = ''
  saved.value = false
  saving.value = true
  try {
    const { data } = await api.patch(`/repos/${props.repo.owner}/${props.repo.name}`, {
      description: form.description,
      visibility: form.visibility,
      defaultBranch: form.defaultBranch
    })
    saved.value = true
    notify('仓库设置已保存', 'success')
    if (props.refreshRepo) await props.refreshRepo()
  } catch (ex) {
    error.value = apiMessage(ex, '保存失败，请稍后再试')
  } finally {
    saving.value = false
  }
}

async function copyClone() {
  const ok = await copyText(props.repo.cloneUrl)
  notify(ok ? '克隆地址已复制' : '复制失败，请手动选择地址', ok ? 'success' : 'error')
}

function cancelDelete() {
  confirming.value = false
  confirmName.value = ''
}

async function remove() {
  if (confirmName.value !== props.repo.name) return
  deleting.value = true
  try {
    await api.delete(`/repos/${props.repo.owner}/${props.repo.name}`)
    notify('仓库已删除', 'success')
    router.push('/dashboard')
  } catch (ex) {
    notify(apiMessage(ex, '删除失败，请稍后再试'), 'error')
    deleting.value = false
  }
}

onMounted(loadBranches)
</script>
