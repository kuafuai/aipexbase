<template>
  <div class="p-6 flex justify-center">
    <div class="max-w-4xl w-full">
      <!-- 顶部导航 -->
      <div class="flex items-center mb-6">
        <el-button
            class="back-button"
            @click="goBack"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/>
          </svg>
          {{ t('page.apimarket.back') }}
        </el-button>
      </div>

      <div v-loading="loading" class="max-w-4xl">
        <!-- API基本信息 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <div class="flex items-start justify-between mb-6">
            <div>
              <h1 class="text-3xl font-bold text-white mb-2">{{ apiInfo.name }}</h1>
              <p class="text-white/60">{{ apiInfo.description }}</p>
            </div>
            <div v-if="apiInfo.owner" class="flex items-center space-x-3">
              <el-button
                  class="edit-button"
                  @click="editApi"
              >
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                </svg>
                {{ t('page.apimarket.edit') }}
              </el-button>
              <el-button
                  class="delete-button"
                  @click="deleteApi"
              >
                <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
                {{ t('page.apimarket.delete') }}
              </el-button>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-6">
            <div>
              <h3 class="text-sm text-white/50 mb-1">{{ t('page.apimarket.detail_category') }}</h3>
              <p class="text-white">{{ apiInfo.category || t('page.apimarket.default_category') }}</p>
            </div>
            <div>
              <h3 class="text-sm text-white/50 mb-1">{{ t('page.apimarket.detail_method') }}</h3>
              <p class="text-white">{{ getMethodText(apiInfo.method) }}</p>
            </div>
            <div>
              <h3 class="text-sm text-white/50 mb-1">{{ t('page.apimarket.detail_protocol') }}</h3>
              <p class="text-white">{{ getProtocolText(apiInfo.protocol) }}</p>
            </div>
            <div>
              <h3 class="text-sm text-white/50 mb-1">{{ t('page.apimarket.detail_status') }}</h3>
              <span :class="apiInfo.status === 1 ? 'text-green-400' : 'text-gray-400'">
              {{ apiInfo.status === 1 ? t('page.apimarket.status_active') : t('page.apimarket.status_inactive') }}
            </span>
            </div>
            <div>
              <h3 class="text-sm text-white/50 mb-1">{{ t('page.apimarket.detail_pricing_model') }}</h3>
              <p class="text-white">{{ getPricingModelText(apiInfo.pricingModel) }}</p>
            </div>
            <div v-if="apiInfo.pricingModel !== 0">
              <h3 class="text-sm text-white/50 mb-1">{{ t('page.apimarket.detail_unit_price') }}</h3>
              <p class="text-white">{{ formatPrice(apiInfo.unitPrice) }}</p>
            </div>
          </div>
        </div>

        <!-- API地址 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-4">{{ t('page.apimarket.detail_url') }}</h2>
          <div class="bg-black/30 rounded-lg p-4 font-mono text-sm text-cyan-400">
            {{ apiInfo.url }}
          </div>
        </div>

        <!-- 认证配置 -->
        <div v-if="apiInfo.authType" class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-4">{{ t('page.apimarket.detail_auth') }}</h2>
          <div>
            <h3 class="text-sm text-white/50 mb-2">{{ t('page.apimarket.detail_auth_type') }}</h3>
            <p class="text-white mb-4">{{ apiInfo.authType }}</p>
            <h3 class="text-sm text-white/50 mb-2">{{ t('page.apimarket.detail_auth_config') }}</h3>
            <div class="bg-black/30 rounded-lg p-4 font-mono text-sm text-white/80">
              {{ apiInfo.authConfig || '-' }}
            </div>
          </div>
        </div>

        <!-- 请求头 -->
        <div v-if="apiInfo.headers" class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-4">{{ t('page.apimarket.detail_headers') }}</h2>
          <div class="bg-black/30 rounded-lg p-4 font-mono text-sm text-white/80 whitespace-pre-wrap">
            {{ apiInfo.headers }}
          </div>
        </div>

        <!-- 请求体 -->
        <div v-if="apiInfo.bodyTemplate"
             class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10 mb-6">
          <h2 class="text-xl font-bold text-white mb-4">{{ t('page.apimarket.detail_body') }}</h2>
          <div>
            <h3 class="text-sm text-white/50 mb-2">{{ t('page.apimarket.detail_body_type') }}</h3>
            <p class="text-white mb-4">{{ getBodyTypeText(apiInfo.bodyType) }}</p>
            <h3 class="text-sm text-white/50 mb-2">{{ t('page.apimarket.detail_body_template') }}</h3>
            <div class="bg-black/30 rounded-lg p-4 font-mono text-sm text-white/80 whitespace-pre-wrap">
              {{ apiInfo.bodyTemplate }}
            </div>
          </div>
        </div>

        <!-- 响应数据配置 -->
        <div class="bg-white/5 backdrop-blur-md rounded-2xl p-8 border border-white/10">
          <h2 class="text-xl font-bold text-white mb-4">{{ t('page.apimarket.detail_response') }}</h2>
          <div class="space-y-4">
            <div v-if="apiInfo.dataPath">
              <h3 class="text-sm text-white/50 mb-2">{{ t('page.apimarket.detail_data_path') }}</h3>
              <p class="text-white font-mono text-sm">{{ apiInfo.dataPath }}</p>
            </div>
            <div v-if="apiInfo.dataType">
              <h3 class="text-sm text-white/50 mb-2">{{ t('page.apimarket.detail_data_type') }}</h3>
              <p class="text-white">{{ getDataTypeText(apiInfo.dataType) }}</p>
            </div>
            <div v-if="apiInfo.dataRow">
              <h3 class="text-sm text-white/50 mb-2">{{ t('page.apimarket.detail_data_row') }}</h3>
              <div class="bg-black/30 rounded-lg p-4 font-mono text-sm text-white/80 whitespace-pre-wrap">
                {{ apiInfo.dataRow }}
              </div>
            </div>
            <div v-if="apiInfo.varRow">
              <h3 class="text-sm text-white/50 mb-2">{{ t('page.apimarket.detail_var_row') }}</h3>
              <div class="bg-black/30 rounded-lg p-4 font-mono text-sm text-white/80 whitespace-pre-wrap">
                {{ apiInfo.varRow }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const route = useRoute();

const loading = ref(false);
const apiInfo = ref({
  id: null,
  name: '',
  description: '',
  category: '',
  url: '',
  method: 0,
  protocol: 0,
  authType: '',
  authConfig: '',
  token: '',
  bodyType: 0,
  owner: false,
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
    loadApiDetail(id);
  }
});

