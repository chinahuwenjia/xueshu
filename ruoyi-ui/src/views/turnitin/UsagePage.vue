<template>
  <div class="usage-page">
    <h2>使用说明</h2>
    <el-steps :active="activeStep" finish-status="success">
      <el-step title="填写信息">
        <template #icon>
          <i class="el-icon-edit"></i>
        </template>
      </el-step>
      <el-step title="上传文件">
        <template #icon>
          <i class="el-icon-upload"></i>
        </template>
      </el-step>
      <el-step title="输入查重码">
        <template #icon>
          <i class="el-icon-document"></i>
        </template>
      </el-step>
      <el-step title="提交查重">
        <template #icon>
          <i class="el-icon-check"></i>
        </template>
      </el-step>
      <el-step title="等待结果">
        <template #icon>
          <i class="el-icon-time"></i>
        </template>
      </el-step>
    </el-steps>
    <div class="step-content">
      <div v-if="activeStep === 0">
        <h3>填写信息</h3>
        <p>在提交查重页面，填写论文的标题和作者信息。</p>
      </div>
      <div v-if="activeStep === 1">
        <h3>上传文件</h3>
        <p>点击或拖拽文件到上传区域。支持的文件格式为DOC、DOCX、PDF和TXT，文件大小不超过30MB。</p>
        <img src="../../static/turnitin/guide.jpg" alt="提交查重示例" class="example-image">
      </div>
      <div v-if="activeStep === 2">
        <h3>输入查重码</h3>
        <p>输入你收到的查重码。</p>
      </div>
      <div v-if="activeStep === 3">
        <h3>提交查重</h3>
        <p>点击“提交查重”按钮，开始查重。</p>
      </div>
      <div v-if="activeStep === 4">
        <h3>等待结果</h3>
        <p>查重过程通常需要5-10分钟，查重时间取决于文档大小。如果超过10分钟没有收到结果，请联系客服。</p>
      </div>
      <el-button @click="prevStep" :disabled="activeStep === 0">上一步</el-button>
      <el-button @click="nextStep" :disabled="activeStep === 4">下一步</el-button>
    </div>
    <el-divider></el-divider>
    <el-steps :active="activeStepDownload" finish-status="success">
      <el-step title="输入查重码">
        <template #icon>
          <i class="el-icon-document"></i>
        </template>
      </el-step>
      <el-step title="选择报告类型">
        <template #icon>
          <i class="el-icon-folder"></i>
        </template>
      </el-step>
      <el-step title="提取报告">
        <template #icon>
          <i class="el-icon-download"></i>
        </template>
      </el-step>
      <el-step title="下载报告">
        <template #icon>
          <i class="el-icon-check"></i>
        </template>
      </el-step>
    </el-steps>
    <div class="step-content">
      <div v-if="activeStepDownload === 0">
        <h3>输入查重码</h3>
        <p>在报告提取页面，输入你收到的查重码。</p>
      </div>
      <div v-if="activeStepDownload === 1">
        <h3>选择报告类型</h3>
        <p>选择要下载的报告类型，查重报告或AI报告。</p>
      </div>
      <div v-if="activeStepDownload === 2">
        <h3>提取报告</h3>
        <p>点击“提取报告”按钮。</p>
      </div>
      <div v-if="activeStepDownload === 3">
        <h3>下载报告</h3>
        <p>如果查重成功，你将看到下载报告按钮。点击按钮下载报告。如果没有报告，请稍后再试或联系客服。</p>
        <img src="../../static/turnitin/guide.jpg" alt="下载报告示例" class="example-image">
      </div>
      <el-button @click="prevStepDownload" :disabled="activeStepDownload === 0">上一步</el-button>
      <el-button @click="nextStepDownload" :disabled="activeStepDownload === 3">下一步</el-button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      activeStep: 0,
      activeStepDownload: 0
    };
  },
  methods: {
    nextStep() {
      if (this.activeStep < 4) {
        this.activeStep++;
      }
    },
    prevStep() {
      if (this.activeStep > 0) {
        this.activeStep--;
      }
    },
    nextStepDownload() {
      if (this.activeStepDownload < 3) {
        this.activeStepDownload++;
      }
    },
    prevStepDownload() {
      if (this.activeStepDownload > 0) {
        this.activeStepDownload--;
      }
    }
  }
};
</script>

<style>
.usage-page {
  background-color: #fff;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  max-width: 800px;
  margin: 20px auto;
}

.usage-page h2 {
  text-align: center;
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.section {
  margin-bottom: 20px;
}

.section h3 {
  font-size: 20px;
  color: #007bff;
  margin-bottom: 10px;
}

.step-content {
  margin: 20px 0;
}

.example-image {
  display: block;
  max-width: 100%;
  margin-top: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.el-button {
  margin: 10px 5px 0 5px;
}
</style>
