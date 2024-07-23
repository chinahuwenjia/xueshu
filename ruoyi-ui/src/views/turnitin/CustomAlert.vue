<template>
  <div class="custom-alert-overlay">
    <div class="custom-alert">
      <p>{{ message }}</p>
      <button @click="$emit('close')">关闭</button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    message: {
      type: String,
      required: true
    }
  }
};
</script>

<style scoped>
.custom-alert-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.custom-alert {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  max-width: 300px;
  width: 100%;
  text-align: center;
}

.custom-alert p {
  margin: 0 0 15px;
  font-size: 14px;
  color: #333;
  word-wrap: break-word;
}

.custom-alert button {
  background-color: #007bff;
  color: #fff;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.custom-alert button:hover {
  background-color: #0056b3;
}
</style>
