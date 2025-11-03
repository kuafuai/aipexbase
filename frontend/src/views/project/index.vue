<template>
  <div
      class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white flex relative overflow-hidden">
    <!-- 背景装饰 -->
    <div
        class="absolute inset-0 bg-[radial-gradient(ellipse_at_left,_var(--tw-gradient-stops))] from-cyan-500/5 via-transparent to-transparent"></div>

    <!-- 侧边栏 -->
    <aside class="relative z-10 w-80 bg-white/5 backdrop-blur-xl border-r border-white/10 p-6 flex flex-col rounded-2xl">
      <!-- 项目标题区域 -->
      <div class="flex items-center space-x-3 mb-8 pb-6 border-b border-white/10 bg-white/5 rounded-xl p-4 shadow-lg shadow-white/5">
        <div
            class="w-10 h-10 bg-gradient-to-br from-cyan-500 to-blue-500 rounded-xl flex items-center justify-center shadow-lg shadow-cyan-500/25">
          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
          </svg>
        </div>
        <div class="flex-1 min-w-0">
          <h2 class="text-xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent truncate">
            {{ appId }}
          </h2>
          <p class="text-sm text-white/60 truncate">{{ t('page.project_detail.project_console') }}</p>
        </div>
      </div>

      <!-- 导航菜单 -->
      <h3 class="text-sm font-semibold text-white/40 uppercase tracking-wider mb-4 px-2">
        {{ t('page.project_detail.project_nav') }}
      </h3>
      <nav class="flex flex-col space-y-2">
        <router-link
            v-for="item in menus"
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

          <!-- 菜单图标 -->
          <div
              class="w-8 h-8 rounded-lg flex items-center justify-center transition-all duration-300"
              :class="{
              'bg-gradient-to-br from-cyan-500 to-blue-500 text-white shadow-md': proxy.$route.path.includes(item.path),
              'bg-white/5 text-white/50 group-hover:bg-cyan-500/10 group-hover:text-cyan-300': !proxy.$route.path.includes(item.path)
            }"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="item.path === 'overview'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
              <path v-if="item.path === 'tables'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
              <path v-if="item.path === 'settings'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
              <path v-if="item.path === 'dynamicapi'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"/>
              <path v-if="item.path === 'apikeys'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"/>
            </svg>
          </div>

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
      </nav>

      <!-- 底部信息 -->
      <div class="mt-auto pt-6 border-t border-white/10">
        <div class="flex items-center space-x-3 p-3 bg-white/5 rounded-xl">
          <div class="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
          <div class="flex-1">
            <p class="text-sm font-medium text-white">{{ t('page.project_detail.healthy') }}</p>
            <p class="text-xs text-white/50">{{ t('page.project_detail.breadcrumb_home') }}: {{ t('page.project_detail.title') }}</p>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区域 -->
    <main class="relative z-10 flex-1 p-6 overflow-auto">
      <div class="mx-auto">
        <!-- 页面标题栏 -->
        <div class="mb-8">
          <div class="flex items-center space-x-3 text-sm text-white/50 mb-2">
            <router-link to="/" class="hover:text-cyan-400 transition-colors duration-200">{{ t('page.project_detail.breadcrumb_home') }}</router-link>
            <span>/</span>
            <span class="text-cyan-400">{{ appId }}</span>
            <span>/</span>
            <span class="text-white">{{ getCurrentPageName() }}</span>
          </div>
        </div>

        <!-- 路由内容 -->
        <router-view/>
      </div>
    </main>
  </div>
</template>
<script setup>
const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const appId = proxy.$route.params.id;

const menus = ref([
  {label: t('page.project_detail.menu_overview'), path: 'overview'},
  {label: t('page.project_detail.menu_tables'), path: 'tables'},
  {label: t('page.project_detail.menu_dynamicapi'), path: 'dynamicapi'},
  {label: t('page.project_detail.menu_apikeys'), path: 'apikeys'},
  {label: t('page.project_detail.menu_settings'), path: 'settings'},
])

function getCurrentPageName() {
  const currentPath = proxy.$route.path;
  const currentMenu = menus.value.find(menu =>
      currentPath.includes(menu.path)
  );
  return currentMenu ? currentMenu.label : t('page.project_detail.project_manage');
}

console.log(appId);
</script>

<style scoped>
/* 路由链接激活状态样式 */
.router-link-active {
  position: relative;
}

/* 自定义滚动条 */
aside::-webkit-scrollbar {
  width: 4px;
}

aside::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 2px;
}

aside::-webkit-scrollbar-thumb {
  background: rgba(6, 182, 212, 0.3);
  border-radius: 2px;
}

aside::-webkit-scrollbar-thumb:hover {
  background: rgba(6, 182, 212, 0.5);
}
</style>