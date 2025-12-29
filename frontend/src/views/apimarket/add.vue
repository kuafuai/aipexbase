<template>
  <div class="p-6 flex justify-center">
    <div class="max-w-4xl w-full">
      <!-- 顶部导航 -->
      <div class="flex items-center mb-6">
        <button
            @click="goBack"
            class="flex items-center space-x-2 px-4 py-2 bg-white/5 rounded-lg border border-white/20 text-white/80 hover:bg-white/10 hover:border-white/30 transition-all duration-300"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/>
          </svg>
          <span>{{ t('page.apimarket.back') }}</span>
        </button>
      </div>
      <h1 class="text-3xl font-bold text-white mb-8">
        {{ isEdit ? t('page.apimarket.edit_api') : t('page.apimarket.add_api') }}
      </h1>

      <el-form :model="formData" label-width="140px" v-loading="loading">
        <!-- 基本信息 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_basic_info') }}</h2>

          <el-form-item :label="t('page.apimarket.form_name')" required :error="nameError">
            <el-input 
              v-model="formData.name" 
              :placeholder="t('page.apimarket.form_name_placeholder')"
              @input="validateName"
            />
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_description')" required>
            <el-input v-model="formData.description" type="textarea" :rows="3"
                      :placeholder="t('page.apimarket.form_description_placeholder')"/>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_category')">
            <el-input v-model="formData.category" :placeholder="t('page.apimarket.form_category_placeholder')"/>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_status')">
            <el-switch v-model="formData.status" :active-value="1" :inactive-value="0"/>
          </el-form-item>
        </div>

        <!-- API配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_api_config') }}</h2>

          <el-form-item :label="t('page.apimarket.form_url')" required>
            <el-input v-model="formData.url" :placeholder="t('page.apimarket.form_url_placeholder')"
                      @input="extractVariables"/>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_protocol')" required>
            <el-select v-model="formData.protocol" :placeholder="t('page.apimarket.form_protocol_placeholder')">
              <el-option label="HTTP" :value="1"/>
              <el-option label="WSS" :value="2"/>
            </el-select>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_method')" required>
            <el-select v-model="formData.method" :placeholder="t('page.apimarket.form_method_placeholder')">
              <el-option label="GET" :value="0"/>
              <el-option label="POST" :value="1"/>
              <!--              <el-option label="PUT" :value="2" />-->
              <!--              <el-option label="DELETE" :value="3" />-->
              <!--              <el-option label="PATCH" :value="4" />-->
            </el-select>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_headers')">
            <el-input v-model="formData.headers" type="textarea" :rows="4"
                      :placeholder="t('page.apimarket.form_headers_placeholder') + ' { Content-Type: application/json } ' "
                      @input="extractVariables"/>
          </el-form-item>
        </div>

        <!-- 认证配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_auth_config') }}</h2>

          <el-form-item :label="t('page.apimarket.form_auth_type')">
            <el-select v-model="formData.authType" :placeholder="t('page.apimarket.form_auth_type_placeholder')"
                       @change="handleAuthTypeChange">
              <el-option label="None" value="None"/>
              <el-option label="Bearer" value="Bearer"/>
            </el-select>
          </el-form-item>

          <el-form-item v-show="false" :label="t('page.apimarket.form_auth_config_value')">
            <el-input v-model="formData.authConfig" type="textarea" :rows="3"
                      :placeholder="t('page.apimarket.form_auth_config_placeholder')"/>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_token')">
            <el-input v-model="formData.token" :placeholder="t('page.apimarket.form_token_placeholder')"/>
          </el-form-item>
        </div>

        <!-- 请求体配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_body_config') }}</h2>

          <el-form-item :label="t('page.apimarket.form_body_type')">
            <el-select v-model="formData.bodyType" :placeholder="t('page.apimarket.form_body_type_placeholder')">
              <el-option label="JSON" :value="0"/>
              <el-option label="Form-Data" :value="1"/>
              <el-option label="URL-Encoded" :value="2"/>
              <el-option label="XML" :value="3"/>
              <el-option label="Text" :value="4"/>
            </el-select>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_body_template')">
            <el-input v-model="formData.bodyTemplate" type="textarea" :rows="6"
                      :placeholder="t('page.apimarket.form_body_template_placeholder')"
                      @input="extractVariables"/>
          </el-form-item>
        </div>

        <!-- 计价信息 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_pricing_info') }}</h2>

          <el-form-item :label="t('page.apimarket.form_pricing_model')">
            <el-select v-model="formData.pricingModel"
                       :placeholder="t('page.apimarket.form_pricing_model_placeholder')">
              <el-option label="FREE" :value="0"/>
              <el-option :label="t('page.apimarket.pricing_per_call')" :value="1"/>
              <el-option :label="t('page.apimarket.pricing_per_token')" :value="2"/>
            </el-select>
          </el-form-item>

          <el-form-item v-if="formData.pricingModel !== 0" :label="t('page.apimarket.form_unit_price')">
            <el-input-number
                v-model="formData.unitPrice"
                :min="0"
                :precision="2"
                :step="0.01"
                :placeholder="t('page.apimarket.form_unit_price_placeholder')"
                class="w-full"
            />
          </el-form-item>
          
          <el-form-item :label="t('page.apimarket.form_is_billing')">
            <el-select v-model="formData.isBilling" :placeholder="t('page.apimarket.form_is_billing_placeholder')">
              <el-option :label="t('page.apimarket.form_is_billing_option_yes')" :value="0"/>
              <el-option :label="t('page.apimarket.form_is_billing_option_no')" :value="1"/>
            </el-select>
          </el-form-item>
        </div>

        <!-- 响应数据配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_response_config') }}</h2>

          <el-form-item :label="t('page.apimarket.form_data_path')">
            <el-input v-model="formData.dataPath" :placeholder="t('page.apimarket.form_data_path_placeholder')"/>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_data_type')">
            <el-select v-model="formData.dataType" :placeholder="t('page.apimarket.form_data_type_placeholder')">
              <el-option :label="t('page.apimarket.data_type_text')" :value="0"/>
              <el-option :label="t('page.apimarket.data_type_object')" :value="1"/>
              <el-option :label="t('page.apimarket.data_type_array')" :value="2"/>
            </el-select>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_data_row')">
            <el-input v-model="formData.dataRow" type="textarea" :rows="4"
                      :placeholder="t('page.apimarket.form_data_row_placeholder')"/>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_var_row')">
            <div class="bg-black/30 rounded-lg p-4">
              <div v-if="extractedVars.length === 0" class="text-white/50 text-sm">
                {{ t('page.apimarket.no_variables_detected_left') }}&#123;&#123;variableName&#125;&#125;{{ t('page.apimarket.no_variables_detected_right') }}
              </div>
              <div v-else class="space-y-3">
                <div v-for="(varItem, index) in extractedVars" :key="index"
                     class="bg-white/5 rounded-lg p-3 border border-white/10">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-cyan-400 font-mono text-sm">{{ varItem.name }}</span>
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click="removeVariable(index)"
                      circle
                      plain
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </el-button>
                  </div>
                  <div class="space-y-2">
                    <el-input
                        v-model="varItem.value"
                        :placeholder="t('page.apimarket.var_value_placeholder')"
                        size="small"
                    />
                    <el-input
                        v-model="varItem.desc"
                        :placeholder="t('page.apimarket.var_desc_placeholder')"
                        size="small"
                    />
                  </div>
                </div>
              </div>
            </div>
          </el-form-item>
        </div>

        <!-- 操作按钮 -->
        <div class="flex justify-end space-x-4">
          <button
              type="button"
              @click="goBack"
              class="flex items-center space-x-2 px-4 py-2 bg-white/5 rounded-lg border border-white/20 text-white/80 hover:bg-white/10 hover:border-white/30 transition-all duration-300"
          >
            {{ t('page.apimarket.cancel') }}
          </button>
          <button
              v-if="!isEdit"
              type="button"
              @click="testApi"
              :disabled="testing"
              class="flex items-center space-x-2 px-4 py-2 bg-white/5 rounded-lg border border-white/20 text-white/80 hover:bg-white/10 hover:border-white/30 transition-all duration-300"
          >
            <svg v-if="testing" class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none"
                 viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span>{{ testing ? t('page.apimarket.testing') : t('page.apimarket.test') }}</span>
          </button>
          <button
              type="button"
              @click="handleSubmit"
              :disabled="submitting"
              class="px-6 py-2.5 bg-gradient-to-r from-cyan-500 to-blue-500 rounded-lg border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="submitting" class="flex items-center space-x-2">
              <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ t('page.apimarket.saving') }}</span>
            </span>
            <span v-else>{{ t('page.apimarket.save') }}</span>
          </button>
        </div>
      </el-form>
    </div>
  </div>

  <!-- 测试结果弹窗 -->
  <el-dialog
      v-model="showTestResult"
      :title="t('page.apimarket.test') + ' ' + t('page.apimarket.result_label')"
      width="50%"
  >
    <div v-if="testResult" class="p-4">
      <div class="mb-4">
        <p class="text-gray-700 mb-1">{{ t('page.apimarket.test_status_code') }}: <span class="font-mono font-bold">{{ testResult.statusCode }}</span></p>
        <p class="text-gray-700 mb-1">{{ t('page.apimarket.test_message') }}: <span class="font-mono">{{ testResult.message }}</span></p>
        <p class="text-gray-700 mb-1">{{ t('page.apimarket.test_success') }}: <span class="font-mono">{{ testResult.success ? t('page.apimarket.test_success_yes') : t('page.apimarket.test_success_no') }}</span></p>
      </div>
      <div>
        <p class="text-gray-700 mb-1">{{ t('page.apimarket.test_response_body') }}:</p>
        <pre class="bg-gray-100 p-3 rounded text-xs overflow-auto max-h-60">{{
            formatJson(testResult.responseBody)
          }}</pre>
      </div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="showTestResult = false">{{ t('page.system.system_cancel') }}</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { nextTick } from 'vue';

