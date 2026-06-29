<template>
  <div class="main">
    <div class="none" v-if="notices.length === 0">
      <svg class="empty-svg" viewBox="0 0 79 86" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs><linearGradient id="linearGradient-1-2" x1="38.8503086%" y1="0%" x2="61.1496914%" y2="100%"><stop stop-color="#FCFCFD" offset="0%"></stop><stop stop-color="#EEEFF3" offset="100%"></stop></linearGradient><linearGradient id="linearGradient-2-2" x1="0%" y1="9.5%" x2="100%" y2="90.5%"><stop stop-color="#FCFCFD" offset="0%"></stop><stop stop-color="#E9EBEF" offset="100%"></stop></linearGradient><rect id="path-3-2" x="0" y="0" width="17" height="36"></rect></defs><g id="Illustrations" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd"><g id="B-type" transform="translate(-1268.000000, -535.000000)"><g id="Group-2" transform="translate(1268.000000, 535.000000)"><path id="Oval-Copy-2" d="M39.5,86 C61.3152476,86 79,83.9106622 79,81.3333333 C79,78.7560045 57.3152476,78 35.5,78 C13.6847524,78 0,78.7560045 0,81.3333333 C0,83.9106622 17.6847524,86 39.5,86 Z" fill="#F7F8FC"></path><polygon id="Rectangle-Copy-14" fill="#E5E7E9" transform="translate(27.500000, 51.500000) scale(1, -1) translate(-27.500000, -51.500000) " points="13 58 53 58 42 45 2 45"></polygon><g id="Group-Copy" transform="translate(34.500000, 31.500000) scale(-1, 1) rotate(-25.000000) translate(-34.500000, -31.500000) translate(7.000000, 10.000000)"><polygon id="Rectangle-Copy-10" fill="#E5E7E9" transform="translate(11.500000, 5.000000) scale(1, -1) translate(-11.500000, -5.000000) " points="2.84078316e-14 3 18 3 23 7 5 7"></polygon><polygon id="Rectangle-Copy-11" fill="#EDEEF2" points="-3.69149156e-15 7 38 7 38 43 -3.69149156e-15 43"></polygon><rect id="Rectangle-Copy-12" fill="url(#linearGradient-1-2)" transform="translate(46.500000, 25.000000) scale(-1, 1) translate(-46.500000, -25.000000) " x="38" y="7" width="17" height="36"></rect><polygon id="Rectangle-Copy-13" fill="#F8F9FB" transform="translate(39.500000, 3.500000) scale(-1, 1) translate(-39.500000, -3.500000) " points="24 7 41 7 55 -3.63806207e-12 38 -3.63806207e-12"></polygon></g><rect id="Rectangle-Copy-15" fill="url(#linearGradient-2-2)" x="13" y="45" width="40" height="36"></rect><g id="Rectangle-Copy-17" transform="translate(53.000000, 45.000000)"><mask id="mask-4-2" fill="white"><use xlink:href="#path-3-2"></use></mask><use id="Mask" fill="#E0E3E9" transform="translate(8.500000, 18.000000) scale(-1, 1) translate(-8.500000, -18.000000) " xlink:href="#path-3-2"></use><polygon id="Rectangle-Copy" fill="#D5D7DE" mask="url(#mask-4-2)" transform="translate(12.000000, 9.000000) scale(-1, 1) translate(-12.000000, -9.000000) " points="7 0 24 0 20 18 -1.70530257e-13 16"></polygon></g><polygon id="Rectangle-Copy-18" fill="#F8F9FB" transform="translate(66.000000, 51.500000) scale(-1, 1) translate(-66.000000, -51.500000) " points="62 45 79 45 70 58 53 58"></polygon></g></g></g></svg>
      <p>暂无通知</p>
    </div>

    <div class="content" v-else>
      <div class="notice" v-for="notice in notices" :key="notice.id">
        <div class="head">
          <div class="title">
            {{notice.title}}
          </div>
          <div class="time">
            {{notice.time}}
          </div>
        </div>
        <div class="msg">
          <div class="detail">
            {{notice.content}}
          </div>
          <div class="operation">
            <button>
              <FontAwesomeIcon :icon="['fas', 'fa-check']" />
            </button>
            <button>
              <FontAwesomeIcon icon="fa-trash" class="delete-icon" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {library} from "@fortawesome/fontawesome-svg-core";
import {faCheck, faTrash} from "@fortawesome/free-solid-svg-icons";
library.add(faCheck, faTrash)

defineProps({
  // 消息数组
  notices: {
    type: Array,
    default: () => [
      {id: 1, title: 'NoticeTitle', content: 'MessageContent', time: 'Timestamp'}
    ]
  }
})

</script>

<style scoped>
.none {
  display: flex;
  flex-direction: column;
  align-items: center;
  p {
    margin-top: 20px;
    font-size: 15px;
    color: rgba(51, 51, 51, 0.8);
  }
}

.content {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
  width: 100%;
  box-sizing: border-box;
}

.head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(51, 51, 51, 0.8);
}

.msg {
  font-size: 15px;
  color: #333;
  display: flex;
  justify-content: space-between;
}

.detail {
  max-width: 600px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice {
  margin-bottom: 20px;
  padding: 10px;
  border-radius: 4px;
  background-color: #f9f9f9;
  cursor: pointer;
  transition: all 0.2s;
}

.notice:hover {
  background-color: #f1f1f1;
}

.title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.operation {
  width: 55px;
  display: none;
  button {
    color: #6c6c6c;
    padding: 3px;
    border-radius: 5px;
    cursor: pointer;
    border: transparent;
    background-color: transparent;
    margin-left: 5px;
  }
  button:hover {
    background-color: #d3d3d3;
  }
  button:active {
    transform: scale(0.95);
  }
}

.notice:hover .operation {
  display: flex;
}

.notice:hover .detail {
  max-width: calc(600px - 55px);
}

.delete-icon {
  position: relative;
  top: 1px;
}
</style>