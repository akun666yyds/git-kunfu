<template>
  <router-link to="/" class="brand">
    <svg class="brand-mark" :width="markSize" :height="markSize" viewBox="0 0 40 40" aria-hidden="true">
      <defs>
        <linearGradient id="brandSeal" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#b85e45" />
          <stop offset="100%" stop-color="#96452f" />
        </linearGradient>
      </defs>
      <rect width="40" height="40" rx="11" fill="url(#brandSeal)" />
      <rect x="4.5" y="4.5" width="31" height="31" rx="8" fill="none" stroke="rgba(255,248,242,.38)" stroke-width="1" />
      <text
        x="20"
        y="26.5"
        text-anchor="middle"
        fill="#fff9f2"
        font-size="18"
        font-weight="700"
        font-family="STKaiti, KaiTi, STSong, SimSun, serif"
      >功</text>
    </svg>
    <span v-if="showText" class="brand-text">
      <span class="brand-name">功夫吉特</span>
      <span class="brand-sub">gitkunfu</span>
    </span>
  </router-link>
</template>

<script setup>
defineProps({
  markSize: { type: Number, default: 34 },
  showText: { type: Boolean, default: true }
})
</script>
