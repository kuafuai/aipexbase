<template>
  <div>
    <h1 class="text-2xl font-semibold text-cyan-400 mb-6">{{ t('page.apikeys.title') }}</h1>
    
    <!-- 生成新的API密钥 -->
    <el-card class="mb-6" shadow="hover" style="background-color: #1e293b; color: #e2e8f0; border: 1px solid #334155;">
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-3 h-3 bg-purple-500 rounded-full"></div>
          <span class="text-lg font-semibold text-purple-600">{{ t('page.apikeys.section_generate') }}</span>
        </div>
      </template>
      
      <el-form :model="newKey" label-width="120px" class="space-y-4">
        <el-form-item :label="t('page.apikeys.name')" required>
          <el-input 
            v-model="newKey.name" 
            :placeholder="t('page.apikeys.name_placeholder')"
            class="w-full"
            style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
          />
        </el-form-item>
        
        <el-form-item :label="t('page.apikeys.desc')">
          <el-input
            v-model="newKey.description"
            type="textarea"
            :rows="2"
            :placeholder="t('page.apikeys.desc_placeholder')"
            class="w-full"
            style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
          />
        </el-form-item>
        
        <el-form-item :label="t('page.apikeys.expire_at')">
          <el-date-picker
            v-model="newKey.expireAt"
            type="datetime"
            :placeholder="t('page.apikeys.expire_placeholder')"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="w-full"
            style="--el-date-editor-bg-color: #334155; --el-date-editor-border-color: #475569; --el-date-editor-text-color: #e2e8f0; --el-date-editor-placeholder-color: #94a3b8;"
          />
          <div class="text-xs text-gray-500 mt-1">{{ t('page.apikeys.expire_hint') }}</div>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="generateKey"
            :loading="generating"
          >
            {{ t('page.apikeys.btn_generate') }}
          </el-button>
          <el-button @click="resetForm">{{ t('page.apikeys.btn_reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- API密钥列表 -->
    <el-card class="mb-6" shadow="hover" style="background-color: #1e293b; color: #e2e8f0; border: 1px solid #334155;">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-3 h-3 bg-blue-500 rounded-full"></div>
            <span class="text-lg font-semibold text-blue-600">{{ t('page.apikeys.list_title') }}</span>
          </div>
          <el-button 
            type="primary" 
            size="small" 
            @click="refreshKeys"
            :loading="loading"
          >
            {{ t('page.apikeys.btn_refresh') }}
          </el-button>
        </div>
      </template>
      
      <!-- API调用方式 -->
      <div class="mb-4 p-3 bg-slate-800 border border-slate-600 rounded-lg">
        <div class="flex items-center gap-2 mb-3">
          <el-icon class="text-blue-500">
            <InfoFilled />
          </el-icon>
          <span class="text-sm font-medium text-blue-400">{{ t('page.apikeys.api_howto') }}</span>
        </div>
        <div class="text-sm text-blue-300">
          <div class="bg-gray-900 text-gray-100 p-3 rounded-lg font-mono text-xs overflow-x-auto">
            <div class="text-gray-400">import { createClient } from 'aipexbase-js';</div>
            <div class="mt-2"></div>
            <div class="text-gray-400">const aipexbase = createClient({</div>
            <div class="text-gray-300 ml-2">baseUrl: '{{ apiBaseUrl }}',</div>
            <div class="text-gray-300 ml-2">apiKey: 'YOUR_API_KEY'</div>
            <div class="text-gray-400">});</div>
          </div>
          <div class="mt-2 text-xs text-blue-400">{{ t('page.apikeys.api_howto_replace_hint') }}</div>
        </div>
      </div>
      
      <div v-if="loading" class="text-center py-8">
        <el-icon class="is-loading text-blue-500 text-2xl">
          <Loading />
        </el-icon>
        <div class="text-gray-500 mt-2">{{ t('page.apikeys.loading') }}</div>
      </div>
      
      <div v-else-if="apiKeys.length === 0" class="text-center py-8">
        <div class="text-gray-500">{{ t('page.apikeys.empty') }}</div>
      </div>
      
      <div v-else class="space-y-4">
        <div 
          v-for="key in apiKeys" 
          :key="key.id"
          class="p-4 bg-slate-700 rounded-lg border border-slate-600 shadow-sm"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-3">
              <div class="flex items-center gap-2">
                <el-switch
                  v-model="key.status"
                  @change="toggleKey(key)"
                  :loading="key.toggling"
                  active-value="ACTIVE"
                  inactive-value="DISABLE"
                />
                <span class="text-gray-100 font-semibold">{{ key.name }}</span>
              </div>
              <el-tag 
                :type="key.status === 'ACTIVE' ? 'success' : 'info'" 
                effect="dark" 
                size="small"
              >
                {{ key.status === 'ACTIVE' ? t('page.apikeys.status_enabled') : t('page.apikeys.status_disabled') }}
              </el-tag>
            </div>
            <div class="flex items-center gap-2">
              <el-button 
                type="primary" 
                size="small" 
                @click="showKey(key)"
              >
                {{ t('page.apikeys.btn_show_key') }}
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click="deleteKey(key)"
              >
                {{ t('page.apikeys.btn_delete') }}
              </el-button>
            </div>
          </div>
          
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div>
              <span class="text-gray-300">{{ t('page.apikeys.field_name') }}</span>
              <span class="text-gray-100 ml-2">{{ key.name }}</span>
            </div>
            <div>
              <span class="text-gray-300">{{ t('page.apikeys.field_created') }}</span>
              <span class="text-gray-100 ml-2">{{ formatDate(key.createAt) }}</span>
            </div>
            <div>
              <span class="text-gray-300">{{ t('page.apikeys.field_last_used') }}</span>
              <span class="text-gray-100 ml-2">{{ key.lastUsedAt ? formatDate(key.lastUsedAt) : t('page.apikeys.never_used') }}</span>
            </div>
          </div>
          
          <div v-if="key.description" class="mt-3 text-sm">
            <span class="text-gray-300">{{ t('page.apikeys.field_desc') }}</span>
            <span class="text-gray-100 ml-2">{{ key.description }}</span>
          </div>
          
          <div v-if="key.expireAt" class="mt-2 text-sm">
            <span class="text-gray-300">{{ t('page.apikeys.field_expire') }}</span>
            <span class="text-gray-100 ml-2">{{ formatDate(key.expireAt) }}</span>
          </div>
          
          <!-- 密钥值显示 -->
          <div v-if="key.showKey" class="mt-4 p-3 bg-slate-800 rounded border border-slate-600">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm text-gray-300">{{ t('page.apikeys.value_label') }}</span>
              <el-button 
                type="text" 
                size="small" 
                @click="copyKey(key.keyName)"
              >
                {{ t('page.apikeys.btn_copy') }}
              </el-button>
            </div>
            <div class="font-mono text-sm text-gray-200 break-all bg-slate-900 p-2 rounded border border-slate-600">
              {{ key.keyName }}
            </div>
          </div>
          
          <div class="mt-3">
            <el-button 
              type="text" 
              size="small" 
              @click="key.showKey = !key.showKey"
            >
              {{ key.showKey ? t('page.apikeys.btn_hide') : t('page.apikeys.btn_show') }}
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 分页组件 -->
      <div v-if="pagination.total > 0" class="mt-6 flex justify-center">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Loading, SuccessFilled, WarningFilled, InfoFilled } from '@element-plus/icons-vue'

const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const appId = proxy.$route.params.id;

// 响应式数据
const loading = ref(false);
const generating = ref(false);
const apiKeys = ref([]);

// 分页数据
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
});