const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const route = useRoute();

const loading = ref(false);
const submitting = ref(false);
const isEdit = ref(false);
const testing = ref(false);
const testResult = ref(null);
const showTestResult = ref(false);
const nameError = ref('');

// 提取的变量列表
const extractedVars = ref([]);

const formData = ref({
  name: '',
  description: '',
  category: '',
  url: '',
  method: 0,
  protocol: 1,
  authType: 'None',
  authConfig: '',
  token: '',
  bodyType: 0,
  bodyTemplate: '',
  headers: '',
  dataPath: '',
  dataType: 0,
  dataRow: '',
  varRow: '',
  status: 1,
  pricingModel: 0,
  unitPrice: 0,
  isBilling: 0
});

onMounted(() => {
  const id = route.params.id;
  if (id) {
    isEdit.value = true;
    loadApiDetail(id);
  } else {
    // 检查是否有来自解析文档的参数
    if (route.query.fromParsed && route.query.parsedData) {
      const parsedData = JSON.parse(decodeURIComponent(route.query.parsedData));
      const apiMarketData = parsedData.apiMarketData;
      
      // 填充基本信息
      if (apiMarketData.name) formData.value.name = apiMarketData.name;
      if (apiMarketData.description) formData.value.description = apiMarketData.description;
      if (apiMarketData.category) formData.value.category = apiMarketData.category;
      
      // 填充API配置
      if (apiMarketData.url) formData.value.url = apiMarketData.url;
      if (apiMarketData.protocol) formData.value.protocol = apiMarketData.protocol;
      if (apiMarketData.method !== undefined) formData.value.method = apiMarketData.method;
      if (apiMarketData.headers) {
        // 如果headers是字符串，直接赋值；如果是对象，转换为格式化的JSON字符串
        if (typeof apiMarketData.headers === 'string') {
          formData.value.headers = apiMarketData.headers;
        } else {
          formData.value.headers = JSON.stringify(apiMarketData.headers, null, 2);
        }
      }
      
      // 填充认证配置
      if (apiMarketData.authType) formData.value.authType = apiMarketData.authType;
      if (apiMarketData.token) formData.value.token = apiMarketData.token;
      
      // 填充请求体配置
      if (apiMarketData.bodyType !== undefined) formData.value.bodyType = apiMarketData.bodyType;
      if (apiMarketData.bodyTemplate) {
        // 如果bodyTemplate是字符串，直接赋值；如果是对象，转换为格式化的JSON字符串
        if (typeof apiMarketData.bodyTemplate === 'string') {
          formData.value.bodyTemplate = apiMarketData.bodyTemplate;
        } else {
          formData.value.bodyTemplate = JSON.stringify(apiMarketData.bodyTemplate, null, 2);
        }
      }
      
      // 填充响应数据配置
      if (apiMarketData.dataPath) formData.value.dataPath = apiMarketData.dataPath;
      if (apiMarketData.dataType !== undefined) formData.value.dataType = apiMarketData.dataType;
      if (apiMarketData.dataRow) {
        // 检查是否已经是字符串，如果不是则转换为JSON字符串
        if (typeof apiMarketData.dataRow === 'string') {
          formData.value.dataRow = apiMarketData.dataRow;
        } else {
          try {
            formData.value.dataRow = JSON.stringify(apiMarketData.dataRow, null, 2);
          } catch {
            // 如果转换失败，直接使用原始值
            formData.value.dataRow = String(apiMarketData.dataRow);
          }
        }
      }
      
      // 填充计价信息
      if (apiMarketData.pricingModel !== undefined) formData.value.pricingModel = apiMarketData.pricingModel;
      if (apiMarketData.unitPrice) formData.value.unitPrice = apiMarketData.unitPrice;
      if (apiMarketData.isBilling !== undefined) formData.value.isBilling = apiMarketData.isBilling;
      
      // 填充变量配置
      if (apiMarketData.varRow) {
        formData.value.varRow = apiMarketData.varRow;
      }
      
      // 重新提取变量
      extractVariables();
      
      // 验证名称
      validateName(formData.value.name);
      
      // 检查是否需要显示测试结果
      if (route.query.shouldShowTestResult === 'true') {
        // 延迟执行测试，确保数据完全加载
        nextTick(() => {
          testApi();
        });
      }
    }
  }
});

