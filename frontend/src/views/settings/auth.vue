<template>
  <div class="space-y-6">
    <!-- 页面标题 -->
    <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
      <div class="p-6 border-b border-white/10">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-3">
            <div class="w-3 h-3 bg-cyan-400 rounded-full animate-pulse"></div>
            <div>
              <h2 class="text-xl font-semibold text-cyan-300">{{ t('page.settings_auth.title') }}</h2>
              <p class="text-xs text-white/60">{{ t('page.settings_auth.subtitle') }}</p>
            </div>
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
            {{ t('page.settings_auth.save') }}
          </el-button>
        </div>
      </div>

      <div class="p-6 space-y-6">
        <!-- 基础配置 -->
        <div class="flex items-center gap-6">
          <div class="flex items-center space-x-3 min-w-48">
            <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
              <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
              </svg>
            </div>
            <span class="text-white/80 font-medium">{{ t('page.settings_auth.enable_login') }}</span>
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
            <span class="text-white/80 font-medium">{{ t('page.settings_auth.bind_business') }}</span>
          </div>
          <el-select
              v-model="appInfoConfig.authTable"
              :options="tables"
              :props="props"
              class="custom-input"
              style="width: 300px;"
              size="large"
              :placeholder="t('page.settings_auth.bind_business_placeholder')"
          />
        </div>
      </div>
    </div>

    <!-- Auth Providers 列表 -->
    <div v-if="appInfoConfig.needAuth" class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden">
      <div class="p-6">
        <div class="space-y-4">
          <!-- 微信登录 -->
          <div 
            class="provider-item flex items-center justify-between p-4 rounded-lg border border-white/10 hover:bg-white/5 transition-all duration-200 cursor-pointer"
            @click="openProviderDrawer('wechat')"
          >
            <div class="flex items-center space-x-4">
              <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-green-400" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8.5 12.5c0-.8-.7-1.5-1.5-1.5s-1.5.7-1.5 1.5.7 1.5 1.5 1.5 1.5-.7 1.5-1.5zm7 0c0-.8-.7-1.5-1.5-1.5s-1.5.7-1.5 1.5.7 1.5 1.5 1.5 1.5-.7 1.5-1.5z"/>
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"/>
                </svg>
              </div>
              <span class="text-white/90 font-medium">{{ t('page.settings_auth.provider_wechat') }}</span>
            </div>
            <div class="flex items-center space-x-3">
              <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </div>
          </div>

          <!-- 邮箱登录 -->
          <div 
            class="provider-item flex items-center justify-between p-4 rounded-lg border border-white/10 hover:bg-white/5 transition-all duration-200 cursor-pointer"
            @click="openProviderDrawer('email')"
          >
            <div class="flex items-center space-x-4">
              <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                </svg>
              </div>
              <span class="text-white/90 font-medium">{{ t('page.settings_auth.provider_email') }}</span>
            </div>
            <div class="flex items-center space-x-3">
              <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </div>
          </div>

          <!-- Google登录 -->
          <div 
            class="provider-item flex items-center justify-between p-4 rounded-lg border border-white/10 hover:bg-white/5 transition-all duration-200 cursor-pointer"
            @click="openProviderDrawer('google')"
          >
            <div class="flex items-center space-x-4">
              <div class="w-8 h-8 bg-red-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-red-400" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
              </div>
              <span class="text-white/90 font-medium">{{ t('page.settings_auth.provider_google') }}</span>
            </div>
            <div class="flex items-center space-x-3">
              <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :show-header="false"
      direction="rtl"
      size="500px"
      class="auth-drawer"
      :modal="true"
      :modal-class="'drawer-modal'"
      :style="{
        '--el-drawer-bg-color': 'rgba(255, 255, 255, 0.05)',
        '--el-drawer-padding-primary': '0px'
      }"
    >
      <div class="p-6 space-y-6 text-white" style="background-color: rgba(255, 255, 255, 0.05); backdrop-filter: blur(16px);">
        <!-- 微信配置 -->
        <div v-if="currentProvider === 'wechat'" class="space-y-6">
          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.wechat_appid') }}</span>
            </div>
            <el-input
                v-model="weChatConfig['oauth2.wechat.app_id']"
                :placeholder="t('page.settings_auth.wechat_appid_placeholder')"
                class="custom-input"
                size="large"
            />
          </div>

          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.wechat_secret') }}</span>
            </div>
            <el-input
                v-model="weChatConfig['oauth2.wechat.app_secret']"
                :placeholder="t('page.settings_auth.wechat_secret_placeholder')"
                type="password"
                class="custom-input"
                size="large"
                show-password
            />
          </div>

          <!-- 微信重定向地址 -->
          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.redirect_uri') }}</span>
            </div>
            <el-input
                v-model="weChatConfig['oauth2.wechat.redirect_uri']"
                :placeholder="t('page.settings_auth.redirect_uri_placeholder')"
                class="custom-input"
                size="large"
            />
            <p class="text-xs text-white/60">{{ t('page.settings_auth.redirect_uri_tip') }}</p>
          </div>

          <!-- 微信回调地址（前半部分，拼接固定后缀） -->
          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-green-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.callback_uri') }}</span>
            </div>
            <el-input
                v-model="weChatConfig['oauth2.wechat.callback_uri']"
                :placeholder="t('page.settings_auth.callback_uri_placeholder')"
                class="custom-input"
                size="large"
            >
              <template #append>/callback/wechat</template>
            </el-input>
            <p class="text-xs text-white/60">{{ t('page.settings_auth.callback_uri_tip') }}</p>
          </div>
        </div>

        <!-- 邮箱配置 -->
        <div v-if="currentProvider === 'email'" class="space-y-6">
          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.smtp_host') }}</span>
            </div>
            <el-input
                v-model="emailConfig['mail.host']"
                :placeholder="t('page.settings_auth.smtp_host_placeholder')"
                class="custom-input"
                size="large"
            />
          </div>

          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.smtp_port') }}</span>
            </div>
            <el-input
                v-model="emailConfig['mail.port']"
                :placeholder="t('page.settings_auth.smtp_port_placeholder')"
                class="custom-input"
                size="large"
            />
          </div>

          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.smtp_user') }}</span>
            </div>
            <el-input
                v-model="emailConfig['mail.user']"
                :placeholder="t('page.settings_auth.smtp_user_placeholder')"
                class="custom-input"
                size="large"
            />
          </div>

          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.smtp_pass') }}</span>
            </div>
            <el-input
                v-model="emailConfig['mail.passwd']"
                type="password"
                :placeholder="t('page.settings_auth.smtp_pass_placeholder')"
                class="custom-input"
                size="large"
                show-password
            />
          </div>

          <div class="space-y-2">
            <div class="flex items-start space-x-3">
              <div class="w-8 h-8 bg-blue-500/20 rounded-lg flex items-center justify-center mt-2">
                <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
              </div>
              <div class="flex-1">
                <span class="text-white/80 font-medium block mb-2">{{ t('page.settings_auth.code_template') }}</span>
                <el-input
                    v-model="emailConfig['login.mail.code_template']"
                    :placeholder="t('page.settings_auth.code_template_placeholder')"
                    type="textarea"
                    :rows="3"
                    class="custom-input"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Google配置 -->
        <div v-if="currentProvider === 'google'" class="space-y-6">
          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-red-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-red-400" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.google_client_id') }}</span>
            </div>
            <el-input
                v-model="googleConfig.clientId"
                :placeholder="t('page.settings_auth.google_client_id_placeholder')"
                class="custom-input"
                size="large"
            />
          </div>

          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-red-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.google_client_secret') }}</span>
            </div>
            <el-input
                v-model="googleConfig.clientSecret"
                type="password"
                :placeholder="t('page.settings_auth.google_client_secret_placeholder')"
                class="custom-input"
                size="large"
                show-password
            />
          </div>

          <!-- Google 重定向地址 -->
          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-red-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.redirect_uri') }}</span>
            </div>
            <el-input
                v-model="googleConfig.redirectUri"
                :placeholder="t('page.settings_auth.redirect_uri_placeholder')"
                class="custom-input"
                size="large"
            />
            <p class="text-xs text-white/60">{{ t('page.settings_auth.redirect_uri_tip') }}</p>
          </div>

          <!-- Google 回调地址（前半部分，拼接固定后缀） -->
          <div class="space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-red-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                </svg>
              </div>
              <span class="text-white/80 font-medium">{{ t('page.settings_auth.callback_uri') }}</span>
            </div>
            <el-input
                v-model="googleConfig.callbackBase"
                :placeholder="t('page.settings_auth.callback_uri_placeholder')"
                class="custom-input"
                size="large"
            >
              <template #append>/callback/google</template>
            </el-input>
            <p class="text-xs text-white/60">oauth2授权服务认证时的回调地址，需要和认证服务商处配置的回调地址保持完全一致</p>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();
