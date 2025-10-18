<template>
  <div class="space-y-6">

    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">

      <div class="p-6 border-b border-white/10">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-3">
            <div class="w-3 h-3 bg-cyan-400 rounded-full animate-pulse"></div>
            <div>
              <h2 class="text-xl font-semibold text-cyan-300">权限策略</h2>
              <p class="text-xs text-white/60">管理表的行级安全策略</p>
            </div>
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
            保存
          </el-button>
        </div>
      </div>

      <div class="p-6 space-y-6">
        <div class="space-y-4">
          <div class="space-y-3">

            <el-checkbox-group v-model="appConfigJson['read_table']" v-if="tables.length > 0">
              <div
                  v-for="table in tables"
                  :key="table.tableName"
                  class="bg-white/5 rounded-xl border border-white/10 p-4 transition-all duration-200 hover:bg-white/10 my-1"
              >
                <div class="flex items-center justify-between">
                  <div class="flex items-center space-x-4 flex-1">
                    <div class="w-12 h-12 bg-blue-500/10 rounded-lg flex items-center justify-center">
                      <svg class="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                      </svg>
                    </div>

                    <div class="flex-1">
                      <div class="flex items-center space-x-3">
                        <h4 class="text-white font-medium">{{ table.tableName }}</h4>
                      </div>
                      <p class="text-white/70 text-sm mt-1">{{ table.description || '暂无描述' }}</p>
                    </div>
                  </div>

                  <div class="flex items-center space-x-4 ml-6">
                    <div class="flex items-center space-x-2 bg-white/5 rounded-lg px-3 py-2">
                      <span class="text-white/90 text-sm font-medium">公开访问</span>
                      <el-checkbox
                          class="text-white"
                          :label="table.tableName"
                          :value="table.tableName"
                      >
                        <span class="text-white/90">启用</span>
                      </el-checkbox>
                    </div>

                    <el-button
                        type="primary"
                        link
                        @click="openTableSettings(table)"
                        class="text-cyan-400 hover:text-cyan-300"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                      </svg>
                    </el-button>
                  </div>
                </div>
              </div>
            </el-checkbox-group>

            <div v-if="tables.length === 0" class="text-center py-8">
              <div class="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path class="w-8 h-8 text-white/60" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                </svg>
              </div>
              <p class="text-white/60">暂无数据表</p>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const appId = proxy.$route.params.id;

const props = {
  value: 'tableName',
  label: 'description',
  options: 'options',
  disabled: 'disabled',
}

const appInfoConfig = ref({});
const appConfigJson = ref({});
const saving = ref(false);
const tables = ref([])

onMounted(() => {
  fetchAppInfo();
  loadTables();
});

const fetchAppInfo = async () => {
  try {
    const res = await proxy.$api.project.overview(appId);
    appInfoConfig.value = res.data;
    appConfigJson.value = JSON.parse(appInfoConfig.value.configJson);
  } catch (error) {
    console.error('Failed to fetch app info:', error);
    proxy.$modal.msgError('获取应用信息失败');
  }
};

function loadTables() {
  proxy.$api.dataset.tables({appId: appId}).then((res) => {
    tables.value = res.data || [];
  });
}


async function saveLoginConfig() {


  saving.value = true;
  try {
    appInfoConfig.value.configJson = JSON.stringify(appConfigJson.value);
    await proxy.$api.project.update(appId, appInfoConfig.value);

    proxy.$modal.msgSuccess('保存成功');
  } catch (error) {
    console.error('Failed to update app name:', error);
    proxy.$modal.msgError('保存失败');
  } finally {
    saving.value = false;
  }

}

const openTableSettings = (table) => {
  proxy.$modal.alertSuccess('规划中....');
}

</script>

<style scoped>

/* 美化复选框 */
:deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-weight: 500;
}

:deep(.el-checkbox__inner) {
  background-color: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #06b6d4;
  border-color: #06b6d4;
}

:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: rgba(255, 255, 255, 0.95) !important;
}

</style>