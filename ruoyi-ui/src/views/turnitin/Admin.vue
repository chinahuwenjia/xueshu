<template>
  <div class="admin">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="兑换码管理" name="codes">
        <CodeManagement />
      </el-tab-pane>
      <el-tab-pane label="Turnitin 账号管理" name="accounts">
        <TurnitinAccountManagement />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CodeManagement from './CodeManagement.vue';
import TurnitinAccountManagement from './TurnitinAccountManagement.vue';

export default {
  data() {
    return {
      activeTab: 'codes'
    };
  },
  components: {
    CodeManagement,
    TurnitinAccountManagement
  }
};
</script>

<style scoped>
.admin {
  padding: 20px;
}
</style>
