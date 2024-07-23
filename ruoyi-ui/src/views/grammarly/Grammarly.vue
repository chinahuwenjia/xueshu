<template>
  <div id="grammary-check">
    <header class="header">
      <img src="../../static/turnitin/grammarly-0fb692ef.svg" alt="Grammarly Logo" class="logo">
      <div class="header-text">
        <span>权威准确</span>
        <span>安全快速</span>
      </div>
    </header>
    <main class="main-content">
      <aside class="sidebar">
        <section class="help-section">
          <h3>获取帮助</h3>
          <ul>
            <li><a href="#"><img src="../../static/turnitin/icons8-常问问题-50.png" width="24" height="24" alt="FAQ">常见问题</a></li>
            <li><a href="//wpa.qq.com/msgrd?v=3&uin=3056308&site=qq&menu=yes" target="_blank"><img src="../../static/turnitin/icons8-客户支持-50.png"
                                                                                                   height="24" width="24"  alt="QQ">QQ客服</a>
            </li>
            <li><a href="#"><img src="../../static/turnitin/icons8-例-50.png" height="24" width="24" alt="Sample">报告样例</a></li>
          </ul>
        </section>
        <section class="database-section">
          <h3>系统检测文献库</h3>
          <p>仅用于英语语法检查</p>
        </section>
      </aside>
      <section class="content">
        <div class="tab-menu">
          <button :class="{ active: currentTab === 'upload' }" @click="currentTab = 'upload'">上传文件</button>
          <button :class="{ active: currentTab === 'results' }" @click="currentTab = 'results'">查重结果</button>
        </div>
        <div v-if="currentTab === 'upload'" class="tab-content">
          <div class="notice">
            请尽量控制在1万字内，否则系统会检测不成功，谢谢...
          </div>
          <div class="upload-section">
            <div class="upload-container">
              <div class="file-upload">
                <label v-if="!uploadedFile" for="file-upload-input" class="file-upload-label">
                  <img src="../../static/turnitin/icons8-upload-to-cloud-50.png" height="80" width="80"/>
                  本地上传
                </label>
                <div v-else class="uploaded-file-info">
                  <p>已上传文件: {{ uploadedFileName }}</p>
                  <p>字数: {{ wordCount }}</p>
                  <button @click="resetUpload">重新上传</button>
                </div>
                <input type="file" id="file-upload-input" @change="handleFileUpload" ref="fileInput" v-if="!uploadedFile"
                       accept=".doc,.docx,.odt,.rtf,.txt">
              </div>
              <div class="upload-instructions">
                <h4>上传须知</h4>
                <ul>
                  <li>仅支持上传doc、docx、odt、rtf、txt</li>
                  <li>文档大小不超过4M</li>
                  <li>论文文档命名格式：<b>作者_标题</b> 例：张三_关于加强发电企业内部控制的思考</li>
                </ul>
                <div v-if="uploadedFile" class="file-details">
                  <p><span class="file-size">{{ wordCount }} 字</span></p>
                  <p>{{ uploadedFileName }}</p>
                </div>
              </div>
            </div>
            <div class="form-section">
              <div class="input-group">
                <label for="redeem-code">兑换码：</label>
                <input type="text" id="redeem-code" v-model="redeemCode" placeholder="请输入兑换码" required>
              </div>
              <div class="input-group" v-if="uploadedFile">
                <label for="paper-title">论文题目：</label>
                <input type="text" id="paper-title" v-model="paperTitle" required>
              </div>
            </div>
          </div>
          <button class="submit-button" @click="submitPaper">提交论文</button>
          <p class="warning-text" @click="handlePaymentIssue">微信或支付宝已支付，但论文提交失败怎么办？</p>
          <section class="submission-notice">
            <h3>论文提交检测须知</h3>
            <ul>
              <li>
                <img src="../../static/turnitin/icons8-介绍-24.png"
                     height="24" width="24" alt="系统介绍图标"/>
                <div>
                  <span>系统介绍：</span>解决英语论文的各类语法问题，24小时自助检测，实时对你提交的文本进行拼写、语法、用词进行校对。
                </div>
              </li>
              <li>
                <img src="../../static/turnitin/icons8-提醒-30.png" alt="重要提醒图标">
                <div>
                  <span>重要提醒：</span>不支持中文的语法检测。
                </div>
              </li>
              <li>
                <img src="../../static/turnitin/icons8-download-50.png" alt="报告下载图标">
                <div>
                  <span>报告下载：</span>语法报告保留7天，请尽快下载报告到电脑永久保存。
                </div>
              </li>
            </ul>
          </section>
        </div>
        <div v-if="currentTab === 'results'" class="tab-content">
          <div class="report-query">
            <h3>报告查询结果</h3>
            <input type="text" v-model="queryRedeemCode" placeholder="请输入兑换码" required>
            <button @click="queryReport">查询报告</button>
          </div>
          <div v-if="reportAvailable" class="report-result">
            <h3>检测报告</h3>
            <div class="report-actions">
              <button @click="downloadReport" class="download-button">
                <img src="../../static/turnitin/icons8-下载-30.png" alt="下载报告">
                下载报告
              </button>
              <button @click="deleteReport" class="delete-button">
                删除报告
              </button>
            </div>
            <p class="warning-text">请在下载后及时删除报告，确保您的隐私安全。</p>
          </div>
          <div v-else-if="queryError" class="report-error">
            <p>未找到报告，错误详情：{{ queryError }}</p>
          </div>
        </div>
      </section>
      <div v-if="loading" class="loading-overlay">
        <div class="loading-spinner"></div>
      </div>
    </main>

    <!-- 弹窗 -->
    <div v-if="showModal" class="modal">
      <div class="modal-content">
        <span class="close" @click="closeModal">&times;</span>
        <p>请刷新页面并重新提交。</p>
      </div>
    </div>
  </div>
