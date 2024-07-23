<template>
  <div class="submission-form card">
    <h2>提交查重</h2>
    <form @submit.prevent="submitForm">
      <div class="form-group">
        <label for="check-code">查重码*</label>
        <input type="text" id="check-code" v-model="form.checkCode" name="checkCode" required>
      </div>
      <div class="form-group">
        <label for="file">文件*</label>
        <div
          class="file-dropzone"
          @dragover.prevent
          @drop.prevent="handleDrop"
          @click="() => !form.file && this.$refs.fileInput.click()">
          <p v-if="!form.file">点击或拖拽文件到这里上传</p>
          <div v-else class="file-selected">
            <p>已选择文件: {{ form.file.name }}</p>
            <span @click.stop="removeFile" class="remove-file-btn">✖️</span>
          </div>
          <p class="file-format-note">支持格式：DOC, DOCX, PDF, TXT (不超过30MB)</p>
          <input
            type="file"
            id="file"
            ref="fileInput"
            @change="handleFileUpload"
            class="file-input"
            name="file"
            required>
        </div>
      </div>
      <div class="form-group">
        <label for="region">Region*</label>
        <div class="region-options">
          <div>
            <input type="radio" id="international" value="intl" v-model="form.region" name="region" required>
            <label for="international">国际版查重 (International)</label>
          </div>
          <div>
            <input type="radio" id="uk" value="uk" v-model="form.region" name="region" required>
            <label for="uk">英国版查重 (United Kingdom)</label>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label for="title">标题</label>
        <input type="text" id="title" v-model="form.title" name="title" readonly>
      </div>
      <button type="submit" class="btn btn-primary btn-lg w-100" :disabled="isSubmitting">
        提交查重
        <span v-if="isSubmitting" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
      </button>
    </form>
    <div class="note mt-3">
      <p>查重时长取决于文档大小。</p>
      <p>通常情况下查重时长在 <span class="highlight">5-10 分钟</span> 之内。</p>
      <p>若已经超过 <span class="highlight">10 分钟</span> 可联系客服确认情况。</p>
    </div>

    <div v-if="showGuide" class="guide-overlay">
      <div class="guide-modal">
        <h2>欢迎使用Turnitin 查重系统</h2>
        <p>文件已设置不收录(No respority)</p>
        <p>步骤如下：</p>
        <ol>
          <li>填写查重码</li>
          <li>上传需要查重的文件（支持 DOC, DOCX, PDF, TXT 格式，最大 15MB）。</li>
          <li>输入查重码。</li>
          <li>点击提交查重按钮，稍等10分钟左右去提取报告即可。</li>
        </ol>
        <button @click="closeGuide" class="btn btn-primary">我知道了</button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      form: {
        checkCode: '',
        file: null,
        region: '',
        title: ''
      },
      isSubmitting: false,
      showGuide: false
    };
  },
  mounted() {
    if (!localStorage.getItem('hasVisited')) {
      this.showGuide = true;
      localStorage.setItem('hasVisited', 'true');
    }
  },
  methods: {
    handleFileUpload(event) {
      const file = event.target.files[0];
      if (this.validateFile(file)) {
        this.form.file = file;
        this.form.title = this.generateTitle(file.name);
      }
    },
    handleDrop(event) {
      const files = event.dataTransfer.files;
      if (files.length > 0) {
        const file = files[0];
        if (this.validateFile(file)) {
          this.form.file = file;
          this.form.title = this.generateTitle(file.name);
          this.$refs.fileInput.files = files;  // 同步更新 input 元素的值
        }
      }
    },
    validateFile(file) {
      const validFormats = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain'];
      if (file && validFormats.includes(file.type) && file.size <= 30 * 1024 * 1024) {
        return true;
      } else {
        alert('请上传符合要求的文件（DOC, DOCX, PDF, TXT格式，不超过15MB，AI目前只支持英文，字数范围在350-1.5万字）');
        return false;
      }
    },
    generateTitle(fileName) {
      const fileExtension = fileName.split('.').pop();
      return fileName.replace(`.${fileExtension}`, '');
    },
    removeFile() {
      this.form.file = null;
      this.form.title = '';
      this.$refs.fileInput.value = '';
    },
    async submitForm() {
      if (!this.form.file) {
        alert('请上传文件');
        return;
      }

      if (!this.form.region) {
        alert('请选择一个区域');
        return;
      }

      this.isSubmitting = true;

      const formData = new FormData();
      formData.append('code', this.form.checkCode);
      formData.append('file', this.form.file);
      formData.append('region', this.form.region);
      formData.append('title', this.form.title);


      try {
        const response = await axios.post(`${process.env.VUE_APP_BASE_API}/turnitin/submit`, formData);

        if (response.status === 200) {
          this.$emit('navigate-to-result');
        } else {
          alert('提交失败');
        }
      } catch (error) {
        alert(`提交失败: ${error.response ? error.response.data : error.message}`);
      } finally {
        this.isSubmitting = false;
      }
    },
    closeGuide() {
      this.showGuide = false;
    }
  }
};
</script>

