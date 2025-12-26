<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-8">
      <div>
        <h2 class="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
          {{ t('page.apimarket.list_title') }}
        </h2>
        <p class="text-white/60 mt-2">{{ t('page.apimarket.list_sub') }}</p>
      </div>
      <div class="flex space-x-4">
        <el-button
            type="primary"
            class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
            @click="showDocumentDialog = true"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"/>
          </svg>
          {{ t('page.apimarket.ai_add_api') }}
        </el-button>
        <el-button
            type="primary"
            class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
            @click="goToAddApi"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          {{ t('page.apimarket.add_api') }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div
          v-for="api in pageRes.records"
          :key="api.id"
          class="group relative bg-white/5 backdrop-blur-md rounded-2xl p-6 cursor-pointer border border-white/10 hover:border-cyan-400/30 transition-all duration-500 hover:scale-105 hover:shadow-2xl hover:shadow-cyan-500/20"
          @click="viewApiDetail(api)"
      >
        <!-- 悬停光效 -->
        <div
            class="absolute inset-0 bg-gradient-to-r from-cyan-500/0 via-cyan-500/5 to-cyan-500/0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 rounded-2xl"></div>
        <!-- API图标 -->
        <div
            class="relative z-10 w-12 h-12 bg-gradient-to-br from-purple-500 to-pink-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
          <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
          </svg>
        </div>

        <h2 class="text-lg font-semibold mb-2 relative z-10 text-white group-hover:text-cyan-300 transition-colors duration-300">
          {{ api.name }}
        </h2>
        <p class="text-sm text-white/70 mb-4 relative z-10 line-clamp-2">
          {{ api.description }}
        </p>

        <div class="relative z-10 flex items-center justify-between text-sm text-white/60">
          <span class="flex items-center space-x-1">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/>
            </svg>
            <span>{{ api.category || t('page.apimarket.default_category') }}</span>
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
                d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
        </svg>
      </div>
      <h3 class="text-xl font-semibold text-white/80 mb-2">{{ t('page.apimarket.empty_title') }}</h3>
      <p class="text-white/60 mb-6">{{ t('page.apimarket.empty_desc') }}</p>
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

    <!-- 文档粘贴弹窗 -->
    <el-dialog
      v-model="showDocumentDialog"
      :title="t('page.apimarket.import_from_document')"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="flex flex-col space-y-4">
        <el-input
          v-model="documentContent"
          type="textarea"
          :rows="10"
          :placeholder="t('page.apimarket.paste_document_tip')"
        />
        <div class="flex justify-end">
          <el-button 
            type="primary" 
            :disabled="!documentContent || parsing"
            @click="parseDocumentContent"
            class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
          >
            <span v-if="parsing" class="flex items-center space-x-2">
              <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ t('page.apimarket.parsing_content') }}</span>
            </span>
            <span v-else>{{ t('page.apimarket.parse_content') }}</span>
          </el-button>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDocumentDialog = false">{{ t('page.apimarket.cancel') }}</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const t = proxy.$tt;

const loading = ref(false)
const parsing = ref(false);
const documentContent = ref('');
const showDocumentDialog = ref(false);

const pageParams = ref({current: 1, pageSize: 12});
// 分页响应数据
const pageRes = ref({current: 1, pages: 1, size: 10, total: 0, records: []});

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
  proxy.$api.apimarket.pages(data).then((res) => {
    pageRes.value = res.data;
  }).finally(() => (loading.value = false))
}

function handlePageChange(val) {
  pageParams.value.current = val;
  getApiData();
}

function viewApiDetail(api) {
  proxy.$router.push({name: 'ApiMarketDetail', params: {id: api.id}});
}

function goToAddApi() {
  proxy.$router.push({name: 'ApiMarketAdd'});
}