</template>

<script>
import mammoth from 'mammoth';
import { submit, query } from '@/api/turnitin/grammarly';

export default {
  data() {
    return {
      currentTab: 'upload',
      redeemCode: '',
      queryRedeemCode: '',
      uploadedFile: null,
      uploadedFileName: '',
      wordCount: 0,
      paperTitle: '',
      reportAvailable: false,
      queryError: '',
      loading: false,
      showModal: false,
    };
  },
  methods: {
    async handleFileUpload(event) {
      const file = event.target.files[0];
      if (file && this.isValidFileType(file)) {
        this.uploadedFile = file;
        this.uploadedFileName = file.name;
        if (file.size > 4 * 1024 * 1024) { // 文件大小超过4M
          alert('文件大小不能超过4M');
          this.resetUpload();
          return;
        }
        try {
          const content = await this.readFileContent(file);
          this.wordCount = this.calculateWordCount(content);
          if (this.wordCount > 100000) { // 字数超过100000
            alert('文件字数不能超过100000');
            this.resetUpload();
            return;
          }
          this.paperTitle = this.extractTitleFromFileName(file.name);
        } catch (error) {
          console.error("Error reading file content:", error);
        }
      } else {
        alert('仅支持上传doc、docx、odt、rtf、txt格式的文件');
        event.target.value = '';
      }
    },
    isValidFileType(file) {
      const validTypes = [
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.oasis.opendocument.text',
        'application/rtf',
        'text/plain'
      ];
      return validTypes.includes(file.type);
    },
    resetUpload() {
      this.uploadedFile = null;
      this.uploadedFileName = '';
      this.wordCount = 0;
      this.paperTitle = '';
      this.$refs.fileInput.value = null;
    },
    readFileContent(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = async e => {
          try {
            const arrayBuffer = e.target.result;
            const result = await mammoth.extractRawText({ arrayBuffer });
            resolve(result.value);
          } catch (error) {
            reject(error);
          }
        };
        reader.onerror = e => reject(e);
        reader.readAsArrayBuffer(file);
      });
    },
    calculateWordCount(content) {
      const text = content.replace(/[\r\n\s]+/g, ' ').trim();
      return text.split(/\s+/).filter(word => word.length > 0).length;
    },
    extractTitleFromFileName(fileName) {
      const title = fileName.split('_')[1] || fileName;
      return title.replace(/\.[^/.]+$/, ''); // 去掉扩展名
    },
    async submitPaper() {
      if (!this.uploadedFile) {
        alert('请上传文件');
        return;
      }
      if (this.uploadedFile && this.redeemCode && this.paperTitle) {
        this.loading = true;

        const formData = new FormData();
        formData.append('code', this.redeemCode);
        formData.append('file', this.uploadedFile);

        try {
          const response = await submit(formData);
          console.log('提交成功:', response.data);
          this.loading = false;
          this.reportAvailable = true;
          this.currentTab = 'results';
        } catch (error) {
          console.error('提交失败:', error);
          this.loading = false;
          alert('提交失败，请重试');
        }
      } else {
        alert('请填写所有必填项');
      }
    },
    async queryReport() {
      if (this.queryRedeemCode) {
        this.loading = true;
        try {
          const response = await query(this.queryRedeemCode);
          console.log('查询报告成功:', response.data);
          this.reportAvailable = true;
          this.loading = false;
          // 假设 response.data 包含报告信息
          // 你可以在这里处理 response.data 以显示报告信息
        } catch (error) {
          console.error('查询报告失败:', error);
          this.queryError = '未找到报告或查询失败，请重试';
          this.reportAvailable = false;
          this.loading = false;
        }
      } else {
        alert('请输入兑换码');
      }
    },
    handlePaymentIssue() {
      this.showModal = true;
    },
    closeModal() {
      this.showModal = false;
    },
  }
};
</script>

