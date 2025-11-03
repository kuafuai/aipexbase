<template>
  <div class="space-y-6">
    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
      <div class="p-6 border-b border-white/10">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-3">
            <div class="w-3 h-3 bg-cyan-400 rounded-full animate-pulse"></div>
            <h2 class="text-xl font-semibold text-cyan-300">{{ t('page.settings_basic.title') }}</h2>
          </div>
          <el-button
              type="primary"
              @click="saveLoginConfig"
              :loading="saving"
              class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
              size="large"
          >
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
            </svg>
            {{ t('page.settings_basic.save') }}
          </el-button>
        </div>
      </div>

      <div class="p-6 space-y-6">
        <div class="flex items-center gap-6">
          <div class="flex items-center space-x-3 min-w-48">
            <div class="w-8 h-8 bg-cyan-500/20 rounded-lg flex items-center justify-center">
              <svg class="w-4 h-4 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"/>
              </svg>
            </div>
            <span class="text-white/80 font-medium">{{ t('page.settings_basic.app_name') }}</span>
          </div>
          <el-input
              v-model="appInfoConfig.appName"
              :placeholder="t('page.settings_basic.app_name_placeholder')"
              class="flex-1 custom-input"
              style="max-width: 400px;"
              size="large"
          />
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const appId = proxy.$route.params.id;

const appInfoConfig = ref({});
const appConfigJson = ref({});
const saving = ref(false);

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
    proxy.$modal.msgError(t('page.settings_basic.fetch_info_failed'));
  }
};

async function saveLoginConfig() {

  if (!appInfoConfig.value.appName.trim()) {
    proxy.$modal.msgWarning(t('page.settings_basic.warn_app_name'));
    return;
  }

  saving.value = true;
  try {
    appInfoConfig.value.configJson = JSON.stringify(appConfigJson.value);
    await proxy.$api.project.update(appId, appInfoConfig.value);

    proxy.$modal.msgSuccess(t('page.settings_basic.save_success'));
  } catch (error) {
    console.error('Failed to update app name:', error);
    proxy.$modal.msgError(t('page.settings_basic.save_failed'));
  } finally {
    saving.value = false;
  }

}

</script>