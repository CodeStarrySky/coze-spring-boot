<template>
  <div class="__container_home_index">
    <a-row class="row" :gutter="[10, 10]">
      <a-col :span="8">
        <a-card class="card chat">
          <template #title>
            <label style="font-size: 25px">Funnair support</label>
          </template>
          <div class="flex-grow">
            <a-card class="chat-body">
              <MessageList :list="messageInfo.list"></MessageList>
              <div
                id="chat-body-id"
                style="height: 5px; margin-top: 20px"
              ></div>
            </a-card>
          </div>
          <a-row class="footer" :gutter="10">
            <a-col :span="24">
              <a-input
                @keydown.enter="forHelp"
                v-model:value="question"
                placeholder="Message"
              >

                <template #suffix>
                  <a-button type="link"
                            :class="{ 'recording': isRecording }"
                            @mousedown="startRecording"
                            @mouseleave="stopAndUpload"
                            :disabled="isUploading"><AudioOutlined /></a-button>
                </template>
              </a-input>
              <br/>
            </a-col>
            <a-button type="primary" shape="circle" @click="forHelp" >
              <template #icon>
                <component :is="buttonIcon" />
              </template>
            </a-button>
          </a-row>
        </a-card>
      </a-col>
      <a-col :span="16">
        <a-card class="card">
          <template #title>
            <label style="font-size: 25px">机票预定信息</label>
          </template>
          <a-table
            :data-source="bookingInfo.dataSource"
            :columns="bookingInfo.columns"
            :pagination="false"
          >
            <template #bodyCell="{ record, index, column, text }">
              <template v-if="column.dataIndex === 'bookingStatus'">
                <template v-if="text === 'CONFIRMED'">
                  <Icon
                    style="color: #52c41a; font-size: 20px; margin-bottom: -4px"
                    icon="material-symbols:check-box-sharp"
                  />
                </template>
                <template v-else>
                  <Icon
                    style="color: #be0b4a; font-size: 20px; margin-bottom: -4px"
                    icon="material-symbols:cancel-presentation-sharp"
                  />
                </template>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { PRIMARY_COLOR } from "@/base/constants";
import { nextTick, onMounted, reactive, ref } from "vue";
import { getBookings } from "@/api/service/booking";
import { Icon } from "@iconify/vue";
import Message from "@/views/home/Message.vue";
import MessageList from "@/views/home/MessageList.vue";
import type { MessageItem } from "@/types/message";
import { chat,cancel } from "@/api/service/assistant";
import { getUUID } from "ant-design-vue/lib/vc-dialog/util";
import { v4 as uuidv4 } from "uuid";
import { message } from "ant-design-vue";
import { SendOutlined,PauseCircleOutlined,AudioOutlined } from '@ant-design/icons-vue';
import axios from 'axios';
import Recorder from 'js-audio-recorder';


let recorder = new Recorder();

const messageInfo: { cur: MessageItem | null; list: MessageItem[] } = reactive({
  cur: null,
  list: [
    {
      role: "assistant",
      content: "欢迎来到 Funnair! 请问有什么可以帮您的?",
    },
  ],
});
const bookingInfo = reactive({
  dataSource: [],
  columns: [
    {
      title: "#",
      dataIndex: "bookingNumber",
      key: "bookingNumber",
    },
    {
      title: "Name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Date",
      dataIndex: "date",
      key: "date",
    },
    {
      title: "From",
      dataIndex: "from",
      key: "from",
    },
    {
      title: "To",
      dataIndex: "to",
      key: "to",
    },
    {
      title: "Status",
      dataIndex: "bookingStatus",
      key: "bookingStatus",
    },
    {
      title: "Booking Class",
      dataIndex: "bookingClass",
      key: "bookingClass",
    },
  ],
});
const question = ref("");
let scrollItem: any = null;

function scrollBottom() {
  scrollItem?.scrollIntoView({ behavior: "smooth", block: "end" });
}

function addMessage(role: "user" | "assistant", content: string) {
  let cur = {
    role,
    content,
  };
  messageInfo.cur = cur;
  messageInfo.list.push(cur);
  nextTick(() => {
    scrollBottom();
  });
}

