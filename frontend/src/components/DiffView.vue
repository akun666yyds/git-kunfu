<template>
  <div v-if="files && files.length" class="diff-list">
    <div v-for="file in files" :key="`${file.oldPath}-${file.path}`" class="diff-file">
      <div class="diff-file-head">
        <span class="file-icon"><Icon name="file" :size="14" /></span>
        <span class="diff-file-path">{{ displayPath(file) }}</span>
        <span v-if="file.changeType === 'ADD'" class="tag">新增</span>
        <span v-else-if="file.changeType === 'DELETE'" class="tag">删除</span>
        <span v-else-if="file.changeType === 'RENAME'" class="tag">重命名</span>
        <span class="diff-file-stats">
          <span class="additions">+{{ file.additions }}</span>
          <span class="deletions">−{{ file.deletions }}</span>
        </span>
      </div>
      <pre class="diff-body"><code><span
        v-for="(line, index) in linesOf(file.patch)"
        :key="index"
        class="diff-line"
        :class="line.cls"
      >{{ line.text || ' ' }}</span></code></pre>
    </div>
  </div>
  <div v-else class="empty-state">
    <div class="empty-icon"><Icon name="commit" :size="22" /></div>
    <h3>没有文件改动</h3>
    <p>这次变更没有可展示的文本差异。</p>
  </div>
</template>

<script setup>
import Icon from './Icon.vue'

defineProps({
  files: { type: Array, default: () => [] }
})

function displayPath(file) {
  if (file.changeType === 'RENAME' && file.oldPath !== file.path) {
    return `${file.oldPath} → ${file.path}`
  }
  return file.path
}

function linesOf(patch) {
  if (!patch) return []
  return patch.split('\n').map((text) => {
    let cls = ''
    if (text.startsWith('@@')) cls = 'hunk'
    else if (text.startsWith('+++') || text.startsWith('---') || text.startsWith('diff ')
      || text.startsWith('index ') || text.startsWith('new file') || text.startsWith('deleted file')
      || text.startsWith('similarity') || text.startsWith('rename ')) cls = 'head'
    else if (text.startsWith('+')) cls = 'add'
    else if (text.startsWith('-')) cls = 'del'
    return { text, cls }
  })
}
</script>
