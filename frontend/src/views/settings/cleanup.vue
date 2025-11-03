<template>
  <div class="space-y-6">

    <div class="bg-red-500/10 backdrop-blur-md rounded-2xl border border-red-500/20 overflow-hidden">
      <div class="p-6 border-b border-red-500/20">
        <div class="flex items-center space-x-3">
          <div class="w-3 h-3 bg-red-400 rounded-full animate-pulse"></div>
          <h2 class="text-xl font-semibold text-red-300">危险操作</h2>
        </div>
      </div>

      <div class="p-6 space-y-4">
        <div class="text-white/80">
          <p class="mb-2">{{ t('page.settings_cleanup.planning') }}</p>
        </div>
      </div>

    </div>

  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const appId = proxy.$route.params.id;
</script>