<template>
  <footer class="site-footer">
    <div class="container footer-inner">
      <div class="footer-left">
        <Brand :mark-size="26" :show-text="false" />
        <span>功夫吉特 gitKunfu · 把功夫下在每一次提交里</span>
      </div>
      <div class="footer-links">
        <router-link to="/explore">探索仓库</router-link>
        <router-link to="/dashboard">我的仓库</router-link>
        <router-link to="/new">新建仓库</router-link>
      </div>
    </div>
  </footer>
</template>

<script setup>
import Brand from './Brand.vue'
</script>
