<template>
  <div class="code-management">
    <div class="header">
      <h2>兑换码管理</h2>
      <div class="stats">
        <span>已兑换：{{ stats.used }}</span>
        <span>未兑换：{{ stats.unredeemed }}</span>
        <span>已失效：{{ stats.expired }}</span>
        <span>兑换码总数：{{ stats.total }}</span>
      </div>
      <button @click="openGenerateModal" class="btn btn-primary">生成兑换码</button>
    </div>

    <div class="filter-bar">
      <input type="text" v-model="searchKeyword" placeholder="搜索兑换码、班级ID或论文ID"/>
      <select v-model="searchForm.businessType" @change="fetchLinkedAccounts">
        <option value="">所有业务类型</option>
        <option value="Turnitin">Turnitin</option>
        <option value="Grammarly">Grammarly</option>
        <option value="Leetcode">Leetcode</option>
      </select>
      <select v-model="searchForm.linkedAccount">
        <option value="">所有账号类型</option>
        <option v-for="account in linkedAccounts" :key="account.id" :value="account.accountName">
          {{ account.accountName }}
        </option>
      </select>
      <select v-model="searchForm.status">
        <option value="">状态</option>
        <option value="active">有效</option>
        <option value="used">已兑换</option>
        <option value="expired">已失效</option>
      </select>
      <input type="text" v-model="searchForm.email" placeholder="邮箱"/>
      <button @click="searchCodes" class="btn btn-primary">搜索</button>
      <button @click="exportCodes" class="btn btn-secondary">导出</button>
    </div>

    <div class="codes mt-4">
      <table>
        <thead>
        <tr>
          <th><input type="checkbox" @change="toggleSelectAll" :checked="allSelected"/></th>
          <th>#</th>
          <th>兑换码</th>
          <th>状态</th>
          <th>类型</th>
          <th>绑定账号</th>
          <th>有效天数</th>
          <th>过期时间</th>
          <th>最近使用时间</th>
          <th>创建时间</th>
          <th>已用</th>
          <th>最多使用</th>
          <th>文章ID</th>
          <th>查重下载</th>
          <th>AI下载</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(code, index) in codes.content" :key="code.id">
          <td><input type="checkbox" :value="code.id" v-model="selectedCodes"/></td>
          <td>{{ index + 1 }}</td>
          <td>{{ code.code }}</td>
          <td>{{ code.status }}</td>
          <td>{{ typeToChinese(code.type) }}</td>
          <td>{{ code.linkedAccount}}</td>
          <td>{{ code.validDays ? code.validDays + ' 天' : '-' }}</td>
          <td>{{ code.expiryDate }}</td>
          <td>{{ code.activeDate }}</td>
          <td>{{ code.createdAt }}</td>
          <td>{{ code.usedCount }}次</td>
          <td>{{ code.usageLimit }}次</td>
          <td>{{ code.paperId }}</td>
          <td>{{ code.similarityPdfUrl }}</td>
          <td>{{ code.aiWritingPdfUrl }}</td>
        </tr>
        </tbody>
      </table>
      <div class="pagination-controls">
        <select v-model="pageSize" @change="fetchCodes(0)">
          <option v-for="size in [10, 30, 50, 100]" :key="size" :value="size">{{ size }} 每页</option>
        </select>
        <pagination :page="page" :total="codes.totalPages" @change="fetchCodes"></pagination>
      </div>
      <button @click="deleteSelectedCodes" class="btn btn-danger mt-3" :disabled="!selectedCodes.length">
        删除选中的兑换码
      </button>
    </div>

    <!-- 生成兑换码的弹窗 -->
    <el-dialog title="生成兑换码" :visible.sync="showGenerateModal" width="50%">
      <el-form :model="form">
        <el-form-item label="业务类型">
          <el-select v-model="form.businessType" @change="handleBusinessTypeChange" placeholder="请选择">
            <el-option label="Turnitin" value="Turnitin"></el-option>
            <el-option label="Grammarly" value="Grammarly"></el-option>
            <el-option label="Leetcode" value="Leetcode"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="兑换码类型" v-if="form.businessType === 'Turnitin'">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option label="仅查重" value="single_check"></el-option>
            <el-option label="仅AI" value="single_ai"></el-option>
            <el-option label="查重和AI" value="both"></el-option>
            <el-option label="按时限" value="timed"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="兑换码类型" v-if="form.businessType === 'Grammarly'">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option label="Grammarly查语法（按天数）" value="Grammarly_grammar_check_days"></el-option>
            <el-option label="Grammarly查语法（按次数）" value="Grammarly_grammar_check_usage"></el-option>
            <el-option label="Grammarly Business" value="Grammarly_business"></el-option>
            <el-option label="Grammarly Edu" value="Grammarly_edu"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="有效天数" v-if="form.type === 'Grammarly_grammar_check_days' || form.type === 'Grammarly_business' || form.type === 'Grammarly_edu' || form.type === 'timed' || form.businessType === 'Leetcode'">
          <el-input-number v-model="form.validDays" :min="1" label="有效天数"></el-input-number>
        </el-form-item>
        <el-form-item label="使用次数" v-if="form.businessType !== 'Grammarly' && form.type !== 'timed' && form.businessType !== 'Leetcode'">
          <el-input-number v-model="form.usageLimit" :min="1" label="使用次数"></el-input-number>
        </el-form-item>
        <el-form-item label="生成数量">
          <el-input-number v-model="form.count" :min="1" label="生成数量"></el-input-number>
        </el-form-item>
        <el-form-item label="可重复兑换" v-if="form.businessType === 'Grammarly'">
          <el-switch v-model="form.repeatable"></el-switch>
        </el-form-item>
      </el-form>
      <div v-if="generatedCodes.length">
        <h3>生成的兑换码:</h3>
        <div class="generated-codes">
          <div v-for="code in generatedCodes" :key="code">{{ code }}</div>
        </div>
        <el-button @click="copyCodes">复制兑换码</el-button>
      </div>
      <template #footer>
        <el-button @click="showGenerateModal = false">取消</el-button>
        <el-button type="primary" @click="generateCodes">
          生成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import Pagination from './Pagination.vue';
