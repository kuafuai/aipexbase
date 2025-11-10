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

          <el-form-item :label="t('page.apimarket.form_name')" required>
            <el-input v-model="formData.name" :placeholder="t('page.apimarket.form_name_placeholder')" />
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_description')" required>
            <el-input v-model="formData.description" type="textarea" :rows="3"
                      :placeholder="t('page.apimarket.form_description_placeholder')" />
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_category')">
            <el-input v-model="formData.category" :placeholder="t('page.apimarket.form_category_placeholder')" />
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_status')">
            <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>

        <!-- API配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_api_config') }}</h2>

          <el-form-item :label="t('page.apimarket.form_url')" required>
            <el-input v-model="formData.url" :placeholder="t('page.apimarket.form_url_placeholder')"
                      @input="extractVariables" />
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_protocol')" required>
            <el-select v-model="formData.protocol" :placeholder="t('page.apimarket.form_protocol_placeholder')">
              <el-option label="HTTP" :value="1" />
              <el-option label="WSS" :value="2" />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_method')" required>
            <el-select v-model="formData.method" :placeholder="t('page.apimarket.form_method_placeholder')">
              <el-option label="GET" :value="0" />
              <el-option label="POST" :value="1" />
<!--              <el-option label="PUT" :value="2" />-->
<!--              <el-option label="DELETE" :value="3" />-->
<!--              <el-option label="PATCH" :value="4" />-->
            </el-select>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_headers')">
            <el-input v-model="formData.headers" type="textarea" :rows="4"
                      :placeholder="t('page.apimarket.form_headers_placeholder')"
                      @input="extractVariables" />
          </el-form-item>
        </div>

        <!-- 认证配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_auth_config') }}</h2>

          <el-form-item :label="t('page.apimarket.form_auth_type')">
            <el-select v-model="formData.authType" :placeholder="t('page.apimarket.form_auth_type_placeholder')"
                       @change="handleAuthTypeChange">
              <el-option label="None" value="None" />
              <el-option label="Bearer" value="Bearer" />
            </el-select>
          </el-form-item>

          <el-form-item v-show="false" :label="t('page.apimarket.form_auth_config_value')">
            <el-input v-model="formData.authConfig" type="textarea" :rows="3"
                      :placeholder="t('page.apimarket.form_auth_config_placeholder')" />
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_token')">
            <el-input v-model="formData.token" :placeholder="t('page.apimarket.form_token_placeholder')" />
          </el-form-item>
        </div>

        <!-- 请求体配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_body_config') }}</h2>

          <el-form-item :label="t('page.apimarket.form_body_type')">
            <el-select v-model="formData.bodyType" :placeholder="t('page.apimarket.form_body_type_placeholder')">
              <el-option label="JSON" :value="0" />
              <el-option label="Form-Data" :value="1" />
              <el-option label="URL-Encoded" :value="2" />
              <el-option label="XML" :value="3" />
              <el-option label="Text" :value="4" />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_body_template')">
            <el-input v-model="formData.bodyTemplate" type="textarea" :rows="6"
                      :placeholder="t('page.apimarket.form_body_template_placeholder')"
                      @input="extractVariables" />
          </el-form-item>
        </div>

        <!-- 计价信息 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_pricing_info') }}</h2>

          <el-form-item :label="t('page.apimarket.form_pricing_model')">
            <el-select v-model="formData.pricingModel" :placeholder="t('page.apimarket.form_pricing_model_placeholder')">
              <el-option label="FREE" :value="0" />
              <el-option :label="t('page.apimarket.pricing_per_call')" :value="1" />
              <el-option :label="t('page.apimarket.pricing_per_token')" :value="2" />
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
        </div>

        <!-- 响应数据配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-6">{{ t('page.apimarket.form_response_config') }}</h2>

          <el-form-item :label="t('page.apimarket.form_data_path')">
            <el-input v-model="formData.dataPath" :placeholder="t('page.apimarket.form_data_path_placeholder')" />
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_data_type')">
            <el-select v-model="formData.dataType" :placeholder="t('page.apimarket.form_data_type_placeholder')">
              <el-option :label="t('page.apimarket.data_type_text')" :value="0" />
              <el-option :label="t('page.apimarket.data_type_object')" :value="1" />
              <el-option :label="t('page.apimarket.data_type_array')" :value="2" />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_data_row')">
            <el-input v-model="formData.dataRow" type="textarea" :rows="4"
                      :placeholder="t('page.apimarket.form_data_row_placeholder')" />
          </el-form-item>

          <el-form-item :label="t('page.apimarket.form_var_row')">
            <div class="bg-black/30 rounded-lg p-4">
              <div v-if="extractedVars.length === 0" class="text-white/50 text-sm">
                {{ t('page.apimarket.no_variables_detected') }}
              </div>
              <div v-else class="space-y-3">
                <div v-for="(varItem, index) in extractedVars" :key="index"
                     class="bg-white/5 rounded-lg p-3 border border-white/10">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-cyan-400 font-mono text-sm">{{ varItem.name }}</span>
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
              @click="goBack"
              class="px-6 py-2.5 bg-white/5 rounded-lg border border-white/20 text-white/80 hover:bg-white/10 hover:border-white/30 transition-all duration-300"
          >
            {{ t('page.apimarket.cancel') }}
          </button>
          <button
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
</template>

<script setup>
const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const route = useRoute();

const loading = ref(false);
const submitting = ref(false);
const isEdit = ref(false);

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
  unitPrice: 0
});

onMounted(() => {
  const id = route.params.id;
  if (id) {
    isEdit.value = true;
    loadApiDetail(id);
  }
});

// 监听变量值和描述的变化
watch(extractedVars, () => {
  updateVarRow();
}, { deep: true });

function loadApiDetail(id) {
  loading.value = true;
  proxy.$api.apimarket.detail(id).then((res) => {
    if (res.success) {
      formData.value = res.data;
      // 加载已有的变量配置
      loadExistingVars();
      // 提取变量
      extractVariables();
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
  const varPattern = /\$?\{\{([^}]+)\}\}/g;
  const allVars = new Set();

  // 从url、headers、bodyTemplate中提取变量
  const sources = [
    formData.value.url || '',
    formData.value.headers || '',
    formData.value.bodyTemplate || ''
  ];

  sources.forEach(source => {
    let match;
    while ((match = varPattern.exec(source)) !== null) {
      allVars.add(match[1].trim());
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

function handleSubmit() {
  // 表单验证
  if (!formData.value.name || !formData.value.description || !formData.value.url) {
    proxy.$modal.msgWarning(t('page.apimarket.form_required_fields'));
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
