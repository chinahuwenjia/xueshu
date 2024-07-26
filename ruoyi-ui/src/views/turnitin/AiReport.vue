<template>
  <div class="container">
    <div class="header">
      <div class="logo-wrapper">
        <img src="../../static/turnitin/turnitin_logo.png" alt="Turnitin Logo" class="logo" />
        <span class="important-note">【重要说明】AI结果提交后一般5-10分钟</span>
      </div>
    </div>
    <div class="form-wrapper">
      <div class="form-container" v-if="!isSubmitted">
        <el-form :model="form" ref="form" :rules="rules" label-position="top">
          <el-form-item label="查询码" prop="code">
            <el-input v-model="form.code" placeholder="请输入查询码"></el-input>
          </el-form-item>
          <el-form-item label="作业ID" prop="paperID">
            <el-input v-model="form.paperID" placeholder="请输入作业编号"></el-input>
          </el-form-item>
          <el-form-item label="学生ID(邮箱)" prop="email">
            <el-input v-model="form.email" placeholder="请输入学生ID(邮箱)"></el-input>
          </el-form-item>
          <div class="note">
            <ul>
              <li>查重时长取决于文档大小。</li>
              <li>通常情况下查重时长在 30 分钟之内。</li>
              <li>若已超过 30 分钟可联系客服确认情况。</li>
            </ul>
          </div>
          <el-form-item>
            <el-button type="primary" @click="handleSubmit" class="submit-button" :disabled="isDisabled">
              {{ isDisabled ? `请稍候... (${disabledCountdown})` : '提取AI报告' }}
            </el-button>
          </el-form-item>
          <div v-if="isDisabled" class="disabled-message">
            <p>请勿重复刷新，五分钟后再提取报告（一般AI报告在提交报告后30分钟左右才会生成）。</p>
          </div>
        </el-form>
      </div>
      <div v-else class="result-container">
        <div v-if="isDownloading" class="countdown">
          <p>正在努力加载中，预计需要 {{ countdown }} 秒，请勿刷新页面...</p>
          <el-progress :percentage="100 - countdown / 78 * 100" status="active"></el-progress>
        </div>
        <div v-else>
          <p>{{ statusMessage }}</p>
          <el-button type="primary" @click="handleDownload" class="submit-button" :disabled="!canDownload">
            立即下载报告
          </el-button>
          <p v-if="!canDownload" class="disabled-message">下载链接已过期，请重新生成报告。</p>
          <div v-if="canDownload" class="countdown">
            <p>报告有效期10分钟，请尽快下载。</p>
            <p>查重码是 {{ form.code }}。</p>
            <p>报告还有 <span class="highlight">{{ minutes }} 分 {{ seconds }} 秒</span> 失效。</p>
            <div class="download-timer">
              <div class="progress-bar" :style="{ width: progressBarWidth }"></div>
              <div class="particles"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { MessageBox } from 'element-ui';

