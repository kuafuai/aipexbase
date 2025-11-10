<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-semibold text-cyan-400">{{ t('page.dynamicapi.title') }}</h1>
      <el-button
          type="primary"
          @click="showMarketDialog = true"
          class="import-from-market-btn"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/>
        </svg>
        {{ t('page.dynamicapi.import_from_market') }}
      </el-button>
    </div>

    <!-- 添加新的外部服务 -->
    <el-card class="mb-6" shadow="hover" style="background-color: #1e293b; color: #e2e8f0; border: 1px solid #334155;">
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-3 h-3 bg-green-500 rounded-full"></div>
          <span class="text-lg font-semibold text-green-600">{{ t('page.dynamicapi.add_service') }}</span>
        </div>
      </template>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <el-form :model="newService" label-width="120px" class="space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <el-form-item :label="t('page.dynamicapi.field_key')" required>
                <el-input
                    v-model="newService.name"
                    :placeholder="t('page.dynamicapi.field_key_placeholder')"
                    class="w-full"
                    style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
                    @blur="validateServiceName"
                />
                <div class="text-xs text-gray-500 mt-1">{{ t('page.dynamicapi.field_key_hint') }}</div>
              </el-form-item>
              <el-form-item :label="t('page.dynamicapi.field_url')" required>
                <el-input
                    v-model="newService.url"
                    :placeholder="t('page.dynamicapi.field_url_placeholder')"
                    class="w-full"
                    style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
                    @blur="validateServiceUrl"
                />
                <div class="text-xs text-gray-500 mt-1">{{ t('page.dynamicapi.field_url_hint') }}</div>
              </el-form-item>
            </div>

            <el-form-item :label="t('page.dynamicapi.field_desc')" required>
              <el-input
                  v-model="newService.description"
                  type="textarea"
                  :rows="2"
                  :placeholder="t('page.dynamicapi.field_desc_placeholder')"
                  class="w-full"
                  style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
              />
            </el-form-item>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <el-form-item :label="t('page.dynamicapi.field_method')">
                <el-select v-model="newService.method" class="w-full"
                           style="--el-select-bg-color: #334155; --el-select-border-color: #475569; --el-select-text-color: #e2e8f0; --el-select-hover-border-color: #475569; --el-select-focus-border-color: #475569;">
                  <el-option label="GET" value="GET"/>
                  <el-option label="POST" value="POST"/>
                </el-select>
              </el-form-item>
              <el-form-item>
                <!-- 占位符，保持布局一致 -->
              </el-form-item>
            </div>

            <el-form-item :label="t('page.dynamicapi.field_headers')">
              <el-input
                  v-model="newService.headers"
                  type="textarea"
                  :rows="4"
                  class="w-full"
                  style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
              />
              <div class="text-xs text-gray-500 mt-1">{{ t('page.dynamicapi.field_headers_hint') }}</div>
            </el-form-item>

            <el-form-item :label="t('page.dynamicapi.field_body')">
              <el-input
                  v-model="newService.body"
                  type="textarea"
                  :rows="6"
                  class="w-full"
                  style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
              />
              <div class="text-xs text-gray-500 mt-1">{{ t('page.dynamicapi.field_body_hint') }}</div>
            </el-form-item>

            <el-form-item :label="t('page.dynamicapi.field_return_example')">
              <el-input
                  v-model="newService.dataRaw"
                  type="textarea"
                  :rows="6"
                  class="w-full"
                  style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
              />
              <div class="text-xs text-red-500 mt-1">
                {{ t('page.dynamicapi.field_return_example_label') }}
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                  type="primary"
                  @click="addService"
                  :loading="adding"
              >
                {{ t('page.dynamicapi.btn_add') }}
              </el-button>
              <el-button @click="resetForm">{{ t('page.dynamicapi.btn_reset') }}</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="p-4 bg-slate-700 rounded-lg border border-slate-600">
          <h3 class="text-gray-200 mb-3 font-semibold">{{ t('page.dynamicapi.test_title') }}</h3>
          <el-button size="small" type="success" @click="updateTestVariables">{{
              t('page.dynamicapi.btn_extract_vars')
            }}
          </el-button>
          <div v-if="testVars.length > 0" class="space-y-2 mt-4">
            <div
                v-for="(info, key) in testVarValues"
                :key="key"
                class="grid grid-cols-3 gap-2 items-center"
            >
              <div class="text-gray-300 text-sm">{{ key }}</div>
              <el-input
                  v-model="info.value"
                  size="small"
                  :placeholder="t('page.dynamicapi.input_var_value')"
              />
              <el-input
                  v-model="info.desc"
                  size="small"
                  :placeholder="t('page.dynamicapi.input_var_desc')"
              />
            </div>
          </div>

          <div v-else class="text-gray-500 mt-3 text-sm">{{ t('page.dynamicapi.no_vars') }}</div>

          <div class="mt-4">
            <el-button
                type="primary"
                size="small"
                @click="testNewService"
                :loading="testing"
                :disabled="!newService.url"
            >
              {{ t('page.dynamicapi.btn_exec_test') }}
            </el-button>
          </div>

          <div v-if="testResult" class="mt-4">
            <div class="text-gray-300 mb-2">{{ t('page.dynamicapi.result_label') }}</div>
            <pre class="bg-slate-800 p-2 rounded text-xs text-gray-200 overflow-x-auto border border-slate-600">
{{ testResult }}
            </pre>
          </div>

        </div>

      </div>


    </el-card>

    <!-- 已配置的外部服务列表 -->
    <el-card class="mb-6" shadow="hover" style="background-color: #1e293b; color: #e2e8f0; border: 1px solid #334155;">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-3 h-3 bg-blue-500 rounded-full"></div>
            <span class="text-lg font-semibold text-blue-600">{{ t('page.dynamicapi.configured_title') }}</span>
          </div>
          <el-button
              type="primary"
              size="small"
              @click="refreshServices"
              :loading="loading"
          >
            {{ t('page.dynamicapi.btn_refresh') }}
          </el-button>
        </div>
      </template>

      <div v-if="loading" class="text-center py-8">
        <el-icon class="is-loading text-blue-500 text-2xl">
          <Loading/>
        </el-icon>
        <div class="text-gray-500 mt-2">{{ t('page.dynamicapi.loading') }}</div>
      </div>

      <div v-else-if="services.length === 0" class="text-center py-8">
        <div class="text-gray-500">{{ t('page.dynamicapi.empty') }}</div>
      </div>

      <div v-else class="space-y-4">
        <div
            v-for="service in services"
            :key="service.id"
            class="p-4 bg-slate-700 rounded-lg border border-slate-600 shadow-sm"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-3">
              <span class="text-gray-100 font-semibold">{{ service.name }}</span>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                  type="danger"
                  size="small"
                  @click="deleteService(service)"
              >
                删除
              </el-button>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <span class="text-gray-300">{{ t('page.dynamicapi.field_url_label') }}</span>
              <span class="text-gray-100 ml-2 font-mono">{{ service.url }}</span>
            </div>
            <div>
              <span class="text-gray-300">{{ t('page.dynamicapi.field_method_label') }}</span>
              <el-tag size="small" class="ml-2">{{ service.method }}</el-tag>
            </div>
            <div v-if="service.description" class="md:col-span-2">
              <span class="text-gray-300">{{ t('page.dynamicapi.field_desc_label') }}</span>
              <span class="text-gray-100 ml-2">{{ service.description }}</span>
            </div>
          </div>

          <!-- 展开详细信息 -->
          <div v-if="service.showDetails" class="mt-4 pt-4 border-t border-slate-600">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div v-if="service.headers">
                <div class="text-gray-300 mb-2">{{ t('page.dynamicapi.field_headers_label') }}</div>
                <pre class="bg-slate-800 p-2 rounded text-xs text-gray-200 overflow-x-auto border border-slate-600">{{
                    formatJson(service.headers)
                  }}</pre>
              </div>
              <div v-if="service.body">
                <div class="text-gray-300 mb-2">{{ t('page.dynamicapi.field_body_label') }}</div>
                <pre class="bg-slate-800 p-2 rounded text-xs text-gray-200 overflow-x-auto border border-slate-600">{{
                    formatJson(service.body)
                  }}</pre>
              </div>
            </div>
            <div v-if="service.dataRaw" class="mt-4 text-sm">
              <div class="text-gray-300 mb-2">{{ t('page.dynamicapi.field_return_example_label') }}</div>
              <pre class="bg-slate-800 p-2 rounded text-xs text-gray-200 overflow-x-auto border border-slate-600">{{
                  formatJson(service.dataRaw)
                }}</pre>
            </div>
          </div>

          <div class="mt-3">
            <el-button
                type="text"
                size="small"
                @click="service.showDetails = !service.showDetails"
            >
              {{
                service.showDetails ? t('page.dynamicapi.btn_toggle_details_show') : t('page.dynamicapi.btn_toggle_details_hide')
              }}
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

    <!-- API市场导入弹窗 -->
    <el-dialog
        v-model="showMarketDialog"
        :title="t('page.dynamicapi.market_dialog_title')"
        width="80%"
        :close-on-click-modal="false"
    >
      <div v-loading="loadingMarketApis" class="market-api-list">
        <!-- 搜索和筛选 -->
        <div class="mb-4 flex gap-4">
          <el-input
              v-model="marketSearchKeyword"
              :placeholder="t('page.dynamicapi.market_search_placeholder')"
              clearable
              @input="searchMarketApis"
              class="flex-1"
          >
            <template #prefix>
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
            </template>
          </el-input>
          <el-select
              v-model="marketCategoryFilter"
              :placeholder="t('page.dynamicapi.market_category_filter')"
              clearable
              @change="filterMarketApis"
              style="width: 200px"
          >
            <el-option
                v-for="cat in marketCategories"
                :key="cat"
                :label="cat"
                :value="cat"
            />
          </el-select>
        </div>

        <!-- API列表 -->
        <div v-if="filteredMarketApis.length === 0" class="text-center py-8 text-gray-400">
          {{ t('page.dynamicapi.market_empty') }}
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4 max-h-96 overflow-y-auto">
          <div
              v-for="api in filteredMarketApis"
              :key="api.id"
              class="p-4 bg-slate-700 rounded-lg border border-slate-600 hover:border-cyan-500 transition-all cursor-pointer"
              :class="{'border-cyan-500 bg-slate-600': selectedMarketApi?.id === api.id}"
              @click="selectMarketApi(api)"
          >
            <div class="flex items-start justify-between mb-2">
              <div class="flex-1">
                <h3 class="text-white font-semibold mb-1">{{ api.name }}</h3>
                <p class="text-gray-400 text-sm">{{ api.description }}</p>
              </div>
              <el-tag v-if="api.category" size="small" class="ml-2">{{ api.category }}</el-tag>
            </div>

            <div class="flex items-center gap-4 mt-3 text-sm">
              <span class="text-gray-300">
                <el-tag size="small" type="info">{{ getMethodText(api.method) }}</el-tag>
              </span>
              <span class="text-gray-400">{{ getPricingModelBadge(api.pricingModel) }}</span>
              <span v-if="api.pricingModel !== 0" class="text-cyan-400">
                ¥{{ Number(api.unitPrice || 0).toFixed(2) }}
              </span>
            </div>

            <!-- 预览URL -->
            <div class="mt-2 text-xs text-gray-500 font-mono truncate">
              {{ api.url }}
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="showMarketDialog = false">
            {{ t('page.dynamicapi.cancel') }}
          </el-button>
          <el-button
              type="primary"
              :disabled="!selectedMarketApi"
              @click="importFromMarket"
          >
            {{ t('page.dynamicapi.import_selected') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {Loading} from '@element-plus/icons-vue'
import {validURL} from '@/utils/validate'

const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const appId = proxy.$route.params.id;

// 响应式数据
const loading = ref(false);
const adding = ref(false);
const services = ref([]);

// 分页数据
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
});

