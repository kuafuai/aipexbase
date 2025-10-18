<template>
  <div>
    <h1 class="text-2xl font-semibold text-cyan-400 mb-6">API 密钥管理</h1>
    
    <!-- 生成新的API密钥 -->
    <el-card class="mb-6" shadow="hover" style="background-color: #1e293b; color: #e2e8f0; border: 1px solid #334155;">
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-3 h-3 bg-purple-500 rounded-full"></div>
          <span class="text-lg font-semibold text-purple-600">生成新密钥</span>
        </div>
      </template>
      
      <el-form :model="newKey" label-width="120px" class="space-y-4">
        <el-form-item label="密钥名称" required>
          <el-input 
            v-model="newKey.name" 
            placeholder="请输入密钥名称，如：生产环境密钥"
            class="w-full"
            style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
          />
        </el-form-item>
        
        <el-form-item label="密钥描述">
          <el-input
            v-model="newKey.description"
            type="textarea"
            :rows="2"
            placeholder="可选：描述此密钥的用途和使用场景"
            class="w-full"
            style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
          />
        </el-form-item>
        
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="newKey.expireAt"
            type="datetime"
            placeholder="选择过期时间（可选）"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="w-full"
            style="--el-date-editor-bg-color: #334155; --el-date-editor-border-color: #475569; --el-date-editor-text-color: #e2e8f0; --el-date-editor-placeholder-color: #94a3b8;"
          />
          <div class="text-xs text-gray-500 mt-1">
            留空表示永不过期
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="generateKey"
            :loading="generating"
          >
            生成密钥
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- API密钥列表 -->
    <el-card class="mb-6" shadow="hover" style="background-color: #1e293b; color: #e2e8f0; border: 1px solid #334155;">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-3 h-3 bg-blue-500 rounded-full"></div>
            <span class="text-lg font-semibold text-blue-600">已生成的密钥</span>
          </div>
          <el-button 
            type="primary" 
            size="small" 
            @click="refreshKeys"
            :loading="loading"
          >
            刷新
          </el-button>
        </div>
      </template>
      
      <!-- API调用方式 -->
      <div class="mb-4 p-3 bg-slate-800 border border-slate-600 rounded-lg">
        <div class="flex items-center gap-2 mb-3">
          <el-icon class="text-blue-500">
            <InfoFilled />
          </el-icon>
          <span class="text-sm font-medium text-blue-400">API调用方式</span>
        </div>
        <div class="text-sm text-blue-300">
          <div class="bg-gray-900 text-gray-100 p-3 rounded-lg font-mono text-xs overflow-x-auto">
            <div class="text-gray-400">import { createClient } from 'baas_js';</div>
            <div class="mt-2"></div>
            <div class="text-gray-400">const baas = createClient({</div>
            <div class="text-gray-300 ml-2">baseUrl: '{{ apiBaseUrl }}',</div>
            <div class="text-gray-300 ml-2">apiKey: 'YOUR_API_KEY'</div>
            <div class="text-gray-400">});</div>
          </div>
          <div class="mt-2 text-xs text-blue-400">
            请将 YOUR_API_KEY 替换为实际的 API 密钥
          </div>
        </div>
      </div>
      
      <div v-if="loading" class="text-center py-8">
        <el-icon class="is-loading text-blue-500 text-2xl">
          <Loading />
        </el-icon>
        <div class="text-gray-500 mt-2">加载中...</div>
      </div>
      
      <div v-else-if="apiKeys.length === 0" class="text-center py-8">
        <div class="text-gray-500">暂无API密钥</div>
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
                {{ key.status === 'ACTIVE' ? '已启用' : '已禁用' }}
              </el-tag>
            </div>
            <div class="flex items-center gap-2">
              <el-button 
                type="primary" 
                size="small" 
                @click="showKey(key)"
              >
                查看密钥
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click="deleteKey(key)"
              >
                删除
              </el-button>
            </div>
          </div>
          
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div>
              <span class="text-gray-300">密钥名称:</span>
              <span class="text-gray-100 ml-2">{{ key.name }}</span>
            </div>
            <div>
              <span class="text-gray-300">创建时间:</span>
              <span class="text-gray-100 ml-2">{{ formatDate(key.createAt) }}</span>
            </div>
            <div>
              <span class="text-gray-300">最后使用:</span>
              <span class="text-gray-100 ml-2">{{ key.lastUsedAt ? formatDate(key.lastUsedAt) : '从未使用' }}</span>
            </div>
          </div>
          
          <div v-if="key.description" class="mt-3 text-sm">
            <span class="text-gray-300">描述:</span>
            <span class="text-gray-100 ml-2">{{ key.description }}</span>
          </div>
          
          <div v-if="key.expireAt" class="mt-2 text-sm">
            <span class="text-gray-300">过期时间:</span>
            <span class="text-gray-100 ml-2">{{ formatDate(key.expireAt) }}</span>
          </div>
          
          <!-- 密钥值显示 -->
          <div v-if="key.showKey" class="mt-4 p-3 bg-slate-800 rounded border border-slate-600">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm text-gray-300">密钥值:</span>
              <el-button 
                type="text" 
                size="small" 
                @click="copyKey(key.keyName)"
              >
                复制
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
              {{ key.showKey ? '隐藏密钥' : '显示密钥' }}
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
  apiBaseUrl.value = import.meta.env.PROJECT_API_ENDPOINT || 'http://localhost:8080/api';
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
    proxy.$modal.msgError('获取密钥列表失败');
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
    proxy.$modal.msgWarning('请输入密钥名称');
    return;
  }

  generating.value = true;
  try {
    const res = await proxy.$api.apikeys.generate(appId, newKey.value);
    console.log('生成密钥API响应:', res);
    proxy.$modal.msgSuccess('密钥生成成功');
    resetForm();
    fetchKeys();
  } catch (error) {
    console.error('Failed to generate key:', error);
    proxy.$modal.msgError('生成密钥失败');
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
    proxy.$modal.msgSuccess(`密钥已${key.status === 'ACTIVE' ? '启用' : '禁用'}`);
  } catch (error) {
    console.error('Failed to toggle key:', error);
    proxy.$modal.msgError('操作失败');
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
    proxy.$modal.msgSuccess('密钥已复制到剪贴板');
  } catch (error) {
    console.error('Failed to copy key:', error);
    proxy.$modal.msgError('复制失败');
  }
};

// 删除密钥
const deleteKey = async (key) => {
  try {
    await proxy.$modal.confirm(
      `确定要删除密钥 "${key.name}" 吗？此操作不可撤销。`,
      '删除密钥',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    await proxy.$api.apikeys.delete(appId, key.keyName);
    proxy.$modal.msgSuccess('密钥删除成功');
    fetchKeys();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete key:', error);
      proxy.$modal.msgError('删除密钥失败');
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