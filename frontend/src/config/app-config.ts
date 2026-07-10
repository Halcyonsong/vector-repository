import { inject, provide, type InjectionKey } from 'vue'

export const DEFAULT_APP_CONFIG = {
  behavior: {
    apiBaseUrl: 'http://localhost:8080',
    defaultKnowledgeBaseId: 'kb001',
    defaultTopK: 3,
    defaultSimilarityThreshold: 0.5,
    topKRange: {
      min: 1,
      max: 20
    },
    similarityThresholdRange: {
      min: 0,
      max: 1
    },
    sessionTitleMaxLength: 100,
    reasoningCacheKey: 'vector-repository:latest-reasoning',
    reasoningCacheTtlMs: 30 * 60 * 1000,
    uploadTaskPollIntervalMs: 1000
  },
  labels: {
    sessionGroups: {
      today: '今天',
      week: '一周内',
      older: '更早'
    },
    feedback: {
      validationTitle: '参数问题',
      documentTitle: '文档处理失败',
      systemTitle: '服务异常',
      interruptedTitle: '已中断'
    },
    ui: {
      untitledSession: 'untitled',
      noFileSelected: '未选择文件',
      notConfigured: '未设置',
      initFailed: '初始化失败',
      sessionTitleEmpty: '会话标题不能为空',
      sessionTitleTooLong: '会话标题长度不能超过 100',
      sessionRenamePrompt: '请输入新的会话标题',
      sessionDeleteConfirmPrefix: '确认删除会话 "',
      sessionDeleteConfirmSuffix: '" 吗?',
      sessionRenameFailed: '重命名失败',
      sessionDeleteFailed: '删除失败',
      sessionCreateRequired: '请先创建会话',
      sessionHistoryFailed: '加载历史对话失败',
      earlierHistoryFailed: '获取更早历史对话失败',
      rollbackFailed: '回滚最后一轮对话失败',
      questionRequired: '请输入问题',
      knowledgeBaseRequired: '请输入 knowledgeBaseId',
      knowledgeBaseRequiredWhenEnabled: '启用知识库时必须填写 knowledgeBaseId',
      uploadSelectDocumentFirst: '请先选择 txt、pdf、ppt 或 pptx 文件',
      uploadFailed: '上传失败',
      uploadSuccessPrefix: '上传完成, 已写入 ',
      uploadSuccessSuffix: ' 个文本块',
      uploadTaskRunning: '当前知识库仍在构建中，请等待任务完成后再上传其他文件',
      chatFailed: '聊天失败',
      chatInterrupted: '本轮回答已中断',
      chatRequestFailed: '请求失败',
      stopRequestFailed: '停止请求失败',
      requestFailedPrefix: '请求失败: ',
      networkError: '无法连接后端服务, 请确认后端已启动',
      unknownError: '未知错误',
      invalidResponse: '响应格式不符合预期'
    },
    chat: {
      pageEyebrow: 'Conversation',
      pageTitle: 'AI 对话',
      currentSessionEyebrow: 'Current Session',
      sessionEyebrow: 'Session',
      reasoningEyebrow: 'Reasoning',
      reasoningTitle: '思考过程',
      knowledgeDisabled: '未启用知识库',
      knowledgeEnabled: '知识库已启用',
      allowEmptyContextLabel: '空检索转发',
      allowEmptyContextEnabled: '允许转发',
      allowEmptyContextDisabled: '禁止转发',
      emptyTitle: '准备开始一轮对话',
      emptyDescription: '选择会话后直接提问。在下方输入区控制知识库是否启用。',
      userRole: 'You',
      assistantRole: 'Assistant',
      answerPending: '正在等待正式回答输出...',
      toggleKnowledgeBase: '启用知识库',
      knowledgeBaseLabel: 'Knowledge Base ID',
      knowledgeBasePlaceholder: '例如 kb001',
      topKLabel: 'Top K',
      topKPlaceholder: '默认 {default}, 范围 {min}-{max}',
      similarityThresholdLabel: 'Similarity Threshold',
      similarityThresholdPlaceholder: '默认 {default}, 范围 {min}-{max}',
      outputStatusLabel: 'Output',
      outputStatusNormal: '正常输出',
      outputStatusError: '服务异常',
      startupTimerLabel: '开始对话用时',
      reasoningTimerLabel: '思考用时',
      answerTimerLabel: '输出用时',
      questionPlaceholder: '输入问题开始对话',
      stopButton: '停止',
      sendButton: '发送',
      reasoningEmpty: '当前没有思考输出。若模型返回 reasoningContent, 这里会实时显示。',
      pullEarlierHistory: '加载更多历史记录',
      loadingEarlierHistory: '正在加载更多历史记录...',
      noMoreHistory: '没有更多历史记录',
      latestRoundActionsLabel: '最后一轮对话操作',
      copyMessageTitle: '复制该回复',
      copyDoneTitle: '已复制',
      rollbackDeleteTitle: '删除最后一轮对话',
      rollbackUndoTitle: '撤销并编辑最后一条问题'
    },
    knowledge: {
      pageEyebrow: 'Knowledge Base',
      pageTitle: '知识库工作台',
      modeLabel: 'Mode',
      formatsLabel: 'Formats',
      librariesLabel: 'Libraries',
      pageMode: 'Ingestion',
      pageModeDescription: 'TXT / PDF / PPT / PPTX',
      heroEyebrow: 'Active Index',
      heroTitle: '向量知识库',
      heroDescription: '上传、分库与索引管理。对话页决定是否启用检索。',
      targetKnowledgeBase: '当前知识库',
      selectedFile: '待上传文件',
      knowledgeBaseLabel: 'Knowledge Base ID',
      knowledgeBasePlaceholder: '例如: kb001',
      uploadEyebrow: 'Source',
      uploadDocumentLabel: '文档索引',
      dropzoneTitle: '拖拽文件到这里',
      dropzoneDescription: '或点击选择文档。支持 TXT / PDF / PPT / PPTX',
      currentFile: '当前选择',
      uploadButton: '写入索引',
      uploadingButton: '上传中...',
      taskRunningButton: '构建中...',
      taskProgressEyebrow: 'Task Progress',
      taskProgressTitle: '知识库构建进度',
      taskIdLabel: 'Task ID',
      taskStatusLabel: 'Status',
      taskChunksLabel: 'Chunks',
      taskBatchesLabel: 'Batches',
      taskPendingMessage: '上传任务已创建，等待后端处理',
      taskCompletedMessage: '知识库构建完成',
      taskFailedMessage: '知识库构建失败',
      backButton: '返回对话',
      libraryEyebrow: 'Indexes',
      existingKnowledgeBasesTitle: '知识库',
      existingKnowledgeBasesDescription: '点击条目即可切换当前知识库。',
      availableKnowledgeBasesSuffix: ' 个索引可用',
      listLoading: '正在加载知识库...',
      emptyKnowledgeBases: '暂未查询到知识库',
      selectButton: '切换',
      selectedBadge: 'Active',
      deleteButton: '删除',
      deletingButton: '删除中...',
      refreshButton: '刷新列表',
      listFailed: '加载知识库列表失败',
      deleteFailed: '删除知识库失败',
      deleteConfirmPrefix: '确认删除知识库 ',
      deleteConfirmSuffix: '? 该操作会删除对应向量数据。'
    }
  }
} as const

export type AppRuntimeConfig = typeof DEFAULT_APP_CONFIG
export type AppBehaviorConfig = AppRuntimeConfig['behavior']

const APP_CONFIG_KEY: InjectionKey<AppRuntimeConfig> = Symbol('app-config')
let runtimeConfig: AppRuntimeConfig = DEFAULT_APP_CONFIG

export function provideAppConfig(config: AppRuntimeConfig = DEFAULT_APP_CONFIG): AppRuntimeConfig {
  runtimeConfig = config
  provide(APP_CONFIG_KEY, config)
  return config
}

export function useAppConfig(): AppRuntimeConfig {
  return inject(APP_CONFIG_KEY, runtimeConfig)
}

export function getAppConfig(): AppRuntimeConfig {
  return runtimeConfig
}