// 监听变量值和描述的变化
watch(extractedVars, () => {
  updateVarRow();
}, {deep: true});

// 监听token值的变化
watch(() => formData.value.token, () => {
  // 当token值变化时重新提取变量
  extractVariables();
});

// 验证API名称是否为英文
function validateName(value) {
  if (value && !/^[a-zA-Z0-9\s\-_]+$/.test(value)) {
    nameError.value = t('page.apimarket.form_name_english_only');
  } else {
    nameError.value = '';
  }
}

function loadApiDetail(id) {
  loading.value = true;
  proxy.$api.apimarket.detail(id).then((res) => {
    if (res.success) {
      formData.value = res.data;
      // 加载已有的变量配置
      loadExistingVars();
      // 提取变量
      extractVariables();
      // 验证已有的名称
      validateName(formData.value.name);
    }
  }).finally(() => {
    loading.value = false;
  });
}

// 从varRow加载已有的变量配置
function loadExistingVars() {
  if (formData.value.varRow) {
    try {
      const varObj = JSON.parse(formData.value.varRow);
      const vars = [];
      for (const key in varObj) {
        vars.push({
          name: key,
          value: varObj[key].value || '',
          desc: varObj[key].desc || ''
        });
      }
      extractedVars.value = vars;
    } catch (e) {
      console.error('解析varRow失败:', e);
    }
  }
}

