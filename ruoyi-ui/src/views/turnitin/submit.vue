<template>
  <div class="submission-container">
    <div class="card">
      <el-form ref="elForm" :model="formData" :rules="rules" size="small" label-width="100px" label-position="top">
        <el-form-item :label="$t('form.checkCode')" prop="checkCode">
          <el-input v-model="formData.checkCode" name="checkCode" required></el-input>
        </el-form-item>

        <el-form-item :label="$t('form.file')" prop="file" required>
          <el-upload
            ref="file"
            :file-list="fileList"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx,.pps,.ppsx,.pdf,.ps,.txt,.html,.wpd,.odt,.rtf,.hwp"
            :auto-upload="false"
          >
            <el-button size="small" type="primary" icon="el-icon-upload">{{$t('form.uploadFile')}}</el-button>
            <div slot="tip" class="el-upload__tip">{{$t('form.fileTip')}}</div>
          </el-upload>
          <div v-if="formData.file" class="file-selected">
            <p>{{$t('form.selectedFile')}} {{ formData.file.name }}</p>
            <el-button type="danger" icon="el-icon-delete" size="small" @click="removeFile">{{$t('form.removeFile')}}</el-button>
          </div>
        </el-form-item>

        <el-form-item :label="$t('form.region')" prop="region">
          <el-radio-group v-model="formData.region" size="medium">
            <el-radio label="intl">{{$t('form.international')}}</el-radio>
            <el-radio label="uk">{{$t('form.uk')}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('form.title')" prop="title">
          <el-input v-model="formData.title" name="title" readonly></el-input>
        </el-form-item>

        <el-form-item :label="$t('form.exclusionOptions')" prop="exclusionOptions">
          <el-checkbox v-model="formData.excludeBibliography" size="medium">{{$t('form.excludeBibliography')}}</el-checkbox>
          <el-checkbox v-model="formData.excludeQuotes" size="medium">{{$t('form.excludeQuotes')}}</el-checkbox>
        </el-form-item>

        <el-form-item :label="$t('form.excludeSmallMatchesMethod')" prop="excludeSmallMatchesMethod">
          <el-radio-group v-model="formData.excludeSmallMatchesMethod" size="medium">
            <el-radio label="disabled">{{$t('form.disabled')}}</el-radio>
            <el-radio label="by_words">{{$t('form.byWords')}}</el-radio>
            <el-radio label="by_percentage">{{$t('form.byPercentage')}}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="formData.excludeSmallMatchesMethod === 'by_words'" :label="$t('form.excludeSmallMatchesValueWords')" prop="excludeSmallMatchesValueWords">
          <el-input-number v-model="formData.excludeSmallMatchesValueWords" :min="0" :max="1000"></el-input-number>
          <span>{{$t('form.words')}}</span>
        </el-form-item>

        <el-form-item v-if="formData.excludeSmallMatchesMethod === 'by_percentage'" :label="$t('form.excludeSmallMatchesValuePercentage')" prop="excludeSmallMatchesValuePercentage">
          <el-input-number v-model="formData.excludeSmallMatchesValuePercentage" :min="0" :max="100"></el-input-number>
          <span>{{$t('form.percentage')}}</span>
        </el-form-item>

        <el-form-item size="large">
          <el-button type="primary" @click="confirmSubmission">{{$t('common.submit')}}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-dialog :visible.sync="isConfirmationDialogVisible" :title="$t('dialog.confirmation')" :before-close="handleDialogClose">
      <p>{{$t('dialog.submissionTime')}}</p>
      <p>{{$t('dialog.usualTime')}} <span class="highlight">{{$t('dialog.minutes')}}</span> {{$t('dialog.contactService')}}</p>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">{{$t('common.confirm')}}</el-button>
      </span>
      <span slot="close" class="el-dialog__headerbtn"><i class="el-dialog__close el-icon el-icon-close" style="color: red;"></i></span>
    </el-dialog>

    <el-dialog :visible.sync="isSubmitting" :title="$t('dialog.submitting')" :show-close="false">
      <div style="text-align: center;">
        <el-spinner type="circle" />
        <p>{{$t('dialog.pleaseWait')}}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      formData: {
        checkCode: '',
        file: null,
        region: '',
        title: '',
        excludeBibliography: null,
        excludeQuotes: null,
        excludeSmallMatchesMethod: 'disabled',
        excludeSmallMatchesValueWords: 0,
        excludeSmallMatchesValuePercentage: 0
      },
      rules: {
        checkCode: [
          { required: true, message: this.$t('form.checkCode'), trigger: 'blur' }
        ],
        file: [
          { required: true, message: this.$t('form.file'), trigger: 'change' }
        ],
        region: [
          { required: true, message: this.$t('form.region'), trigger: 'change' }
        ],
        excludeSmallMatchesMethod: [
          { required: true, message: this.$t('form.excludeSmallMatchesMethod'), trigger: 'change' }
        ]
      },
      fileList: [],
      isSubmitting: false,
      isConfirmationDialogVisible: false,
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
    beforeUpload(file) {
      const isRightSize = file.size / 1024 / 1024 < 30;
      if (!isRightSize) {
        this.$message.error(this.$t('form.fileTip'));
      }
      const validFormats = [
        'application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain',
        'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation', 'application/postscript', 'text/html',
        'application/wordperfect', 'application/vnd.oasis.opendocument.text', 'application/rtf', 'application/x-hwp'
      ];
      const isAccept = validFormats.includes(file.type);
      if (!isAccept) {
        this.$message.error(this.$t('form.fileTip'));
      }
      return isRightSize && isAccept;
    },
    handleFileChange(file, fileList) {
      this.fileList = fileList;
      if (fileList.length > 0) {
        this.formData.file = fileList[0].raw;
        this.formData.title = this.generateTitle(fileList[0].name);
      }
    },
    generateTitle(fileName) {
      const fileExtension = fileName.split('.').pop();
      return fileName.replace(`.${fileExtension}`, '');
    },
    removeFile() {
      this.fileList = [];
      this.formData.file = null;
      this.formData.title = '';
    },
    confirmSubmission() {
      this.isConfirmationDialogVisible = true;
    },
    async submitForm() {
      this.$refs.elForm.validate(async (valid) => {
        if (valid) {
          this.isConfirmationDialogVisible = false;
          this.isSubmitting = true;
          const formData = new FormData();
          formData.append('code', this.formData.checkCode);
          formData.append('file', this.formData.file);
          formData.append('region', this.formData.region);
          formData.append('title', this.formData.title);
          formData.append('excludeBibliography', this.formData.excludeBibliography ? 'on' : null);
          formData.append('excludeQuotes', this.formData.excludeQuotes ? 'on' : null);
          formData.append('excludeSmallMatchesMethod', this.formData.excludeSmallMatchesMethod);
          formData.append('excludeSmallMatchesValueWords', this.formData.excludeSmallMatchesValueWords);
          formData.append('excludeSmallMatchesValuePercentage', this.formData.excludeSmallMatchesValuePercentage);

          try {
            const response = await axios.post(`${process.env.VUE_APP_BASE_API}/turnitin/submit`, formData);

            if (response.status === 200) {
              this.$emit('navigate-to-result');
              this.$message.success(this.$t('form.successMessage'));
            } else {
              this.$message.error(this.$t('form.errorMessage'));
            }
          } catch (error) {
            this.$message.error(`${this.$t('form.errorMessage')}: ${error.response ? error.response.data : error.message}`);
          } finally {
            this.isSubmitting = false;
          }
        }
      });
    },
    handleDialogClose() {
      this.isConfirmationDialogVisible = false;
    },
    closeGuide() {
      this.showGuide = false;
    }
  }
};
</script>

<style scoped>
.submission-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f0f2f5;
  padding: 20px;
  box-sizing: border-box;
}

.card {
  background-color: #fff;
  padding: 20px 30px;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  max-width: 900px;
  width: 100%;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.card:hover {
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2);
}

.el-form-item {
  margin-bottom: 15px;
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

.el-button--primary {
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

.el-button--primary:disabled {
  background-color: #6c757d;
}

.el-button--primary .spinner-border {
  margin-left: 10px;
}

.el-button--primary:hover {
  background-color: #0056b3;
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
  color: red;
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
  to {
    transform: rotate(360deg);
  }
}
</style>
