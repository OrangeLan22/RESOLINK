<template>
  <div class="menu">
    <button>一键已读</button>
    <button>删除已读</button>
  </div>
</template>

<script setup>

</script>

<style scoped>
.menu {
  margin-left: 20px;
  display: flex;
  justify-content: flex-end;
  button {
    color: #6c6c6c;
    padding: 3px;
    border-radius: 5px;
    cursor: pointer;
    border: transparent;
    background-color: transparent;
    margin-left: 10px;
  }
}
</style>