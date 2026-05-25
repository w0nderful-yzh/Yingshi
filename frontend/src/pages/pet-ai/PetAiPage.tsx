import { useState, useRef, useEffect, useCallback } from 'react';
import {
  Input,
  Select,
  message,
  Spin,
  Tag,
  Collapse,
  Progress,
} from 'antd';
import {
  SendOutlined,
  RobotOutlined,
  UserOutlined,
  SearchOutlined,
  HeartOutlined,
  ThunderboltOutlined,
  StopOutlined,
  PictureOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import {
  analyzePetBehavior,
  getHealthAdvice,
  petAiChatStream,
} from '@/api/petAi';
import type { BehaviorAnalysisResult } from '@/api/petAi';
import { getPets } from '@/api/pet';
import type { PetVO } from '@/types';
import { PetTypeMap } from '@/utils/constants';

const { TextArea } = Input;

/* ====================================================================
 *  格式化 AI 回复内容 —— 加粗、分段、图标
 * ==================================================================== */
function formatAIContent(text: string) {
  if (!text) return null;

  // 将 **text** 转换为加粗
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  const elements: React.ReactNode[] = [];

  parts.forEach((part, idx) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      elements.push(
        <strong key={idx} className="pet-ai-bold">
          {part.slice(2, -2)}
        </strong>,
      );
    } else {
      // 处理换行
      const lines = part.split('\n');
      lines.forEach((line, lineIdx) => {
        if (lineIdx > 0) {
          elements.push(<br key={`br-${idx}-${lineIdx}`} />);
        }
        // 添加图标映射
        let processedLine = line;
        // 针对列表项添加图标
        if (/^[\s]*[-•]\s/.test(processedLine)) {
          processedLine = processedLine.replace(/^([\s]*)[-•]\s/, '$1');
          elements.push(
            <span key={`icon-${idx}-${lineIdx}`} className="pet-ai-list-icon">
              {'🔹 '}
            </span>,
          );
        } else if (/^[\s]*\d+[.、]\s/.test(processedLine)) {
          // 数字列表保持原样但加粗数字
          const match = processedLine.match(/^([\s]*)(\d+[.、])\s(.*)/);
          if (match) {
            elements.push(
              <span key={`num-${idx}-${lineIdx}`}>
                <strong className="pet-ai-bold">{match[2]}</strong> {match[3]}
              </span>,
            );
            return;
          }
        }
        if (processedLine) {
          elements.push(
            <span key={`text-${idx}-${lineIdx}`}>{processedLine}</span>,
          );
        }
      });
    }
  });

  return elements;
}

/* ====================================================================
 *  AI 聊天 Tab（流式 SSE）
 * ==================================================================== */
