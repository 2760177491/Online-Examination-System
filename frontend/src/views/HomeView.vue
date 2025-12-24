<template>
  <div class="home">
    <el-container class="full-height">
      <el-header class="app-header">
        <div class="header-inner">
          <div class="logo">
            <i class="el-icon-monitor"></i>
            <h1>在线考试系统</h1>
          </div>
          <div class="header-actions">
            <!-- 可以放置一些顶部导航或链接 -->
          </div>
        </div>
      </el-header>

      <el-main class="app-main">
        <div class="hero-section">
          <div class="hero-content">
            <h2 class="hero-title">智能、高效、便捷的在线考核平台</h2>
            <p class="hero-subtitle">
              全流程数字化管理，支持题库建设、智能组卷、在线监考与自动阅卷。
            </p>
            <div class="hero-actions">
              <el-button
                type="primary"
                size="large"
                class="action-btn teacher-btn"
                @click="$router.push('/login')"
              >
                我是教师
              </el-button>
              <el-button
                type="success"
                size="large"
                class="action-btn student-btn"
                @click="$router.push('/login')"
              >
                我是学生
              </el-button>
            </div>
          </div>
          <div class="hero-image">
            <!-- 这里可以用一个 SVG 插图或者图片，暂时用 CSS 模拟 -->
            <div class="placeholder-img">
              <span>Online Exam</span>
            </div>
          </div>
        </div>

        <div class="features-section">
          <h3 class="section-title">核心功能</h3>
          <el-row :gutter="30">
            <el-col :xs="24" :sm="12" :md="8" v-for="(feature, index) in features" :key="index">
              <el-card class="feature-card" shadow="hover">
                <div class="feature-icon">
                  <component :is="feature.icon" />
                </div>
                <h4>{{ feature.title }}</h4>
                <p>{{ feature.desc }}</p>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-main>

      <el-footer class="app-footer">
        <p>&copy; 2025 在线考试系统. All Rights Reserved.</p>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import {
  Document,
  Files,
  Timer,
  Check,
  DataAnalysis,
  Monitor
} from '@element-plus/icons-vue';

const features = ref([
  { title: '题库管理', desc: '支持单选、多选、判断、主观题等多种题型，便捷录入与管理。', icon: Document },
  { title: '智能组卷', desc: '灵活的组卷策略，支持随机抽题与自定义考试参数设置。', icon: Files },
  { title: '在线考试', desc: '实时倒计时，答题进度自动保存，断点续考无忧。', icon: Timer },
  { title: '自动阅卷', desc: '客观题系统自动秒批，主观题教师在线批阅，效率倍增。', icon: Check },
  { title: '成绩分析', desc: '多维度成绩统计与图表展示，教学效果一目了然。', icon: DataAnalysis },
  { title: '考试监控', desc: '实时监控考试状态，防作弊机制保障考试公平公正。', icon: Monitor },
]);
</script>

<style scoped>
.home {
  min-height: 100vh;
  background-color: #f8f9fa;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

.full-height {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Header Styles */
.app-header {
  background: linear-gradient(90deg, #409eff, #337ecc);
  color: white;
  height: 64px;
  line-height: 64px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0 40px;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 1px;
}

/* Main Content Styles */
.app-main {
  flex: 1;
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* Hero Section */
.hero-section {
  background: white;
  padding: 80px 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 60px;
  flex-wrap: wrap;
}

.hero-content {
  max-width: 500px;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 20px;
  line-height: 1.2;
  background: -webkit-linear-gradient(45deg, #303133, #409eff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 18px;
  color: #606266;
  margin-bottom: 40px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 20px;
}

.action-btn {
  width: 140px;
  font-weight: 600;
  border-radius: 8px;
  transition: transform 0.2s;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.hero-image .placeholder-img {
  width: 400px;
  height: 300px;
  background: #ecf5ff;
  border-radius: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #409eff;
  font-size: 24px;
  font-weight: bold;
  box-shadow: 0 10px 30px rgba(64, 158, 255, 0.1);
}

/* Features Section */
.features-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 20px;
  width: 100%;
}

.section-title {
  text-align: center;
  font-size: 32px;
  color: #303133;
  margin-bottom: 50px;
  position: relative;
}

.section-title::after {
  content: '';
  display: block;
  width: 60px;
  height: 4px;
  background: #409eff;
  margin: 15px auto 0;
  border-radius: 2px;
}

.feature-card {
  text-align: center;
  padding: 30px 20px;
  border: none;
  border-radius: 12px;
  margin-bottom: 30px;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 40px;
  color: #409eff;
  margin-bottom: 20px;
  height: 60px;
  width: 60px;
  line-height: 60px;
  background: #ecf5ff;
  border-radius: 50%;
  margin: 0 auto 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.feature-card h4 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 15px;
}

.feature-card p {
  color: #909399;
  line-height: 1.6;
  font-size: 14px;
}

/* Footer */
.app-footer {
  background: #303133;
  color: #909399;
  text-align: center;
  padding: 20px;
  height: auto;
}

@media (max-width: 768px) {
  .hero-section {
    flex-direction: column-reverse;
    text-align: center;
    padding: 40px 20px;
  }

  .hero-image .placeholder-img {
    width: 100%;
    max-width: 300px;
    height: 200px;
  }

  .hero-actions {
    justify-content: center;
  }

  .hero-title {
    font-size: 32px;
  }
}
</style>
