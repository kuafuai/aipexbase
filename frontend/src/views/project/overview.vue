<template>
  <div class="space-y-6">

    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
          应用概览
        </h1>
        <p class="text-white/60 mt-2">查看应用的基本信息和运行状态</p>
      </div>
      <div class="flex items-center space-x-2 px-3 py-2 bg-white/5 rounded-lg border border-white/10">
        <div class="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
        <span class="text-sm text-white/80">运行正常</span>
      </div>
    </div>

    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
      <div class="p-6 border-b border-white/10">
        <div class="flex items-center space-x-3">
          <div class="w-3 h-3 bg-cyan-400 rounded-full animate-pulse"></div>
          <h2 class="text-xl font-semibold text-cyan-300">应用信息</h2>
        </div>
      </div>

      <div class="p-6">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">

          <div
              class="group relative p-6 bg-white/5 rounded-xl border border-white/10 hover:border-cyan-400/30 transition-all duration-300">

            <div class="flex flex-col items-center text-center">

              <div
                  class="w-12 h-12 bg-gradient-to-br from-cyan-500 to-blue-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"/>
                </svg>
              </div>

              <div class="text-sm text-white/60 mb-2">应用名称</div>
              <div class="text-lg font-semibold text-white truncate w-full">{{ appOverview.appName || '未设置' }}</div>

            </div>
          </div>

          <div
              class="group relative p-6 bg-white/5 rounded-xl border border-white/10 hover:border-green-400/30 transition-all duration-300">

            <div class="flex flex-col items-center text-center">

              <div
                  class="w-12 h-12 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              </div>

              <div class="text-sm text-white/60 mb-2">应用状态</div>
              <el-tag
                  :type="getStatusType(appOverview.status)"
                  effect="dark"
                  class="rounded-full px-4 py-1 text-sm font-medium border-0"
                  :class="{
                  'bg-gradient-to-r from-green-500 to-emerald-500': getStatusType(appOverview.status) === 'success',
                  'bg-gradient-to-r from-yellow-500 to-amber-500': getStatusType(appOverview.status) === 'warning',
                  'bg-gradient-to-r from-red-500 to-pink-500': getStatusType(appOverview.status) === 'danger',
                  'bg-gradient-to-r from-gray-500 to-slate-500': getStatusType(appOverview.status) === 'info'
                }"
              >
                {{ appOverview.status || '未知' }}
              </el-tag>

            </div>
          </div>

          <div
              class="group relative p-6 bg-white/5 rounded-xl border border-white/10 hover:border-blue-400/30 transition-all duration-300">

            <div class="flex flex-col items-center text-center">

              <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-500 rounded-xl flex items-center
              justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                </svg>
              </div>
              <div class="text-sm text-white/60 mb-2">数据表数量</div>
              <div
                  class="text-3xl font-bold bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">
                {{ appOverview.appTableNum || 0 }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
      <div class="p-6 border-b border-white/10">
        <div class="flex items-center space-x-3">
          <div class="w-3 h-3 bg-purple-400 rounded-full animate-pulse"></div>
          <h2 class="text-xl font-semibold text-purple-300">快速操作</h2>
        </div>
      </div>

      <div class="p-6">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <el-button
              type="primary"
              size="large"
              class="h-32 flex flex-col items-center justify-center gap-3 bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25 group"
              @click="$router.push(`/project/${appId}/tables`)"
          >
            <div
                class="w-9 h-9 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
              </svg>
            </div>
            <span class="font-medium">管理数据表</span>
          </el-button>

          <el-button
              type="success"
              size="large"
              class="h-20 flex flex-col items-center justify-center gap-3 bg-gradient-to-r from-green-500 to-emerald-500 border-0 text-white hover:from-green-600 hover:to-emerald-600 transition-all duration-300 shadow-lg shadow-green-500/25 group"
              @click="$router.push(`/project/${appId}/apikeys`)"
          >
            <div
                class="w-9 h-9 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"/>
              </svg>
            </div>
            <span class="font-medium">API 密钥</span>
          </el-button>

          <el-button
              type="warning"
              size="large"
              class="h-20 flex flex-col items-center justify-center gap-3 bg-gradient-to-r from-amber-500 to-orange-500 border-0 text-white hover:from-amber-600 hover:to-orange-600 transition-all duration-300 shadow-lg shadow-amber-500/25 group"
              @click="$router.push(`/project/${appId}/settings`)"
          >
            <div
                class="w-9 h-9  rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
              </svg>
            </div>
            <span class="font-medium">基础设置</span>
          </el-button>

          <el-button
              type="info"
              size="large"
              class="h-20 flex flex-col items-center justify-center gap-3 bg-gradient-to-r from-purple-500 to-pink-500 border-0 text-white hover:from-purple-600 hover:to-pink-600 transition-all duration-300 shadow-lg shadow-purple-500/25 group"
              @click="$router.push(`/project/${appId}/thirdpart`)"
          >
            <div
                class="w-9 h-9 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"/>
              </svg>
            </div>
            <span class="font-medium">第三方集成</span>
          </el-button>
        </div>
      </div>
    </div>

    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">

      <div class="p-6 border-b border-white/10">
        <div class="flex items-center space-x-3">
          <div class="w-3 h-3 bg-orange-400 rounded-full animate-pulse"></div>
          <h2 class="text-xl font-semibold text-orange-300">详细信息</h2>
        </div>
      </div>

      <div class="p-6">

        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">

          <div class="space-y-4">

            <div
                class="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl border border-white/10 hover:border-cyan-400/30 transition-all duration-300">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-cyan-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/>
                  </svg>
                </div>
                <span class="text-white/80">应用ID</span>
              </div>
              <span class="text-white font-mono text-sm bg-white/5 px-3 py-1 rounded-lg">{{
                  appOverview.appId || 'N/A'
                }}</span>
            </div>

            <div
                class="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl border border-white/10 hover:border-green-400/30 transition-all duration-300">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                  </svg>
                </div>
                <span class="text-white/80">所有者</span>
              </div>
              <span class="text-white">{{ appOverview.owner || 'N/A' }}</span>
            </div>

            <div
                class="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl border border-white/10 hover:border-yellow-400/30 transition-all duration-300">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-yellow-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                  </svg>
                </div>
                <span class="text-white/80">需要认证</span>
              </div>
              <el-tag
                  :type="appOverview.needAuth ? 'success' : 'info'"
                  effect="dark"
                  size="small"
                  class="rounded-full px-3 border-0"
                  :class="{
                  'bg-gradient-to-r from-green-500 to-emerald-500': appOverview.needAuth,
                  'bg-gradient-to-r from-gray-500 to-slate-500': !appOverview.needAuth
                }"
              >
                {{ appOverview.needAuth ? '是' : '否' }}
              </el-tag>
            </div>

          </div>

          <div class="space-y-4">

            <div
                class="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl border border-white/10 hover:border-blue-400/30 transition-all duration-300">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                  </svg>
                </div>
                <span class="text-white/80">权限控制</span>
              </div>
              <el-tag
                  :type="appOverview.enablePermission ? 'success' : 'info'"
                  effect="dark"
                  size="small"
                  class="rounded-full px-3 border-0"
                  :class="{
                  'bg-gradient-to-r from-green-500 to-emerald-500': appOverview.enablePermission,
                  'bg-gradient-to-r from-gray-500 to-slate-500': !appOverview.enablePermission
                }"
              >
                {{ appOverview.enablePermission ? '启用' : '禁用' }}
              </el-tag>
            </div>

            <div
                class="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl border border-white/10 hover:border-purple-400/30 transition-all duration-300">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-purple-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                  </svg>
                </div>
                <span class="text-white/80">Web控制台</span>
              </div>
              <el-tag
                  :type="appOverview.enableWebConsole ? 'success' : 'info'"
                  effect="dark"
                  size="small"
                  class="rounded-full px-3 border-0"
                  :class="{
                  'bg-gradient-to-r from-green-500 to-emerald-500': appOverview.enableWebConsole,
                  'bg-gradient-to-r from-gray-500 to-slate-500': !appOverview.enableWebConsole
                }"
              >
                {{ appOverview.enableWebConsole ? '启用' : '禁用' }}
              </el-tag>
            </div>

            <div
                class="flex justify-between items-center py-3 px-4 bg-white/5 rounded-xl border border-white/10 hover:border-orange-400/30 transition-all duration-300">
              <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-orange-500/20 rounded-lg flex items-center justify-center">
                  <svg class="w-4 h-4 text-orange-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                  </svg>
                </div>
                <span class="text-white/80">认证表</span>
              </div>
              <span class="text-white font-mono text-sm bg-white/5 px-3 py-1 rounded-lg">{{
                  appOverview.authTable || 'N/A'
                }}</span>
            </div>

          </div>
        </div>

        <div v-if="appOverview.description" class="mt-6 p-4 bg-white/5 rounded-xl border border-white/10">
          <div class="flex items-center space-x-3 mb-3">
            <div class="w-2 h-2 bg-cyan-400 rounded-full"></div>
            <div class="text-sm font-medium text-cyan-300">应用描述</div>
          </div>
          <div class="text-white/90 leading-relaxed">{{ appOverview.description }}</div>
        </div>

      </div>
    </div>
  </div>
</template>


<script setup>
const {proxy} = getCurrentInstance();
const appId = proxy.$route.params.id;

var appOverview = reactive({
  appId: '',
  appName: '',
  iconUrl: null,
  needAuth: null,
  authTable: null,
  enablePermission: null,
  enableWebConsole: null,
  status: '',
  description: null,
  owner: null,
  appTableNum: 0
});

// 获取状态类型，用于标签颜色
const getStatusType = (status) => {
  switch (status?.toLowerCase()) {
    case 'active':
    case '运行中':
    case '正常':
      return 'success';
    case 'inactive':
    case '已停止':
    case '停止':
      return 'warning';
    case 'error':
    case '错误':
    case '异常':
      return 'danger';
    default:
      return 'info';
  }
};

// 定义项目概览信息响应式数据
onMounted(() => {
  console.log(appId);
  proxy.$api.project.overview(appId).then((res) => {
    console.log(res);
    Object.assign(appOverview, res.data);
  });
});

</script>