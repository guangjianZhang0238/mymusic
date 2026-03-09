<template>
  <div class="test-encoding">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>字符编码测试</span>
        </div>
      </template>
      
      <div class="test-content">
        <el-button @click="testApi" type="primary">测试API字符编码</el-button>
        <el-button @click="clearLog" type="warning">清空日志</el-button>
        
        <div class="log-area">
          <h3>测试日志：</h3>
          <pre>{{ logContent }}</pre>
        </div>
        
        <div v-if="responseData" class="response-data">
          <h3>API响应数据：</h3>
          <pre>{{ JSON.stringify(responseData, null, 2) }}</pre>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import request from '../api/request'

const logContent = ref('')
const responseData = ref<any>(null)

const log = (message: string) => {
  logContent.value += `[${new Date().toLocaleTimeString()}] ${message}\n`
  console.log(message)
}

const testApi = async () => {
  log('开始测试字符编码...')
  
  try {
    log('发送请求到: /app/music/feedback/admin/list')
    const response = await request.get('/app/music/feedback/admin/list', {
      params: { page: 1, size: 5 }
    })
    
    log('请求成功接收响应')
    responseData.value = response
    
    // 检查数据中的中文字符
    if (response && response.records && response.records.length > 0) {
      const firstRecord = response.records[0]
      log(`第一条记录内容: ${firstRecord.content}`)
      log(`第一条记录类型: ${firstRecord.type}`)
      
      // 检查是否有乱码字符
      const hasGarbledText = firstRecord.content.includes('?') || 
                            firstRecord.content.includes('')
      
      if (hasGarbledText) {
        log('❌ 检测到乱码字符!')
      } else {
        log('✅ 字符编码正常')
      }
    }
    
  } catch (error: any) {
    log(`❌ 请求失败: ${error.message}`)
    log(`错误详情: ${JSON.stringify(error.response?.data || error, null, 2)}`)
  }
}

const clearLog = () => {
  logContent.value = ''
  responseData.value = null
}
</script>

<style scoped lang="scss">
.test-encoding {
  padding: 20px;
  
  .card-header {
    font-size: 18px;
    font-weight: bold;
  }
  
  .test-content {
    .log-area {
      margin-top: 20px;
      padding: 15px;
      background-color: #f5f5f5;
      border-radius: 4px;
      
      pre {
        white-space: pre-wrap;
        word-break: break-all;
        margin: 0;
        font-family: 'Courier New', monospace;
        font-size: 14px;
      }
    }
    
    .response-data {
      margin-top: 20px;
      padding: 15px;
      background-color: #e8f4fd;
      border-radius: 4px;
      
      pre {
        white-space: pre-wrap;
        word-break: break-all;
        margin: 0;
        font-family: 'Courier New', monospace;
        font-size: 12px;
        max-height: 300px;
        overflow-y: auto;
      }
    }
  }
}
</style>