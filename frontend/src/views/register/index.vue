<template>
  <div
      class="relative w-screen h-screen overflow-hidden bg-gradient-to-br from-[#0a0f1a] via-[#0e1b2a] to-[#1a2740] flex items-center justify-center"
  >
    <div
        class="z-10 w-[480px] p-8 rounded-2xl bg-white/10 backdrop-blur-md border border-cyan-400/50 shadow-lg shadow-cyan-500/30"
    >
      <h2 class="text-center text-2xl font-bold text-cyan-400 mb-8 tracking-widest">
        {{ $t('page.register.title') }}
      </h2>

      <el-form :model="form" ref="formRef" :rules="rules" label-position="left">
        <el-form-item :label="$t('page.register.email')" prop="email">
          <el-input
              v-model="form.email"
              :placeholder="$t('page.register.email_placeholder')"
              prefix-icon="User"
              clearable
          />
        </el-form-item>

        <el-form-item :label="$t('page.register.passwd')" prop="password">
          <el-input
              v-model="form.password"
              type="password"
              :placeholder="$t('page.register.passwd_placeholder')"
              prefix-icon="Lock"
              show-password
              clearable
          />
        </el-form-item>

        <el-form-item :label="$t('page.register.confirm_passwd')" prop="confirmPassword">
          <el-input
              v-model="form.confirmPassword"
              type="password"
              :placeholder="$t('page.register.confirm_passwd_placeholder')"
              prefix-icon="Lock"
              show-password
              clearable
          />
        </el-form-item>

        <div class="mt-6">
          <el-button
              type="primary"
              class="w-full bg-cyan-500/90 hover:bg-cyan-400 transition-all duration-300 shadow-md shadow-cyan-400/50 animate-pulse"
              @click="handleRegister"
          >
            {{ $t('page.register.register_btn') }}
          </el-button>
        </div>

        <div class="mt-4 text-center text-sm text-cyan-400">
          {{ $t('page.register.login_left') }}
          <span
              class="cursor-pointer text-cyan-300 hover:text-cyan-200 underline"
              @click="goLogin"
          >
            {{ $t('page.register.login_link') }}
          </span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
const {proxy} = getCurrentInstance();

const form = ref({
  email: "",
  password: "",
  confirmPassword: "",
});

const rules = {
  email: [{required: true, message: proxy.$tt('page.register.email_placeholder'), trigger: "blur"}],
  password: [{required: true, message: proxy.$tt('page.register.passwd_placeholder'), trigger: "blur"}],
  confirmPassword: [
    {required: true, message: proxy.$tt('page.register.confirm_passwd_placeholder'), trigger: "blur"},
    {
      validator: (rule, value, callback) => {
        if (value !== form.value.password) {
          callback(new Error(proxy.$tt('page.register.passwd_same_error')));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
};

function handleRegister() {
  proxy.$refs.formRef
      .validate()
      .then(() => {
        proxy.$api.login.register(form.value).then((r) => {
          if (r.success) {
            proxy.$router.push({path: "/login"});
          }
        });
      })
      .catch((err) => {
        console.log("注册验证失败:", err);
      });
}

function goLogin() {
  proxy.$router.push({path: "/login"});
}
</script>
