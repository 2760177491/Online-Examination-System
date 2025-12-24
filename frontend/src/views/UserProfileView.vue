<template>
  <div class="page-container">
    <el-container class="full-height">
      <el-header class="app-header">
        <div class="header-inner">
          <div class="left">
            <el-button icon="el-icon-back" @click="goBack" plain>返回仪表板</el-button>
          </div>
          <div class="center-title">个人中心</div>
          <div class="right">
            <span class="welcome">当前用户：{{ currentUser?.username || username }}</span>
            <el-button type="danger" plain size="small" @click="logout">退出登录</el-button>
          </div>
        </div>
      </el-header>

      <el-main class="main-content">
        <div class="content-wrapper">
          <el-row :gutter="24">
            <!-- 左侧：个人信息卡片 -->
            <el-col :xs="24" :sm="24" :md="10" :lg="8">
              <el-card class="profile-card" shadow="hover">
                <div class="profile-header">
                  <el-avatar :size="80" icon="el-icon-user-solid" class="profile-avatar"></el-avatar>
                  <h3 class="username">{{ currentUser?.username }}</h3>
                  <el-tag size="small" effect="dark">{{ currentUser?.role }}</el-tag>
                </div>

                <el-divider></el-divider>

                <div class="profile-details">
                  <div class="detail-item">
                    <span class="label">用户ID</span>
                    <span class="value">{{ currentUser?.id }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">真实姓名</span>
                    <span class="value">{{ currentUser?.realName || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">邮箱</span>
                    <span class="value">{{ currentUser?.email || '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">注册时间</span>
                    <span class="value">{{ currentUser?.createdTime || '-' }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>

            <!-- 右侧：修改信息表单 -->
            <el-col :xs="24" :sm="24" :md="14" :lg="16">
              <el-card class="form-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span><i class="el-icon-edit"></i> 修改用户名</span>
                  </div>
                </template>

                <el-form :model="usernameForm" label-width="100px" class="user-form">
                  <el-form-item label="新用户名">
                    <el-input v-model="usernameForm.newUsername" placeholder="请输入新用户名" prefix-icon="el-icon-user" />
                  </el-form-item>

                  <el-form-item label="验证码">
                    <div class="captcha-row">
                      <el-input v-model="usernameForm.captchaCode" placeholder="计算结果" style="width: 140px" />
                      <div class="captcha-img-wrapper" @click="refreshCaptcha('username')" title="点击刷新">
                        <img
                          v-if="usernameCaptcha.imageUrl"
                          :src="usernameCaptcha.imageUrl"
                          alt="captcha"
                          class="captcha-img"
                        />
                        <span v-else class="captcha-placeholder">获取验证码</span>
                      </div>
                    </div>
                  </el-form-item>

                  <el-form-item>
                    <el-button type="primary" @click="submitUsername">保存修改</el-button>
                  </el-form-item>
                </el-form>
              </el-card>

              <el-card class="form-card" shadow="hover" style="margin-top: 20px">
                <template #header>
                  <div class="card-header">
                    <span><i class="el-icon-lock"></i> 修改密码</span>
                  </div>
                </template>

                <el-form :model="pwdForm" label-width="100px" class="user-form">
                  <el-form-item label="旧密码">
                    <el-input v-model="pwdForm.oldPassword" type="password" show-password prefix-icon="el-icon-key" />
                  </el-form-item>
                  <el-form-item label="新密码">
                    <el-input v-model="pwdForm.newPassword" type="password" show-password prefix-icon="el-icon-key" />
                  </el-form-item>

                  <el-form-item label="验证码">
                    <div class="captcha-row">
                      <el-input v-model="pwdForm.captchaCode" placeholder="计算结果" style="width: 140px" />
                      <div class="captcha-img-wrapper" @click="refreshCaptcha('password')" title="点击刷新">
                        <img
                          v-if="pwdCaptcha.imageUrl"
                          :src="pwdCaptcha.imageUrl"
                          alt="captcha"
                          class="captcha-img"
                        />
                        <span v-else class="captcha-placeholder">获取验证码</span>
                      </div>
                    </div>
                  </el-form-item>

                  <el-form-item>
                    <el-button type="danger" @click="submitPassword">修改密码</el-button>
                    <span class="pwd-tip"><i class="el-icon-warning-outline"></i> 修改成功后需重新登录</span>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import api from '@/config/api';

const router = useRouter();

const username = ref(localStorage.getItem('username') || '');
const currentUser = ref(null);

const usernameCaptcha = reactive({ captchaId: '', imageUrl: '' });
const pwdCaptcha = reactive({ captchaId: '', imageUrl: '' });

const usernameForm = reactive({
  newUsername: '',
  captchaCode: '',
});

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  captchaCode: '',
});

const loadCurrentUser = async () => {
  try {
    const resp = await request.get(api.USER_CURRENT);
    if (resp && resp.success) {
      currentUser.value = resp.data;
      username.value = resp.data?.username || username.value;
      localStorage.setItem('username', username.value);
      localStorage.setItem('user', JSON.stringify(resp.data || {}));
    } else {
      ElMessage.warning(resp?.message || '获取当前用户信息失败');
    }
  } catch (e) {
    console.error(e);
    ElMessage.warning('获取当前用户信息失败（可能未登录或会话过期）');
  }
};

const refreshCaptcha = async (type) => {
  const resp = await request.get(api.USER_CAPTCHA);
  if (!resp || !resp.success) {
    ElMessage.error(resp?.message || '获取验证码失败');
    return;
  }
  const data = resp.data || {};

  // 后端返回 imageBase64（不含 data 前缀），前端拼接成可直接展示的 img src
  const imageUrl = data.imageBase64 ? `data:image/png;base64,${data.imageBase64}` : '';

  if (type === 'username') {
    usernameCaptcha.captchaId = data.captchaId;
    usernameCaptcha.imageUrl = imageUrl;
    usernameForm.captchaCode = '';
  } else {
    pwdCaptcha.captchaId = data.captchaId;
    pwdCaptcha.imageUrl = imageUrl;
    pwdForm.captchaCode = '';
  }
};

const submitUsername = async () => {
  if (!usernameForm.newUsername.trim()) {
    ElMessage.warning('请输入新用户名');
    return;
  }
  if (!usernameCaptcha.captchaId) {
    ElMessage.warning('请先获取验证码');
    return;
  }
  if (!usernameForm.captchaCode.trim()) {
    ElMessage.warning('请输入图片中的验证码');
    return;
  }

  const payload = {
    newUsername: usernameForm.newUsername.trim(),
    captchaId: usernameCaptcha.captchaId,
    captchaCode: usernameForm.captchaCode.trim(),
  };

  const resp = await request.post(api.USER_UPDATE_USERNAME, payload);
  if (resp && resp.success) {
    ElMessage.success(resp.message || '用户名修改成功');

    // 前端同步更新本地缓存，但不登出
    const user = resp.data || {};
    localStorage.setItem('username', String(user.username || payload.newUsername));
    localStorage.setItem('user', JSON.stringify(user || {}));
    username.value = String(user.username || payload.newUsername);

    // 清空表单
    usernameForm.newUsername = '';
    usernameForm.captchaCode = '';
    usernameCaptcha.imageUrl = '';
    usernameCaptcha.captchaId = '';

    loadCurrentUser(); // 重新加载最新信息
  } else {
    ElMessage.error(resp?.message || '修改失败');
    refreshCaptcha('username');
  }
};

const submitPassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请输入旧密码和新密码');
    return;
  }
  if (!pwdCaptcha.captchaId) {
    ElMessage.warning('请先获取验证码');
    return;
  }
  if (!pwdForm.captchaCode.trim()) {
    ElMessage.warning('请输入图片中的验证码');
    return;
  }

  const payload = {
    oldPassword: pwdForm.oldPassword,
    newPassword: pwdForm.newPassword,
    captchaId: pwdCaptcha.captchaId,
    captchaCode: pwdForm.captchaCode.trim(),
  };

  try {
    const resp = await request.post(api.USER_UPDATE_PASSWORD, payload);
    if (resp && resp.success) {
      ElMessage.success('密码修改成功，请重新登录');
      logout();
    } else {
      ElMessage.error(resp?.message || '修改失败');
      refreshCaptcha('password');
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '修改失败');
    refreshCaptcha('password');
  }
};

const logout = async () => {
  try {
    await request.post('/api/users/logout');
  } catch (e) {
    // ignore
  }
  localStorage.clear();
  router.push('/login');
};

const goBack = () => {
  const role = localStorage.getItem('userRole');
  if (role === 'TEACHER') {
    router.push('/teacher-dashboard');
  } else {
    router.push('/student-dashboard');
  }
};

onMounted(() => {
  loadCurrentUser();
  refreshCaptcha('username');
  refreshCaptcha('password');
});
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

.full-height {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Header */
.app-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
  height: 60px;
  line-height: 60px;
  z-index: 10;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.center-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.welcome {
  margin-right: 15px;
  font-size: 14px;
  color: #606266;
}

/* Main Content */
.main-content {
  flex: 1;
  padding: 20px;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

/* Profile Card */
.profile-card {
  text-align: center;
  margin-bottom: 20px;
}

.profile-header {
  padding: 20px 0;
}

.profile-avatar {
  background-color: #409eff;
  margin-bottom: 15px;
}

.username {
  margin: 10px 0;
  font-size: 22px;
  color: #303133;
}

.profile-details {
  padding: 10px 20px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item .label {
  color: #909399;
}

.detail-item .value {
  color: #303133;
  font-weight: 500;
}

/* Form Card */
.form-card {
  border-radius: 4px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.user-form {
  padding: 10px 20px 0 0;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 15px;
}

.captcha-img-wrapper {
  height: 32px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.captcha-img {
  height: 100%;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.captcha-placeholder {
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
}

.pwd-tip {
  margin-left: 15px;
  font-size: 12px;
  color: #e6a23c;
}
</style>
