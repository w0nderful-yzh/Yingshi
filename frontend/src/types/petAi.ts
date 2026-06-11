export type PetAiSourceType = 'ALARM' | 'DETECTION' | 'IMAGE';
export type PetAiRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface PetAiReportGenerateRequest {
  sourceType: PetAiSourceType;
  sourceId?: number;
  petId?: number;
  imageUrl?: string;
  question?: string;
  regenerate?: boolean;
}

export interface PetAiReportVO {
  id: number;
  petId: number;
  petName: string;
  sourceType: PetAiSourceType;
  sourceId?: number;
  sourceTime: string;
  imageUrl: string;
  reportType: string;
  riskLevel: PetAiRiskLevel;
  title: string;
  summary: string;
  observedBehavior: string;
  evidenceBasis: string;
  recommendations: string[];
  uncertainties: string[];
  evidence: Record<string, unknown>;
  modelName: string;
  promptVersion: string;
  createdAt: string;
  updatedAt: string;
}

export interface PetAiReportQuery {
  petId?: number;
  sourceType?: PetAiSourceType;
  riskLevel?: PetAiRiskLevel;
}