function loadApiDetail(id) {
  loading.value = true;
  proxy.$api.apimarket.detail(id).then((res) => {
    if (res.success) {
      apiInfo.value = res.data;
    }
  }).finally(() => {
    loading.value = false;
  });
}

function getMethodText(method) {
  const methods = {
    0: 'GET',
    1: 'POST',
    2: 'PUT',
    3: 'DELETE',
    4: 'PATCH'
  };
  return methods[method] || 'GET';
}

function getProtocolText(protocol) {
  const protocols = {
    1: 'HTTP',
    2: 'WSS'
  };
  return protocols[protocol] || 'HTTP';
}

function getBodyTypeText(bodyType) {
  const types = {
    0: 'JSON',
    1: 'Form-Data',
    2: 'URL-Encoded',
    3: 'XML',
    4: 'Text'
  };
  return types[bodyType] || 'JSON';
}

function getDataTypeText(dataType) {
  const types = {
    0: t('page.apimarket.data_type_text'),
    1: t('page.apimarket.data_type_object'),
    2: t('page.apimarket.data_type_array')
  };
  return types[dataType] || types[0];
}

function getPricingModelText(pricingModel) {
  const models = {
    0: 'FREE',
    1: t('page.apimarket.pricing_per_call'),
    2: t('page.apimarket.pricing_per_token')
  };
  return models[pricingModel] || 'FREE';
}

function formatPrice(price) {
  if (!price && price !== 0) return '-';
  return `¥${Number(price).toFixed(2)}`;
}

function goBack() {
  proxy.$router.push({path: '/apimarket'});
}

function editApi() {
  proxy.$router.push({name: 'ApiMarketEdit', params: {id: apiInfo.value.id}});
}

function deleteApi() {
  proxy.$modal.confirm(
      t('page.apimarket.delete_confirm_message', {name: apiInfo.value.name}),
      t('page.apimarket.delete_confirm_title'),
      {
        confirmButtonText: t('page.apimarket.delete_confirm_ok'),
        cancelButtonText: t('page.apimarket.delete_confirm_cancel'),
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
  ).then(() => {
    proxy.$api.apimarket.delete(apiInfo.value.id).then((res) => {
      if (res.success) {
        proxy.$modal.msgSuccess(t('page.apimarket.delete_success'));
        goBack();
      }
    });
  }).catch(() => {
    // 用户取消删除
  });
}
</script>

<style scoped>
.el-loading-mask {
  backdrop-filter: blur(6px);
  background-color: rgba(15, 20, 35, 0.5);
}

/* 返回按钮 */
.back-button {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.back-button:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  color: #fff;
  transform: translateX(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.back-button:active {
  transform: translateX(-2px);
}

/* 编辑按钮 */
.edit-button {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(37, 99, 235, 0.15));
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 12px;
  color: #60a5fa;
  font-weight: 500;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.edit-button:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25), rgba(37, 99, 235, 0.25));
  border-color: rgba(59, 130, 246, 0.5);
  color: #93c5fd;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.2);
}

.edit-button:active {
  transform: translateY(0);
}

/* 删除按钮 */
.delete-button {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.15), rgba(220, 38, 38, 0.15));
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 12px;
  color: #f87171;
  font-weight: 500;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.delete-button:hover {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.25), rgba(220, 38, 38, 0.25));
  border-color: rgba(239, 68, 68, 0.5);
  color: #fca5a5;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(239, 68, 68, 0.2);
}

.delete-button:active {
  transform: translateY(0);
}

/* 按钮图标动画 */
.back-button svg,
.edit-button svg,
.delete-button svg {
  transition: transform 0.3s ease;
}

.back-button:hover svg {
  transform: translateX(-2px);
}

.edit-button:hover svg {
  transform: rotate(15deg);
}

.delete-button:hover svg {
  transform: scale(1.1);
}
</style>
