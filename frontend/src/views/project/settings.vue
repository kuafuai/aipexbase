<template>

  <div
      class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white flex relative overflow-hidden rounded-2xl">

    <div
        class="absolute inset-0 bg-[radial-gradient(ellipse_at_left,_var(--tw-gradient-stops))] from-cyan-500/5 via-transparent to-transparent"></div>

    <aside
        class="relative z-10 w-80 bg-white/5 backdrop-blur-xl border-r border-white/10 p-6 flex flex-col h-screen rounded-2xl shadow-xl shadow-white/5">

      <div class="flex items-center space-x-3 mb-8 px-2">
        <div class="w-8 h-8 bg-gradient-to-r from-cyan-400 to-blue-400 rounded-lg flex items-center justify-center">
          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
          </svg>
        </div>
        <div>
          <h1 class="text-xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
            {{ appInfoConfig.appName || t('page.settings.app_manage_title') }}
          </h1>
          <p class="text-xs text-white/40">{{ t('page.settings.app_config_center') }}</p>
        </div>
      </div>

      <nav class="flex flex-col space-y-2">

        <div class="px-4 py-2">
          <h3 class="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">{{ t('page.settings.section_app_manage') }}</h3>

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
              <span class="font-medium">{{ t('page.settings.menu_users') }}</span>

            </router-link>
          </div>
        </div>

        <div class="px-4 py-2">
          <h3 class="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">{{ t('page.settings.section_config_center') }}</h3>
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
          <h3 class="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">{{ t('page.settings.section_danger') }}</h3>
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
</template>

<script setup>

const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const appId = proxy.$route.params.id;

const appInfoConfig = ref({});
const tables = ref([])
const appConfigJson = ref({});


const emailConfig = ref({});
const weChatConfig = ref({});


const configMenus = computed(() => [
  {
    path: 'settings/basic',
    label: t('page.settings.menu_basic'),
  },
  {
    path: 'settings/auth',
    label: t('page.settings.menu_auth'),
  },
  {
    path: 'settings/permission',
    label: t('page.settings.menu_permission'),
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
    label: t('page.settings.menu_delete'),
  }
])

// Fetch app info on mount
onMounted(() => {

  fetchAppInfo();

});

// Fetch app information
const fetchAppInfo = async () => {
  try {
    const res = await proxy.$api.project.overview(appId);
    appInfoConfig.value = res.data;
    appConfigJson.value = JSON.parse(appInfoConfig.value.configJson);
  } catch (error) {
    console.error('Failed to fetch app info:', error);
    proxy.$modal.msgError(t('page.settings.fetch_info_failed'));
  }
};

</script>