import { getAllCodes, addCode, deleteCodes, exportCodes, searchCodes, getStats } from '@/api/turnitin/codeApi';
import { getAccountsByBusinessType } from '@/api/turnitin/turnitinAccountApi';

export default {
  components: { Pagination },
  data() {
    return {
      form: {
        businessType: 'Turnitin',
        type: 'single_check',
        usageLimit: 1,
        validDays: 30,
        count: 1,
        repeatable: false,
        status: '',
      },
      searchForm: {
        businessType: 'Turnitin',
        linkedAccount: '',
        status: '',
        email: ''
      },
      searchKeyword: '',
      linkedAccounts: [],
      codes: {
        content: [],
        totalPages: 1,
      },
      selectedCodes: [],
      page: 0,
      pageSize: 10,
      showGenerateModal: false,
      generatedCodes: [], // 新增，用于存储生成的兑换码
      stats: {
        used: 0,
        unredeemed: 0,
        expired: 0,
        total: 0,
      },
      allSelected: false,
    };
  },
  methods: {
    openGenerateModal() {
      this.showGenerateModal = true;
      this.generatedCodes = [];
    },
    closeGenerateModal() {
      this.showGenerateModal = false;
    },
    async generateCodes() {
      const codeData = {
        businessType: this.form.businessType,
        type: this.form.type,
        validDays: this.form.businessType === 'Turnitin' && this.form.type !== 'timed' ? null : this.form.validDays,
        usageLimit: this.form.businessType !== 'Grammarly' && this.form.type !== 'timed' && this.form.businessType !== 'Leetcode' ? this.form.usageLimit : null,
        count: this.form.count,
        repeatable: this.form.repeatable
      };
      try {
        const response = await addCode(codeData);
        this.generatedCodes = response.map(code => code.code); // 假设生成的兑换码数组在response中
        this.fetchCodes();
      } catch (error) {
        console.error('Error generating codes:', error);
      }
    },
    copyCodes() {
      const codes = this.generatedCodes.join('\n');
      const textarea = document.createElement('textarea');
      textarea.value = codes;
      document.body.appendChild(textarea);
      textarea.select();
      try {
        document.execCommand('copy');
        this.$message.success('兑换码已复制');
      } catch (err) {
        this.$message.error('复制失败');
      }
      document.body.removeChild(textarea);
    },
    async fetchCodes(newPage = 0) {
      this.page = newPage;
      const params = {
        page: this.page,
        size: this.pageSize,
        keyword: this.searchKeyword,
        businessType: this.searchForm.businessType,
        linkedAccount: this.searchForm.linkedAccount,
        status: this.searchForm.status,
        email: this.searchForm.email
      };
      try {
        const response = await getAllCodes(params);
        this.codes = response; // 确保正确地获取数据
      } catch (error) {
        console.error('Error fetching codes:', error);
      }
    },
    async searchCodes() {
      const params = {
        code: this.searchKeyword,
        businessType: this.searchForm.businessType,
        linkedAccount: this.searchForm.linkedAccount,
        status: this.searchForm.status,
        email: this.searchForm.email,
        page: this.page,
        size: this.pageSize,
      };
      try {
        const response = await searchCodes(params);
        this.codes = response; // 确保正确地获取数据
      } catch (error) {
        console.error('Error searching codes:', error);
      }
    },
    async deleteSelectedCodes() {
      try {
        await deleteCodes(this.selectedCodes);
        this.fetchCodes();
        this.selectedCodes = [];
      } catch (error) {
        console.error('Error deleting codes:', error);
      }
    },
    async exportCodes() {
      try {
        const response = await exportCodes();
        const url = window.URL.createObjectURL(new Blob([response]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'codes.csv');
        document.body.appendChild(link);
        link.click();
      } catch (error) {
        console.error('Error exporting codes:', error);
      }
    },
    toggleSelectAll() {
      this.allSelected = !this.allSelected;
      if (this.allSelected) {
        this.selectedCodes = this.codes.content.map(code => code.id);
      } else {
        this.selectedCodes = [];
      }
    },
    handleBusinessTypeChange() {
      this.fetchLinkedAccounts();
      if (this.form.businessType === 'Grammarly') {
        this.form.type = 'Grammarly_grammar_check_days';
        this.form.usageLimit = null;
        this.form.repeatable = false;
        this.form.validDays = 7; // 默认为7天
      } else if (this.form.businessType === 'Leetcode') {
        this.form.type = null;
        this.form.usageLimit = null;
        this.form.repeatable = false;
      } else {
        this.form.type = 'single_check';
        this.form.usageLimit = 1;
        this.form.repeatable = false;
      }
    },
    async fetchLinkedAccounts() {
      try {
        const response = await getAccountsByBusinessType(this.searchForm.businessType);
        this.linkedAccounts = response; // 确保正确地获取数据
      } catch (error) {
        console.error('Error fetching linked accounts:', error);
      }
    },
    async fetchStats() {
      try {
        const response = await getStats();
        this.stats = response; // 确保正确地获取数据
      } catch (error) {
        console.error('Error fetching stats:', error);
      }
    },
    typeToChinese(type) {
      switch (type) {
        case 'single_check':
          return '仅查重';
        case 'single_ai':
          return '仅AI';
        case 'both':
          return '查重和AI';
        case 'timed':
          return '按时限';
        case 'Grammarly_grammar_check_days':
          return 'Grammarly查语法（按天数）';
        case 'Grammarly_grammar_check_usage':
          return 'Grammarly查语法（按次数）';
        case 'Grammarly_business':
          return 'Grammarly Business';
        case 'Grammarly_edu':
          return 'Grammarly Edu';
        default:
          return type;
      }
    }
  },
  mounted() {
    this.fetchCodes();
    this.fetchStats(); // 在组件挂载时获取统计数据
  }
};
</script>

<style>
/* 保持你现有的样式不变 */
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: #f5f5f5;
  margin: 0;
  padding: 0;
}

