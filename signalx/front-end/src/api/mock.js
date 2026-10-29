const MOCK_NOW = new Date('2026-04-09T10:00:00+08:00')

let idSeed = 2000

const delay = (ms = 180) => new Promise((resolve) => setTimeout(resolve, ms))

const clone = (value) => JSON.parse(JSON.stringify(value))

const pad = (value) => String(value).padStart(2, '0')

const shiftTime = ({ days = 0, hours = 0, minutes = 0 }) => {
  const offset = (((days * 24) + hours) * 60 + minutes) * 60 * 1000
  return new Date(MOCK_NOW.getTime() - offset)
}

const isoAgo = (offset = {}) => shiftTime(offset).toISOString()

const pageResult = (list, params = {}) => {
  const page = Number(params.page || 1)
  const size = Number(params.size || 10)
  const start = (page - 1) * size
  return {
    list: clone(list.slice(start, start + size)),
    total: list.length
  }
}

const sortByDateDesc = (list, fieldNames) => {
  return [...list].sort((a, b) => {
    const aField = fieldNames.find((field) => a[field])
    const bField = fieldNames.find((field) => b[field])
    const aTime = new Date(aField ? a[aField] : 0).getTime()
    const bTime = new Date(bField ? b[bField] : 0).getTime()
    return bTime - aTime
  })
}

const normalizeKeywords = (keywords) => {
  if (Array.isArray(keywords)) return keywords.filter(Boolean)
  if (!keywords) return []
  return [String(keywords)]
}

const nextId = (prefix) => `${prefix}-${++idSeed}`

const makeTaskId = () => `task-${String(++idSeed).padStart(4, '0')}`

const sentimentLabel = (value) => {
  const sentiment = String(value || '').toLowerCase()
  if (sentiment.includes('positive')) return '正向'
  if (sentiment.includes('negative')) return '负向'
  return '中性'
}

const detectPreset = (text = '') => {
  const source = String(text).toLowerCase()
  if (/ai|人工智能|大模型|gpt|模型|算力/.test(source)) return 'ai'
  if (/新能源|储能|光伏|风电|电池|energy/.test(source)) return 'energy'
  if (/跨境|出海|电商|消费|品牌|零售/.test(source)) return 'commerce'
  if (/医保|医药|创新药|医疗|health/.test(source)) return 'health'
  if (/iphone|手机|苹果|消费电子|硬件/.test(source)) return 'consumer'
  if (/高校|保研|学院|大学|campus|招生/.test(source)) return 'academia'
  if (/tesla|特斯拉|股价|投资者|财报|资本/.test(source)) return 'capital'
  return 'generic'
}