// API基础地址
const apiBaseUrl = ref('');

// 新密钥表单数据
const newKey = ref({
  name: '',
  description: '',
  expireAt: null
});

// 页面加载时获取密钥列表
onMounted(() => {
  // 获取API基础地址
  apiBaseUrl.value = import.meta.env.VITE_PROJECT_API_ENDPOINT || 'http://HOST:PORT/baas-api';
  fetchKeys();
});

// 获取密钥列表
const fetchKeys = async () => {
  loading.value = true;
  try {
    const res = await proxy.$api.apikeys.list(appId, {
      current: pagination.value.current,
      pageSize: pagination.value.pageSize
    });
    
    // 处理分页响应数据
    if (res.data && res.data.records) {
      apiKeys.value = res.data.records.map(key => ({
        // 后端字段映射到前端字段
        id: key.id,
        name: key.name,                    // 密钥名称
        keyName: key.keyName,              // 密钥标识
        description: key.description,      // 描述
        status: key.status,                // 状态 (ACTIVE/DISABLE)
        createAt: key.createAt,            // 创建时间
        lastUsedAt: key.lastUsedAt,        // 最后使用时间
        expireAt: key.expireAt,            // 过期时间
        // 前端状态字段
        showKey: false,
        toggling: false
      }));
      pagination.value.total = res.data.total || 0;
    } else {
      // 兼容非分页格式
      apiKeys.value = (res.data || []).map(key => ({
        // 后端字段映射到前端字段
        id: key.id,
        name: key.name,                    // 密钥名称
        keyName: key.keyName,              // 密钥标识
        description: key.description,      // 描述
        status: key.status,                // 状态 (ACTIVE/DISABLE)
        createAt: key.createAt,            // 创建时间
        lastUsedAt: key.lastUsedAt,        // 最后使用时间
        expireAt: key.expireAt,            // 过期时间
        // 前端状态字段
        showKey: false,
        toggling: false
      }));
      pagination.value.total = apiKeys.value.length;
    }
  } catch (error) {
    console.error('Failed to fetch keys:', error);
    proxy.$modal.msgError(t('page.apikeys.fetch_failed') || '获取密钥列表失败');
  } finally {
    loading.value = false;
  }
};

