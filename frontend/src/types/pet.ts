export interface PetVO {
  id: number;
  petName: string;
  petType: string;
  age: number;
  gender: string;
  remark: string;
  avatarUrl: string;
  createdAt: string;
}

export interface PetCreateRequest {
  petName: string;
  petType: string;
  age?: number;
  gender?: string;
  remark?: string;
  avatarUrl?: string;
}