<style scoped>
#grammary-check {
  font-family: Arial, sans-serif;
  color: #333;
}

.header {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: #f8f8f8;
  border-bottom: 1px solid #ddd;
}

.logo {
  width: 150px;
}

.header-text {
  margin-left: 20px;
}

.header-text span {
  display: block;
}

.main-content {
  display: flex;
  padding: 20px;
}

.sidebar {
  width: 200px;
  margin-right: 20px;
}

.help-section, .database-section {
  margin-bottom: 20px;
}

.help-section h3, .database-section h3 {
  font-size: 18px;
  margin-bottom: 10px;
}

.help-section ul {
  list-style: none;
  padding: 0;
}

.help-section ul li {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.help-section ul li a {
  text-decoration: none;
  color: #4caf50;
  display: flex;
  align-items: center;
}

.help-section ul li img {
  margin-right: 10px;
}

.database-section p {
  margin: 0;
}

.content {
  flex: 1;
}

.tab-menu {
  display: flex;
  margin-bottom: 20px;
}

.tab-menu button {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  background-color: #f8f8f8;
  cursor: pointer;
  outline: none;
}

.tab-menu button.active {
  background-color: #4caf50;
  color: #fff;
}

.tab-content {
  border: 1px solid #ddd;
  padding: 20px;
}

.notice {
  background-color: #ffeb3b;
  padding: 10px;
  border-radius: 5px;
  margin-bottom: 20px;
  text-align: center;
  font-weight: bold;
}

.upload-section {
  margin-bottom: 20px;
}

.upload-container {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
}

.file-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 20px;
}

.file-upload input {
  display: none;
}

.file-upload-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  background-color: #4caf50;
  color: #fff;
  cursor: pointer;
  border-radius: 5px;
  font-size: 16px;
  margin-bottom: 10px;
  text-align: center;
}

.file-upload-label img {
  width: 50px;
  height: 50px;
  margin-bottom: 10px;
}

.uploaded-file-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 20px;
  background-color: #4caf50;
  color: #fff;
  border-radius: 5px;
  font-size: 16px;
}

.uploaded-file-info p {
  margin: 5px 0;
}

.upload-instructions {
  flex: 1;
  margin-left: 20px;
}

.upload-instructions h4 {
  margin-bottom: 10px;
  font-size: 14px; /* 调整字体大小 */
  color: #333;
}

