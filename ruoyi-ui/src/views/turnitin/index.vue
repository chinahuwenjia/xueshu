<template>
  <div class="container">
    <header class="header d-flex align-items-center justify-content-between">
      <div class="logo-container d-flex align-items-center">
        <img src="../../static/turnitin/turnitin_logo.png" alt="TurnitinPlus Logo" class="logo">
        <h1 class="site-title">Turnitin 查重AI检测系统</h1>
      </div>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="常见问题" name="home"></el-tab-pane>
        <el-tab-pane label="提交查重" name="submission"></el-tab-pane>
<!--        <el-tab-pane label="使用说明" name="usage"></el-tab-pane>-->
        <el-tab-pane label="报告提取" name="result"></el-tab-pane>
      </el-tabs>
    </header>
    <main>
      <component :is="currentComponent" @navigate-to-result="navigateToResult"></component>
    </main>
  </div>
</template>

<script>
import HomePage from './HomePage.vue';
import SubmissionPage from './submit.vue';
import UsagePage from './UsagePage.vue';
import ResultPage from './result.vue';

export default {
  data() {
    return {
      activeTab: 'submission',
      components: {
        home: HomePage,
        submission: SubmissionPage,
        usage: UsagePage,
        result: ResultPage
      }
    };
  },
  computed: {
    currentComponent() {
      return this.components[this.activeTab];
    }
  },
  methods: {
    handleTabClick(tab) {
      this.activeTab = tab.name;
    },
    navigateToResult() {
      this.activeTab = 'result';
    }
  }
};
</script>

<style>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 1200px;
  margin: auto;
}

.header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background-color: #f8f8f8;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.logo-container {
  display: flex;
  align-items: center;
}

.header .logo {
  height: 40px;
  margin-right: 10px;
}

.site-title {
  font-size: 24px;
  font-weight: bold;
  margin: 0;
}

main {
  width: 100%;
  padding: 20px;
}
</style>
