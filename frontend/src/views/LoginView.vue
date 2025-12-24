<template>
  <div class="login-container">
    <div class="login-wrapper">
      <!-- 左侧品牌区域 -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="brand-logo">
            <el-icon :size="48"><Monitor /></el-icon>
          </div>
          <h1 class="brand-title">在线考试系统</h1>
          <p class="brand-desc">
            智能组卷 · 在线监考 · 自动阅卷 · 数据分析
          </p>
          <div class="brand-footer">
            <p>&copy; 2025 Online Exam System</p>
          </div>
        </div>
        <!-- 装饰背景圆 -->
        <div class="circle c1"></div>
        <div class="circle c2"></div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-container">
        <div class="form-header">
          <h2>欢迎登录</h2>
          <p>请输入您的账号和密码以继续</p>
        </div>

        <el-form
          :model="loginForm"
          :rules="rules"
          ref="loginFormRef"
          class="login-form"
          size="large"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
            ></el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="submitForm"
            ></el-input>
          </el-form-item>

          <el-form-item prop="role">
            <el-select v-model="loginForm.role" placeholder="请选择登录身份" style="width: 100%">
              <template #prefix>
                <el-icon><UserFilled /></el-icon>
              </template>
              <el-option label="我是教师" value="TEACHER"></el-option>
              <el-option label="我是学生" value="STUDENT"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="submit-btn" @click="submitForm" :loading="loading">
              登 录
            </el-button>
          </el-form-item>

          <div class="form-footer">
             <el-button link @click="resetForm">重置输入</el-button>
             <span class="divider">|</span>
             <router-link to="/register" class="register-link">注册新账号</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { User, Lock, Monitor, UserFilled } from '@element-plus/icons-vue';
import request from "../utils/request";
import api from "../config/api";

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
  role: [{ required: true, message: "请选择登录身份", trigger: "change" }],
};

const submitForm = async () => {
  const valid = await loginFormRef.value.validate();
  if (!valid) return;

  loading.value = true;
  try {
    const response = await request.post(api.USER_LOGIN, loginForm);

    if (response.success) {
      const userData = response.data;

      localStorage.setItem("userId", String(userData?.id ?? ""));
      localStorage.setItem("username", String(userData?.username ?? loginForm.username));
      localStorage.setItem("userRole", String(userData?.role ?? loginForm.role));
      localStorage.setItem("user", JSON.stringify(userData || {}));

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
</script>

<style scoped>
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%239C92AC' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.login-wrapper {
  display: flex;
  width: 900px;
  height: 550px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 左侧品牌区 */
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  padding: 40px;
  text-align: center;
}

.brand-content {
  position: relative;
  z-index: 2;
}

.brand-logo {
  margin-bottom: 20px;
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 15px;
  letter-spacing: 2px;
}

.brand-desc {
  font-size: 16px;
  opacity: 0.9;
  line-height: 1.6;
  margin-bottom: 40px;
}

.brand-footer {
  position: absolute;
  bottom: -100px;
  width: 100%;
  font-size: 12px;
  opacity: 0.6;
}

/* 装饰圆 */
.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.c1 {
  width: 200px;
  height: 200px;
  top: -50px;
  left: -50px;
}

.c2 {
  width: 300px;
  height: 300px;
  bottom: -100px;
  right: -100px;
}

/* 右侧表单区 */
.login-form-container {
  flex: 1;
  padding: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 30px;
  text-align: center;
}

.form-header h2 {
  font-size: 26px;
  color: #303133;
  margin-bottom: 10px;
}

.form-header p {
  color: #909399;
  font-size: 14px;
}

.login-form {
  margin-top: 10px;
}

.submit-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 2px;
  margin-top: 10px;
}

.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  font-size: 14px;
}

.divider {
  margin: 0 15px;
  color: #dcdfe6;
}

.register-link {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.register-link:hover {
  text-decoration: underline;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .login-wrapper {
    width: 90%;
    height: auto;
    flex-direction: column;
  }

  .login-brand {
    padding: 30px 20px;
    flex: none;
  }

  .brand-footer {
    display: none;
  }

  .login-form-container {
    padding: 30px 20px;
  }
}
</style>