// 替换模板变量的函数，与add.vue保持一致
function replaceTemplateVars(str, varValues) {
  if (!str || typeof str !== 'string') return str;

  // 替换变量格式，支持 {{var}}、${{var}}、{var} 和 $var 格式
  let result = str.replace(/\$?\{\{([^}]+)\}\}|\$?\{([^}]+)\}|\$([a-zA-Z0-9_]+)/g, (match, curlyBraceVar, singleBraceVar, dollarVar) => {
    const varName = (curlyBraceVar || singleBraceVar || dollarVar).trim();

    // 如果是token变量，使用formData中的token值
    if (varName === 'token' && varValues['token']) {
      return encodeURIComponent(varValues['token']); // URL编码token值
    }
    
    // 对于其他变量，使用varValues中的值
    // 从varValues中获取值，varValues可能是字符串或对象
    let varValue = varValues[varName];
    if (varValue !== undefined && varValue !== '') {
      // 如果varValue是对象，取其value属性或直接值
      if (typeof varValue === 'object' && varValue.value !== undefined) {
        varValue = varValue.value;
      } else if (typeof varValue === 'object') {
        // 如果是对象但没有value属性，尝试获取第一个属性值
        const keys = Object.keys(varValue);
        if (keys.length > 0) {
          varValue = varValue[keys[0]];
        }
      }
      return encodeURIComponent(varValue); // URL编码变量值
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

// 解析文档内容
async function parseDocumentContent() {
  if (!documentContent.value || documentContent.value.trim() === '') {
    proxy.$modal.msgWarning(t('page.apimarket.content_required'));
    return;
  }

  parsing.value = true;
  try {
    const res = await proxy.$api.apimarket.parseDocumentContent(documentContent.value);
    if (res.success) {
      // 解析成功后直接测试API
      if (!res.data || !res.data.parsedData || !res.data.apiMarketData) {
        proxy.$modal.msgError('解析API文档失败：返回的数据格式不正确');
        return;
      }
      
      const parsedData = res.data.parsedData;
      const apiMarketData = res.data.apiMarketData;
      
      // 准备测试数据
      let testData = { ...apiMarketData };
      
      // 创建变量映射对象，包含变量名和其值
      let varValues = {};
      if (apiMarketData.varRow) {
        try {
          const varObj = JSON.parse(apiMarketData.varRow);
          for (const key in varObj) {
            // 修复变量映射逻辑，确保处理包含value和desc的对象格式
            const varValue = varObj[key];
            if (typeof varValue === 'object' && varValue.value !== undefined) {
              varValues[key] = varValue.value;
            } else {
              varValues[key] = varValue;
            }
          }
        } catch (e) {
          console.error('解析varRow失败:', e);
        }
      }

      // 前端替换参数，类似于add.vue中的testApi方法
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
      
      if (testData.bodyTemplate) {
        testData.bodyTemplate = replaceTemplateVars(testData.bodyTemplate, varValues);
      }
      
      // 检查后端的测试结果
      if (res.data && res.data.testResult) {
        // 使用后端的测试结果
        if (res.data.testResult.success) {
          // 如果测试成功且已保存
          if (res.data.saved === true) {
            proxy.$modal.msgSuccess(t('page.apimarket.add_success'));
            // 清空文档内容
            documentContent.value = '';
            // 关闭弹窗
            showDocumentDialog.value = false;
            // 刷新列表
            refresh();
          }
        } else {
          // 测试失败，显示错误信息和缺失字段
          let errorMessage = res.data.testResult.message || t('page.apimarket.test_failed');
          if (res.data.missingFields && res.data.missingFields.length > 0) {
            errorMessage += '。可能缺少以下字段：' + res.data.missingFields.join(', ');
          }
          proxy.$modal.msgError(errorMessage);
          
          // 不再跳转到添加页面，只关闭弹窗
          showDocumentDialog.value = false;
        }
      } else {
        // 如果后端没有返回测试结果，进行前端测试
        const testRes = await proxy.$api.apimarket.test(testData);
        if (testRes.success && testRes.data && testRes.data.success) {
          // 测试成功，自动保存API
          const saveRes = await proxy.$api.apimarket.add(testData);
          if (saveRes.success) {
            proxy.$modal.msgSuccess(t('page.apimarket.add_success'));
            // 清空文档内容
            documentContent.value = '';
            // 关闭弹窗
            showDocumentDialog.value = false;
            // 刷新列表
            refresh();
          } else {
            proxy.$modal.msgError(saveRes.message || t('page.apimarket.document_parse_failed'));
          }
        } else {
          // 测试失败，显示错误信息
          proxy.$modal.msgError(testRes.message || t('page.apimarket.test_failed'));
          
          // 不再跳转到添加页面，只关闭弹窗
          showDocumentDialog.value = false;
        }
      }
    } else {
      proxy.$modal.msgError(res.message || t('page.apimarket.document_parse_failed'));
    }
  } catch (err) {
    console.error('解析API文档时发生错误:', err);
    // 更好地处理不同类型的错误
    if (err.response && err.response.status === 405) {
      proxy.$modal.msgError('后端接口配置错误：请求方法不被允许 (405)');
    } else if (err.message) {
      proxy.$modal.msgError(t('page.apimarket.document_parse_failed') + ': ' + err.message);
    } else {
      proxy.$modal.msgError(t('page.apimarket.document_parse_failed'));
    }
  } finally {
    parsing.value = false;
  }
}

</script>

<style scoped>

.el-loading-mask {
  backdrop-filter: blur(6px);
  background-color: rgba(15, 20, 35, 0.5);
}

</style>