// 新服务表单数据
const newService = ref({
  name: '',
  url: '',
  method: 'POST',
  headers: '{"Authorization": "Bearer {{token}}", "Content-Type": "application/json"}',
  body: '{"input": "{{text}}", "content": "{{content}}"}',
  description: '',
  dataRaw: '{"status": "success //状态", "data": {"result": "string //结果"}}'  // 新增：返回值样例
});

// 页面加载时获取服务列表
onMounted(() => {
  fetchServices();
});

// 获取服务列表
const fetchServices = async () => {
  loading.value = true;
  console.log('开始获取服务列表，appId:', appId, '分页参数:', {
    current: pagination.value.current,
    pageSize: pagination.value.pageSize
  });

  try {
    const res = await proxy.$api.dynamicapi.list(appId, {
      current: pagination.value.current,
      pageSize: pagination.value.pageSize
    });

    console.log('API响应:', res);

    // 处理分页响应数据
    if (res.data && res.data.records) {
      services.value = res.data.records.map(service => ({
        // 后端字段映射到前端字段
        id: service.id,
        name: service.keyName,           // keyName → name
        description: service.description,
        url: service.url,
        method: service.method,
        headers: service.header,          // header → headers
        body: service.bodyTemplate,       // bodyTemplate → body
        // 保留其他后端字段
        token: service.token,
        protocol: service.protocol,
        dataRaw: service.dataRaw,
        dataType: service.dataType,
        bodyType: service.bodyType,
        // 前端状态字段
        showDetails: false
      }));
      pagination.value.total = res.data.total || 0;
      console.log('分页格式处理完成，服务数量:', services.value.length, '总数:', pagination.value.total);
    } else {
      // 兼容非分页格式
      services.value = (res.data || []).map(service => ({
        // 后端字段映射到前端字段
        id: service.id,
        name: service.keyName,           // keyName → name
        description: service.description,
        url: service.url,
        method: service.method,
        headers: service.header,          // header → headers
        body: service.bodyTemplate,       // bodyTemplate → body
        // 保留其他后端字段
        token: service.token,
        protocol: service.protocol,
        dataRaw: service.dataRaw,
        dataType: service.dataType,
        bodyType: service.bodyType,
        // 前端状态字段
        showDetails: false
      }));
      pagination.value.total = services.value.length;
      console.log('非分页格式处理完成，服务数量:', services.value.length);
    }
  } catch (error) {
    console.error('Failed to fetch services:', error);
    proxy.$modal.msgError(t('page.dynamicapi.fetch_failed'));
  } finally {
    loading.value = false;
  }
};