const PRESETS = {
  ai: {
    title: '人工智能',
    focus: '落地效果与合规治理',
    overview: '与 {subject} 相关的讨论已经从“概念新鲜感”转向“业务 ROI 和部署成本”，适合在演示里突出商业化价值。',
    recommendation: '先展示落地场景，再补充风险控制和预算效率，最容易形成闭环。',
    riskLine: '数据安全、模型幻觉和预算控制的边界',
    valueLine: '企业客户已经把 {subject} 从试点推向规模化验证',
    channels: [38, 31, 19, 12],
    sources: ['36氪', '量子位', '钛媒体', 'InfoQ']
  },
  energy: {
    title: '新能源',
    focus: '项目进度与价格弹性',
    overview: '{subject} 的讨论热度主要来自招投标、储能配套和产能消化，整体情绪偏积极但对盈利能力保持谨慎。',
    recommendation: '演示时把“景气度提升”和“盈利承压”同时讲清楚，会更像真实业务分析。',
    riskLine: '价格波动、产能过剩和政策节奏变化',
    valueLine: '{subject} 在项目落地和装机节奏上仍有明显增量',
    channels: [34, 24, 28, 14],
    sources: ['中国能源报', '财联社', '北极星电力网', '证券时报']
  },
  commerce: {
    title: '跨境电商',
    focus: '平台规则与转化效率',
    overview: '{subject} 的舆情以平台政策变化、物流效率和投放回报率为核心，适合用于展示经营分析类能力。',
    recommendation: '建议先讲增长机会，再补充平台规则和履约风险，叙事会更平衡。',
    riskLine: '平台规则变动、履约成本上升和投放效率下滑',
    valueLine: '{subject} 在品牌化和精细化运营上出现了新的增长窗口',
    channels: [29, 37, 21, 13],
    sources: ['雨果跨境', '亿邦动力', '晚点', '品牌星球']
  },
  health: {
    title: '医药健康',
    focus: '政策变化与支付能力',
    overview: '{subject} 的讨论高度依赖政策节点，正向关注点是准入扩容，负向关注点是价格压缩与执行落差。',
    recommendation: '现场演示可以重点强调政策驱动与市场反馈如何同时进入监测视图。',
    riskLine: '支付端约束、价格压缩和地方执行差异',
    valueLine: '{subject} 相关议题具有明显的政策带动效应和高关注度',
    channels: [42, 18, 24, 16],
    sources: ['医药魔方', '健康界', '第一财经', '财新']
  },
  consumer: {
    title: '消费电子',
    focus: '口碑反馈与产品迭代',
    overview: '{subject} 的讨论更偏用户视角，最容易引发传播的是续航、影像、价格和使用体验。',
    recommendation: '演示时可先展示用户好评点，再切到争议点，画面感会更强。',
    riskLine: '价格预期管理、质量争议和负面口碑扩散',
    valueLine: '{subject} 的真实用户反馈足够密集，适合做口碑追踪演示',
    channels: [27, 44, 17, 12],
    sources: ['少数派', '极客公园', 'PhoneArena', '新浪科技']
  },
  academia: {
    title: '高校舆情',
    focus: '招生政策与节点公告',
    overview: '{subject} 的传播结构相对集中，官方网站与社群讨论会在关键时间点同时放量，适合展示定向采集价值。',
    recommendation: '建议突出“时间节点明确、来源集中、需求强烈”这三个特点。',
    riskLine: '政策解读偏差、信息滞后和社群误传',
    valueLine: '{subject} 非常适合通过定向站点采集形成高信噪比数据',
    channels: [46, 22, 12, 20],
    sources: ['学院官网', '研招办公告', '高校论坛', '学生社群']
  },
  capital: {
    title: '资本市场',
    focus: '财报兑现与预期管理',
    overview: '{subject} 的舆情明显受财报、评级和价格动作驱动，适合拿来演示事件驱动型监测。',
    recommendation: '先讲事件，再讲市场反应，最后补一句投资者预期变化，逻辑会最顺。',
    riskLine: '业绩兑现不及预期、估值回调和舆论分化',
    valueLine: '{subject} 兼具高热度和高时效性，很适合做仪表盘演示',
    channels: [36, 26, 23, 15],
    sources: ['路透', '华尔街见闻', '财联社', '彭博']
  },
  generic: {
    title: '行业监测',
    focus: '热点事件与风险变化',
    overview: '{subject} 的讨论在近几天持续升温，话题从单点事件扩散到市场预期与执行层面。',
    recommendation: '先用热点事件吸引注意，再讲趋势和风险，适合做通用演示。',
    riskLine: '负面舆情扩散速度和执行不确定性',
    valueLine: '{subject} 的讨论已经形成连续性，足够支撑一场完整演示',
    channels: [35, 28, 22, 15],
    sources: ['行业媒体', '财经媒体', '社群渠道', '公众号']
  }
}