// 提取变量
function extractVariables() {
  // 修正正则表达式以匹配 {{var}}、${{var}}、{var} 和 $var 格式
  const varPattern = /\$?\{\{([^}]+)\}\}|\$?\{([^}]+)\}|\$([a-zA-Z][a-zA-Z0-9_]*)/g;
  const allVars = new Set();

  // 从url、headers、bodyTemplate中提取变量
  const sources = [
    formData.value.url || '',
    formData.value.headers || '',
    formData.value.bodyTemplate || ''
  ];

  sources.forEach(source => {
    // 重置正则表达式的lastIndex，确保能够匹配所有变量
    varPattern.lastIndex = 0;
    let match;
    while ((match = varPattern.exec(source)) !== null) {
      // 处理多种匹配格式：{{var}}/${{var}} 格式 (match[1])、{var} 格式 (match[2]) 和 $var 格式 (match[3])
      const varName = (match[1] || match[2] || match[3]).trim();
      if (varName && varName !== 'token') { // 排除token变量
        allVars.add(varName);
      }
    }
  });

  // 更新extractedVars，保留已有的值和描述
  const existingVarsMap = {};
  extractedVars.value.forEach(v => {
    existingVarsMap[v.name] = v;
  });

  const newVars = [];
  allVars.forEach(varName => {
    if (existingVarsMap[varName]) {
      // 保留已有的配置
      newVars.push(existingVarsMap[varName]);
    } else {
      // 新变量
      newVars.push({
        name: varName,
        value: '',
        desc: ''
      });
    }
  });

  extractedVars.value = newVars;

  // 同步更新formData.varRow
  updateVarRow();
}

