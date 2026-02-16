export type RedactionConfig = {
  redactHeaders?: string[];
  redactJsonFields?: string[];
};

export type LoggerConfig = {
  enabled?: boolean;
  maxBodySize?: number;
} & RedactionConfig;

export type RequestMeta = {
  id: string;
  method: string;
  url: string;
  headers?: Record<string, string>;
  params?: Record<string, unknown>;
  dataType?: string;
  requestBody?: string | null;
  requestBodyTruncated?: boolean;
  requestBodySize?: number;
  startTs: number;
};

export type ResponseMeta = {
  id: string;
  status?: number;
  headers?: Record<string, string>;
  responseBody?: string | null;
  responseBodyTruncated?: boolean;
  responseBodySize?: number;
  durationMs?: number;
  error?: string;
};

export type LogRecord = RequestMeta & ResponseMeta;
