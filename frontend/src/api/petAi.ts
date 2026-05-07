import request from './request';

/** 宠物行为分析请求 */
export interface PetAnalyzeRequest {
  petId?: number;
  petName?: string;
  petType?: string;
  imageUrl: string;
  detectionJson?: string;
  userQuestion?: string;
}

/** LLM 请求超时时间 (60s，与后端一致) */
const LLM_TIMEOUT = 60000;

/** 分析宠物行为 */
export function analyzePetBehavior(data: PetAnalyzeRequest) {
  return request.post<any, string>('/api/pet-ai/analyze', data, {
    timeout: LLM_TIMEOUT,
  });
}

/** 获取宠物健康建议 */
export function getHealthAdvice(petName: string, recentRecords?: string) {
  return request.get<any, string>('/api/pet-ai/health-advice', {
    params: { petName, recentRecords },
    timeout: LLM_TIMEOUT,
  });
}

/** 宠物AI聊天 */
export function petAiChat(message: string) {
  return request.post<any, string>('/api/pet-ai/chat', null, {
    params: { message },
    timeout: LLM_TIMEOUT,
  });
}
