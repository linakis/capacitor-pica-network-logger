export type StartRequestOptions = {
  method: string;
  url: string;
  headers?: Record<string, string>;
  body?: unknown;
};

export type FinishRequestOptions = {
  id: string;
  status?: number;
  headers?: Record<string, string>;
  body?: unknown;
  error?: string;
};