const t = proxy.$tt;
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
const googleConfig = ref({});

// 抽屉相关状态
const drawerVisible = ref(false);
const currentProvider = ref('');

// 打开提供商配置抽屉
const openProviderDrawer = (provider) => {
  currentProvider.value = provider;
  drawerVisible.value = true;
};

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
    proxy.$modal.msgError(t('page.settings_auth.fetch_info_failed'));
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
      getGoogleSetting(map);
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
  const keys = [
    "wx.pay.mp-app-id",
    "wx.pay.mp-app-secret",
    "oauth2.wechat.app_id",
    "oauth2.wechat.app_secret",
    "oauth2.wechat.redirect_uri",
    "oauth2.wechat.callback_uri"
  ];
  weChatConfig.value = keys.reduce((obj, key) => {
    obj[key] = map[key];
    return obj;
  }, {});
  // 展示给前端时移除固定后缀
  if (weChatConfig.value['oauth2.wechat.callback_uri']) {
    weChatConfig.value['oauth2.wechat.callback_uri'] = stripCallbackSuffix(
        weChatConfig.value['oauth2.wechat.callback_uri'],
        '/callback/wechat'
    );
  }
}

function getGoogleSetting(map) {
  const keys = [
    'oauth2.google.client_id',
    'oauth2.google.client_secret',
    'oauth2.google.redirect_uri',
    'oauth2.google.callback_uri'
  ];
  const g = keys.reduce((obj, key) => {
    obj[key] = map[key];
    return obj;
  }, {});
  // 前端模型
  googleConfig.value = {
    clientId: g['oauth2.google.client_id'],
    clientSecret: g['oauth2.google.client_secret'],
    redirectUri: g['oauth2.google.redirect_uri'],
    // 展示给前端时移除固定后缀
    callbackBase: stripCallbackSuffix(g['oauth2.google.callback_uri'], '/callback/google')
  };
}

