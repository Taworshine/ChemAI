<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="手写批注" name="annotations">
        <data-table :loader="loadAnnotations" :deleter="delAnnotation" :columns="annotationCols" />
      </el-tab-pane>
      <el-tab-pane label="会议聊天" name="chat">
        <data-table :loader="loadChat" :deleter="delChat" :columns="chatCols" />
      </el-tab-pane>
      <el-tab-pane label="投票" name="votes">
        <data-table :loader="loadVotes" :deleter="delVote" :columns="voteCols" />
      </el-tab-pane>
      <el-tab-pane label="会议纪要" name="summaries">
        <data-table :loader="loadSummaries" :deleter="delSummary" :columns="summaryCols" />
      </el-tab-pane>
      <el-tab-pane label="会后任务" name="tasks">
        <data-table :loader="loadTasks" :deleter="delTask" :columns="taskCols" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { h, ref } from 'vue'
import { ElMessage, ElTag } from 'element-plus'
import http from '../api'
import DataTable from './DataTable.vue'

const tab = ref('annotations')

// 手写批注
const annotationCols = [
  { prop: 'id', label: 'ID', width: 60 },
  { prop: 'meetingId', label: '会议ID', width: 80 },
  { prop: 'userId', label: '作者ID', width: 80 },
  { prop: 'type', label: '类型', width: 80 },
  { prop: 'page', label: '页码', width: 70 },
  { prop: 'content', label: '内容', minWidth: 260, overflow: true },
  { label: '已解决', width: 80, render: r => h(ElTag, { type: r.isResolved ? 'success' : 'info' }, () => (r.isResolved ? '是' : '否')) }
]
async function loadAnnotations() { return (await http.get('/dbadmin/annotations')).data }
async function delAnnotation(row) { await http.delete(`/dbadmin/annotations/${row.id}`); ElMessage.success('已删除') }

// 会议聊天
const chatCols = [
  { prop: 'id', label: 'ID', width: 60 },
  { prop: 'meetingId', label: '会议ID', width: 80 },
  { prop: 'userId', label: '发送人ID', width: 90 },
  { prop: 'msgType', label: '类型', width: 80 },
  { prop: 'content', label: '内容', minWidth: 320, overflow: true },
  { prop: 'createTime', label: '时间', width: 160 }
]
async function loadChat() { return (await http.get('/dbadmin/chat-messages')).data }
async function delChat(row) { await http.delete(`/dbadmin/chat-messages/${row.id}`); ElMessage.success('已删除') }

// 投票
const voteCols = [
  { prop: 'id', label: 'ID', width: 60 },
  { prop: 'meetingId', label: '会议ID', width: 80 },
  { prop: 'title', label: '标题', minWidth: 220, overflow: true },
  { prop: 'voteType', label: '类型', width: 90 },
  { label: '匿名', width: 70, render: r => (r.anonymous ? '是' : '否') },
  { label: '状态', width: 90, render: r => h(ElTag, { type: r.status ? 'info' : 'success' }, () => (r.status ? '已结束' : '进行中')) }
]
async function loadVotes() { return (await http.get('/dbadmin/votes')).data }
async function delVote(row) { await http.delete(`/dbadmin/votes/${row.id}`); ElMessage.success('已删除') }

// 会议纪要
const summaryCols = [
  { prop: 'id', label: 'ID', width: 60 },
  { prop: 'meetingId', label: '会议ID', width: 80 },
  { label: '来源', width: 90, render: r => (r.autoGenerate ? '自动生成' : '人工') },
  { label: '状态', width: 100, render: r => h(ElTag, { type: r.isConfirmed ? 'success' : 'info' }, () => (r.isConfirmed ? '已确认' : '草稿')) },
  { prop: 'updateTime', label: '更新时间', width: 160 }
]
async function loadSummaries() { return (await http.get('/dbadmin/summaries')).data }
async function delSummary(row) { await http.delete(`/dbadmin/summaries/${row.id}`); ElMessage.success('已删除') }

// 会后任务
const taskCols = [
  { prop: 'id', label: 'ID', width: 60 },
  { prop: 'meetingId', label: '会议ID', width: 80 },
  { prop: 'title', label: '任务内容', minWidth: 260, overflow: true },
  { prop: 'assigneeId', label: '负责人ID', width: 90 },
  { prop: 'deadline', label: '截止时间', width: 160 },
  { label: '状态', width: 90, render: r => h(ElTag, { type: ['primary', 'warning', 'success'][r.status] || 'info' }, () => ['待办', '进行中', '已完成'][r.status] ?? r.status) }
]
async function loadTasks() { return (await http.get('/dbadmin/tasks')).data }
async function delTask(row) { await http.delete(`/dbadmin/tasks/${row.id}`); ElMessage.success('已删除') }
</script>