// 更新varRow字段
function updateVarRow() {
  const varObj = {};
  extractedVars.value.forEach(v => {
    varObj[v.name] = {
      value: v.value,
      desc: v.desc
    };
  });
  formData.value.varRow = JSON.stringify(varObj, null, 2);
}

// 处理认证类型变化
function handleAuthTypeChange(value) {
  if (value === 'Bearer') {
    // 解析现有的headers
    let headersObj = {};
    if (formData.value.headers) {
      try {
        headersObj = JSON.parse(formData.value.headers);
      } catch (e) {
        // 如果不是JSON格式，创建新对象
        headersObj = {};
      }
    }

    // 添加Authorization头
    headersObj['Authorization'] = 'Bearer ${{token}}';

    // 更新headers
    formData.value.headers = JSON.stringify(headersObj, null, 2);

    // 触发变量提取
    extractVariables();
  } else if (value === 'None') {
    // 移除Authorization头
    if (formData.value.headers) {
      try {
        const headersObj = JSON.parse(formData.value.headers);
        delete headersObj['Authorization'];
        formData.value.headers = JSON.stringify(headersObj, null, 2);

        // 触发变量提取
        extractVariables();
      } catch (e) {
        // 忽略解析错误
      }
    }

    // 清空token
    formData.value.token = '';
  }
}

// 格式化JSON显示
function formatJson(jsonString) {
  try {
    const parsed = JSON.parse(jsonString);
    return JSON.stringify(parsed, null, 2);
  } catch (e) {
    return jsonString;
  }
}

// 替换模板变量的函数
function replaceTemplateVars(str, varValues) {
  if (!str || typeof str !== 'string') return str;
  
  // 替换变量格式，支持 {{var}}、${{var}}、{var} 和 $var 格式
  let result = str.replace(/\$?\{\{([^}]+)\}\}|\$?\{([^}]+)\}|\$([a-zA-Z0-9_]+)/g, (match, curlyBraceVar, singleBraceVar, dollarVar) => {
    const varName = (curlyBraceVar || singleBraceVar || dollarVar).trim();
    
    // 如果是token变量，使用formData中的token值
    if (varName === 'token' && formData.value.token) {
      return encodeURIComponent(formData.value.token); // URL编码token值
    }
    
    // 对于其他变量，使用varValues中的值
    if (varValues && varValues[varName] !== undefined && varValues[varName] !== '') {
      return encodeURIComponent(varValues[varName]); // URL编码变量值
    }
    
    // 如果没有找到变量值，返回原字符串
    return match;
  });
  
  // 最后清理任何可能的不完整模板格式
  // 移除任何未完成的模板格式，如"{search"或"${search"等
  result = result.replace(/\$?\{[^}]*$/, ""); // 移除以{或${开头但未闭合的部分（在字符串末尾）
  result = result.replace(/\$[a-zA-Z0-9_]*$/, ""); // 移除以$开头但未完成的变量（在字符串末尾）
  
  return result;
}