async function saveLoginConfig() {
  saving.value = true;
  try {
    appInfoConfig.value.configJson = JSON.stringify(appConfigJson.value);
    await proxy.$api.project.update(appId, appInfoConfig.value);
    if (appInfoConfig.value.needAuth) {
      await proxy.$api.setting.saveSetting(appId, emailConfig.value);

      // 保存微信配置（回调地址追加固定后缀）
      const wechatSettings = {
        'wx.pay.mp-app-id': weChatConfig.value['wx.pay.mp-app-id'],
        'wx.pay.mp-app-secret': weChatConfig.value['wx.pay.mp-app-secret'],
        'oauth2.wechat.app_id': weChatConfig.value['oauth2.wechat.app_id'],
        'oauth2.wechat.app_secret': weChatConfig.value['oauth2.wechat.app_secret'],
        'oauth2.wechat.redirect_uri': weChatConfig.value['oauth2.wechat.redirect_uri'],
        'oauth2.wechat.callback_uri': appendCallbackSuffix(
            weChatConfig.value['oauth2.wechat.callback_uri'],
            '/callback/wechat'
        ),
      };
      await proxy.$api.setting.saveSetting(appId, wechatSettings);
      
      // 保存Google配置
      const googleSettings = {
        'oauth2.google.client_id': googleConfig.value.clientId,
        'oauth2.google.client_secret': googleConfig.value.clientSecret,
        'oauth2.google.redirect_uri': googleConfig.value.redirectUri,
        'oauth2.google.callback_uri': appendCallbackSuffix(
            googleConfig.value.callbackBase,
            '/callback/google'
        )
      };
      await proxy.$api.setting.saveSetting(appId, googleSettings);
    }
    proxy.$modal.msgSuccess(t('page.settings_auth.login_save_success'));
  } catch (error) {
    console.error('Failed to update app name:', error);
    proxy.$modal.msgError(t('page.settings_auth.save_failed'));
  } finally {
    saving.value = false;
  }
}

// 工具方法：处理回调地址后缀
function stripCallbackSuffix(uri, suffix) {
  if (!uri) return '';
  try {
    if (uri.endsWith(suffix)) {
      return uri.slice(0, -suffix.length);
    }
    return uri;
  } catch (e) {
    return uri;
  }
}

