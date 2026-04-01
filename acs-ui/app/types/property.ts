export interface Property {
  id: number;
  application: string;
  profile: string;
  label: string;
  propKey: string;
  propValue: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyCreateRequest {
  application: string;
  profile: string;
  label: string;
  propKey: string;
  propValue: string;
}

export interface Filters {
  application: string;
  profile: string;
  label: string;
}
