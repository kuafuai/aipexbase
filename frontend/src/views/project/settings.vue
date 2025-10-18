<template>

  <div
      class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white flex relative overflow-hidden rounded-2xl">

    <div
        class="absolute inset-0 bg-[radial-gradient(ellipse_at_left,_var(--tw-gradient-stops))] from-cyan-500/5 via-transparent to-transparent"></div>

    <aside class="relative z-10 w-80 bg-white/5 backdrop-blur-xl border-r border-white/10 p-6 flex flex-col h-screen rounded-2xl shadow-xl shadow-white/5">

      <div class="flex items-center space-x-3 mb-8 px-2">
        <div class="w-8 h-8 bg-gradient-to-r from-cyan-400 to-blue-400 rounded-lg flex items-center justify-center">
          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
          </svg>
        </div>
        <div>
          <h1 class="text-xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
            {{ appInfoConfig.appName || '应用管理' }}
          </h1>
          <p class="text-xs text-white/40">应用配置中心</p>
        </div>
      </div>

      <nav class="flex flex-col space-y-2">

        <div class="px-4 py-2">
          <h3 class="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">应用管理</h3>

          <div class="space-y-1">
            <router-link
                :to="`/project/${appId}/settings/users`"
                class="group relative py-3 px-4 rounded-xl transition-all duration-300 flex items-center space-x-3"
                :class="{
                'bg-gradient-to-r from-cyan-500/20 to-blue-500/20 text-cyan-300 shadow-lg shadow-cyan-500/10': proxy.$route.path.includes('/users'),
                'text-white/70 hover:text-white hover:bg-white/5': !proxy.$route.path.includes('/users')
              }"
            >
              <!-- 激活状态指示器 -->
              <div
                  v-if="proxy.$route.path.includes('/users')"
                  class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-gradient-to-b from-cyan-400 to-blue-400 rounded-r-full"
              ></div>
              <span class="font-medium">登录用户</span>

            </router-link>
          </div>
        </div>

        <div class="px-4 py-2">
          <h3 class="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">配置中心</h3>
          <div class="space-y-1">
            <router-link
                v-for="item in configMenus"
                :key="item.path"
                :to="`/project/${appId}/${item.path}`"
                class="group relative py-3 px-4 rounded-xl transition-all duration-300 flex items-center space-x-3"
                :class="{
                'bg-gradient-to-r from-cyan-500/20 to-blue-500/20 text-cyan-300 shadow-lg shadow-cyan-500/10': proxy.$route.path.includes(item.path),
                'text-white/70 hover:text-white hover:bg-white/5': !proxy.$route.path.includes(item.path)
              }"
            >
              <!-- 激活状态指示器 -->
              <div
                  v-if="proxy.$route.path.includes(item.path)"
                  class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-gradient-to-b from-cyan-400 to-blue-400 rounded-r-full"
              ></div>


              <span class="font-medium">{{ item.label }}</span>


              <!-- 悬停箭头 -->
              <svg
                  class="w-4 h-4 ml-auto opacity-0 group-hover:opacity-100 transform -translate-x-1 group-hover:translate-x-0 transition-all duration-300"
                  :class="{
                  'text-cyan-400': proxy.$route.path.includes(item.path),
                  'text-white/50': !proxy.$route.path.includes(item.path)
                }"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </router-link>
          </div>
        </div>

        <div class="px-4 py-2">
          <h3 class="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">危险区域</h3>
          <div class="space-y-1">
            <router-link
                v-for="item in dangerMenus"
                :key="item.path"
                :to="`/project/${appId}/${item.path}`"
                class="group relative py-3 px-4 rounded-xl transition-all duration-300 flex items-center space-x-3"
                :class="{
                'bg-gradient-to-r from-red-500/20 to-pink-500/20 text-red-300 shadow-lg shadow-red-500/10': proxy.$route.path.includes(item.path),
                'text-red-400/70 hover:text-red-300 hover:bg-red-500/5': !proxy.$route.path.includes(item.path)
              }"
            >
              <!-- 激活状态指示器 -->
              <div
                  v-if="proxy.$route.path.includes(item.path)"
                  class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-gradient-to-b from-red-400 to-pink-400 rounded-r-full"
              ></div>

              <span class="font-medium">{{ item.label }}</span>

              <!-- 悬停箭头 -->
              <svg
                  class="w-4 h-4 ml-auto opacity-0 group-hover:opacity-100 transform -translate-x-1 group-hover:translate-x-0 transition-all duration-300"
                  :class="{
                  'text-red-400': proxy.$route.path.includes(item.path),
                  'text-red-400/50': !proxy.$route.path.includes(item.path)
                }"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </router-link>
          </div>
        </div>

      </nav>
    </aside>

    <main class="flex-1 relative z-10 overflow-auto ml-6">
      <router-view/>
    </main>

  </div>

  <div class="space-y-6">
    <div>
      <h1 class="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
        应用设置
      </h1>
      <p class="text-white/60 mt-2">管理应用的基础配置和第三方服务</p>
    </div>

    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">

      <div class="p-6 border-b border-white/10">
        <div class="flex items-center space-x-3">
          <div class="w-3 h-3 bg-cyan-400 rounded-full animate-pulse"></div>
          <h2 class="text-xl font-semibold text-cyan-300">基础配置</h2>
        </div>
      </div>

      <div class="p-6 space-y-6">

        <div class="flex items-center gap-6">
          <div class="flex items-center space-x-3 min-w-48">
            <div class="w-8 h-8 bg-cyan-500/20 rounded-lg flex items-center justify-center">
              <svg class="w-4 h-4 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"/>
              </svg>
            </div>
            <span class="text-white/80 font-medium">应用名称</span>
          </div>
          <el-input
              v-model="appInfoConfig.appName"
              placeholder="请输入应用名称"
              class="flex-1 custom-input"
              style="max-width: 400px;"
              size="large"
          />
        </div>

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

        <div class="flex items-center gap-6">
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


        <div class="flex items-start gap-6">
          <div class="flex items-start space-x-3 min-w-48 pt-2">
            <div class="w-8 h-8 bg-purple-500/20 rounded-lg flex items-center justify-center">
              <svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
              </svg>
            </div>
            <span class="text-white/80 font-medium">只读业务放行</span>
          </div>
          <el-checkbox-group
              v-model="appConfigJson['read_table']"
              :options="tables"
              :props="props"
              class="custom-checkbox-group"
              style="width: 400px;"
          />
        </div>

        <div class="flex justify-start pt-4">
          <el-button
              type="primary"
              @click="updateAppName"
              :loading="updating"
              class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
              size="large"
          >
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
            </svg>
            保存基础配置
          </el-button>
        </div>
      </div>
    </div>

    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
      <div class="p-6 border-b border-white/10">
        <div class="flex justify-between items-center">
          <div class="flex items-center space-x-3">
            <div class="w-3 h-3 bg-green-400 rounded-full animate-pulse"></div>
            <h2 class="text-xl font-semibold text-green-300">微信授权登录配置</h2>
          </div>
          <el-switch
              v-model="weChatLoginEnabled"
              active-color="#06b6d4"
              class="custom-switch"
          />
        </div>
      </div>

      <div v-if="weChatLoginEnabled" class="p-6 space-y-6">
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

        <div class="flex justify-start">
          <el-button
              type="primary"
              @click="saveWeChatConfig"
              class="bg-gradient-to-r from-green-500 to-emerald-500 border-0 text-white hover:from-green-600 hover:to-emerald-600 transition-all duration-300 shadow-lg shadow-green-500/25"
              size="large"
          >
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
            </svg>
            保存微信配置
          </el-button>
        </div>
      </div>
    </div>

    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
      <div class="p-6 border-b border-white/10">
        <div class="flex justify-between items-center">
          <div class="flex items-center space-x-3">
            <div class="w-3 h-3 bg-blue-400 rounded-full animate-pulse"></div>
            <h2 class="text-xl font-semibold text-blue-300">邮箱登录配置</h2>
          </div>
          <el-switch
              v-model="emailLoginEnabled"
              active-color="#06b6d4"
              class="custom-switch"
          />
        </div>
      </div>

      <div v-if="emailLoginEnabled" class="p-6 space-y-6">
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

        <div class="flex justify-start">
          <el-button
              type="primary"
              @click="saveEmailConfig"
              class="bg-gradient-to-r from-blue-500 to-indigo-500 border-0 text-white hover:from-blue-600 hover:to-indigo-600 transition-all duration-300 shadow-lg shadow-blue-500/25"
              size="large"
          >
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
            </svg>
            保存邮箱配置
          </el-button>
        </div>
      </div>
    </div>

    <div class="bg-red-500/10 backdrop-blur-md rounded-2xl border border-red-500/20 overflow-hidden">
      <div class="p-6 border-b border-red-500/20">
        <div class="flex items-center space-x-3">
          <div class="w-3 h-3 bg-red-400 rounded-full animate-pulse"></div>
          <h2 class="text-xl font-semibold text-red-300">危险操作</h2>
        </div>
      </div>

      <div class="p-6 space-y-4">
        <div class="text-white/80">
          <p class="mb-2">删除应用将永久移除所有相关数据，此操作不可撤销。</p>
          <p class="text-sm text-white/60">包括：数据表、API密钥、配置信息等</p>
        </div>
        <el-button
            type="danger"
            @click="deleteApp"
            :loading="deleting"
            class="bg-gradient-to-r from-red-500 to-pink-500 border-0 text-white hover:from-red-600 hover:to-pink-600 transition-all duration-300 shadow-lg shadow-red-500/25"
            size="large"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
          删除应用
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>