// 测试API功能
async function testApi() {
  // 表单验证
  if (!formData.value.name || !formData.value.description || !formData.value.url) {
    proxy.$modal.msgWarning(t('page.apimarket.form_required_fields'));
    return;
  }
  
  // 验证API名称必须为英文
  if (nameError.value) {
    proxy.$modal.msgWarning(nameError.value);
    return;
  }

  testing.value = true;
  testResult.value = null;

  try {
    // 准备测试数据，使用formData的完整副本
    const testData = { ...formData.value };
    
    // 创建变量映射对象，包含变量名和其值
    const varValues = {};
    if (extractedVars.value && extractedVars.value.length > 0) {
      extractedVars.value.forEach(varItem => {
        varValues[varItem.name] = varItem.value || '';
      });
    }
    
    console.log('原始请求头:', testData.headers);
    console.log('提取的变量:', extractedVars.value);
    console.log('变量映射:', varValues);
    
    // 前端替换参数
    if (testData.url) {
      testData.url = replaceTemplateVars(testData.url, varValues);
    }
    
    // 特别处理请求头，确保变量被正确替换
    if (testData.headers) {
      // 如果headers是JSON字符串，先解析为对象再替换变量，最后转回字符串
      let processedHeaders = testData.headers;
      try {
        const headersObj = JSON.parse(testData.headers);
        // 遍历header对象，对每个值进行变量替换
        for (const headerName in headersObj) {
          if (typeof headersObj[headerName] === 'string') {
            headersObj[headerName] = replaceTemplateVars(headersObj[headerName], varValues);
          }
        }
        processedHeaders = JSON.stringify(headersObj, null, 2);
      } catch (e) {
        // 如果不是JSON格式，直接进行字符串替换
        processedHeaders = replaceTemplateVars(testData.headers, varValues);
      }
      testData.headers = processedHeaders;
    }
    
    console.log('处理后的请求头:', testData.headers);
    
    if (testData.bodyTemplate) {
      testData.bodyTemplate = replaceTemplateVars(testData.bodyTemplate, varValues);
    }
    
    console.log('发送到后端的测试数据:', testData); // 调试日志
    
    // 发起请求到后端测试接口
    const res = await proxy.$api.apimarket.test(testData);
    
    if (res.success && res.data) {
      testResult.value = res.data;
      showTestResult.value = true;
    } else {
      proxy.$modal.msgError(res.message || t('page.apimarket.document_parse_failed'));
    }
  } catch (err) {
    console.error('测试API时发生错误:', err);
    proxy.$modal.msgError(t('page.dynamicapi.request_failed_prefix') + (err.message || '未知错误'));
  } finally {
    testing.value = false;
  }
}

function handleSubmit() {
  // 表单验证
  if (!formData.value.name || !formData.value.description || !formData.value.url) {
    proxy.$modal.msgWarning(t('page.apimarket.form_required_fields'));
    return;
  }
  
  // 验证API名称必须为英文
  if (nameError.value) {
    proxy.$modal.msgWarning(nameError.value);
    return;
  }

  submitting.value = true;
  const apiCall = isEdit.value
      ? proxy.$api.apimarket.update(formData.value)
      : proxy.$api.apimarket.add(formData.value);

  apiCall.then((res) => {
    if (res.success) {
      proxy.$modal.msgSuccess(
          isEdit.value ? t('page.apimarket.update_success') : t('page.apimarket.add_success')
      );
      goBack();
    }
  }).finally(() => {
    submitting.value = false;
  });
}

function removeVariable(index) {
  extractedVars.value.splice(index, 1);
}

function goBack() {
  proxy.$router.push({path: '/apimarket'});
}
</script>

<style scoped>
.el-loading-mask {
  backdrop-filter: blur(6px);
  background-color: rgba(15, 20, 35, 0.5);
}
</style>