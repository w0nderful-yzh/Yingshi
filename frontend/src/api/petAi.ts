import request from './request';
import { getToken } from '@/utils/token';

/** 宠物行为分析请求 */
export interface PetAnalyzeRequest {
  petId?: number;
  petName?: string;
  petType?: string;
  imageUrl: string;
  detectionJson?: string;
  userQuestion?: string;
}

/** 行为分析结构化结果 */
export interface BehaviorAnalysisResult {
  status: 'NORMAL' | 'ABNORMAL' | 'UNCERTAIN';
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  summary: string;
  evidence: string[];
  possibleCauses: string[];
  suggestions: string[];
  needVet: boolean;
  confidence: number;
  snapshotUrl: string;
}

/** LLM 请求超时（同步接口） */
const LLM_TIMEOUT = 60000;

/** 分析宠物行为 —— 返回结构化结果 */
export function analyzePetBehavior(data: PetAnalyzeRequest): Promise<BehaviorAnalysisResult> {
  return request.post<any, BehaviorAnalysisResult>('/api/pet-ai/analyze', data, {
    timeout: LLM_TIMEOUT,
  });
}

/** 获取宠物健康建议 */
export function getHealthAdvice(petName: string, recentRecords?: string): Promise<string> {
  return request.get<any, string>('/api/pet-ai/health-advice', {
    params: { petName, recentRecords },
    timeout: LLM_TIMEOUT,
  });
}

/** 宠物AI聊天（同步） */
export function petAiChat(message: string): Promise<string> {
  return request.post<any, string>('/api/pet-ai/chat', null, {
    params: { message },
    timeout: LLM_TIMEOUT,
  });
}

/**
 * 宠物AI聊天（流式 SSE）
 * 返回 AbortController，调用方可通过 controller.abort() 取消
 * onToken: 每收到一个 token 时回调
 * onDone: 流结束时回调
 * onError: 出错时回调
 */
export function petAiChatStream(
  message: string,
  onToken: (token: string) => void,
  onDone: () => void,
  onError: (err: string) => void,
): AbortController {
  const controller = new AbortController();
  const token = getToken();

  fetch(`/api/pet-ai/chat/stream?message=${encodeURIComponent(message)}`, {
    method: 'GET',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        const text = await response.text();
        onError(text || `HTTP ${response.status}`);
        return;
      }
      const reader = response.body?.getReader();
      if (!reader) {
        onError('浏览器不支持流式读取');
        return;
      }
      const decoder = new TextDecoder();
      let buffer = '';
      const consumeLine = (line: string) => {
        const normalized = line.endsWith('\r') ? line.slice(0, -1) : line;
        if (!normalized.startsWith('data:')) return;
        let payload = normalized.slice(5).replace(/^ /, '');
        // 换行 token 经 SSE 编码后恰好与分行符重合，payload 为空时还原为 \n
        if (payload === '') payload = '\n';
        onToken(payload);
      };
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        // SSE 格式通常是 "data: token\n\n"，Spring 也可能输出 "data:token\n\n"。
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        for (const line of lines) {
          consumeLine(line);
        }
      }
      if (buffer) {
        consumeLine(buffer);
      }
      onDone();
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError(err.message || '流式请求失败');
      }
    });

  return controller;
}