const {proxy} = getCurrentInstance();
const appId = proxy.$route.params.id;

// Reactive data
const updating = ref(false);
const deleting = ref(false);

const appInfoConfig = ref({});
const tables = ref([])
const appConfigJson = ref({});

const emailLoginEnabled = ref(false);
const emailConfig = ref({});

const weChatLoginEnabled = ref(false);
const weChatConfig = ref({});

const props = {
  value: 'tableName',
  label: 'description',
  options: 'options',
  disabled: 'disabled',
}

const configMenus = computed(() => [
  {
    path: 'settings/basic',
    label: '基本信息',
  },
  {
    path: 'settings/auth',
    label: '登录策略',
  },
  {
    path: 'settings/permission',
    label: '权限策略',
  }
])

// 危险区域菜单
const dangerMenus = computed(() => [
  // {
  //   path: 'settings/cleanup',
  //   label: '数据清理',
  // },
  {
    path: 'settings/delete',
    label: '删除应用',
  }
])

// Fetch app info on mount
onMounted(() => {
  loadTables();
  fetchAppInfo();
  fetchSettings();
});

// Fetch app information
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

function saveEmailConfig() {
  proxy.$api.setting.saveSetting(appId, emailConfig.value).then((res) => {
    if (res.success) {
      proxy.$modal.msgSuccess("保存成功");
    }
  });
}