function appendCallbackSuffix(base, suffix) {
  if (!base) return '';
  // 去除末尾斜杠，拼接固定后缀
  const normalized = base.endsWith('/') ? base.slice(0, -1) : base;
  return `${normalized}${suffix}`;
}

</script>

<style scoped>
/* 强制覆盖Element Plus抽屉样式 */
:global(.el-drawer) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
}

:global(.el-drawer__wrapper) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
}

:global(.el-drawer__container) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
}

/* 最高优先级覆盖 */
:global(.el-drawer__header) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  color: white !important;
  border-bottom: none !important;
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

:global(.el-drawer__body) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  color: white !important;
  padding: 0 !important;
  margin-top: 0 !important;
}

:global(.el-drawer__title) {
  color: #06b6d4 !important;
}

/* 抽屉遮罩层 */
.drawer-modal {
  background-color: rgba(0, 0, 0, 0.3) !important;
}

.auth-drawer :deep(.el-drawer) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  color: white !important;
}

.auth-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px 24px;
  border-bottom: none !important;
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  color: white !important;
}

.auth-drawer :deep(.el-drawer__body) {
  padding: 0 !important;
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  color: white !important;
  margin-top: 0 !important;
}

.auth-drawer :deep(.el-drawer__title) {
  color: #06b6d4 !important;
  font-size: 18px;
  font-weight: 600;
}

.auth-drawer :deep(.el-drawer__close-btn) {
  color: rgba(255, 255, 255, 0.8) !important;
}

.auth-drawer :deep(.el-drawer__close-btn:hover) {
  color: #06b6d4 !important;
}

/* 提供商列表项悬停效果 */
.provider-item:hover {
  background-color: rgba(255, 255, 255, 0.05);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 状态标签样式 */
.status-enabled {
  background-color: rgba(34, 197, 94, 0.2);
  color: #22c55e;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.status-disabled {
  background-color: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 类型标签样式 */
.type-tag {
  background-color: rgba(6, 182, 212, 0.1);
  color: #06b6d4;
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  border: 1px solid rgba(6, 182, 212, 0.2);
  backdrop-filter: blur(8px);
}

/* 抽屉内输入框样式 */
.auth-drawer :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: none !important;
}

.auth-drawer :deep(.el-input__wrapper:hover) {
  border-color: rgba(6, 182, 212, 0.3) !important;
}

.auth-drawer :deep(.el-input__wrapper.is-focus) {
  border-color: #06b6d4 !important;
  box-shadow: 0 0 0 2px rgba(6, 182, 212, 0.1) !important;
}

.auth-drawer :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9) !important;
  background-color: transparent !important;
}

.auth-drawer :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* 抽屉内文本域样式 */
.auth-drawer :deep(.el-textarea__inner) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

.auth-drawer :deep(.el-textarea__inner:hover) {
  border-color: rgba(6, 182, 212, 0.3) !important;
}

.auth-drawer :deep(.el-textarea__inner:focus) {
  border-color: #06b6d4 !important;
  box-shadow: 0 0 0 2px rgba(6, 182, 212, 0.1) !important;
}

.auth-drawer :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* 确保抽屉无缝连接 */
.auth-drawer :deep(.el-drawer__container) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
}

.auth-drawer :deep(.el-drawer__wrapper) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
}

/* 移除任何可能的间隙 */
.auth-drawer :deep(.el-drawer__header),
.auth-drawer :deep(.el-drawer__body) {
  margin: 0 !important;
  border: none !important;
  outline: none !important;
}

/* 确保抽屉内所有文字都是白色 */
.auth-drawer {
  color: white !important;
}

.auth-drawer * {
  color: inherit;
}

.auth-drawer span {
  color: rgba(255, 255, 255, 0.8) !important;
}

.auth-drawer .text-white\/80 {
  color: rgba(255, 255, 255, 0.8) !important;
}
</style>

<style>
/* 全局样式覆盖 - 非scoped */
.el-drawer__header {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  border-bottom: none !important;
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

.el-drawer__body {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
  padding: 0 !important;
  margin-top: 0 !important;
}

.el-drawer {
  background-color: rgba(255, 255, 255, 0.05) !important;
  backdrop-filter: blur(16px) !important;
}
</style>