// 刷新服务列表
const refreshServices = () => {
  pagination.value.current = 1;
  fetchServices();
};

// 分页变化处理
const handlePageChange = (page) => {
  pagination.value.current = page;
  fetchServices();
};

// 每页大小变化处理
const handlePageSizeChange = (pageSize) => {
  pagination.value.pageSize = pageSize;
  pagination.value.current = 1;
  fetchServices();
};

// 验证服务标识
const validateServiceName = () => {
  const name = newService.value.name;
  if (!name) return true;

  // 校验规则：仅允许小写字母+数字，长度小于16个字符，必须以小写字母开头
  const regex = /^[a-z][a-z0-9]{0,14}$/;
  if (!regex.test(name)) {
    proxy.$modal.msgError(t('page.dynamicapi.key_invalid'));
    return false;
  }
  return true;
};

// 验证服务URL
const validateServiceUrl = () => {
  const url = newService.value.url;
  if (!url) return true;

  // 增强的HTTP/HTTPS URL验证，支持更多顶级域名和URL参数
  const httpUrlRegex = /^https?:\/\/([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.([a-zA-Z]{2,}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]*\/?)*)?(\?[a-zA-Z0-9.,?'\\+&%$#=~_-]*)?(\#[a-zA-Z0-9.,?'\\+&%$#=~_-]*)?$/;

  if (!httpUrlRegex.test(url)) {
    proxy.$modal.msgError(t('page.dynamicapi.url_invalid'));
    return false;
  }
  return true;
};

// 添加新服务
const addService = async () => {
  if (!newService.value.name || !newService.value.url || !newService.value.description) {
    proxy.$modal.msgWarning(t('page.dynamicapi.need_fields_warning'));
    return;
  }

  if (!testResult.value) {
    proxy.$modal.msgWarning(t('page.dynamicapi.need_test_warning'));
    return;
  }

  // 验证服务标识格式
  if (!validateServiceName()) {
    return;
  }

  // 验证URL格式
  if (!validateServiceUrl()) {
    return;
  }

  adding.value = true;
  try {
    // 将前端字段转换为后端字段
    const backendData = {
      keyName: newService.value.name,           // name → keyName
      description: newService.value.description,
      url: newService.value.url,
      method: newService.value.method,
      headerTemplate: newService.value.headers,          // headers → header
      bodyTemplate: newService.value.body,      // body → bodyTemplate
      dataRaw: newService.value.dataRaw,       // 新增：返回值样例
      vars: JSON.stringify(testVarValues.value, null, 2)                // 新增：提取的模版模版变量
    };

    await proxy.$api.dynamicapi.add(appId, backendData);
    proxy.$modal.msgSuccess(t('page.dynamicapi.btn_add') + t('page.tables.delete_success').replace('删除', '添加'));
    resetForm();
    fetchServices();
  } catch (error) {
    console.error('Failed to add service:', error);
    proxy.$modal.msgError(t('page.dynamicapi.delete_failed').replace('删除', '添加'));
  } finally {
    adding.value = false;
  }
};

// 重置表单
const resetForm = () => {
  newService.value = {
    name: '',
    url: '',
    method: 'POST',
    headers: '{"Authorization": "Bearer {{token}}", "Content-Type": "application/json"}',
    body: '{"input": "{{text}}", "content": "{{content}}"}',
    description: '',
    dataRaw: '{"status": "success //状态", "data": {"result": "string //结果"}}'  // 新增：返回值样例
  };

  testVars.value = [];
  testVarValues.value = {};
  testResult.value = '';

};

// 删除服务
const deleteService = async (service) => {
  try {
    await proxy.$modal.confirm(
        t('page.dynamicapi.confirm_delete_message', {name: service.name}),
        t('page.dynamicapi.confirm_delete_title'),
        {
          confirmButtonText: t('page.dynamicapi.confirm_delete_ok'),
          cancelButtonText: t('page.dynamicapi.confirm_delete_cancel'),
          type: 'warning'
        }
    );

    await proxy.$api.dynamicapi.delete(appId, service.name);
    proxy.$modal.msgSuccess(t('page.dynamicapi.delete_success'));
    fetchServices();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete service:', error);
      proxy.$modal.msgError(t('page.dynamicapi.delete_failed'));
    }
  }
};

// 格式化JSON显示
const formatJson = (jsonString) => {
  try {
    return JSON.stringify(JSON.parse(jsonString), null, 2);
  } catch (error) {
    return jsonString;
  }
};


const extractVariables = (text) => {
  const regex = /\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g;
  const vars = new Set();
  let match;
  while ((match = regex.exec(text)) !== null) {
    vars.add(match[1]);
  }
  return Array.from(vars);
};

const testVars = ref([]);
const testVarValues = ref({});
const testing = ref(false);
const testResult = ref('');

// API市场相关状态
const showMarketDialog = ref(false);
const loadingMarketApis = ref(false);
const marketApis = ref([]);
const filteredMarketApis = ref([]);
const selectedMarketApi = ref(null);
const marketSearchKeyword = ref('');
const marketCategoryFilter = ref('');
const marketCategories = ref([]);

// 监听弹窗打开，加载市场API
watch(showMarketDialog, (newVal) => {
  if (newVal) {
    loadMarketApis();
  }
});

// 加载市场API列表
const loadMarketApis = async () => {
  loadingMarketApis.value = true;
  try {
    const res = await proxy.$api.apimarket.list();
    if (res.success && res.data) {
      marketApis.value = res.data;
      filteredMarketApis.value = res.data;

      // 提取所有分类
      const categories = new Set();
      res.data.forEach(api => {
        if (api.category) {
          categories.add(api.category);
        }
      });
      marketCategories.value = Array.from(categories);
    }
  } catch (error) {
    console.error('Failed to load market APIs:', error);
    proxy.$modal.msgError(t('page.dynamicapi.market_load_failed'));
  } finally {
    loadingMarketApis.value = false;
  }
};

// 搜索市场API
const searchMarketApis = () => {
  filterMarketApis();
};

// 筛选市场API
const filterMarketApis = () => {
  let result = marketApis.value;

  // 按关键词搜索
  if (marketSearchKeyword.value) {
    const keyword = marketSearchKeyword.value.toLowerCase();
    result = result.filter(api =>
        api.name.toLowerCase().includes(keyword) ||
        api.description?.toLowerCase().includes(keyword)
    );
  }

  // 按分类筛选
  if (marketCategoryFilter.value) {
    result = result.filter(api => api.category === marketCategoryFilter.value);
  }

  filteredMarketApis.value = result;
};

// 选择市场API
const selectMarketApi = (api) => {
  selectedMarketApi.value = api;
};

// 从市场导入API
const importFromMarket = async () => {
  if (!selectedMarketApi.value) return;

  const api = selectedMarketApi.value;
  let res = await proxy.$api.dynamicapi.addMarket(appId, api);
  if (res.success) {
    proxy.$modal.msgSuccess(t('page.dynamicapi.btn_add') + t('page.tables.delete_success').replace('删除', '添加'));
    // 关闭弹窗
    showMarketDialog.value = false;
    selectedMarketApi.value = null;
  } else {
    proxy.$modal.msgSuccess(t('page.dynamicapi.btn_add') + t('page.tables.delete_failed').replace('删除', '添加'));
  }


};

// 获取请求方法文本
const getMethodText = (method) => {
  const methods = {
    0: 'GET',
    1: 'POST',
    2: 'PUT',
    3: 'DELETE',
    4: 'PATCH'
  };
  return methods[method] || 'GET';
};

// 获取计费模式标签
const getPricingModelBadge = (pricingModel) => {
  const models = {
    0: 'FREE',
    1: t('page.apimarket.pricing_per_call'),
    2: t('page.apimarket.pricing_per_token')
  };
  return models[pricingModel] || 'FREE';
};


const updateTestVariables = () => {
  testVars.value = [];
  testVarValues.value = {};
  testResult.value = '';

  const allVars = new Set([
    ...extractVariables(newService.value.headers || ''),
    ...extractVariables(newService.value.body || '')
  ]);

  testVars.value = [...allVars];
  const newValues = {};
  for (const v of testVars.value) {
    newValues[v] = testVarValues.value[v] || {value: '', desc: ''};
  }
  testVarValues.value = newValues;
};


const testNewService = async () => {
  testing.value = true;
  testResult.value = '';
  try {
    // 验证URL格式
    if (!validateServiceUrl()) {
      testing.value = false;
      return;
    }

    let headers = {};
    let body = {};
    try {
      headers = JSON.parse(newService.value.headers || '{}');
      body = JSON.parse(newService.value.body || '{}');
    } catch (e) {
      proxy.$modal.msgError(t('page.dynamicapi.json_invalid'));
      testing.value = false;
      return;
    }
    console.log('---------', body);

    const vars = testVarValues.value;
    console.log(testVarValues.value)

    const replaceVars = (obj) => {
      // 字符串 -> 做占位符替换
      if (typeof obj === 'string') {
        return obj.replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (_, key) => {
          // 如果找不到 key，就返回空字符串（或你可以改成保留原变量）
          const v = vars[key];
          // 支持 vars[key] 是对象{ value: ... } 的情况
          return v && v.value != null ? String(v.value) : '';
        });
      }

      // 数组 -> 递归 map 每一项，保持数组结构
      if (Array.isArray(obj)) {
        return obj.map(item => replaceVars(item));
      }

      // 非空对象 -> 递归每个键（保持对象结构）
      if (typeof obj === 'object' && obj !== null) {
        const newObj = {};
        for (const [k, v] of Object.entries(obj)) {
          newObj[k] = replaceVars(v);
        }
        return newObj;
      }

      // 其他（number / boolean / null / undefined）直接返回
      return obj;
    };

    headers = replaceVars(headers);
    body = replaceVars(body);

    const response = await fetch(newService.value.url, {
      method: newService.value.method,
      headers,
      body: newService.value.method === 'POST' ? JSON.stringify(body) : undefined
    });

    const data = await response.text();
    try {
      testResult.value = JSON.stringify(JSON.parse(data), null, 2);
    } catch {
      testResult.value = data;
    }
  } catch (err) {
    testResult.value = t('page.dynamicapi.request_failed_prefix') + err.message;
  } finally {
    testing.value = false;
  }
};

</script>

<style scoped>
.import-from-market-btn {
  display: flex;
  align-items: center;
}

.market-api-list {
  min-height: 300px;
}

/* 弹窗样式覆盖 */
:deep(.el-dialog) {
  background-color: #1e293b;
  border: 1px solid #334155;
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid #334155;
  color: #e2e8f0;
}

:deep(.el-dialog__title) {
  color: #e2e8f0;
}

:deep(.el-dialog__body) {
  color: #e2e8f0;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #334155;
}
</style>