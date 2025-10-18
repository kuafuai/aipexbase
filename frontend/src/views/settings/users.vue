<template>
  <div class="space-y-6">
    <p>规划中(近期即将上线)...</p>
  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const appId = proxy.$route.params.id;
</script>