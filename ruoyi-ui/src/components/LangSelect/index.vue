<template>
  <el-dropdown trigger="click" class="international" @command="handleSetLanguage">
    <div>
      <svg-icon class-name="international-icon" icon-class="language" />
    </div>
    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item :disabled="language==='zh_CN'" command="zh_CN">
        中文
      </el-dropdown-item>
      <el-dropdown-item :disabled="language==='en_US'" command="en_US">
        English
      </el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>
import { changeLanguage } from "@/api/login";


export default {
  computed: {
    language() {
      return this.$store.getters.language
    }
  },
  methods: {
    handleSetLanguage(value) {
      this.$i18n.locale = value
      this.$store.dispatch('app/setLanguage', value)
      this.$message({ message: '设置语言成功', type: 'success' })
      changeLanguage(value).then(response => {
        window.location.reload();
      });
    }
  }
}
</script>
