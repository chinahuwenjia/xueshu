<template>
  <div class="result-form card">
    <h2>报告提取</h2>

    <form v-if="!result" @submit.prevent="fetchResult">
      <div class="form-group">
        <label for="check-code">查重码</label>
        <input type="text" id="check-code" v-model="form.checkCode" required>
      </div>
      <div class="note">
        <p>查重时长取决于文档大小。</p>
        <p>通常情况下查重时长在 <span class="highlight">5-10 分钟</span> 之内。</p>
        <p>若已经超过 <span class="highlight">10 分钟</span> 可联系客服确认情况。</p>
      </div>
      <button type="submit" class="btn btn-primary btn-lg w-100">提取报告</button>
    </form>

    <div v-if="loading" class="loading-overlay">
      <div class="spinner"></div>
    </div>

    <div v-if="result">
      <h3>报告详情</h3>
      <div class="report-section">
        <div class="btn-group">
          <template v-if="result.type !== 'single_ai' && result.similarityPdfUrl">
            <a :href="result.similarityPdfUrl" target="_blank" class="icon-button success">
              <i class="fas fa-file-download"></i> 查重报告 ({{ result.similarity }}%)
            </a>
          </template>
          <template v-if="result.type !== 'single_check'">
            <a v-if="result.aiWritingPdfUrl && result.aiWritingPdfUrl.trim() !== ''" :href="result.aiWritingPdfUrl" target="_blank" class="icon-button info">
              <i class="fas fa-file-download"></i> AI报告 ({{ result.aiWriting }}%)
            </a>
            <span v-else class="icon-button warning">
          <i class="fas fa-exclamation-circle"></i> 该报告不符合Turnitin-AI生成规范，原因是({{ result.aiWriting }})
        </span>
          </template>
        </div>
      </div>

      <button @click="confirmDelete" class="icon-button danger delete-btn">
        <i class="fas fa-trash-alt"></i> 删除提交(下载完务必要删除)
      </button>
      <div class="note mt-3">
        <p>请注意：下载链接在 <span class="highlight">{{ countdown }} 分钟</span> 后将失效并删除报告。</p>
        <div class="countdown-bar">
          <div class="progress" :style="{ width: countdownWidth + '%' }"></div>
        </div>
      </div>
    </div>

    <custom-alert v-if="showAlert" :message="errorMessage" @close="showAlert = false"></custom-alert>
    <custom-alert v-if="showSuccessAlert" :message="successMessage" @close="showSuccessAlert = false"></custom-alert>
  </div>
</template>

<script>
import axios from 'axios';
import CustomAlert from './CustomAlert.vue';

export default {
  components: {
    CustomAlert,
  },
  data() {
    return {
      form: {
        checkCode: '',
      },
      result: null,
      errorMessage: '',
      successMessage: '',
      loading: false,
      showAlert: false,
      showSuccessAlert: false,
      countdown: 10,
      countdownInterval: null,
    };
  },
  computed: {
    countdownWidth() {
      return (this.countdown / 10) * 100;
    },
  },
  methods: {
    async fetchResult() {
      this.loading = true;
      try {
        let url = process.env.VUE_APP_BASE_API + '/turnitin/result';
        const response = await axios.get(url, {
          params: {code: this.form.checkCode}
        });
        const result = response;
        if (result.status === 200) {
          this.result = result.data;
          this.errorMessage = '';
          this.startCountdown();
        } else {
          this.errorMessage = result.message || '报告未找到';
          this.showAlert = true;
        }
      } catch (error) {
        this.errorMessage = `提取失败: ${error.response.data}`;
        this.showAlert = true;
      } finally {
        this.loading = false;
      }
    },
    startCountdown() {
      this.countdown = 10;
      this.countdownInterval = setInterval(() => {
        this.countdown--;
        if (this.countdown === 0) {
          clearInterval(this.countdownInterval);
          // Handle countdown end logic, such as deleting the report
        }
      }, 60000); // Update every minute
    },
    confirmDelete() {
      if (confirm('确认删除提交的文件与查重报告吗？')) {
        this.deleteReport();
      }
    },
    async deleteReport() {
      this.loading = true;
      try {
        let url = process.env.VUE_APP_BASE_API + '/turnitin/deleteResult';
        const response = await axios.get(url, {
          params: {code: this.form.checkCode}
        });
        const result = response;
        console.log(result);

        if (result.status === 200) {
          this.result = null;
          this.errorMessage = '';
          this.form.checkCode = '';
          this.showAlert = false;
          this.successMessage = '报告删除成功';
          this.showSuccessAlert = true;
        } else {
          this.errorMessage = result.body || '删除失败';
          this.showAlert = true;
        }
      } catch (error) {
        this.errorMessage = `删除失败: ${error.response.data}`;
        this.showAlert = true;
      } finally {
        this.loading = false;
      }
    }
  },
  beforeDestroy() {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }
};
</script>