const EVENT_TEMPLATES = {
  ai: [
    {
      title: '{subject} 从试点进入规模化采购阶段',
      summary: '多家企业的讨论重心转向成本、交付和组织协同，说明 {subject} 已经不再只是概念型热点。',
      sentiment: 'positive'
    },
    {
      title: '{subject} 的成本与效果争议同步升温',
      summary: '讨论聚焦模型效果稳定性、算力预算和私有化部署，现场演示时很适合用来体现“热度与争议并存”。',
      sentiment: 'neutral'
    },
    {
      title: '{subject} 的合规要求被反复提及',
      summary: '围绕数据出境、权限分层和输出审核的讨论明显增多，负向情绪主要来自风险感知而非功能本身。',
      sentiment: 'negative'
    },
    {
      title: '客户重新评估 {subject} 产业链兑现节奏',
      summary: '从模型厂商到应用集成商，市场更关注签单、复购与交付周期，热点已经开始向商业化收敛。',
      sentiment: 'positive'
    }
  ],
  energy: [
    {
      title: '{subject} 项目招标节奏加快',
      summary: '近期项目推进速度明显提升，舆情热度主要来自装机、并网和配套储能方案的讨论。',
      sentiment: 'positive'
    },
    {
      title: '{subject} 价格竞争拉高行业关注度',
      summary: '价格波动带来情绪分化，一部分观点看好需求恢复，另一部分则担心利润空间继续被压缩。',
      sentiment: 'neutral'
    },
    {
      title: '{subject} 的盈利与现金流压力被放大',
      summary: '负面讨论主要集中在利润率、库存和融资压力，适合在演示里展示风险识别能力。',
      sentiment: 'negative'
    },
    {
      title: '{subject} 配套基础设施成为新增话题',
      summary: '从储能到配网改造，新的配套议题让传播热度保持在高位，并形成更多业务机会讨论。',
      sentiment: 'positive'
    }
  ],
  commerce: [
    {
      title: '{subject} 进入平台增长窗口',
      summary: '平台活动节奏与流量回升带动品牌讨论升温，多个渠道都在强调转化效率改善。',
      sentiment: 'positive'
    },
    {
      title: '{subject} 的履约与物流问题被集中讨论',
      summary: '虽然整体趋势向上，但履约体验和物流波动仍是用户抱怨的重点。',
      sentiment: 'neutral'
    },
    {
      title: '平台规则变化影响 {subject} 投放策略',
      summary: '商家更关心广告成本、内容审核和平台政策更新，这部分很适合做风险预警展示。',
      sentiment: 'negative'
    },
    {
      title: '{subject} 的品牌化打法开始受到关注',
      summary: '从价格竞争转向品牌资产沉淀，舆情里出现了更多关于长期经营和复购率的讨论。',
      sentiment: 'positive'
    }
  ],
  health: [
    {
      title: '{subject} 的政策窗口释放正向信号',
      summary: '政策和支付端的变化带来集中关注，市场更关心准入扩容和执行落地速度。',
      sentiment: 'positive'
    },
    {
      title: '{subject} 的支付能力与落地效率被持续追问',
      summary: '讨论一方面认可政策方向，另一方面也关注地方执行节奏和真实使用反馈。',
      sentiment: 'neutral'
    },
    {
      title: '{subject} 的价格压力引发负面讨论',
      summary: '部分舆情聚焦价格压缩、利润承压和后续商业可持续性，适合用来演示风险识别视图。',
      sentiment: 'negative'
    },
    {
      title: '{subject} 的患者可及性成为高频话题',
      summary: '从专家观点到患者反馈，舆情里不断出现“可及性提升”和“决策效率改善”的正向表达。',
      sentiment: 'positive'
    }
  ],
  consumer: [
    {
      title: '{subject} 的续航和流畅度评价走高',
      summary: '首批用户反馈集中在基础体验改善，正向内容更容易被社交平台放大传播。',
      sentiment: 'positive'
    },
    {
      title: '{subject} 的价格讨论引发分化',
      summary: '消费者对新品定位、价格带和换机必要性看法不一，中性讨论量明显增加。',
      sentiment: 'neutral'
    },
    {
      title: '{subject} 的影像与质量争议出现扩散',
      summary: '负面内容主要围绕细节体验和品控讨论，适合用来演示异常话题捕捉能力。',
      sentiment: 'negative'
    },
    {
      title: '{subject} 的内容生态和配件需求被带动',
      summary: '除产品本身外，周边配件、系统生态与使用场景也在拉高整体讨论热度。',
      sentiment: 'positive'
    }
  ],
  academia: [
    {
      title: '{subject} 的官方通知触发集中检索',
      summary: '一旦官网出现新通知，社群和论坛会迅速转发并解读，非常适合展示定向采集结果。',
      sentiment: 'positive'
    },
    {
      title: '{subject} 的政策解读出现版本差异',
      summary: '部分讨论对规则理解不一致，中性与疑问型内容快速增多。',
      sentiment: 'neutral'
    },
    {
      title: '{subject} 的时间节点焦虑带来负面情绪',
      summary: '临近报名或结果节点时，用户对信息滞后和传言的敏感度明显提升。',
      sentiment: 'negative'
    },
    {
      title: '{subject} 的高质量信息源逐渐收敛',
      summary: '随着信息核验完成，更多讨论重新回流至官网、权威通知和经验帖。',
      sentiment: 'positive'
    }
  ],
  capital: [
    {
      title: '{subject} 相关事件带动市场短线热度',
      summary: '财报、评级和价格动作共同推动讨论升温，媒体与投资者社区同步放量。',
      sentiment: 'positive'
    },
    {
      title: '{subject} 的估值分歧正在扩大',
      summary: '乐观观点和保守观点并存，中性讨论明显增加，市场在重新校准预期。',
      sentiment: 'neutral'
    },
    {
      title: '{subject} 的利润压力引发负面情绪',
      summary: '成本、毛利率和需求弹性成为担忧重点，负面内容主要来自盈利能力判断。',
      sentiment: 'negative'
    },
    {
      title: '机构继续跟踪 {subject} 的兑现节奏',
      summary: '在事件驱动之后，讨论焦点逐步转向中长期兑现与管理层沟通。',
      sentiment: 'positive'
    }
  ],
  generic: [
    {
      title: '{subject} 的热度继续抬升',
      summary: '近几天内相关讨论从单点事件扩散至行业趋势，说明 {subject} 已经具备持续监测价值。',
      sentiment: 'positive'
    },
    {
      title: '{subject} 的看法出现分化',
      summary: '不同平台对 {subject} 的观点不完全一致，中性和观望型内容占比提升。',
      sentiment: 'neutral'
    },
    {
      title: '{subject} 的风险提示增加',
      summary: '负面内容主要围绕执行不确定性和市场预期落差，适合用来演示预警能力。',
      sentiment: 'negative'
    },
    {
      title: '{subject} 的机会点被重新梳理',
      summary: '在讨论升温后，更多内容开始回到商业机会、竞争格局和执行效率上。',
      sentiment: 'positive'
    }
  ]
}

