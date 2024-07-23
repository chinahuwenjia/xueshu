<template>
  <div class="container mt-5">
    <div class="header">
      <img src="../../static/turnitin/turnitin_logo.png" alt="Logo" class="logo">
      <div class="brand-info">
        <h1>Turnitin 查重AI监测系统</h1>
        <nav>
          <ul>
            <li><a href="#">Assignments</a></li>
            <li><a href="#">Students</a></li>
            <li><a href="#">Grade Book</a></li>
            <li><a href="#">Libraries</a></li>
            <li><a href="#">Calendar</a></li>
            <li><a href="#">Discussion</a></li>
            <li><a href="#">Preferences</a></li>
          </ul>
        </nav>
      </div>
    </div>
    <div class="content">
      <div class="content-header">
        <h2>查重</h2>
        <button class="btn btn-primary btn-lg" :disabled="isSubmitting" @click="submitFile">
          {{ submitButtonText }}
        </button>
        <p>今天还能查 {{ remainingSubmissions }} 次</p>
      </div>
      <div class="card">
        <div class="card-body">
          <table class="table table-striped table-responsive">
            <thead>
            <tr>
              <th>文件名字</th>
              <th>文件</th>
              <th>查重结果</th>
              <th>AI结果</th>
              <th>删除文章</th>
            </tr>
            </thead>
            <tbody>
            <tr v-if="files.length > 0" v-for="(file, index) in files" :key="index">
              <td>{{ file.name }}</td>
              <td>{{ file.file }}</td>
              <td>
                <button
                  class="btn btn-light"
                  @mouseover="showTooltip($event, '下载查重报告')"
                  @mouseout="hideTooltip"
                  @click="downloadFile(file.checkResultLink)"
                  :disabled="!file.canFetchCheckResult"
                >
                    <span :class="getColorClass(file.checkResultRate)">
                      {{ file.checkResultRate !== null ? `${file.checkResultRate}%` : '等待中...' }}
                    </span>
                </button>
              </td>
              <td>
                <button
                  class="btn btn-light"
                  @mouseover="showTooltip($event, '下载AI结果')"
                  @mouseout="hideTooltip"
                  @click="downloadFile(file.aiResultLink)"
                  :disabled="!file.canFetchAiResult"
                >
                    <span :class="getColorClass(file.aiResultRate)">
                      {{ file.aiResultRate !== null ? `${file.aiResultRate}%` : '等待中...' }}
                    </span>
                </button>
              </td>
              <td>
                <button class="btn btn-danger" @click="deleteFile(file.id, index)">删除</button>
              </td>
            </tr>
            <tr v-if="files.length === 0">
              <td colspan="5" class="text-center">暂无数据</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div v-if="tooltip.show" :style="tooltipStyle" class="tooltip">
        {{ tooltip.text }}
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      files: [],
      tooltip: {
        show: false,
        text: '',
        x: 0,
        y: 0,
      },
      isSubmitting: false,
      canSubmit: true,
      submitCooldown: 0,
      maxSubmissionsPerDay: 50,
      remainingSubmissions: 50,
      interval: null,
      submitButtonText: 'Submit File',
      checkInterval: null,
    };
  },
  methods: {
    async submitFile() {
      const lastSubmitTime = localStorage.getItem('lastSubmitTime');
      const currentTime = new Date().getTime();

      if (lastSubmitTime && (currentTime - lastSubmitTime) < 1 * 60 * 1000) {
        alert('请等待5分钟后再提交');
        return;
      }

      if (this.remainingSubmissions <= 0) {
        alert('今日提交次数已达上限');
        return;
      }

      // 模拟提交文件，并覆盖现有数据
      const newFile = {
        id: new Date().getTime(), // Simulate an ID for the file
        name: '新文件',
        file: 'newfile.txt',
        checkResultRate: null,
        checkResultLink: 'http://example.com/newCheckResult.pdf',
        aiResultRate: null,
        aiResultLink: 'http://example.com/newAiResult.pdf',
        canFetchCheckResult: false,
        canFetchAiResult: false
      };
      this.files = [newFile]; // 覆盖现有数据
      this.canSubmit = false;
      this.isSubmitting = true;
      this.submitCooldown = 1 * 60;
      this.remainingSubmissions--;

      localStorage.setItem('lastSubmitTime', currentTime);
      localStorage.setItem('remainingSubmissions', this.remainingSubmissions);
      localStorage.setItem('files', JSON.stringify(this.files));

      this.interval = setInterval(() => {
        if (this.submitCooldown > 0) {
          this.submitCooldown--;
          this.submitButtonText = `请等待 ${Math.floor(this.submitCooldown / 60)} 分钟 ${this.submitCooldown % 60} 秒`;
        } else {
          this.canSubmit = true;
          this.isSubmitting = false;
          this.submitButtonText = 'Submit File';
          clearInterval(this.interval);
          this.checkResults();
        }
      }, 1000);
    },
    async deleteFile(fileId, index) {
      try {
        // Call the backend API to delete the file
        await axios.delete(`https://your-backend-api.com/files/${fileId}`);

        // If successful, remove the file from the list
        this.files.splice(index, 1);
        localStorage.setItem('files', JSON.stringify(this.files));

        // Allow file re-upload
        this.isSubmitting = false;
        this.canSubmit = true;
        this.submitButtonText = 'Submit File';
        clearInterval(this.checkInterval); // Stop checking results when file is deleted
      } catch (error) {
        console.error('Error deleting file:', error);
        alert('删除文件时出错，请稍后再试');
      }
    },
    downloadFile(link) {
      const lastFetchTime = localStorage.getItem('lastFetchTime');
      const currentTime = new Date().getTime();

      if (lastFetchTime && (currentTime - lastFetchTime) < 1 * 60 * 1000) {
        alert('请等待5分钟后再获取结果');
        return;
      }

      localStorage.setItem('lastFetchTime', currentTime);

      const a = document.createElement('a');
      a.href = link;
      a.download = '';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    },
    showTooltip(event, text) {
      this.tooltip.show = true;
      this.tooltip.text = text;
      this.tooltip.x = event.clientX;
      this.tooltip.y = event.clientY;
    },
    hideTooltip() {
      this.tooltip.show = false;
      this.tooltip.text = '';
    },
    getColorClass(rate) {
      if (rate < 30) return 'text-success';
      if (rate >= 30 && rate <= 50) return 'text-warning';
      return 'text-danger';
    },
    async checkResults() {
      this.checkInterval = setInterval(async () => {
        // 模拟API请求以检查查重结果和AI结果
        console.log('检查查重和AI结果...');
        const file = this.files[0];

        // 模拟API请求延迟
        await new Promise(resolve => setTimeout(resolve, 2000));

        // 模拟检查结果
        file.checkResultRate = file.checkResultRate !== null ? file.checkResultRate : Math.random() > 0.5 ? Math.floor(Math.random() * 100) : null;
        file.aiResultRate = file.aiResultRate !== null ? file.aiResultRate : Math.random() > 0.5 ? Math.floor(Math.random() * 100) : null;

        if (file.checkResultRate !== null && file.aiResultRate !== null) {
          file.canFetchCheckResult = true;
          file.canFetchAiResult = true;
          localStorage.setItem('files', JSON.stringify(this.files));
          clearInterval(this.checkInterval);
        } else {
          console.log('查重和AI结果暂时没有结果，请稍后再试。');
        }
      }, 5 * 60 * 1000); // 每5分钟检查一次
    },
  },
  mounted() {
    const remainingSubmissions = localStorage.getItem('remainingSubmissions');
    this.remainingSubmissions = remainingSubmissions !== null ? parseInt(remainingSubmissions) : this.maxSubmissionsPerDay;

    const lastSubmitTime = localStorage.getItem('lastSubmitTime');
    const currentTime = new Date().getTime();

    if (lastSubmitTime && (currentTime - lastSubmitTime) < 1 * 60 * 1000) {
      this.canSubmit = false;
      this.submitCooldown = Math.floor((1 * 5 * 1000 - (currentTimelastSubmitTime)) / 1000);
      this.interval = setInterval(() => {
        if (this.submitCooldown > 0) {
          this.submitCooldown--;
          this.submitButtonText = `请等待 ${Math.floor(this.submitCooldown / 5)} 分钟 ${this.submitCooldown % 5} 秒`;
        } else {
          this.canSubmit = true;
          this.isSubmitting = false;
          this.submitButtonText = 'Submit File';
          clearInterval(this.interval);
          this.checkResults();
        }
      }, 1000);
    }
    const files = localStorage.getItem('files');
    if (files) {
      this.files = JSON.parse(files);
    }
  },
  computed: {
    tooltipStyle() {
      return {
        position: 'fixed',
        top: `${this.tooltip.y + 20}px`,
        left: `${this.tooltip.x + 20}px`,
        background: '#333',
        color: '#fff',
        padding: '5px 10px',
        borderRadius: '5px',
        pointerEvents: 'none',
    };  },
  },
};
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: auto;
  padding: 20px;
  background-color: #f9f9f9;
}