.upload-instructions ul {
  list-style: none;
  padding: 0;
}

.upload-instructions ul li {
  margin-bottom: 5px; /* 调整间距 */
  color: #666;
  font-size: 14px; /* 调整字体大小 */
}

.file-details {
  margin-top: 10px;
}

.file-details p {
  margin: 0;
}

.file-size {
  color: red;
}

.form-section {
  margin-top: 20px;
}

.input-group {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.input-group label {
  width: 80px;
  font-weight: bold;
  margin-right: 10px;
}

.input-group input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  box-sizing: border-box;
}

.price-section {
  margin-bottom: 20px;
}

.payment-methods {
  display: flex;
  align-items: center;
}

.payment-methods img {
  margin-right: 10px;
}

.payment-methods p {
  margin: 0;
  font-size: 16px;
}

.submit-button {
  width: 100%;
  padding: 10px;
  background-color: #4caf50;
  color: #fff;
  border: none;
  cursor: pointer;
  font-size: 16px;
  border-radius: 5px;
}

.warning-text {
  margin-top: 10px;
  color: red;
  text-align: center;
  cursor: pointer; /* 添加鼠标悬停效果 */
}

.submission-notice {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ddd;
}

.submission-notice h3 {
  font-size: 18px;
  margin-bottom: 10px;
}

.submission-notice ul {
  list-style: none;
  padding: 0;
}

.submission-notice ul li {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.submission-notice ul li img {
  width: 20px;
  height: 20px;
  margin-right: 10px;
}

.submission-notice ul li div {
  display: flex;
  flex-direction: column;
}

.submission-notice ul li span {
  font-weight: bold;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid #ddd;
}

.footer-bottom p {
  margin: 0;
  font-size: 14px;
}

.partners {
  list-style: none;
  padding: 0;
  display: flex;
}

.partners li {
  margin-right: 10px;
}

.partners li a {
  text-decoration: none;
  color: #4caf50;
}

.report-query {
  text-align: center;
}

.report-query input {
  padding: 5px;
  width: 200px;
  margin-right: 10px;
  border: 1px solid #ddd;
  border-radius: 3px;
}

.report-query button {
  padding: 5px 10px;
  background-color: #4caf50;
  color: #fff;
  border: none;
  cursor: pointer;
  border-radius: 3px;
}

.report-result {
  text-align: center;
}

.report-actions {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.download-button, .delete-button {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  margin: 0 10px;
  border: none;
  cursor: pointer;
  font-size: 16px;
  border-radius: 5px;
  outline: none;
}

.download-button {
  background-color: #4caf50;
  color: #fff;
}

.download-button img {
  margin-right: 10px;
}

.delete-button {
  background-color: #f44336;
  color: #fff;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
}

.loading-spinner {
  border: 16px solid #f3f3f3;
  border-top: 16px solid #3498db;
  border-radius: 50%;
  width: 120px;
  height: 120px;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
  }

  .upload-container {
    flex-direction: column;
    align-items: center;
  }

  .upload-instructions, .form-section, .price-section {
    width: 100%;
    text-align: center;
    margin-left: 0;
  }

  .file-upload-label, .uploaded-file-info {
    width: 100%;
  }

  .input-group label {
    width: auto;
    margin-right: 0;
  }

  .input-group {
    flex-direction: column;
  }

  .input-group input {
    width: 100%;
    margin-top: 10px;
  }

  .tab-menu button {
    padding: 10px 5px;
  }
}

.modal {
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  z-index: 1000;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  overflow: auto;
  background-color: rgb(0, 0, 0);
  background-color: rgba(0, 0, 0, 0.4);
}

.modal-content {
  background-color: #fefefe;
  margin: auto;
  padding: 20px;
  border: 1px solid #888;
  width: 80%;
  max-width: 600px;
}

.close {
  color: #aaa;
  float: right;
  font-size: 28px;
  font-weight: bold;
}

.close:hover,
.close:focus {
  color: black;
  text-decoration: none;
  cursor: pointer;
}
</style>
