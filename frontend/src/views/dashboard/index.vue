<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-8">
      <div>
        <h2 class="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
          {{ t('page.project.list_title') }}
        </h2>
        <p class="text-white/60 mt-2">{{ t('page.project.list_sub') }}</p>
      </div>
      <div class="flex space-x-3">
        <el-button
            v-if="hasInactiveProjects"
            type="warning"
            class="bg-gradient-to-r from-amber-500 to-orange-500 border-0 text-white hover:from-amber-600 hover:to-orange-600 transition-all duration-300 shadow-lg shadow-amber-500/25"
            @click="copyInactiveAppIds"
            :title="t('page.project.copy_inactive_tooltip')"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
          {{ t('page.project.copy_inactive_btn') }}
        </el-button>
        <el-button
            type="info"
            class="bg-gradient-to-r from-purple-500 to-pink-500 border-0 text-white hover:from-purple-600 hover:to-pink-600 transition-all duration-300 shadow-lg shadow-purple-500/25"
            @click="openImportDialog"
            :title="t('page.project.import_tooltip')"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
          </svg>
          {{ t('page.project.import_app') }}
        </el-button>
        <el-button
            type="primary"
            class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
            @click="openDialog"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          {{ t('page.project.add_app') }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div
          v-for="project in pageRes.records"
          :key="project.id"
          class="group relative bg-white/5 backdrop-blur-md rounded-2xl p-6 cursor-pointer border border-white/10 hover:border-cyan-400/30 transition-all duration-500 hover:scale-105 hover:shadow-2xl hover:shadow-cyan-500/20"
          @click="openProject(project)"
      >
        <!-- 导出按钮 - 悬停时显示 -->
        <button
            class="absolute top-3 right-20 z-20 w-7 h-7 bg-green-500/80 hover:bg-green-500 rounded-lg flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-300 shadow-md"
            @click.stop="exportProject(project)"
            :title="t('page.project.export_tooltip')"
        >
          <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
        </button>
        <!-- 复制按钮 - 悬停时显示 -->
        <button
            class="absolute top-3 right-11 z-20 w-7 h-7 bg-blue-500/80 hover:bg-blue-500 rounded-lg flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-300 shadow-md"
            @click.stop="copyProject(project)"
            :title="t('page.project.copy_tooltip')"
        >
          <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
        </button>
        <!-- 回收按钮 - 悬停时显示 -->
        <button
            v-if="isInactive(project.updatedAt)"
            class="absolute top-3 right-3 z-20 w-7 h-7 bg-amber-500/80 hover:bg-amber-500 rounded-lg flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-300 shadow-md"
            @click.stop="recycleProject(project)"
            :title="t('page.project.recycle_tooltip')"
        >
          <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
        </button>
        <!-- 悬停光效 -->
        <div
            class="absolute inset-0 bg-gradient-to-r from-cyan-500/0 via-cyan-500/5 to-cyan-500/0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 rounded-2xl"></div>
        <!-- 项目图标 -->
        <div
            class="relative z-10 w-12 h-12 bg-gradient-to-br from-cyan-500 to-blue-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
          <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/>
          </svg>
        </div>

        <h2 class="text-lg font-semibold mb-2 relative z-10 text-white group-hover:text-cyan-300 transition-colors duration-300">
          {{ project.appId }}
        </h2>
        <p class="text-sm text-white/70 mb-2 relative z-10 line-clamp-2">
          {{ project.appName }}
        </p>

        <!-- 时间信息 -->
        <div class="relative z-10 text-xs text-white/50 mb-3 space-y-1">
          <div class="flex items-center justify-between">
            <span class="flex items-center">
              <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              创建: {{ formatDate(project.createdAt) }}
            </span>
          </div>
          <div class="flex items-center justify-between">
            <span class="flex items-center">
              <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
              </svg>
              活跃: {{ formatDate(project.updatedAt) }}
            </span>
            <span v-if="isInactive(project.updatedAt)"
                  class="flex items-center text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded">
              <svg class="w-3 h-3 mr-0.5" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd"
                      d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                      clip-rule="evenodd"/>
              </svg>
              不活跃
            </span>
          </div>
        </div>

        <div class="relative z-10 flex items-center justify-between text-sm text-white/60">
          <span class="flex items-center space-x-1">
            <div class="w-2 h-2 rounded-full"
                 :class="{
                    'bg-green-400': project.status === 'active',
                    'bg-gray-400': project.status !== 'active' ,
                  }">
            </div>
            <span>{{
                project.status === 'active'
                    ? t('page.project.status_running')
                    : t('page.project.status_creating')
              }}</span>
          </span>
          <svg class="w-4 h-4 transform group-hover:translate-x-1 transition-transform duration-300" fill="none"
               stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div
        v-if="!loading && pageRes.records.length === 0"
        class="text-center py-20"
    >
      <div class="w-24 h-24 mx-auto mb-6 bg-white/5 rounded-full flex items-center justify-center">
        <svg class="w-12 h-12 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1"
                d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
        </svg>
      </div>
      <h3 class="text-xl font-semibold text-white/80 mb-2">{{ t('page.project.empty_title') }}</h3>
      <p class="text-white/60 mb-6">{{ t('page.project.empty_desc') }}</p>
      <el-button
          type="primary"
          class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0"
          @click="openDialog"
      >
        {{ t('page.project.empty_btn') }}
      </el-button>
    </div>

    <div class="mt-6 flex justify-center" v-if="pageRes.pages > 1">
      <el-pagination
          background
          layout="prev, pager, next"
          :page-size="pageParams.pageSize"
          :current-page.sync="pageParams.current"
          :total="pageRes.total"
          @current-change="handlePageChange"
      />
    </div>

    <el-dialog
        v-model="showDialog"
        :title="t('page.project.dialog_add_title')"
        width="480px"
        class="ai-dialog"
        :close-on-click-modal="false"
    >
      <div class="p-2">
        <el-form :model="newProject" label-width="100px">
          <el-form-item :label="t('page.project.dialog_app_name')">
            <el-input
                v-model="newProject.name"
                :placeholder="t('page.project.dialog_app_name_placeholder')"
                size="large"
                class="custom-input"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="flex justify-end space-x-3 px-6 pb-6">
          <el-button
              @click="showDialog = false"
              class="border-white/20 text-white/80 hover:bg-white/5"
          >
            {{ t('page.project.cancel') }}
          </el-button>
          <el-button
              type="primary"
              :loading="adding"
              @click="submitAdd"
              class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600"
          >
            {{ t('page.project.create_app') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
        v-model="showImportDialog"
        :title="t('page.project.dialog_import_title')"
        width="480px"
        class="ai-dialog"
        :close-on-click-modal="false"
    >
      <div class="p-2">
        <el-form :model="importData" label-width="100px">
          <el-form-item :label="t('page.project.import_file_label')">
            <div class="w-full">
              <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :limit="1"
                  :on-change="handleFileChange"
                  :on-exceed="handleExceed"
                  accept=".json,.sql"
                  class="w-full"
              >
                <template #trigger>
                  <el-button
                      type="primary"
                      class="w-full bg-gradient-to-r from-purple-500 to-pink-500 border-0"
                  >
                    <svg class="w-5 h-5 mr-2 inline-block" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                    </svg>
                    {{ t('page.project.select_file') }}
                  </el-button>
                </template>
              </el-upload>
              <div v-if="importData.fileName" class="mt-2 text-sm text-white/70">
                {{ t('page.project.file_selected') }}: {{ importData.fileName }}
              </div>
              <div class="mt-2 text-xs text-white/50">
                {{ t('page.project.import_file_tip') }}
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="flex justify-end space-x-3 px-6 pb-6">
          <el-button
              @click="showImportDialog = false"
              class="border-white/20 text-white/80 hover:bg-white/5"
          >
            {{ t('page.project.cancel') }}
          </el-button>
          <el-button
              type="primary"
              :loading="importing"
              @click="submitImport"
              :disabled="!importData.file"
              class="bg-gradient-to-r from-purple-500 to-pink-500 border-0 text-white hover:from-purple-600 hover:to-pink-600"
          >
            {{ t('page.project.import_btn') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import {ElMessageBox} from 'element-plus';

const {proxy} = getCurrentInstance();
const t = proxy.$tt;

const loading = ref(false)
const adding = ref(false)
const importing = ref(false)

const showDialog = ref(false)
const showImportDialog = ref(false)
const uploadRef = ref(null)
const pageParams = ref({current: 1, pageSize: 20});
// 分页响应数据
const pageRes = ref({current: 1, pages: 1, size: 10, total: 0, records: []});

const newProject = ref({
  name: '',
})

const importData = ref({
  file: null,
  fileName: ''
})

// 检查是否有不活跃的项目
const hasInactiveProjects = computed(() => {
  return pageRes.value.records.some(project => isInactive(project.updatedAt));
})

onMounted(() => {
  refresh();
});

function refresh() {
  pageRes.value = {
    current: 1,
    pages: 1,
    size: 10,
    total: 0,
    records: [],
  };
  pageParams.value.current = 1;
  getApiData();
}

function getApiData() {
  loading.value = true
  let data = {...pageParams.value}
  proxy.$api.project.projects(data).then((res) => {
    pageRes.value = res.data;
  }).finally(() => (loading.value = false))
}

function handlePageChange(val) {
  pageParams.value.current = val;
  getApiData();
}

function openProject(project) {
  if (project.status !== 'active') {
    proxy.$modal.msgWarning(t('page.project.creating_warning'));
  } else {
    proxy.$router.push({name: 'ProjectDetail', params: {id: project.appId}});
  }
}

function submitAdd() {
  if (!newProject.value.name.trim()) {
    proxy.$modal.msgWarning(t('page.project.input_project_name'));
    return
  }

  adding.value = true
  proxy.$api.project.add(newProject.value).then((res) => {
    if (res.success) {
      proxy.$modal.msgSuccess(t('page.project.create_success'));
      showDialog.value = false;
      refresh();
    }
  }).finally(() => (adding.value = false))
}

function openDialog() {
  newProject.value = {name: ''}
  showDialog.value = true
}

function openImportDialog() {
  importData.value = {file: null, fileName: ''}
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  showImportDialog.value = true
}

function handleFileChange(file) {
  importData.value.file = file.raw
  importData.value.fileName = file.name
}

function handleExceed() {
  proxy.$modal.msgWarning('只能上传一个文件')
}

async function submitImport() {
  if (!importData.value.file) {
    proxy.$modal.msgWarning('请选择要导入的文件')
    return
  }

  importing.value = true

  try {
    // 读取文件内容
    const fileContent = await readFileAsText(importData.value.file)

    // 解析 JSON
    let jsonData
    try {
      jsonData = JSON.parse(fileContent)
    } catch (e) {
      proxy.$modal.msgError('文件格式错误，请上传有效的 JSON 文件')
      importing.value = false
      return
    }

    console.log(jsonData);

    // 调用导入 API
    const res = await proxy.$api.project.import(jsonData)

    if (res.success) {
      proxy.$modal.msgSuccess(t('page.project.import_success'))
      showImportDialog.value = false
      refresh()
    } else {
      proxy.$modal.msgError(t('page.project.import_failed') + ': ' + (res.message || 'Unknown error'))
    }
  } catch (error) {
    proxy.$modal.msgError(t('page.project.import_failed') + ': ' + (error.message || 'Unknown error'))
  } finally {
    importing.value = false
  }
}

function readFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = (e) => reject(e)
    reader.readAsText(file)
  })
}

// 格式化日期时间
function formatDate(dateString) {
  if (!dateString) return '-';
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

// 判断项目是否不活跃（超过15天未活跃）
function isInactive(updatedAt) {
  if (!updatedAt) return false;
  const updateDate = new Date(updatedAt);
  const now = new Date();
  const diffTime = now - updateDate;
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
  return diffDays > 15;
}

// 回收项目
async function recycleProject(project) {
  try {
    await ElMessageBox.confirm(
        t('page.project.recycle_confirm_msg', {appName: project.appName || project.appId}),
        t('page.project.recycle_confirm_title'),
        {
          confirmButtonText: t('page.project.recycle_confirm_btn'),
          cancelButtonText: t('page.project.cancel'),
          type: "warning",
        }
    );

    const loadingInstance = proxy.$loading({text: t('page.project.recycle_processing')})

    try {
      await proxy.$api.project.recycle(project.appId, {appId: project.appId})
      proxy.$modal.msgSuccess(t('page.project.recycle_success'))
      refresh()
    } finally {
      loadingInstance.close()
    }
  } catch (e) {
    // 用户取消
  }
}

// 复制项目
async function copyProject(project) {
  try {
    await ElMessageBox.confirm(
        t('page.project.copy_confirm_msg', {appName: project.appName || project.appId}),
        t('page.project.copy_confirm_title'),
        {
          confirmButtonText: t('page.project.copy_confirm_btn'),
          cancelButtonText: t('page.project.cancel'),
          type: "info",
        }
    );

    const loadingInstance = proxy.$loading({text: t('page.project.copy_processing')})

    try {
      const res = await proxy.$api.project.copy(project.appId);
      if (res.success) {
        proxy.$modal.msgSuccess(t('page.project.copy_success'))
        refresh()
      } else {
        proxy.$modal.msgError(t('page.project.copy_failed') + ': ' + (res.message || 'Unknown error'))
      }
    } finally {
      loadingInstance.close()
    }
  } catch (e) {
    // 用户取消
  }
}

// 导出项目
async function exportProject(project) {
  const loadingInstance = proxy.$loading({text: t('page.project.export_processing') || '正在导出项目...'})

  try {
    const res = await proxy.$api.project.export(project.appId);
    if (res.success && res.data) {
      // 创建JSON文件并下载
      const jsonData = JSON.stringify(res.data, null, 2);
      const blob = new Blob([jsonData], {type: 'application/json'});
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${project.appId}_export_${new Date().getTime()}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      proxy.$modal.msgSuccess(t('page.project.export_success') || '导出成功')
    } else {
      proxy.$modal.msgError(t('page.project.export_failed') + ': ' + (res.message || 'Unknown error'))
    }
  } catch (error) {
    proxy.$modal.msgError(t('page.project.export_failed') || '导出失败' + ': ' + (error.message || 'Unknown error'))
  } finally {
    loadingInstance.close()
  }
}

// 复制所有不活跃的appId
function copyInactiveAppIds() {
  const inactiveAppIds = pageRes.value.records
      .filter(project => isInactive(project.updatedAt))
      .map(project => project.appId);

  if (inactiveAppIds.length === 0) {
    proxy.$modal.msgWarning(t('page.project.no_inactive_projects'));
    return;
  }

  const appIdsText = inactiveAppIds.join(' ');

  copyText(appIdsText).then(() => {
    proxy.$modal.msgSuccess(t('page.project.copy_inactive_success', {count: inactiveAppIds.length}));
  });
}

const copyText = async (text) => {
  if (navigator?.clipboard?.writeText) {
    return navigator.clipboard.writeText(text);
  }

  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
};

</script>

<style scoped>

.el-loading-mask {
  backdrop-filter: blur(6px);
  background-color: rgba(15, 20, 35, 0.5);
}


.ai-dialog .el-dialog {
  background: rgba(20, 30, 50, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  border: 1px solid rgba(0, 255, 255, 0.2);
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.15);
}
</style>