<style>
/* 通用样式 */
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: #f8f9fa;
  margin: 0;
  padding: 0;
}

.submission-form {
  background-color: #fff;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  max-width: 700px;
  margin: 20px auto;
}

.submission-form:hover {
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2);
}

.submission-form h2 {
  margin-bottom: 20px;
  text-align: center;
  font-size: 24px;
  color: #333;
  font-weight: 600;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 600;
  color: #555;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 10px;
  box-sizing: border-box;
  border: 2px solid #ddd;
  border-radius: 8px;
  transition: border 0.3s ease;
}

.form-group input[type="text"]:focus {
  border-color: #007bff;
  outline: none;
}

.file-dropzone {
  padding: 15px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s ease;
  margin-bottom: 10px;
}

.file-dropzone:hover {
  border-color: #007bff;
}

.file-input {
  display: none;
}

.file-format-note {
  color: #777;
  font-size: 14px;
}

.file-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.file-selected p {
  margin: 0;
  flex: 1;
}

.remove-file-btn {
  background-color: transparent;
  border: none;
  color: red;
  cursor: pointer;
  font-size: 20px;
  margin-left: 10px;
  flex-shrink: 0;
}

.remove-file-btn:hover {
  color: darkred;
}

.region-options {
  display: flex;
  justify-content: space-around;
  margin-top: 10px;
}

.region-options div {
  display: flex;
  align-items: center;
}

.region-options input[type="radio"] {
  margin-right: 5px;
}

button {
  width: 100%;
  background-color: #007bff;
  color: #fff;
  padding: 15px;
  border: none;
  cursor: pointer;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  transition: background-color 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
}

button:disabled {
  background-color: #6c757d;
}

button .spinner-border {
  margin-left: 10px;
}

button:hover {
  background-color: #0056b3;
}

.card {
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  padding: 30px;
  transition: all 0.3s ease;
}

.card:hover {
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2);
}

.note {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-left: 5px solid #007bff;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.note:hover {
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
}

.note p {
  margin: 0;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
}

.note .highlight {
  font-weight: 700;
  color: #007bff;
  font-size: 16px;
}

.guide-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.guide-modal {
  background-color: #fff;
  padding: 20px;
  border-radius: 10px;
  max-width: 500px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.guide-modal h2 {
  margin-bottom: 10px;
  font-size: 22px;
  color: #333;
  font-weight: 600;
}

.guide-modal p, .guide-modal ol {
  margin: 0;
  margin-bottom: 10px;
  color: #555;
}

.guide-modal button {
  background-color: #007bff;
  color: #fff;
  padding: 10px 20px;
  border: none;
  cursor: pointer;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  transition: background-color 0.3s ease;
}

.guide-modal button:hover {
  background-color: #0056b3;
}

/* Spinner animation */
.spinner-border-sm {
  width: 1rem;
  height: 1rem;
  border: 0.15em solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  display: inline-block;
  animation: spinner-border 0.75s linear infinite;
}

@keyframes spinner-border {
  to { transform: rotate(360deg); }
}
</style>