function saveWeChatConfig() {
  proxy.$api.setting.saveSetting(appId, weChatConfig.value).then((res) => {
    if (res.success) {
      proxy.$modal.msgSuccess("保存成功");
    }
  });
}

// Update app name
const updateAppName = async () => {

  if (!appInfoConfig.value.appName.trim()) {
    proxy.$modal.msgWarning('请输入应用名称');
    return;
  }

  updating.value = true;
  try {
    appInfoConfig.value.configJson = JSON.stringify(appConfigJson.value);
    await proxy.$api.project.update(appId, appInfoConfig.value);
    proxy.$modal.msgSuccess('保存成功');
  } catch (error) {
    console.error('Failed to update app name:', error);
    proxy.$modal.msgError('更新应用名称失败');
  } finally {
    updating.value = false;
  }
};

// Delete app
const deleteApp = async () => {
  try {
    const {value} = await proxy.$modal.prompt(
        `请输入应用名称 "${appInfoConfig.value.appName}" 以确认删除操作`,
        '删除应用',
        {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'error',
          inputPattern: new RegExp(`^${appInfoConfig.value.appName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`),
          inputErrorMessage: '应用名称不匹配，请重新输入'
        }
    );

    // 额外校验输入的应用名称
    if (value !== appInfoConfig.value.appName) {
      proxy.$modal.msgError('应用名称不匹配，删除操作已取消');
      return;
    }

    deleting.value = true;
    await proxy.$api.project.delete(appId);
    proxy.$modal.msgSuccess('应用删除成功');
    proxy.$router.push('/');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete app:', error);
      proxy.$modal.msgError('删除应用失败');
    }
  } finally {
    deleting.value = false;
  }
};
</script>