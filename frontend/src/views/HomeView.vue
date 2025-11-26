<template>
  <div class="home">
    <el-container>
      <el-header>
        <h1>在线考试系统</h1>
      </el-header>

      <el-main>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card class="welcome-card">
              <template #header>
                <div class="card-header">
                  <span>欢迎使用在线考试系统</span>
                </div>
              </template>
              <p>
                这是一个面向教师和学生的在线考试平台，支持题库管理、试卷生成、在线考试和自动阅卷等功能。
              </p>
              <el-row :gutter="20" style="margin-top: 20px">
                <el-col :span="12">
                  <el-button
                    type="primary"
                    size="large"
                    @click="$router.push('/login')"
                    >教师登录</el-button
                  >
                </el-col>
                <el-col :span="12">
                  <el-button
                    type="success"
                    size="large"
                    @click="$router.push('/login')"
                    >学生登录</el-button
                  >
                </el-col>
              </el-row>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card class="feature-card">
              <template #header>
                <div class="card-header">
                  <span>系统功能</span>
                </div>
              </template>
              <ul>
                <li>题库管理：支持多种题型的添加、编辑和删除</li>
                <li>试卷生成：灵活组卷，自定义考试参数</li>
                <li>在线考试：倒计时功能，自动保存答案</li>
                <li>自动阅卷：客观题自动批改，即时出分</li>
                <li>成绩分析：多维度统计分析，支持导出</li>
              </ul>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: "HomeView",
};
</script>

<style scoped>
.home {
  height: 100vh;
  background-color: #f5f5f5;
}

.el-header {
  background-color: #409eff;
  color: white;
  text-align: center;
  line-height: 60px;
}

.welcome-card,
.feature-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feature-card ul {
  padding-left: 20px;
}

.feature-card li {
  margin-bottom: 10px;
}
</style>