.header {
  display: flex;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 2px solid #ddd;
}

.logo {
  max-width: 100px;
}

.brand-info {
  margin-left: 20px;
}

.brand-info h1 {
  margin: 0;
}

.brand-info nav ul {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
  display: flex;
}

.brand-info nav ul li {
  margin-right: 20px;
}

.brand-info nav ul li a {
  text-decoration: none;
  color: #007bff;
}

.brand-info nav ul li a:hover {
  text-decoration: underline;
}

.content {
  margin-top: 20px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card {
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-top: 20px;
  background-color: #fff;
}

.card-body {
  padding: 15px;
}

.table {
  width: 100%;
  margin-bottom: 0;
}

.table thead th {
  text-align: left;
  padding: 10px;
}

.table tbody td {
  text-align: left;
  padding: 10px;
  vertical-align: middle;
}

.table tbody tr:hover {
  background-color: #f5f5f5;
}

.tooltip {
  z-index: 1000;
}

.text-success {
  color: #28a745 !important;
}

.text-warning {
  color: #ffc107 !important;
}

.text-danger {
  color: #dc3545 !important;
}

.btn-light {
  border: none;
  background-color: transparent;
}

.btn-light:hover {
  background-color: #f5f5f5;
}

.btn-primary {
  font-size: 1.25rem;
  padding: 10px 20px;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}

.btn-danger:hover {
  background-color: #c82333;
  color: white;
}
</style>