const users = [
  { id: '1', username: '__CONFIGURE_LOCALLY__', email: 'admin@signalx.demo', role: 'ADMIN', status: 'active' },
  { id: '2', username: '__CONFIGURE_LOCALLY__', email: 'demo@signalx.demo', role: 'USER', status: 'active' },
  { id: '3', username: '__CONFIGURE_LOCALLY__', email: 'analyst@signalx.demo', role: 'USER', status: 'active' },
  { id: '4', username: '__CONFIGURE_LOCALLY__', email: 'operator@signalx.demo', role: 'USER', status: 'inactive' }
]

const userPasswords = new Map([
  ['1', '123456'],
  ['2', '123456'],
  ['3', '123456'],
  ['4', '123456']
])

let tasks = [
  {
    taskId: 'task-001',
    userId: '1',
    keywords: ['人工智能', '大模型'],
    time_range: '24h',
    interval: 60,
    status: 'active',
    created_at: isoAgo({ days: 2, hours: 1 })
  },
  {
    taskId: 'task-002',
    userId: '1',
    keywords: ['新能源', '储能'],
    time_range: '7d',
    interval: 360,
    status: 'active',
    created_at: isoAgo({ days: 4, hours: 3 })
  },
  {
    taskId: 'task-003',
    userId: '2',
    keywords: ['跨境电商', '品牌出海'],
    time_range: '7d',
    interval: 720,
    status: 'active',
    created_at: isoAgo({ days: 6, hours: 6 })
  },
  {
    taskId: 'task-004',
    userId: '3',
    keywords: ['创新药', '医保谈判'],
    time_range: '30d',
    interval: 1440,
    status: 'inactive',
    created_at: isoAgo({ days: 8, hours: 5 })
  }
]

let openclawTasks = [
  {
    id: 101,
    name: '特斯拉投资者关系动态追踪',
    url: 'https://ir.tesla.com',
    crawlLogic: '采集财报、经营指标、投资者问答和重点媒体评论。',
    startTime: isoAgo({ days: 12 }),
    endTime: isoAgo({ days: -8 }),
    interval: 360,
    status: 1,
    createTime: isoAgo({ days: 5, hours: 1 })
  },
  {
    id: 102,
    name: '国家医保目录调整专题采集',
    url: 'https://www.nhsa.gov.cn',
    crawlLogic: '跟踪医保目录调整、专家解读、药企反馈和患者讨论。',
    startTime: isoAgo({ days: 7 }),
    endTime: isoAgo({ days: -18 }),
    interval: 720,
    status: 1,
    createTime: isoAgo({ days: 7, hours: 4 })
  },
  {
    id: 103,
    name: 'iPhone 16 口碑与测评监控',
    url: 'https://www.apple.com',
    crawlLogic: '聚合测评媒体、社交平台和论坛中关于续航、影像和价格的讨论。',
    startTime: isoAgo({ days: 4 }),
    endTime: isoAgo({ days: -20 }),
    interval: 60,
    status: 0,
    createTime: isoAgo({ days: 1, hours: 8 })
  }
]

let slices = []
let openclawSlices = []
let newsRecords = []
let reports = []
let reportDetails = new Map()

const buildSubject = (task, taskType) => {
  if (taskType === 1) return task.name
  const keywords = normalizeKeywords(task.keywords)
  return keywords.length > 0 ? keywords.join(' / ') : '未命名任务'
}

const interpolate = (template, subject) => template.replace(/\{subject\}/g, subject)