const lock = ref(false);
function appendMessage(content: string) {
  if (messageInfo.cur) {
    messageInfo.cur.content += content;
  }
  scrollBottom();
}

let chatId = null;
const buttonIcon = ref(SendOutlined);

function forHelp() {
  if (lock.value) {
    doCancel();
    return;
  }
  chatId = uuidv4();
  let userMessage = question.value;
  addMessage("user", userMessage);
  question.value = "";
  const eventSource = new EventSource(
    `/api/assistant/chat?chatId=${chatId}&userMessage=${userMessage}`,
    {},
  );
  eventSource.onopen = function (event) {
    lock.value = true;
    buttonIcon.value = PauseCircleOutlined;
    addMessage("assistant", "");
  };
  eventSource.onmessage = function (event) {
    if (!lock.value) {
      buttonIcon.value = SendOutlined;
      eventSource.close();
      return;
    }
    appendMessage(event.data);
  };
  eventSource.onerror = function () {
    eventSource.close();
    bookings();
    lock.value = false;
    buttonIcon.value = SendOutlined;
  };
}

function doCancel() {
  cancel(chatId).then((res) => {
    lock.value = false;
  })
}
function bookings() {
  getBookings({}).then((res) => {
    bookingInfo.dataSource = res;
  });
}


const isRecording = ref(false);
const isUploading = ref(false);
const audioChunks = ref([]);
const statusMessage = ref('');
const mediaRecorder = ref<MediaRecorder | null>(null);
function startRecording() {
  if (isUploading.value) return;
  Recorder.getPermission().then(() => {
    console.log('录音给权限了');
  }, (error) => {
    console.log(`${error.name} : ${error.message}`);
  });
  // navigator.mediaDevices.getUserMedia({ audio: true })
  //     .then(stream => {
  //       mediaRecorder.value = new MediaRecorder(stream);
  //       audioChunks.value = [];
  //
  //       mediaRecorder.value.ondataavailable = event => {
  //         if (event.data.size > 0) {
  //           console.log(event.data);
  //           audioChunks.value.push(event.data);
  //         }
  //       };
  //
  //       mediaRecorder.value.onstart = () => {
  //         isRecording.value = true;
  //         statusMessage.value = '录音中...';
  //       };
  //
  //       mediaRecorder.value.start();
  //     })
  //     .catch(err => {
  //       console.error('无法访问麦克风:', err);
  //       alert('请允许访问麦克风');
  //     });

  recorder.start().then(() => {
    // 开始录音
    console.log("开始录音");
    isRecording.value = true;
  }, (error) => {
    // 出错了
    console.log(`${error.name} : ${error.message}`);
  });
}

function stopAndUpload() {
  if (!isRecording.value) return;

  debugger;

  const blob = recorder.getWAVBlob();
  console.log('录音完成，Blob size:', blob.size); // 检查是否为 0

  if (blob.size === 0) {
    statusMessage.value = '录音内容为空，请重新录制';
    return;
  }

  const formData = new FormData();
  formData.append('audio', blob, 'voice-message.wav');

  isUploading.value = true;
  statusMessage.value = '发送中...';

  axios.post('/api/assistant/transcriptions', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => {
    statusMessage.value = '发送成功';
    question.value = res.data;
    forHelp();
    setTimeout(() => statusMessage.value = '', 2000);
  }).catch(err => {
    console.error(err);
    statusMessage.value = '发送失败';
  }).finally(() => {
    isUploading.value = false;
  });
}





let __null = PRIMARY_COLOR;
onMounted(() => {
  scrollItem = document.getElementById("chat-body-id");
  bookings();
});
</script>
<style lang="less" scoped>
.__container_home_index {
  height: 100vh;
  max-height: 100vh;
  overflow: auto;
  padding-top: 2px;

  .row {
    height: 100%;
  }

  .card {
    height: 100%;
  }

  :deep(.ant-card-body) {
    height: calc(100vh - 180px);
    display: flex;
    flex-direction: column;
    padding: 5px;
    border-radius: 0;

    .chat-body {
      border: none;
      height: calc(100% - 80px);
      overflow: auto;
      background: #f4f5f7;
    }
  }

  .flex-grow {
    flex-grow: 1; /* 让其他元素占据剩余空间 */
  }

  .footer {
    width: 100%;
  }
}
</style>