// 刷新密钥列表
const refreshKeys = () => {
  pagination.value.current = 1;
  fetchKeys();
};

// 分页变化处理
const handlePageChange = (page) => {
  pagination.value.current = page;
  fetchKeys();
};

// 每页大小变化处理
const handlePageSizeChange = (pageSize) => {
  pagination.value.pageSize = pageSize;
  pagination.value.current = 1;
  fetchKeys();
};

// 生成新密钥
const generateKey = async () => {
  if (!newKey.value.name) {
    proxy.$modal.msgWarning(t('page.apikeys.warn_input_name'));
    return;
  }

  generating.value = true;
  try {
    const res = await proxy.$api.apikeys.generate(appId, newKey.value);
    console.log('生成密钥API响应:', res);
    proxy.$modal.msgSuccess(t('page.apikeys.gen_success'));
    resetForm();
    fetchKeys();
  } catch (error) {
    console.error('Failed to generate key:', error);
    proxy.$modal.msgError(t('page.apikeys.gen_failed'));
  } finally {
    generating.value = false;
  }
};

// 重置表单
const resetForm = () => {
  newKey.value = {
    name: '',
    description: '',
    expireAt: null
  };
};

// 切换密钥状态
const toggleKey = async (key) => {
  key.toggling = true;
  try {
    await proxy.$api.apikeys.toggle(appId, { status: key.status , keyName: key.keyName });
    proxy.$modal.msgSuccess(`${t('page.apikeys.toggle_success_prefix')}${key.status === 'ACTIVE' ? t('page.apikeys.enabled') : t('page.apikeys.disabled')}`);
  } catch (error) {
    console.error('Failed to toggle key:', error);
    proxy.$modal.msgError(t('page.apikeys.op_failed'));
    // 恢复原状态
    key.status = key.status === 'ACTIVE' ? 'DISABLE' : 'ACTIVE';
  } finally {
    key.toggling = false;
  }
};

// 显示密钥
const showKey = (key) => {
  key.showKey = !key.showKey;
};

// 复制密钥
const copyKey = async (keyValue) => {
  try {
    await navigator.clipboard.writeText(keyValue);
    proxy.$modal.msgSuccess(t('page.apikeys.copy_success'));
  } catch (error) {
    console.error('Failed to copy key:', error);
    proxy.$modal.msgError(t('page.apikeys.copy_failed'));
  }
};

// 删除密钥
const deleteKey = async (key) => {
  try {
    await proxy.$modal.confirm(
      t('page.apikeys.confirm_delete_message', { name: key.name }),
      t('page.apikeys.confirm_delete_title'),
      {
        confirmButtonText: t('page.apikeys.confirm_delete_ok'),
        cancelButtonText: t('page.apikeys.confirm_delete_cancel'),
        type: 'warning'
      }
    );

    await proxy.$api.apikeys.delete(appId, key.keyName);
    proxy.$modal.msgSuccess(t('page.apikeys.delete_success'));
    fetchKeys();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete key:', error);
      proxy.$modal.msgError(t('page.apikeys.delete_failed'));
    }
  }
};

// 关闭密钥生成对话框
const closeKeyDialog = () => {
  keyGeneratedDialog.value = false;
  generatedKey.value = {};
};


// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString('zh-CN');
};
</script>