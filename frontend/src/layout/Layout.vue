<template>
  <div
      class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white flex flex-col relative overflow-hidden">
    <!-- 顶部导航 -->
    <header class="flex justify-between items-center px-8 py-4 border-b border-white/20">
      <div class="flex items-center space-x-3">
        <div class="w-8 h-8 bg-gradient-to-r from-cyan-500 to-blue-500 rounded-lg flex items-center justify-center">
          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
          </svg>
        </div>
        <h1
            class="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent cursor-pointer hover:from-cyan-300 hover:to-blue-300 transition-all duration-300"
            @click="goHome"
        >
          控制台
        </h1>

        <div v-if="versionInfo"
             class="flex items-center space-x-2 px-3 py-1 rounded-lg border border-white/20 text-sm ml-4">
          <span class="text-white/70">版本 {{ versionInfo.version }}</span>
          <template v-if="versionInfo.hasUpdate">
            <span class="text-yellow-400 font-semibold">有新版本！</span>
            <a
                href="https://gitee.com/kuafuai/aipexbase"
                target="_blank"
                class="text-cyan-400 underline hover:text-cyan-300 transition-colors"
            >
              查看更新
            </a>
          </template>
          <template v-else>
            <span class="text-green-400 font-semibold">已是最新版本</span>
          </template>
        </div>

      </div>


      <div class="flex items-center space-x-4">
        <!-- 说明文档按钮 -->
        <div
            class="flex items-center space-x-2 px-3 py-2 bg-white/5 rounded-lg cursor-pointer hover:bg-white/10 transition-all duration-300"
            @click="openDocumentation"
        >
          <svg class="w-4 h-4 text-white/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          <span class="text-sm text-white/80">说明文档</span>
        </div>
        <!-- 运行状态 -->
        <div class="flex items-center space-x-2 px-3 py-2 bg-white/5 rounded-lg">
          <div class="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
          <span class="text-sm text-white/80">运行中</span>
        </div>
        <!-- 登出按钮 -->
        <div
            class="flex items-center space-x-2 px-3 py-2 bg-red-600/20 border border-red-500/30 rounded-lg cursor-pointer hover:bg-red-600/30 hover:border-red-500/50 transition-all duration-300"
            @click="logout"
        >
          <svg class="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
          </svg>
          <span class="text-sm text-red-400">登出</span>
        </div>
      </div>
    </header>
    <main class="flex-1 overflow-auto p-8">
      <router-view/>
    </main>
  </div>
</template>
<script setup>
  const {proxy} = getCurrentInstance();

  // 说明文档配置 - 请在这里填写您的文档网址
  const documentationConfig = {
    url: 'https://vvx03gck2p.feishu.cn/docx/LSsLdYZQfoAo3zxTkwrcJuGVnC3', // 请替换为您的实际文档网址
    openInNewTab: true // 是否在新标签页中打开
  }

  const versionInfo = ref(null)

  function logout() {
    proxy.$modal.confirm(
        '确定要登出吗？',
        '确认登出',
        {
          confirmButtonText: '确定登出',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
    ).then(() => {
      proxy.$api.login.logout().then((res) => {
        localStorage.removeItem('token');
        proxy.$router.push({path: '/login'});
      })
    }).catch(() => {
      // 用户取消登出，不做任何操作
    })
  }

  function goHome() {
    proxy.$router.push({path: '/'});
  }

  // 打开说明文档
  function openDocumentation() {
    if (!documentationConfig.url || documentationConfig.url === 'https://your-documentation-url.com') {
      proxy.$modal.msgWarning('请先配置文档网址');
      return;
    }

    if (documentationConfig.openInNewTab) {
      window.open(documentationConfig.url, '_blank');
    } else {
      window.location.href = documentationConfig.url;
    }
  }

  onMounted(() => {
    proxy.$api.setting.version().then((res) => {
      if (res.success) {
        versionInfo.value = res.data
      }
    })
  });

</script>