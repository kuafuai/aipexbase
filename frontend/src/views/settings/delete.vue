<template>
  <div class="space-y-6">

    <div class="bg-red-500/10 backdrop-blur-md rounded-2xl border border-red-500/20 overflow-hidden">
      <div class="p-6 border-b border-red-500/20">
        <div class="flex items-center space-x-3">
          <div class="w-3 h-3 bg-red-400 rounded-full animate-pulse"></div>
          <h2 class="text-xl font-semibold text-red-300">{{ t('page.settings_delete.danger_title') }}</h2>
        </div>
      </div>

      <div class="p-6 space-y-4">
        <div class="text-white/80">
          <p class="mb-2">{{ t('page.settings_delete.warning') }}</p>
          <p class="text-sm text-white/60">{{ t('page.settings_delete.includes') }}</p>
        </div>
        <el-button
            type="danger"
            @click="deleteApp"
            :loading="deleting"
            class="bg-gradient-to-r from-red-500 to-pink-500 border-0 text-white hover:from-red-600 hover:to-pink-600 transition-all duration-300 shadow-lg shadow-red-500/25"
            size="large"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
          {{ t('page.settings_delete.delete_btn') }}
        </el-button>
      </div>

    </div>

  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const appId = proxy.$route.params.id;

const deleting = ref(false);

const appInfoConfig = ref({});
const appConfigJson = ref({});

onMounted(() => {
  fetchAppInfo();
});

const fetchAppInfo = async () => {
  try {
    const res = await proxy.$api.project.overview(appId);
    appInfoConfig.value = res.data;
    appConfigJson.value = JSON.parse(appInfoConfig.value.configJson);
  } catch (error) {
    console.error('Failed to fetch app info:', error);
    proxy.$modal.msgError(t('page.settings_delete.fetch_info_failed'));
  }
};

const deleteApp = async () => {
  try {
    const {value} = await proxy.$modal.prompt(
        t('page.settings_delete.confirm_message', { name: appInfoConfig.value.appName }),
        t('page.settings_delete.confirm_title'),
        {
          confirmButtonText: t('page.settings_delete.confirm_ok'),
          cancelButtonText: t('page.settings_delete.confirm_cancel'),
          type: 'error',
          inputPattern: new RegExp(`^${appInfoConfig.value.appName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`),
          inputErrorMessage: t('page.settings_delete.name_mismatch')
        }
    );

    // 额外校验输入的应用名称
    if (value !== appInfoConfig.value.appName) {
      proxy.$modal.msgError(t('page.settings_delete.name_mismatch_cancelled'));
      return;
    }

    deleting.value = true;
    await proxy.$api.project.delete(appId);
    proxy.$modal.msgSuccess(t('page.settings_delete.delete_success'));
    proxy.$router.push('/');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete app:', error);
      proxy.$modal.msgError(t('page.settings_delete.delete_failed'));
    }
  } finally {
    deleting.value = false;
  }
};

</script>