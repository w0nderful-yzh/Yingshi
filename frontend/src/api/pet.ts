import request from './request';
import type { PetVO, PetCreateRequest } from '@/types';

export function getPets() {
  return request.get<any, PetVO[]>('/api/pets');
}

export function getPetById(id: number) {
  return request.get<any, PetVO>(`/api/pets/${id}`);
}

export function createPet(data: PetCreateRequest) {
  return request.post<any, PetVO>('/api/pets', data);
}

export function updatePet(id: number, data: PetCreateRequest) {
  return request.put<any, PetVO>(`/api/pets/${id}`, data);
}

export function deletePet(id: number) {
  return request.delete<any, null>(`/api/pets/${id}`);
}
