import { useState, useRef, useEffect } from 'react';
import {
  Card,
  Button,
  Input,
  Select,
  Tabs,
  message,
  Spin,
  Avatar,
  Empty,
} from 'antd';
import {
  SendOutlined,
  RobotOutlined,
  UserOutlined,
  SearchOutlined,
  HeartOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { analyzePetBehavior, getHealthAdvice, petAiChat } from '@/api/petAi';
import { getPets } from '@/api/pet';
import type { PetVO } from '@/types';
import { PetTypeMap } from '@/utils/constants';

const { TextArea } = Input;

/* ================================================================== */
/*  消息类型                                                            */
/* ================================================================== */
interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

/* ================================================================== */
/*  AI 聊天 Tab                                                        */
/* ================================================================== */
function ChatTab() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return;

    setMessages((prev) => [...prev, { role: 'user', content: text }]);
    setInput('');
    setLoading(true);

    try {
      const reply = await petAiChat(text);
      setMessages((prev) => [...prev, { role: 'assistant', content: reply }]);
    } catch (err: any) {
      message.error(err.message);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: '抱歉，AI 服务暂时不可用，请稍后再试。' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col" style={{ height: 520 }}>
      {/* 消息列表 */}
      <div
        ref={listRef}
        className="flex-1 overflow-y-auto p-4 space-y-4"
        style={{
          background: 'rgba(255,255,255,0.40)',
          borderRadius: 12,
          border: '1px solid rgba(167,139,250,0.08)',
        }}
      >
        {messages.length === 0 && (
          <Empty
            description={
              <span style={{ color: '#94a3b8' }}>
                向 AI 助手提问关于宠物的任何问题
              </span>
            }
            style={{ marginTop: 120 }}
          />
        )}

        {messages.map((msg, i) => (
          <div
            key={i}
            className={`flex gap-3 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}
          >
            <Avatar
              icon={msg.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
              style={{
                backgroundColor:
                  msg.role === 'user' ? '#a78bfa' : 'rgba(167,139,250,0.2)',
                color: msg.role === 'user' ? '#fff' : '#7c3aed',
                flexShrink: 0,
              }}
            />
            <div
              className="px-4 py-2.5 text-sm"
              style={{
                maxWidth: '70%',
                borderRadius: 12,
                background:
                  msg.role === 'user'
                    ? 'linear-gradient(135deg, #a78bfa, #c4b5fd)'
                    : 'rgba(255,255,255,0.80)',
                color: msg.role === 'user' ? '#fff' : '#1e293b',
                border:
                  msg.role === 'user'
                    ? 'none'
                    : '1px solid rgba(167,139,250,0.10)',
                whiteSpace: 'pre-wrap',
                lineHeight: 1.6,
              }}
            >
              {msg.content}
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex gap-3">
            <Avatar
              icon={<RobotOutlined />}
              style={{
                backgroundColor: 'rgba(167,139,250,0.2)',
                color: '#7c3aed',
                flexShrink: 0,
              }}
            />
            <div
              className="px-4 py-3 flex items-center gap-2"
              style={{
                borderRadius: 12,
                background: 'rgba(255,255,255,0.80)',
                border: '1px solid rgba(167,139,250,0.10)',
              }}
            >
              <Spin size="small" />
              <span style={{ color: '#94a3b8', fontSize: 13 }}>思考中...</span>
            </div>
          </div>
        )}
      </div>

      {/* 输入区 */}
      <div className="flex gap-3 mt-3">
        <TextArea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onPressEnter={(e) => {
            if (!e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
          placeholder="输入你的问题... (Shift+Enter 换行)"
          autoSize={{ minRows: 1, maxRows: 3 }}
          disabled={loading}
          style={{
            borderRadius: 12,
            background: 'rgba(255,255,255,0.80)',
            borderColor: 'rgba(167,139,250,0.20)',
          }}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          loading={loading}
          disabled={!input.trim()}
          style={{
            height: 'auto',
            borderRadius: 12,
            background: input.trim()
              ? 'linear-gradient(135deg, #a78bfa, #f0abfc)'
              : undefined,
          }}
        >
          发送
        </Button>
      </div>
    </div>
  );
}

/* ================================================================== */
/*  行为分析 Tab                                                        */
/* ================================================================== */
function AnalyzeTab() {
  const [pets, setPets] = useState<PetVO[]>([]);
  const [petId, setPetId] = useState<number | undefined>();
  const [imageUrl, setImageUrl] = useState('');
  const [userQuestion, setUserQuestion] = useState('');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getPets().then(setPets).catch(() => {});
  }, []);

  const handleAnalyze = async () => {
    if (!imageUrl.trim()) {
      message.warning('请输入截图地址');
      return;
    }
    setLoading(true);
    setResult('');
    try {
      const res = await analyzePetBehavior({
        petId: petId || undefined,
        imageUrl: imageUrl.trim(),
        userQuestion: userQuestion.trim() || undefined,
      });
      setResult(res);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <Card
        title={
          <span>
            <SearchOutlined className="mr-2" style={{ color: '#8b5cf6' }} />
            宠物行为分析
          </span>
        }
      >
        <div className="space-y-4">
          <div>
            <label
              className="block mb-1.5 text-sm"
              style={{ color: '#475569', fontWeight: 500 }}
            >
              选择宠物（可选）
            </label>
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

          <div>
            <label
              className="block mb-1.5 text-sm"
              style={{ color: '#475569', fontWeight: 500 }}
            >
              截图地址 <span style={{ color: '#e11d48' }}>*</span>
            </label>
            <Input
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="输入监控截图的 URL 地址"
              style={{
                borderRadius: 12,
                background: 'rgba(255,255,255,0.80)',
                borderColor: 'rgba(167,139,250,0.20)',
              }}
            />
          </div>

          <div>
            <label
              className="block mb-1.5 text-sm"
              style={{ color: '#475569', fontWeight: 500 }}
            >
              附带问题（可选）
            </label>
            <TextArea
              value={userQuestion}
              onChange={(e) => setUserQuestion(e.target.value)}
              placeholder="例如：我的猫为什么一直盯着墙角？"
              autoSize={{ minRows: 2, maxRows: 4 }}
              style={{
                borderRadius: 12,
                background: 'rgba(255,255,255,0.80)',
                borderColor: 'rgba(167,139,250,0.20)',
              }}
            />
          </div>

          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            onClick={handleAnalyze}
            loading={loading}
            disabled={!imageUrl.trim()}
            style={{
              borderRadius: 12,
              background: imageUrl.trim()
                ? 'linear-gradient(135deg, #a78bfa, #f0abfc)'
                : undefined,
            }}
          >
            开始分析
          </Button>
        </div>
      </Card>

      {(loading || result) && (
        <Card
          title={
            <span>
              <RobotOutlined className="mr-2" style={{ color: '#8b5cf6' }} />
              分析结果
            </span>
          }
        >
          {loading ? (
            <div className="flex items-center justify-center py-8 gap-3">
              <Spin />
              <span style={{ color: '#94a3b8' }}>AI 正在分析中...</span>
            </div>
          ) : (
            <div
              className="text-sm"
              style={{ color: '#334155', lineHeight: 1.8, whiteSpace: 'pre-wrap' }}
            >
              {result}
            </div>
          )}
        </Card>
      )}
    </div>
  );
}

/* ================================================================== */
/*  健康建议 Tab                                                        */
/* ================================================================== */
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
      const res = await getHealthAdvice(
        petName.trim(),
        recentRecords.trim() || undefined,
      );
      setResult(res);
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <Card
        title={
          <span>
            <HeartOutlined className="mr-2" style={{ color: '#8b5cf6' }} />
            宠物健康建议
          </span>
        }
      >
        <div className="space-y-4">
          <div>
            <label
              className="block mb-1.5 text-sm"
              style={{ color: '#475569', fontWeight: 500 }}
            >
              宠物名称 <span style={{ color: '#e11d48' }}>*</span>
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
              className="mt-2"
              value={petName}
              onChange={(e) => setPetName(e.target.value)}
              placeholder="或手动输入宠物名称"
              style={{
                borderRadius: 12,
                background: 'rgba(255,255,255,0.80)',
                borderColor: 'rgba(167,139,250,0.20)',
              }}
            />
          </div>

          <div>
            <label
              className="block mb-1.5 text-sm"
              style={{ color: '#475569', fontWeight: 500 }}
            >
              近期活动记录（可选）
            </label>
            <TextArea
              value={recentRecords}
              onChange={(e) => setRecentRecords(e.target.value)}
              placeholder="例如：最近食欲下降，活动量减少，偶尔呕吐..."
              autoSize={{ minRows: 3, maxRows: 6 }}
              style={{
                borderRadius: 12,
                background: 'rgba(255,255,255,0.80)',
                borderColor: 'rgba(167,139,250,0.20)',
              }}
            />
          </div>

          <Button
            type="primary"
            icon={<HeartOutlined />}
            onClick={handleGetAdvice}
            loading={loading}
            disabled={!petName.trim()}
            style={{
              borderRadius: 12,
              background: petName.trim()
                ? 'linear-gradient(135deg, #a78bfa, #f0abfc)'
                : undefined,
            }}
          >
            获取建议
          </Button>
        </div>
      </Card>

      {(loading || result) && (
        <Card
          title={
            <span>
              <RobotOutlined className="mr-2" style={{ color: '#8b5cf6' }} />
              健康建议
            </span>
          }
        >
          {loading ? (
            <div className="flex items-center justify-center py-8 gap-3">
              <Spin />
              <span style={{ color: '#94a3b8' }}>AI 正在分析中...</span>
            </div>
          ) : (
            <div
              className="text-sm"
              style={{ color: '#334155', lineHeight: 1.8, whiteSpace: 'pre-wrap' }}
            >
              {result}
            </div>
          )}
        </Card>
      )}
    </div>
  );
}

/* ================================================================== */
/*  主页面                                                              */
/* ================================================================== */
export default function PetAiPage() {
  const tabItems = [
    {
      key: 'chat',
      label: (
        <span className="flex items-center gap-1.5">
          <RobotOutlined />
          AI 聊天
        </span>
      ),
      children: <ChatTab />,
    },
    {
      key: 'analyze',
      label: (
        <span className="flex items-center gap-1.5">
          <SearchOutlined />
          行为分析
        </span>
      ),
      children: <AnalyzeTab />,
    },
    {
      key: 'health',
      label: (
        <span className="flex items-center gap-1.5">
          <HeartOutlined />
          健康建议
        </span>
      ),
      children: <HealthAdviceTab />,
    },
  ];

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold m-0">AI 宠物助手</h2>
      </div>

      <Tabs defaultActiveKey="chat" items={tabItems} />
    </div>
  );
}