export default {
  name: 'Turnitin Ai检测系统',
  data() {
    return {
      title: 'Turnitin Ai检测系统',
      form: {
        code: localStorage.getItem('lastCode') || '',
        paperID: '',
        email: ''
      },
      isSubmitted: false,
      loading: false,
      statusMessage: '',
      downloadUrl: '',
      isDownloading: false,
      countdown: 78,
      downloadCountdown: 600,
      disabledCountdown: localStorage.getItem('disabledCountdown') ? parseInt(localStorage.getItem('disabledCountdown')) : 20,
      isDisabled: localStorage.getItem('isDisabled') === 'true',
      countdownInterval: null,
      downloadInterval: null,
      disabledInterval: null,
      rules: {
        code: [
          { required: true, message: '请输入查询码', trigger: 'blur' }
        ],
        paperID: [
          { required: true, message: '请输入作业编号', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入学生ID(邮箱)', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ]
      }
    };
  },
  mounted() {
    if (this.isDisabled) {
      this.startDisabledCountdown();
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.resetStatus(); // Reset status before submitting
          this.isSubmitted = true;
          this.isDownloading = true;
          this.isDisabled = true;
          localStorage.setItem('isDisabled', 'true');
          this.startCountdown();
          this.startDisabledCountdown();
          localStorage.setItem('lastCode', this.form.code);

          let url = process.env.VUE_APP_BASE_API + '/turnitin/aiResult';
          console.log('Sending request with:', this.form);

          axios.get(url, { params: this.form })
            .then(response => {
              if (response.status === 200 && response.data) {
                this.isDownloading = false;
                this.statusMessage = response.data.msg;
                this.downloadUrl = response.data.downloaderUrl;
                this.startDownloadCountdown();
                clearInterval(this.countdownInterval);
              } else {
                this.isDownloading = false;
                clearInterval(this.countdownInterval);
                this.resetStatus();
                MessageBox.alert('请求失败，请稍后再试。', '错误', {
                  confirmButtonText: '确定',
                  type: 'error'
                }).then(() => {
                  this.resetForm();
                });
              }
            })
            .catch(error => {
              this.isDownloading = false;
              clearInterval(this.countdownInterval);
              this.resetStatus();
              if (error.response && error.response.data) {
                MessageBox.alert(error.response.data, '错误', {
                  confirmButtonText: '确定',
                  type: 'error'
                }).then(() => {
                  this.resetForm();
                });
              } else {
                MessageBox.alert('请求失败，请稍后再试。', '错误', {
                  confirmButtonText: '确定'
                }).then(() => {
                  this.resetForm();
                });
              }
              console.error('Error:', error);
            });
        } else {
          console.log('Error: form validation failed');
        }
      });
    },
    handleDownload() {
      if (this.downloadUrl) {
        window.location.href = this.downloadUrl;
      } else {
        console.log('Download URL is not set.');
      }
    },
    startCountdown() {
      this.countdown = 78;
      this.countdownInterval = setInterval(() => {
        if (this.countdown > 0) {
          this.countdown -= 1;
        } else {
          clearInterval(this.countdownInterval);
        }
      }, 1000);
    },
    startDownloadCountdown() {
      this.downloadCountdown = 600;
      this.downloadInterval = setInterval(() => {
        if (this.downloadCountdown > 0) {
          this.downloadCountdown -= 1;
        } else {
          clearInterval(this.downloadInterval);
          this.canDownload = false; // Set canDownload to false when the countdown reaches 0
        }
      }, 1000);
    },
    startDisabledCountdown() {
      this.disabledInterval = setInterval(() => {
        if (this.disabledCountdown > 0) {
          this.disabledCountdown -= 1;
          localStorage.setItem('disabledCountdown', this.disabledCountdown);
        } else {
          this.isDisabled = false;
          localStorage.setItem('isDisabled', 'false');
          clearInterval(this.disabledInterval);
        }
      }, 1000);
    },
    resetStatus() {
      this.statusMessage = '';
      this.downloadUrl = '';
      this.isSubmitted = false;
      this.isDownloading = false;
      clearInterval(this.downloadInterval);
    },
    resetForm() {
      this.resetStatus();
      this.form.code = localStorage.getItem('lastCode') || '';
      this.form.paperID = '';
      this.form.email = '';
      this.isDisabled = true;
      localStorage.setItem('isDisabled', 'true');
      this.disabledCountdown = 6;
      localStorage.setItem('disabledCountdown', this.disabledCountdown);
      this.startDisabledCountdown();
    }
  },
  computed: {
    canDownload() {
      return this.downloadCountdown > 0;
    },
    minutes() {
      return Math.floor(this.downloadCountdown / 60);
    },
    seconds() {
      return this.downloadCountdown % 60;
    },
    progressBarWidth() {
      return `${(this.downloadCountdown / 600) * 100}%`;
    }
  },
  beforeDestroy() {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
    if (this.disabledInterval) {
      clearInterval(this.disabledInterval);
    }
    if (this.downloadInterval) {
      clearInterval(this.downloadInterval);
    }
  }
};
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  min-height: 100vh;
  background-color: #f7f8fa;
}

.header {
  display: flex;
  justify-content: flex-start;
  width: 100%;
  margin-bottom: 20px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.logo {
  width: 140px;
  height: auto;
  margin-right: 20px;
}

.important-note {
  font-size: 16px;
  color: #333;
}

.form-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

.form-container, .result-container {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 800px;
  box-sizing: border-box;
}

.el-form-item {
  margin-bottom: 15px;
}

.note {
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.note ul {
  padding-left: 20px;
  list-style-type: disc;
}

.submit-button {
  width: 100%;
  transition: background-color 0.3s, color 0.3s;
}

.submit-button[disabled] {
  background-color: #d3d3d3;
  color: #666;
}

.disabled-message {
  margin-top: 10px;
  font-size: 14px;
  color: #666;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.loading-spinner {
  margin-bottom: 20px;
}

.countdown {
  margin-top: 20px;
  font-size: 16px;
  color: #333;
}

.error-message {
  color: red;
  font-size: 14px;
  margin-top: 10px;
}

.download-timer {
  width: 100%;
  height: 10px;
  background-color: #e0e0e0;
  border-radius: 5px;
  overflow: hidden;
  margin-top: 10px;
  position: relative;
}

.progress-bar {
  height: 100%;
  background-color: #007bff;
  transition: width 1s linear;
  position: absolute;
}

.particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
}

.particles::before, .particles::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0) 70%);
  opacity: 0.7;
  animation: particle-animation 3s infinite linear;
}

.particles::after {
  animation-delay: 1.5s;
}

@keyframes particle-animation {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

.highlight {
  font-weight: bold;
  font-size: 18px;
  color: #ff0000;
  animation: blink 1s steps(1) infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
