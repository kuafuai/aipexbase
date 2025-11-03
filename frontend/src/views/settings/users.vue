<template>
  <div class="space-y-6">
    <p>Planning (coming soon)...</p>
  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const appId = proxy.$route.params.id;
</script>