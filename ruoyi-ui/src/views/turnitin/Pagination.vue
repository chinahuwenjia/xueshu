<template>
  <div class="pagination">
    <button @click="prevPage" :disabled="page === 0" class="btn btn-primary">上一页</button>
    <span>第 {{ page + 1 }} 页，共 {{ total }} 页</span>
    <button @click="nextPage" :disabled="page === total - 1" class="btn btn-primary">下一页</button>
  </div>
</template>

<script>
export default {
  props: {
    page: {
      type: Number,
      required: true
    },
    total: {
      type: Number,
      required: true
    }
  },
  methods: {
    prevPage() {
      this.$emit('change', this.page - 1);
    },
    nextPage() {
      this.$emit('change', this.page + 1);
    }
  }
};
</script>

<style>
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}
</style>