#app {
  padding: 20px;
}

.code-management {
  background-color: #fff;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.code-management:hover {
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.2);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.stats {
  display: flex;
  gap: 20px;
}

.stats span {
  font-weight: bold;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.filter-bar input,
.filter-bar select {
  padding: 10px;
  border: 2px solid #ddd;
  border-radius: 8px;
  transition: border 0.3s ease;
}

.filter-bar input:focus,
.filter-bar select:focus {
  border-color: #007bff;
  outline: none;
}

.codes {
  margin-top: 20px;
}

.codes table {
  width: 100%;
  border-collapse: collapse;
}

.codes th,
.codes td {
  padding: 8px;
  text-align: left;
  border: 1px solid #ddd;
  word-break: break-all; /* 使超长文本自动换行 */
}

.codes th {
  background-color: #f5f5f5;
  font-size: 14px; /* 调整表头字体大小 */
  text-align: center; /* 表头文字居中 */
}

.codes td {
  font-size: 13px; /* 调整单元格字体大小 */
}

.codes tbody tr:hover {
  background-color: #f1f1f1;
}

button {
  background-color: #007bff;
  color: #fff;
  padding: 10px 20px;
  border: none;
  cursor: pointer;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  transition: background-color 0.3s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

button:hover {
  background-color: #0056b3;
}

.btn-secondary {
  background-color: #6c757d;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.mt-3 {
  margin-top: 1rem;
}

.generated-codes {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #ddd;
  padding: 10px;
  margin-top: 10px;
  background-color: #f9f9f9;
}

.generated-codes div {
  margin-bottom: 5px;
}
</style>