<style>
/* 样式代码保持不变 */
.result-form {
  background-color: #fff;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.result-form h2 {
  margin-bottom: 15px;
  text-align: center;
  font-size: 22px;
  color: #333;
  font-weight: 600;
}

.form-group {
  margin-bottom: 15px;
}

.icon-button {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  margin: 5px;
  border-radius: 4px;
  text-decoration: none;
  font-size: 14px;
  color: #fff;
}

.icon-button i {
  margin-right: 6px;
}

.icon-button.success {
  background-color: #28a745;
}

.icon-button.info {
  background-color: #17a2b8;
}

.icon-button.warning {
  background-color: #ffc107;
  color: #212529; /* 修改为深色字体以确保可读性 */
}


.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: #555;
}

.form-group input[type="text"],
.form-group select {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
  border: 2px solid #ddd;
  border-radius: 6px;
  transition: border 0.3s ease;
}

.form-group input[type="text"]:focus,
.form-group select:focus {
  border-color: #007bff;
  outline: none;
}

button {
  background-color: #007bff;
  color: #fff;
  padding: 12px;
  border: none;
  cursor: pointer;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  transition: background-color 0.3s ease;
}

button:hover {
  background-color: #0056b3;
}

.icon-button {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 15px;
  border-radius: 6px;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.3s ease;
  font-size: 14px;
}

.icon-button i {
  margin-right: 8px;
}

.icon-button.success {
  background-color: #28a745;
}

.icon-button.success:hover {
  background-color: #218838;
}

.icon-button.info {
  background-color: #17a2b8;
}

.icon-button.info:hover {
  background-color: #138496;
}

.icon-button.danger {
  background-color: #dc3545;
}

.icon-button.danger:hover {
  background-color: #c82333;
}

.report-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border: 1px dashed #007bff;
  border-radius: 10px;
  margin-top: 20px;
}

.btn-group {
  display: flex;
  flex-direction: column;
  gap: 15px;
  width: 100%;
}

.delete-btn {
  margin-top: 20px;
  align-self: flex-end;
}

.note {
  margin-top: 15px;
  padding: 15px;
  background-color: #f8f9fa;
  border-left: 4px solid #007bff;
  border-radius: 6px;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.05);
}

.note p {
  margin: 0;
  color: #333;
  font-size: 14px;
}

.note .highlight {
  font-weight: 700;
  color: #007bff;
  font-size: 14px;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.spinner {
  border: 8px solid #f3f3f3;
  border-top: 8px solid #007bff;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* Countdown bar */
.countdown-bar {
  position: relative;
  width: 100%;
  height: 10px;
  background-color: #e9ecef;
  border-radius: 5px;
  overflow: hidden;
  margin-top: 10px;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
}

.progress {
  position: absolute;
  height: 100%;
  background-color: #007bff;
  transition: width 0.6s ease;
}
</style>
