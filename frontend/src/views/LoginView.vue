<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>在线考试系统</h2>
        </div>
      </template>

      <el-form
        :model="loginForm"
        :rules="rules"
        ref="loginFormRef"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
          ></el-input>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
          ></el-input>
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="loginForm.role" placeholder="请选择角色">
            <el-option label="教师" value="TEACHER"></el-option>
            <el-option label="学生" value="STUDENT"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="loading"
            >登录</el-button
          >
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="register-link">
        <router-link to="/register">没有账号？立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "../utils/request";
import api from "../config/api";

export default {
  name: "LoginView",
  setup() {
    const router = useRouter();
    const loginFormRef = ref();
    const loading = ref(false);

    const loginForm = reactive({
      username: "",
      password: "",
      role: "",
    });

    const rules = {
      username: [
        { required: true, message: "请输入用户名", trigger: "blur" },
        { min: 3, max: 20, message: "长度在 3 到 20 个字符", trigger: "blur" },
      ],
      password: [
        { required: true, message: "请输入密码", trigger: "blur" },
        { min: 6, max: 20, message: "长度在 6 到 20 个字符", trigger: "blur" },
      ],
      role: [{ required: true, message: "请选择角色", trigger: "change" }],
    };

    const submitForm = async () => {
      const valid = await loginFormRef.value.validate();
      if (!valid) return;

      loading.value = true;
      try {
        const response = await request.post(api.USER_LOGIN, loginForm);

        if (response.success) {
          const userData = response.data;

          // 说明：后端使用 HttpSession 保存了 currentUser/userId/userRole。
          // 前端同时把关键信息缓存到 localStorage，便于页面直接读取。
          // 这里 userRole 以“后端返回”为准，避免用户在登录页选错角色导致前端状态错误。
          localStorage.setItem("userId", String(userData?.id ?? ""));
          localStorage.setItem("username", String(userData?.username ?? loginForm.username));
          localStorage.setItem("userRole", String(userData?.role ?? loginForm.role));
          localStorage.setItem("user", JSON.stringify(userData || {}));

          // 兼容：项目之前用 token 触发请求拦截器，这里继续保留一个占位 token。
          // 真实项目应由后端返回 JWT。
          if (!localStorage.getItem("token")) {
            localStorage.setItem("token", "mock-token-" + Date.now());
          }

          ElMessage.success(response.message || "登录成功");

          const role = String(userData?.role ?? loginForm.role);
          if (role === "TEACHER") {
            router.push("/teacher-dashboard");
          } else {
            router.push("/student-dashboard");
          }
        } else {
          ElMessage.error(response.message || "登录失败");
        }
      } catch (error) {
        ElMessage.error(error.message || "登录失败");
      } finally {
        loading.value = false;
      }
    };

    const resetForm = () => {
      loginFormRef.value.resetFields();
    };

    return {
      loginForm,
      rules,
      loginFormRef,
      loading,
      submitForm,
      resetForm,
    };
  },
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.login-card {
  width: 400px;
}

.card-header {
  text-align: center;
}

.register-link {
  text-align: center;
  margin-top: 15px;
}
</style>