function ChatTab() {
  const [messages, setMessages] = useState<
    { role: 'user' | 'assistant'; content: string; streaming?: boolean; imageUrl?: string }[]
  >([
    {
      role: 'assistant',
      content:
        '你好！👋 我是你的 **AI 宠物助手** 🐾\n\n我可以帮助你解答关于宠物的各种问题，包括：\n\n- **健康护理** 🏥 — 了解宠物常见疾病与预防方法\n- **行为分析** 🔍 — 解读宠物的肢体语言与行为含义\n- **饮食建议** 🍖 — 为你的毛孩子制定科学的饮食计划\n- **日常护理** 🛁 — 毛发、指甲、牙齿等护理指南\n\n请随时向我提问，我会尽力为你提供 **专业、详细** 的解答！😊',
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [pendingImage, setPendingImage] = useState<string | null>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);
  const textareaRef = useRef<any>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages]);

  const handleAbort = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    setLoading(false);
    setMessages((prev) =>
      prev.map((m) => (m.streaming ? { ...m, streaming: false } : m)),
    );
  }, []);

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      message.warning('请选择图片文件');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      message.warning('图片大小不能超过 10MB');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setPendingImage(reader.result as string);
    };
    reader.readAsDataURL(file);
    // 清空 input 以便可以重复选择同一文件
    e.target.value = '';
  };

  const handleSend = async () => {
    const text = input.trim();
    if (!text && !pendingImage) return;
    if (loading) return;

    const sentImage = pendingImage;
    setMessages((prev) => [...prev, { role: 'user', content: text, imageUrl: sentImage || undefined }]);
    setInput('');
    setPendingImage(null);
    setLoading(true);

    const assistantIndex = messages.length + 1;
    setMessages((prev) => [
      ...prev,
      { role: 'assistant', content: '', streaming: true },
    ]);

    abortRef.current = petAiChatStream(
      text || '请分析这张图片',
      (token) => {
        setMessages((prev) =>
          prev.map((m, i) =>
            i === assistantIndex ? { ...m, content: m.content + token } : m,
          ),
        );
      },
      () => {
        setMessages((prev) =>
          prev.map((m, i) =>
            i === assistantIndex ? { ...m, streaming: false } : m,
          ),
        );
        setLoading(false);
        abortRef.current = null;
      },
      (err) => {
        message.error(err);
        setMessages((prev) =>
          prev.map((m, i) =>
            i === assistantIndex
              ? {
                  ...m,
                  content: m.content + '\n\n⚠️ **请求失败**: ' + err,
                  streaming: false,
                }
              : m,
          ),
        );
        setLoading(false);
        abortRef.current = null;
      },
    );
  };

  return (
    <div className="pet-ai-chat-wrapper">
      {/* 消息列表 */}
      <div ref={listRef} className="pet-ai-messages">
        {messages.map((msg, i) => (
          <div
            key={i}
            className={`pet-ai-msg ${msg.role === 'user' ? 'pet-ai-msg--user' : 'pet-ai-msg--assistant'}`}
            style={{ animationDelay: `${i * 0.05}s` }}
          >
            <div className={`pet-ai-avatar ${msg.role === 'user' ? 'pet-ai-avatar--user' : 'pet-ai-avatar--assistant'}`}>
              {msg.role === 'user' ? <UserOutlined /> : <span>🐱</span>}
            </div>
            <div className={`pet-ai-bubble ${msg.role === 'user' ? 'pet-ai-bubble--user' : 'pet-ai-bubble--assistant'}`}>
              {msg.imageUrl && (
                <div className="pet-ai-bubble-image">
                  <img src={msg.imageUrl} alt="上传的图片" />
                </div>
              )}
              {msg.content && (
                <div className="pet-ai-bubble-content">
                  {msg.role === 'assistant' ? formatAIContent(msg.content) : msg.content}
                </div>
              )}
              {msg.streaming && (
                <span className="pet-ai-cursor" />
              )}
            </div>
          </div>
        ))}
        {loading && messages[messages.length - 1]?.content === '' && messages[messages.length - 1]?.streaming && (
          <div className="pet-ai-typing-indicator">
            <div className="pet-ai-typing-dot" style={{ animationDelay: '0ms' }} />
            <div className="pet-ai-typing-dot" style={{ animationDelay: '150ms' }} />
            <div className="pet-ai-typing-dot" style={{ animationDelay: '300ms' }} />
          </div>
        )}
        {/* 底部预留空间 */}
        <div style={{ height: 32, flexShrink: 0 }} />
      </div>

      {/* 输入区 — 固定底部 */}
      <div className="pet-ai-input-area">
        <div className="pet-ai-input-container">
          {/* 图片预览 */}
          {pendingImage && (
            <div className="pet-ai-image-preview">
              <img src={pendingImage} alt="待发送图片" />
              <button
                className="pet-ai-image-preview-remove"
                onClick={() => setPendingImage(null)}
                title="移除图片"
              >
                <CloseCircleOutlined />
              </button>
            </div>
          )}
          <div className="pet-ai-input-actions-left">
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleImageUpload}
              style={{ display: 'none' }}
            />
            <button
              className="pet-ai-action-btn"
              title="上传图片"
              onClick={() => fileInputRef.current?.click()}
            >
              <PictureOutlined />
            </button>
          </div>
          <div className="pet-ai-input-field-wrapper">
            <TextArea
              ref={textareaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault();
                  handleSend();
                }
              }}
              placeholder="输入你的问题... (Shift+Enter 换行)"
              autoSize={{ minRows: 1, maxRows: 4 }}
              disabled={loading}
              className="pet-ai-textarea"
            />
          </div>
          {loading ? (
            <button
              className="pet-ai-send-btn pet-ai-send-btn--stop"
              onClick={handleAbort}
              title="停止生成"
            >
              <StopOutlined />
            </button>
          ) : (
            <button
              className={`pet-ai-send-btn ${input.trim() ? 'pet-ai-send-btn--active' : ''}`}
              onClick={handleSend}
              disabled={!input.trim()}
              title="发送"
            >
              <SendOutlined />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

/* ====================================================================
 *  行为分析 Tab
 * ==================================================================== */
const statusMap: Record<string, { label: string; color: string }> = {
  NORMAL: { label: '正常', color: 'green' },
  ABNORMAL: { label: '异常', color: 'red' },
  UNCERTAIN: { label: '不确定', color: 'orange' },
};
const riskMap: Record<string, { label: string; color: string }> = {
  LOW: { label: '低风险', color: 'green' },
  MEDIUM: { label: '中风险', color: 'orange' },
  HIGH: { label: '高风险', color: 'red' },
};

function AnalyzeTab() {
  const [pets, setPets] = useState<PetVO[]>([]);
  const [petId, setPetId] = useState<number | undefined>();
  const [imageUrl, setImageUrl] = useState('');
  const [detectionJson, setDetectionJson] = useState('');
  const [userQuestion, setUserQuestion] = useState('');
  const [result, setResult] = useState<BehaviorAnalysisResult | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getPets().then(setPets).catch(() => {});
  }, []);

  const handleAnalyze = async () => {
    if (!imageUrl.trim()) {
      message.warning('请输入截图地址');
      return;
    }
    if (!detectionJson.trim()) {
      message.warning('请粘贴 AI 检测数据 JSON');
      return;
    }
    try {
      const parsedDetection = JSON.parse(detectionJson);
      if (parsedDetection === null || typeof parsedDetection !== 'object') {
        message.warning('AI 检测数据必须是 JSON 对象或数组');
        return;
      }
    } catch {
      message.warning('AI 检测数据不是合法 JSON');
      return;
    }
    setLoading(true);
    setResult(null);
    try {
      const res = await analyzePetBehavior({
        petId: petId || undefined,
        imageUrl: imageUrl.trim(),
        detectionJson: detectionJson.trim(),
        userQuestion: userQuestion.trim() || undefined,
      });
      setResult(res);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const evidence = result?.evidence ?? [];
  const suggestions = result?.suggestions ?? [];
  const possibleCauses = result?.possibleCauses ?? [];
  const confidence = Math.round(Math.max(0, Math.min(1, result?.confidence ?? 0)) * 100);

  return (
    <div className="pet-ai-tab-content">
      <div className="pet-ai-form-card">
        <div className="pet-ai-form-card-header">
          <SearchOutlined className="pet-ai-form-icon" />
          <span>宠物行为分析</span>
        </div>
        <div className="pet-ai-form-card-body">
          <div className="pet-ai-form-group">
            <label className="pet-ai-label">选择宠物（可选）</label>
            <Select
              placeholder="选择宠物以提供更精准的分析"
              value={petId}
              onChange={setPetId}
              allowClear
              style={{ width: '100%' }}
              showSearch
              optionFilterProp="label"
              options={pets.map((p) => ({
                label: `${p.petName} (${PetTypeMap[p.petType] || p.petType})`,
                value: p.id,
              }))}
            />
          </div>

          <div className="pet-ai-form-group">
            <label className="pet-ai-label">
              截图地址 <span className="pet-ai-required">*</span>
            </label>
            <Input
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="输入监控截图的 URL 地址"
              className="pet-ai-input"
            />
          </div>

          <div className="pet-ai-form-group">
            <label className="pet-ai-label">
              AI 检测数据 JSON <span className="pet-ai-required">*</span>
            </label>
            <TextArea
              value={detectionJson}
              onChange={(e) => setDetectionJson(e.target.value)}
              placeholder='例如：{"detections":[{"label":"cat","confidence":0.92,"bbox":{"x":120,"y":80,"w":180,"h":140}}],"inSafeZone":true}'
              autoSize={{ minRows: 4, maxRows: 8 }}
              className="pet-ai-input pet-ai-input--mono"
            />
          </div>

          <div className="pet-ai-form-group">
            <label className="pet-ai-label">附带问题（可选）</label>
            <TextArea
              value={userQuestion}
              onChange={(e) => setUserQuestion(e.target.value)}
              placeholder="例如：我的猫为什么一直盯着墙角？"
              autoSize={{ minRows: 2, maxRows: 4 }}
              className="pet-ai-input"
            />
          </div>

          <button
            className={`pet-ai-form-btn ${imageUrl.trim() && detectionJson.trim() ? 'pet-ai-form-btn--active' : ''}`}
            onClick={handleAnalyze}
            disabled={loading || !imageUrl.trim() || !detectionJson.trim()}
          >
            {loading ? <Spin size="small" /> : <ThunderboltOutlined />}
            <span>{loading ? '分析中...' : '开始分析'}</span>
          </button>
        </div>
      </div>

      {/* 分析结果 */}
      {(loading || result) && (
        <div className="pet-ai-form-card">
          <div className="pet-ai-form-card-header">
            <RobotOutlined className="pet-ai-form-icon" />
            <span>分析结果</span>
          </div>
          <div className="pet-ai-form-card-body">
            {loading ? (
              <div className="pet-ai-loading-box">
                <Spin />
                <span>AI 正在分析中...</span>
              </div>
            ) : result ? (
              <div className="space-y-4">
                <div className="flex items-center gap-3 flex-wrap">
                  <Tag color={statusMap[result.status]?.color}>
                    {statusMap[result.status]?.label || result.status}
                  </Tag>
                  <Tag color={riskMap[result.riskLevel]?.color}>
                    {riskMap[result.riskLevel]?.label || result.riskLevel}
                  </Tag>
                  {result.needVet && <Tag color="red">🏥 建议就医</Tag>}
                  <span className="text-sm text-slate-400">
                    置信度: {confidence}%
                  </span>
                  <Progress
                    percent={confidence}
                    size="small"
                    style={{ width: 120 }}
                    showInfo={false}
                  />
                </div>

                <div className="pet-ai-result-summary">
                  <div className="pet-ai-result-summary-title">📋 分析摘要</div>
                  <div className="pet-ai-result-summary-text">{result.summary}</div>
                </div>

                <Collapse
                  size="small"
                  items={[
                    evidence.length > 0 && {
                      key: 'evidence',
                      label: `🔍 判断依据 (${evidence.length}条)`,
                      children: (
                        <ul className="list-disc pl-4 space-y-1">
                          {evidence.map((e, i) => (
                            <li key={i} className="text-sm text-slate-600">{e}</li>
                          ))}
                        </ul>
                      ),
                    },
                    suggestions.length > 0 && {
                      key: 'suggestions',
                      label: `💡 建议 (${suggestions.length}条)`,
                      children: (
                        <ul className="list-disc pl-4 space-y-1">
                          {suggestions.map((s, i) => (
                            <li key={i} className="text-sm text-slate-600">{s}</li>
                          ))}
                        </ul>
                      ),
                    },
                    possibleCauses.length > 0 && {
                      key: 'causes',
                      label: `🔬 可能原因 (${possibleCauses.length}条)`,
                      children: (
                        <ul className="list-disc pl-4 space-y-1">
                          {possibleCauses.map((c, i) => (
                            <li key={i} className="text-sm text-slate-600">{c}</li>
                          ))}
                        </ul>
                      ),
                    },
                  ].filter(Boolean) as any}
                />
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}

/* ====================================================================
 *  健康建议 Tab
 * ==================================================================== */
function HealthAdviceTab() {
  const [pets, setPets] = useState<PetVO[]>([]);
  const [petName, setPetName] = useState('');
  const [recentRecords, setRecentRecords] = useState('');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getPets().then(setPets).catch(() => {});
  }, []);

  const handleGetAdvice = async () => {
    if (!petName.trim()) {
      message.warning('请输入或选择宠物名称');
      return;
    }
    setLoading(true);
    setResult('');
    try {
      const res = await getHealthAdvice(petName.trim(), recentRecords.trim() || undefined);
      setResult(res);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="pet-ai-tab-content">
      <div className="pet-ai-form-card">
        <div className="pet-ai-form-card-header">
          <HeartOutlined className="pet-ai-form-icon" />
          <span>宠物健康建议</span>
        </div>
        <div className="pet-ai-form-card-body">
          <div className="pet-ai-form-group">
            <label className="pet-ai-label">
              宠物名称 <span className="pet-ai-required">*</span>
            </label>
            <Select
              placeholder="选择宠物"
              value={petName || undefined}
              onChange={(v) => setPetName(v)}
              allowClear
              showSearch
              style={{ width: '100%' }}
              optionFilterProp="label"
              options={pets.map((p) => ({ label: p.petName, value: p.petName }))}
              onClear={() => setPetName('')}
            />
            <Input
              className="mt-2 pet-ai-input"
              value={petName}
              onChange={(e) => setPetName(e.target.value)}
              placeholder="或手动输入宠物名称"
            />
          </div>

          <div className="pet-ai-form-group">
            <label className="pet-ai-label">近期活动记录（可选）</label>
            <TextArea
              value={recentRecords}
              onChange={(e) => setRecentRecords(e.target.value)}
              placeholder="例如：最近食欲下降，活动量减少，偶尔呕吐..."
              autoSize={{ minRows: 3, maxRows: 6 }}
              className="pet-ai-input"
            />
          </div>

          <button
            className={`pet-ai-form-btn ${petName.trim() ? 'pet-ai-form-btn--active' : ''}`}
            onClick={handleGetAdvice}
            disabled={loading || !petName.trim()}
          >
            {loading ? <Spin size="small" /> : <HeartOutlined />}
            <span>{loading ? '分析中...' : '获取建议'}</span>
          </button>
        </div>
      </div>

      {/* 健康建议结果 */}
      {(loading || result) && (
        <div className="pet-ai-form-card">
          <div className="pet-ai-form-card-header">
            <RobotOutlined className="pet-ai-form-icon" />
            <span>健康建议</span>
          </div>
          <div className="pet-ai-form-card-body">
            {loading ? (
              <div className="pet-ai-loading-box">
                <Spin />
                <span>AI 正在分析中...</span>
              </div>
            ) : (
              <div className="pet-ai-result-text">
                {formatAIContent(result)}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

/* ====================================================================
 *  主页面
 * ==================================================================== */
export default function PetAiPage() {
  const [activeTab, setActiveTab] = useState('chat');

  const tabs = [
    { key: 'chat', label: 'AI 聊天', icon: '🤖' },
    { key: 'analyze', label: '行为分析', icon: '🔍' },
    { key: 'health', label: '健康建议', icon: '💚' },
  ];

  return (
    <div className="pet-ai-page">
      {/* 顶部导航栏 */}
      <div className="pet-ai-header">
        <div className="pet-ai-header-left">
          <span className="pet-ai-header-icon">🐾</span>
          <h1 className="pet-ai-header-title">AI 宠物助手</h1>
        </div>
        <div className="pet-ai-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={`pet-ai-tab ${activeTab === tab.key ? 'pet-ai-tab--active' : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <span className="pet-ai-tab-icon">{tab.icon}</span>
              <span className="pet-ai-tab-label">{tab.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 内容区 — 使用 display 切换保留组件状态 */}
      <div className="pet-ai-body">
        <div style={{ display: activeTab === 'chat' ? 'flex' : 'none', flexDirection: 'column', height: '100%' }}>
          <ChatTab />
        </div>
        <div style={{ display: activeTab === 'analyze' ? 'flex' : 'none', flexDirection: 'column', height: '100%' }}>
          <AnalyzeTab />
        </div>
        <div style={{ display: activeTab === 'health' ? 'flex' : 'none', flexDirection: 'column', height: '100%' }}>
          <HealthAdviceTab />
        </div>
      </div>
    </div>
  );
}
