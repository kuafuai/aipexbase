<template>
  <div
      class="relative w-screen h-screen overflow-hidden bg-gradient-to-br from-[#0a0f1a] via-[#0e1b2a] to-[#1a2740] flex items-center justify-center"
  >
    <div
        class="z-10 w-[380px] p-8 rounded-2xl bg-white/10 backdrop-blur-md border border-cyan-400/50 shadow-lg shadow-cyan-500/30"
    >
      <h2 class="text-center text-2xl font-bold text-cyan-400 mb-8 tracking-widest">
        用户注册
      </h2>

      <el-form :model="form" ref="formRef" :rules="rules" label-position="left">
        <el-form-item label="邮箱" prop="email">
          <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              prefix-icon="User"
              clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              clearable
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
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
            注册账户
          </el-button>
        </div>

        <div class="mt-4 text-center text-sm text-cyan-400">
          已有账号？
          <span
              class="cursor-pointer text-cyan-300 hover:text-cyan-200 underline"
              @click="goLogin"
          >
            返回登录
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
  email: [{required: true, message: "请输入邮箱", trigger: "blur"}],
  password: [{required: true, message: "请输入密码", trigger: "blur"}],
  confirmPassword: [
    {required: true, message: "请再次输入密码", trigger: "blur"},
    {
      validator: (rule, value, callback) => {
        if (value !== form.value.password) {
          callback(new Error("两次输入的密码不一致"));
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
            proxy.$modal.msgSuccess("注册成功，请登录！");
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
