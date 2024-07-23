<template>
  <div>
    <div style="margin-bottom: 20px;">
      <el-button type="primary" @click="showAddAccountDialog">新增账号</el-button>
      <el-button type="primary" @click="fetchAccounts">刷新</el-button>
    </div>

    <div v-if="loading">Loading...</div>
    <div v-else>
      <el-table v-if="accounts && accounts.length" :data="accounts" style="width: 100%" stripe>
        <el-table-column prop="accountName" label="账号名称" width="180"></el-table-column>
        <el-table-column prop="accountType" label="账号类型" width="180"></el-table-column>
        <el-table-column label="操作" width="180">
          <template slot-scope="scope">
            <el-button size="mini" @click="editAccount(scope.row.id)">编辑</el-button>
            <el-button size="mini" type="danger" @click.native.stop="deleteAccount(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="accounts && accounts.length === 0">No data available</div>
    </div>

    <el-dialog :visible.sync="accountDialogVisible" title="管理账号" width="50%">
      <el-form :model="currentAccount" label-width="120px">
        <el-form-item label="账号名称">
          <el-input v-model="currentAccount.accountName"></el-input>
        </el-form-item>
        <el-form-item label="Curl 字符串">
          <el-input v-model="currentAccount.curlString"></el-input>
        </el-form-item>
        <el-form-item label="账号类型">
          <el-select v-model="currentAccount.accountType" placeholder="请选择">
            <el-option label="查重" value="check"></el-option>
            <el-option label="AI" value="ai"></el-option>
            <el-option label="网页自助查" value="pro"></el-option>
            <el-option label="力扣账号" value="leetcode"></el-option>
            <el-option label="Grammarly" value="grammarly"></el-option>
          </el-select>
          <el-tooltip v-if="currentAccount.accountType === 'check'" class="item" effect="dark" content="查重表示只能查重复率，不能查AI。" placement="top">
            <i class="el-icon-info" style="margin-left: 10px;"></i>
          </el-tooltip>
          <el-tooltip v-else-if="currentAccount.accountType === 'ai'" class="item" effect="dark" content="AI表示能查重复率也可以查AI报告。" placement="top">
            <i class="el-icon-info" style="margin-left: 10px;"></i>
          </el-tooltip>
          <el-tooltip v-else-if="currentAccount.accountType === 'pro'" class="item" effect="dark" content="用pro去查" placement="top">
            <i class="el-icon-info" style="margin-left: 10px;"></i>
          </el-tooltip>
          <el-tooltip v-else-if="currentAccount.accountType === 'leetcode'" class="item" effect="dark" content="leetcode账号" placement="top">
            <i class="el-icon-info" style="margin-left: 10px;"></i>
          </el-tooltip>
          <el-tooltip v-else-if="currentAccount.accountType === 'grammarly'" class="item" effect="dark" content="Grammarly账号" placement="top">
            <i class="el-icon-info" style="margin-left: 10px;"></i>
          </el-tooltip>
        </el-form-item>
        <el-form-item v-if="currentAccount.accountType === 'grammarly'" label="Grammarly 类型">
          <el-select v-model="currentAccount.type" placeholder="请选择">
            <el-option label="Grammarly 语法检查天数" value="Grammarly_grammar_check_days"></el-option>
            <el-option label="Grammarly 商业" value="Grammarly_business"></el-option>
            <el-option label="Grammarly 教育" value="Grammarly_edu"></el-option>
            <el-option label="Grammarly 语法检查使用" value="Grammarly_grammar_check_usage"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAccount">保存账号</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getAllTurnitinAccounts, addTurnitinAccount, updateTurnitinAccount, deleteTurnitinAccount } from '@/api/turnitin/turnitinAccountApi';

export default {
  data() {
    return {
      accounts: [],
      accountDialogVisible: false,
      currentAccount: {
        id: '',
        accountName: '',
        curlString: '',
        accountType: 'check', // 默认值为 "check"
        type: '' // Grammarly 二级类型
      },
      loading: true,
    };
  },
  created() {
    this.fetchAccounts();
  },
  methods: {
    fetchAccounts() {
      this.loading = true;
      getAllTurnitinAccounts().then(response => {
        console.log('API Response:', response);
        this.accounts = response; // 确保使用 response
        console.log('Accounts:', this.accounts);
        this.loading = false;
      }).catch(error => {
        console.error('Error fetching accounts:', error);
        this.loading = false;
      });
    },
    showAddAccountDialog() {
      this.currentAccount = { id: '', accountName: '', curlString: '', accountType: 'check', type: '' }; // 默认值为 "check"
      console.log('Show Add Account Dialog:', this.currentAccount);
      this.accountDialogVisible = true;
    },
    handleRowClick(row) {
      this.currentAccount = { ...row };
      console.log('Row clicked:', this.currentAccount);
      this.accountDialogVisible = true;
    },
    editAccount(id) {
      const account = this.accounts.find(account => account.id === id);
      if (account) {
        this.currentAccount = {...account};
        this.accountDialogVisible = true;
      }
    },
    saveAccount() {
      console.log('Saving account:', this.currentAccount);
      if (this.currentAccount.id) {
        updateTurnitinAccount(this.currentAccount).then(() => {
          this.fetchAccounts();
          this.accountDialogVisible = false;
          console.log('Account updated successfully');
        }).catch(error => {
          console.error('Error updating account:', error);
        });
      } else {
        addTurnitinAccount(this.currentAccount).then(() => {
          this.fetchAccounts();
          this.accountDialogVisible = false;
          console.log('Account added successfully');
        }).catch(error => {
          console.error('Error adding account:', error);
        });
      }
    },
    deleteAccount(id) {
      console.log('Deleting account ID:', id);
      deleteTurnitinAccount(id).then(() => {
        this.fetchAccounts();
        console.log('Account deleted successfully');
      }).catch(error => {
        console.error('Error deleting account:', error);
      });
    }
  }
};
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style>
