<template>
  <div>
    <h1 class="text-2xl font-semibold text-cyan-400 mb-6">第三方服务集成</h1>

    <!-- 添加新的外部服务 -->
    <el-card class="mb-6" shadow="hover" style="background-color: #1e293b; color: #e2e8f0; border: 1px solid #334155;">
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-3 h-3 bg-green-500 rounded-full"></div>
          <span class="text-lg font-semibold text-green-600">添加外部服务</span>
        </div>
      </template>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <el-form :model="newService" label-width="120px" class="space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <el-form-item label="服务标识" required>
                <el-input
                    v-model="newService.name"
                    placeholder="请输入服务标识，如：weather_service"
                    class="w-full"
                    style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
                    @blur="validateServiceName"
                />
                <div class="text-xs text-gray-500 mt-1">
                  仅允许小写字母+数字，长度小于16个字符，必须以小写字母开头
                </div>
              </el-form-item>
              <el-form-item label="请求URL" required>
                <el-input
                    v-model="newService.url"
                    placeholder="https://api.example.com/get_weather"
                    class="w-full"
                    style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
                    @blur="validateServiceUrl"
                />
                <div class="text-xs text-gray-500 mt-1">
                  请输入完整的API接口地址，支持 http:// 或 https:// 协议，各国顶级域名(.cn, .uk, .de等)，以及URL参数(?param=1)
                </div>
              </el-form-item>
            </div>

            <el-form-item label="服务描述" required>
              <el-input
                  v-model="newService.description"
                  type="textarea"
                  :rows="2"
                  placeholder="可选：描述此服务的用途和使用场景"
                  class="w-full"
                  style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
              />
            </el-form-item>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <el-form-item label="请求方法">
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

            <el-form-item label="请求头">
              <el-input
                  v-model="newService.headers"
                  type="textarea"
                  :rows="4"
                  class="w-full"
                  style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
              />
              <div class="text-xs text-gray-500 mt-1">
                支持占位符，如 &#123;&#123;token&#125;&#125;、&#123;&#123;userId&#125;&#125; 等
              </div>
            </el-form-item>

            <el-form-item label="请求体">
              <el-input
                  v-model="newService.body"
                  type="textarea"
                  :rows="6"
                  class="w-full"
                  style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
              />
              <div class="text-xs text-gray-500 mt-1">
                支持占位符，如 &#123;&#123;text&#125;&#125;、&#123;&#123;content&#125;&#125;、&#123;&#123;location&#125;&#125;
                等
              </div>
            </el-form-item>

            <el-form-item label="返回值样例">
              <el-input
                  v-model="newService.dataRaw"
                  type="textarea"
                  :rows="6"
                  class="w-full"
                  style="--el-input-bg-color: #334155; --el-input-border-color: #475569; --el-input-text-color: #e2e8f0; --el-input-placeholder-color: #94a3b8;"
              />
              <div class="text-xs text-red-500 mt-1">
                第三方服务返回的JSON结构示例。建议结合测试结果，补齐返回参数描述。以便AI更好的理解
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                  type="primary"
                  @click="addService"
                  :loading="adding"
              >
                添加服务
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="p-4 bg-slate-700 rounded-lg border border-slate-600">
          <h3 class="text-gray-200 mb-3 font-semibold">测试服务调用</h3>
          <el-button size="small" type="success" @click="updateTestVariables">提取变量</el-button>
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
                  placeholder="输入变量值"
              />
              <el-input
                  v-model="info.desc"
                  size="small"
                  placeholder="变量描述（可选）"
              />
            </div>
          </div>

          <div v-else class="text-gray-500 mt-3 text-sm">
            未检测到任何变量。
          </div>

          <div class="mt-4">
            <el-button
                type="primary"
                size="small"
                @click="testNewService"
                :loading="testing"
                :disabled="!newService.url"
            >
              执行测试
            </el-button>
          </div>

          <div v-if="testResult" class="mt-4">
            <div class="text-gray-300 mb-2">返回结果：</div>
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
            <span class="text-lg font-semibold text-blue-600">已配置的服务</span>
          </div>
          <el-button
              type="primary"
              size="small"
              @click="refreshServices"
              :loading="loading"
          >
            刷新
          </el-button>
        </div>
      </template>

      <div v-if="loading" class="text-center py-8">
        <el-icon class="is-loading text-blue-500 text-2xl">
          <Loading/>
        </el-icon>
        <div class="text-gray-500 mt-2">加载中...</div>
      </div>

      <div v-else-if="services.length === 0" class="text-center py-8">
        <div class="text-gray-500">暂无配置的外部服务</div>
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
              <span class="text-gray-300">URL:</span>
              <span class="text-gray-100 ml-2 font-mono">{{ service.url }}</span>
            </div>
            <div>
              <span class="text-gray-300">方法:</span>
              <el-tag size="small" class="ml-2">{{ service.method }}</el-tag>
            </div>
            <div v-if="service.description" class="md:col-span-2">
              <span class="text-gray-300">描述:</span>
              <span class="text-gray-100 ml-2">{{ service.description }}</span>
            </div>
          </div>

          <!-- 展开详细信息 -->
          <div v-if="service.showDetails" class="mt-4 pt-4 border-t border-slate-600">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div v-if="service.headers">
                <div class="text-gray-300 mb-2">请求头:</div>
                <pre class="bg-slate-800 p-2 rounded text-xs text-gray-200 overflow-x-auto border border-slate-600">{{
                    formatJson(service.headers)
                  }}</pre>
              </div>
              <div v-if="service.body">
                <div class="text-gray-300 mb-2">请求体:</div>
                <pre class="bg-slate-800 p-2 rounded text-xs text-gray-200 overflow-x-auto border border-slate-600">{{
                    formatJson(service.body)
                  }}</pre>
              </div>
            </div>
            <div v-if="service.dataRaw" class="mt-4 text-sm">
              <div class="text-gray-300 mb-2">返回值样例:</div>
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
              {{ service.showDetails ? '收起' : '展开详情' }}
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
import {Loading} from '@element-plus/icons-vue'
import {validURL} from '@/utils/validate'

const {proxy} = getCurrentInstance();
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
    proxy.$modal.msgError('获取服务列表失败');
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
    proxy.$modal.msgError('服务标识格式不正确：仅允许小写字母+数字，长度小于16个字符，必须以小写字母开头');
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
    proxy.$modal.msgError('URL格式不正确');
    return false;
  }
  return true;
};

// 添加新服务
const addService = async () => {
  if (!newService.value.name || !newService.value.url || !newService.value.description) {
    proxy.$modal.msgWarning('请填写服务标识和URL和服务描述');
    return;
  }

  if (!testResult.value) {
    proxy.$modal.msgWarning('请先测试一下服务');
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
    proxy.$modal.msgSuccess('服务添加成功');
    resetForm();
    fetchServices();
  } catch (error) {
    console.error('Failed to add service:', error);
    proxy.$modal.msgError('添加服务失败');
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
        `确定要删除服务 "${service.name}" 吗？此操作不可撤销。`,
        '删除服务',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
    );

    await proxy.$api.dynamicapi.delete(appId, service.name);
    proxy.$modal.msgSuccess('服务删除成功');
    fetchServices();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete service:', error);
      proxy.$modal.msgError('删除服务失败');
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
      proxy.$modal.msgError('请求头或请求体不是合法的 JSON');
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
    testResult.value = '请求失败: ' + err.message;
  } finally {
    testing.value = false;
  }
};

</script>