const buildReportMarkdown = ({ reportName, subject, preset, events, taskType, createdAt }) => {
  const positives = events.filter((item) => item.sentiment === 'positive').length
  const neutrals = events.filter((item) => item.sentiment === 'neutral').length
  const negatives = events.filter((item) => item.sentiment === 'negative').length
  const topEvents = [...events].sort((a, b) => b.heat - a.heat).slice(0, 3)

  const lines = [
    `# ${reportName}`,
    '',
    `**监测对象**：${subject}`,
    `**任务类型**：${taskType === 1 ? 'Openclaw 定向采集' : '常规监测任务'}`,
    `**生成时间**：${new Date(createdAt).toLocaleString('zh-CN', { hour12: false })}`,
    '',
    '## 一、结论速览',
    interpolate(preset.overview, subject),
    '',
    `- 事件总量：${events.length}`,
    `- 情绪结构：正向 ${positives} / 中性 ${neutrals} / 负向 ${negatives}`,
    `- 最高热度：${topEvents[0]?.heat || 0}`,
    `- 建议动作：${preset.recommendation}`,
    '',
    '## 二、重点事件'
  ]

  topEvents.forEach((event, index) => {
    lines.push('')
    lines.push(`${index + 1}. **${event.title}**`)
    lines.push(`热度：${event.heat} | 情绪：${sentimentLabel(event.sentiment)}`)
    lines.push(event.summary)
  })

  lines.push('')
  lines.push('## 三、传播结构')
  lines.push('')
  lines.push('```mermaid')
  lines.push('pie showData')
  lines.push('    title 渠道占比')
  lines.push(`    "媒体" : ${preset.channels[0]}`)
  lines.push(`    "社交平台" : ${preset.channels[1]}`)
  lines.push(`    "论坛/社区" : ${preset.channels[2]}`)
  lines.push(`    "自有站点" : ${preset.channels[3]}`)
  lines.push('```')
  lines.push('')
  lines.push('## 四、演示话术')
  lines.push(`- 开场先讲“${topEvents[0]?.title || subject}”，最容易把注意力拉到高热事件。`)
  lines.push(`- 如果被问到风险，可重点说明 ${preset.riskLine}。`)
  lines.push(`- 如果要强调业务价值，可以补充 ${interpolate(preset.valueLine, subject)}。`)

  return lines.join('\n')
}

const buildNewsEntries = ({ event, subject, preset, taskType, index, taskId }) => {
  const sourceA = preset.sources[index % preset.sources.length]
  const sourceB = preset.sources[(index + 1) % preset.sources.length]

  return [
    {
      id: `${event.id}-news-1`,
      taskId,
      taskType,
      eventId: event.id,
      title: `${event.title}：一线反馈开始集中释放`,
      url: `https://example.com/${event.id}/briefing`,
      content: `${subject} 相关讨论在多个渠道同步升温，最新反馈集中在 ${preset.focus}，说明该话题已经进入可持续跟踪阶段。`,
      publishTime: new Date(new Date(event.eventTime).getTime() - 45 * 60 * 1000).toISOString(),
      source: sourceA,
      sentiment: event.sentiment
    },
    {
      id: `${event.id}-news-2`,
      taskId,
      taskType,
      eventId: event.id,
      title: `${event.title}：分析观点开始分层`,
      url: `https://example.com/${event.id}/analysis`,
      content: `媒体与社群对于 ${subject} 的看法出现分层，主线仍围绕 ${preset.focus} 和执行节奏展开。`,
      publishTime: new Date(new Date(event.eventTime).getTime() - 2 * 60 * 60 * 1000).toISOString(),
      source: sourceB,
      sentiment: event.sentiment === 'negative' ? 'neutral' : event.sentiment
    }
  ]
}

const buildArtifactsForTask = (task, taskType) => {
  const subject = buildSubject(task, taskType)
  const presetKey = detectPreset(
    taskType === 1
      ? `${task.name} ${task.url} ${task.crawlLogic || ''}`
      : normalizeKeywords(task.keywords).join(' ')
  )
  const preset = PRESETS[presetKey]
  const templates = EVENT_TEMPLATES[presetKey]
  const baseId = taskType === 1 ? String(task.id) : task.taskId

  const eventList = templates.map((template, index) => {
    const eventTime = new Date(
      MOCK_NOW.getTime() - ((index * 10 + (taskType === 1 ? 6 : 3)) * 60 * 60 * 1000)
    ).toISOString()

    return {
      id: `slice-${baseId}-${index + 1}`,
      taskId: taskType === 1 ? Number(task.id) : task.taskId,
      title: interpolate(template.title, subject),
      heat: 96 - index * 7 - (taskType === 1 ? 1 : 0),
      sentiment: template.sentiment,
      summary: interpolate(template.summary, subject),
      eventTime,
      date: eventTime,
      status: index < 2 ? 'completed' : 'processing'
    }
  })

  const newsList = eventList.flatMap((event, index) =>
    buildNewsEntries({
      event,
      subject,
      preset,
      taskType,
      index,
      taskId: taskType === 1 ? Number(task.id) : task.taskId
    })
  )

  const reportNames = [
    taskType === 1 ? `${subject} 定向采集日报` : `${subject} 舆情晨报`,
    taskType === 1 ? `${subject} 热点事件解读` : `${subject} 重点事件解读`,
    taskType === 1 ? `${subject} 风险与机会复盘` : `${subject} 风险与机会跟踪`
  ]

  const reportList = reportNames.map((name, index) => {
    const reportId = nextId(taskType === 1 ? 'ocl-report' : 'report')
    const createTime = new Date(
      MOCK_NOW.getTime() - ((index * 18 + (taskType === 1 ? 2 : 1)) * 60 * 60 * 1000)
    ).toISOString()

    reportDetails.set(reportId, {
      id: reportId,
      reportId,
      reportName: name,
      name,
      taskId: taskType === 1 ? Number(task.id) : task.taskId,
      taskType,
      createTime,
      summary: buildReportMarkdown({
        reportName: name,
        subject,
        preset,
        events: eventList,
        taskType,
        createdAt: createTime
      }),
      topEvents: eventList.map((item) => item.id)
    })

    return {
      id: reportId,
      reportId,
      taskId: taskType === 1 ? Number(task.id) : task.taskId,
      taskType,
      name,
      reportName: name,
      status: index === 0 ? 'success' : 'completed',
      createTime,
      sentiment: eventList[0]?.sentiment || 'neutral'
    }
  })

  return { events: eventList, news: newsList, reports: reportList }
}

