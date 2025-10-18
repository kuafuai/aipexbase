<template>

  <div class="space-y-6">

    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">

      <div class="p-6 border-b border-white/10">
        <div class="flex items-center justify-between">

          <div class="flex items-center space-x-3">
            <div class="w-3 h-3 bg-cyan-400 rounded-full animate-pulse"></div>
            <h2 class="text-xl font-semibold text-cyan-300">登录配置</h2>
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

        <div class="flex items-center gap-6">
          <div class="flex items-center space-x-3 min-w-48">
            <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
              <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
            </div>
            <span class="text-white/80 font-medium">是否开启登录</span>
          </div>
          <el-switch
              v-model="appInfoConfig.needAuth"
              active-color="#06b6d4"
              class="custom-switch"
          />
        </div>

        <div v-if="appInfoConfig.needAuth" class="flex items-center gap-6">
          <div class="flex items-center space-x-3 min-w-48">
            <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
              <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
              </svg>
            </div>
            <span class="text-white/80 font-medium">登录关联业务</span>
          </div>
          <el-select
              v-model="appInfoConfig.authTable"
              :options="tables"
              :props="props"
              class="custom-input"
              style="width: 300px;"
              size="large"
              placeholder="请选择关联业务"
          />
        </div>


        <div v-if="appInfoConfig.needAuth"
             class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
          <div class="p-6 border-b border-white/10">
            <div class="flex justify-between items-center">
              <div class="flex items-center space-x-3">
                <div class="w-3 h-3 bg-green-400 rounded-full animate-pulse"></div>
                <h2 class="text-xl font-semibold text-green-300">微信授权登录配置</h2>
              </div>
            </div>
          </div>

          <div class="p-6 space-y-6">
            <div class="space-y-4">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/>
                  </svg>
                </div>
                <span class="text-white/80 font-medium min-w-32">微信服务号APPID</span>
                <el-input
                    v-model="weChatConfig['wx.pay.mp-app-id']"
                    placeholder="微信服务号APPID"
                    class="custom-input"
                    style="max-width: 400px;"
                    size="large"
                />
              </div>

              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                  </svg>
                </div>
                <span class="text-white/80 font-medium min-w-32">微信服务号SECRET</span>
                <el-input
                    v-model="weChatConfig['wx.pay.mp-app-secret']"
                    placeholder="微信服务号SECRET"
                    class="custom-input"
                    style="max-width: 400px;"
                    size="large"
                />
              </div>
            </div>
          </div>
        </div>

        <div v-if="appInfoConfig.needAuth"
             class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
          <div class="p-6 border-b border-white/10">
            <div class="flex justify-between items-center">
              <div class="flex items-center space-x-3">
                <div class="w-3 h-3 bg-blue-400 rounded-full animate-pulse"></div>
                <h2 class="text-xl font-semibold text-blue-300">邮箱登录配置</h2>
              </div>
            </div>
          </div>

          <div class="p-6 space-y-6">
            <div class="space-y-4">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                  </svg>
                </div>
                <span class="text-white/80 font-medium min-w-32">SMTP服务器地址</span>
                <el-input
                    v-model="emailConfig['mail.host']"
                    placeholder="例如：smtp.qq.com"
                    class="custom-input"
                    style="max-width: 400px;"
                    size="large"
                />
              </div>

              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                  </svg>
                </div>
                <span class="text-white/80 font-medium min-w-32">端口号</span>
                <el-input
                    v-model="emailConfig['mail.port']"
                    placeholder="例如：465"
                    class="custom-input"
                    style="max-width: 200px;"
                    size="large"
                />
              </div>

              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                  </svg>
                </div>
                <span class="text-white/80 font-medium min-w-32">发件邮箱</span>
                <el-input
                    v-model="emailConfig['mail.user']"
                    placeholder="例如：admin@example.com"
                    class="custom-input"
                    style="max-width: 400px;"
                    size="large"
                />
              </div>

              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                  </svg>
                </div>
                <span class="text-white/80 font-medium min-w-32">授权码/密码</span>
                <el-input
                    v-model="emailConfig['mail.passwd']"
                    type="password"
                    placeholder="请输入授权码"
                    class="custom-input"
                    style="max-width: 400px;"
                    size="large"
                    show-password
                />
              </div>

              <div class="flex items-start space-x-3">
                <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center mt-2">
                  <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                </div>
                <div class="flex-1">
                  <span class="text-white/80 font-medium block mb-2">验证码模版</span>
                  <el-input
                      v-model="emailConfig['login.mail.code_template']"
                      placeholder="验证码模版,可以在合适的位置放入变量${{code}}"
                      type="textarea"
                      :rows="3"
                      class="custom-input"
                      style="max-width: 400px;"
                  />
                </div>
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
const appId = proxy.$route.params.id;

const props = {
  value: 'tableName',
  label: 'description',
  options: 'options',
  disabled: 'disabled',
}

const saving = ref(false);

const appInfoConfig = ref({});
const appConfigJson = ref({});
const tables = ref([])

const weChatConfig = ref({});
const emailConfig = ref({});

onMounted(() => {
  fetchAppInfo();
  loadTables();
  fetchSettings();
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

const fetchSettings = async () => {
  proxy.$api.setting.settings(appId).then((res) => {
    if (res.success) {
      const map = Object.fromEntries(
          res.data.map(item => [item.name, item.content])
      );

      getMailSetting(map);
      getWeChatSetting(map);
    }
  })
}

function getMailSetting(map) {
  const keys = ["mail.host", "mail.user", "mail.passwd", "mail.port", "login.mail.code_template",];
  emailConfig.value = keys.reduce((obj, key) => {
    obj[key] = map[key];
    return obj;
  }, {});
}

function getWeChatSetting(map) {
  const keys = ["wx.pay.mp-app-id", "wx.pay.mp-app-secret"];
  weChatConfig.value = keys.reduce((obj, key) => {
    obj[key] = map[key];
    return obj;
  }, {});
}

async function saveLoginConfig() {
  saving.value = true;
  try {
    appInfoConfig.value.configJson = JSON.stringify(appConfigJson.value);
    await proxy.$api.project.update(appId, appInfoConfig.value);
    if (appInfoConfig.value.needAuth) {
      await proxy.$api.setting.saveSetting(appId, emailConfig.value);
      await proxy.$api.setting.saveSetting(appId, weChatConfig.value);
    }
    proxy.$modal.msgSuccess('登录配置保存成功');
  } catch (error) {
    console.error('Failed to update app name:', error);
    proxy.$modal.msgError('保存失败');
  } finally {
    saving.value = false;
  }
  
}

</script>