const removeArtifacts = (taskId, taskType) => {
  const normalizedTaskId = String(taskId)
  const toDeleteReportIds = reports
    .filter((item) => String(item.taskId) === normalizedTaskId && Number(item.taskType) === Number(taskType))
    .map((item) => item.id || item.reportId)

  toDeleteReportIds.forEach((id) => reportDetails.delete(id))
  reports = reports.filter(
    (item) => !(String(item.taskId) === normalizedTaskId && Number(item.taskType) === Number(taskType))
  )
  newsRecords = newsRecords.filter(
    (item) => !(String(item.taskId) === normalizedTaskId && Number(item.taskType) === Number(taskType))
  )

  if (Number(taskType) === 1) {
    openclawSlices = openclawSlices.filter((item) => String(item.taskId) !== normalizedTaskId)
  } else {
    slices = slices.filter((item) => String(item.taskId) !== normalizedTaskId)
  }
}

const seedTaskArtifacts = (task, taskType) => {
  removeArtifacts(taskType === 1 ? task.id : task.taskId, taskType)
  const payload = buildArtifactsForTask(task, taskType)

  if (taskType === 1) {
    openclawSlices = [...openclawSlices, ...payload.events]
  } else {
    slices = [...slices, ...payload.events]
  }

  newsRecords = [...newsRecords, ...payload.news]
  reports = [...reports, ...payload.reports]
}

const initMockState = () => {
  slices = []
  openclawSlices = []
  newsRecords = []
  reports = []
  reportDetails = new Map()

  tasks.forEach((task) => seedTaskArtifacts(task, 0))
  openclawTasks.forEach((task) => seedTaskArtifacts(task, 1))
}

initMockState()

const findReportOrEvent = (id) => {
  const report = reports.find((item) => item.id === id || item.reportId === id)
  if (report) return { type: 'report', value: report }

  const event = [...slices, ...openclawSlices].find((item) => item.id === id)
  if (event) return { type: 'event', value: event }

  return null
}

const buildReportAnswer = (question, report) => {
  const detail = reportDetails.get(report.id || report.reportId)
  const events = (detail?.topEvents || [])
    .map((eventId) => [...slices, ...openclawSlices].find((item) => item.id === eventId))
    .filter(Boolean)

  const topEvent = events[0]
  const negativeEvent = events.find((item) => item.sentiment === 'negative')

  if (/风险|隐患|负面/.test(question)) {
    return [
      `围绕“${question}”，当前最值得关注的风险主要有三点：`,
      `1. ${negativeEvent ? negativeEvent.title : '热点话题存在情绪分化'}。`,
      `2. 讨论重心已经从热度本身转向执行细节，说明外部预期更高。`,
      `3. 如果现场被追问，建议同步补充应对动作和监测频率。`
    ].join('\n')
  }

  if (/建议|怎么讲|演示|汇报/.test(question)) {
    return [
      '演示时可以这样讲：',
      `1. 先用“${topEvent ? topEvent.title : report.name}”作为开场，快速说明为什么热。`,
      '2. 再用情绪分布和重点事件告诉观众，这不是单点消息，而是连续趋势。',
      '3. 最后补一句风险与机会并存，展示系统不仅能看热度，也能支持判断。'
    ].join('\n')
  }

  return [
    `关于“${question}”，报告里的核心结论是：${detail ? detail.reportName : report.name}对应的话题仍在持续升温。`,
    `当前最热事件是“${topEvent ? topEvent.title : '重点事件'}”，说明市场关注点已经聚焦到执行层面。`,
    `如果需要一句话总结，可以说：这是一组“热度高、信号多、适合持续跟踪”的舆情样本。`
  ].join('\n')
}

const buildEventAnswer = (question, event) => {
  const relatedNews = newsRecords.filter((item) => item.eventId === event.id)

  return [
    `针对“${question}”，当前事件“${event.title}”可以这样概括：`,
    `1. 事件热度为 ${event.heat}，属于近期关注度较高的话题。`,
    `2. 情绪判断为${sentimentLabel(event.sentiment)}，相关资讯共 ${relatedNews.length} 条。`,
    `3. 这类事件最适合在演示里展示“从热点发现到资讯联动”的完整链路。`
  ].join('\n')
}

export default {
  async login(data) {
    await delay()
    const user = users.find((item) => item.username === data.username)

    if (!user) {
      throw new Error('用户不存在')
    }

    const password = userPasswords.get(user.id) || '123456'
    if (data.password && data.password !== password) {
      throw new Error('密码错误')
    }

    return {
      token: `mock-token-${user.id}-${Date.now()}`,
      id: user.id,
      username: user.username,
      email: user.email,
      role: user.role
    }
  },

  async getUserList(params = {}) {
    await delay()
    const list = [...users].filter((item) => {
      if (!params.keyword) return true
      return item.username.includes(params.keyword) || item.email.includes(params.keyword)
    })
    return pageResult(list, params)
  },

  async addUser(data) {
    await delay()
    const duplicated = users.find((item) => item.username === data.username)
    if (duplicated) {
      throw new Error('用户名已存在')
    }

    const id = String(++idSeed)
    const newUser = {
      id,
      username: data.username,
      email: data.email || `${data.username}@signalx.demo`,
      role: data.role || 'USER',
      status: 'active'
    }

    users.unshift(newUser)
    userPasswords.set(id, data.password || '123456')
    return { id }
  },

  async updateUser(id, data) {
    await delay()
    const target = users.find((item) => item.id === id)
    if (!target) {
      throw new Error('用户不存在')
    }

    Object.assign(target, data)
    return true
  },

  async deleteUser(id) {
    await delay()
    const index = users.findIndex((item) => item.id === id)
    if (index === -1) {
      throw new Error('用户不存在')
    }

    users.splice(index, 1)
    userPasswords.delete(id)
    return true
  },

  async createTask(data) {
    await delay()
    const taskId = makeTaskId()
    const newTask = {
      taskId,
      userId: data.userId || '1',
      keywords: normalizeKeywords(data.keywords),
      time_range: data.time_range || '24h',
      interval: Number(data.interval || 60),
      status: 'active',
      created_at: new Date().toISOString()
    }

    tasks.unshift(newTask)
    seedTaskArtifacts(newTask, 0)
    return { taskId }
  },

  async getTaskList(params = {}) {
    await delay()
    let list = sortByDateDesc(tasks, ['created_at', 'createTime'])
    if (params.userId) {
      list = list.filter((item) => String(item.userId) === String(params.userId))
    }
    return pageResult(list, params)
  },

  async updateTask(taskId, data) {
    await delay()
    const task = tasks.find((item) => item.taskId === taskId)
    if (!task) {
      throw new Error('任务不存在')
    }

    task.keywords = data.keywords ? normalizeKeywords(data.keywords) : task.keywords
    task.time_range = data.time_range || task.time_range
    task.interval = Number(data.interval || task.interval)
    seedTaskArtifacts(task, 0)
    return true
  },

  async switchTask(taskId, enable) {
    await delay()
    const task = tasks.find((item) => item.taskId === taskId)
    if (!task) {
      throw new Error('任务不存在')
    }

    task.status = enable ? 'active' : 'inactive'
    return { status: task.status }
  },

  async deleteTask(taskId) {
    await delay()
    const index = tasks.findIndex((item) => item.taskId === taskId)
    if (index === -1) {
      throw new Error('任务不存在')
    }

    tasks.splice(index, 1)
    removeArtifacts(taskId, 0)
    return true
  },

  async getAnalysisSlices(params = {}) {
    await delay()
    const taskId = params.taskId
    const list = sortByDateDesc(
      [...slices, ...openclawSlices].filter((item) => String(item.taskId) === String(taskId)),
      ['eventTime', 'date', 'createTime']
    )
    return pageResult(list, params)
  },

  async createOpenclawTask(data) {
    await delay()
    const id = openclawTasks.length > 0 ? Math.max(...openclawTasks.map((item) => Number(item.id))) + 1 : 101
    const newTask = {
      id,
      name: data.name,
      url: data.url,
      crawlLogic: data.crawlLogic || '定向采集任务',
      startTime: data.startTime || new Date().toISOString(),
      endTime: data.endTime || isoAgo({ days: -7 }),
      interval: Number(data.interval || 60),
      status: 1,
      createTime: new Date().toISOString()
    }

    openclawTasks.unshift(newTask)
    seedTaskArtifacts(newTask, 1)
    return { id }
  },

  async getOpenclawTaskList(params = {}) {
    await delay()
    const list = sortByDateDesc(openclawTasks, ['createTime', 'created_at'])
    return pageResult(list, params)
  },

  async updateOpenclawTask(taskId, data) {
    await delay()
    const task = openclawTasks.find((item) => String(item.id) === String(taskId))
    if (!task) {
      throw new Error('Openclaw 任务不存在')
    }

    Object.assign(task, {
      name: data.name ?? task.name,
      url: data.url ?? task.url,
      crawlLogic: data.crawlLogic ?? task.crawlLogic,
      interval: Number(data.interval ?? task.interval)
    })

    seedTaskArtifacts(task, 1)
    return true
  },

  async switchOpenclawTask(taskId, enable) {
    await delay()
    const task = openclawTasks.find((item) => String(item.id) === String(taskId))
    if (!task) {
      throw new Error('Openclaw 任务不存在')
    }

    task.status = enable ? 1 : 0
    return { status: enable ? 'ACTIVE' : 'INACTIVE' }
  },

  async deleteOpenclawTask(taskId) {
    await delay()
    const index = openclawTasks.findIndex((item) => String(item.id) === String(taskId))
    if (index === -1) {
      throw new Error('Openclaw 任务不存在')
    }

    openclawTasks.splice(index, 1)
    removeArtifacts(taskId, 1)
    return true
  },

  async getNewsList(params = {}) {
    await delay()
    let list = [...newsRecords]

    if (params.sliceId || params.eventId) {
      const eventId = params.sliceId || params.eventId
      list = list.filter((item) => item.eventId === eventId)
    } else if (params.taskId) {
      list = list.filter((item) => String(item.taskId) === String(params.taskId))
    } else {
      list = []
    }

    list = sortByDateDesc(list, ['publishTime', 'createTime'])
    return pageResult(list, params)
  },

  async getReportList(params = {}) {
    await delay()
    let list = [...reports]

    if (params.taskId) {
      list = list.filter((item) => String(item.taskId) === String(params.taskId))
    }

    if (params.taskType !== undefined && params.taskType !== null && params.taskType !== '') {
      list = list.filter((item) => Number(item.taskType) === Number(params.taskType))
    }

    list = sortByDateDesc(list, ['createTime', 'created_at'])
    return pageResult(list, params)
  },

  async getReportDetail(id) {
    await delay(260)
    const found = findReportOrEvent(id)

    if (!found) {
      throw new Error('报告不存在')
    }

    if (found.type === 'report') {
      const detail = reportDetails.get(found.value.id || found.value.reportId)
      return clone({
        id: detail.id,
        reportId: detail.reportId,
        reportName: detail.reportName,
        name: detail.name,
        taskId: detail.taskId,
        taskType: detail.taskType,
        createTime: detail.createTime,
        summary: detail.summary
      })
    }

    const event = found.value
    const preset = PRESETS[detectPreset(event.title)]
    const summary = buildReportMarkdown({
      reportName: event.title,
      subject: event.title,
      preset,
      events: [event],
      taskType: String(event.taskId).startsWith('task-') ? 0 : 1,
      createdAt: event.eventTime
    })

    return {
      id: event.id,
      reportId: event.id,
      reportName: event.title,
      name: event.title,
      taskId: event.taskId,
      createTime: event.eventTime,
      summary
    }
  },

  async deleteReport(id) {
    await delay()
    const index = reports.findIndex((item) => item.id === id || item.reportId === id)
    if (index === -1) {
      throw new Error('报告不存在')
    }

    const report = reports[index]
    reportDetails.delete(report.id || report.reportId)
    reports.splice(index, 1)
    return true
  },

  async askReport(data) {
    await delay(360)
    const report = reports.find((item) => item.id === data.reportId || item.reportId === data.reportId)
    if (!report) {
      throw new Error('报告不存在')
    }

    return {
      answer: buildReportAnswer(data.question, report)
    }
  },

  async askEvent(data) {
    await delay(360)
    const event = [...slices, ...openclawSlices].find((item) => item.id === data.eventId)
    if (!event) {
      throw new Error('事件不存在')
    }

    return {
      answer: buildEventAnswer(data.question, event)
    }
  }
}
