import { CapacitorHttp, type HttpOptions, type HttpResponse } from '@capacitor/core';
import { PicaNetworkLogger } from './index';
import type { LoggerConfig, RequestMeta, ResponseMeta } from './types';

const REQUEST_ID_HEADER = 'X-Cap-ReqId';

const defaultConfig: LoggerConfig = {
  enabled: true,
  maxBodySize: 131072,
  redactHeaders: ['authorization', 'cookie'],
  redactJsonFields: ['password', 'token']
};

const toLower = (value: string) => value.toLowerCase();

const redactHeaders = (headers: Record<string, string>, redactList: string[] = []) => {
  const redactSet = new Set(redactList.map(toLower));
  const output: Record<string, string> = {};
  Object.keys(headers).forEach((key) => {
    const lower = toLower(key);
    output[key] = redactSet.has(lower) ? '[REDACTED]' : headers[key];
  });
  return output;
};

const truncateBody = (body: string | null | undefined, maxBodySize: number) => {
  if (body == null) {
    return { value: null, truncated: false, size: 0 };
  }
  const size = body.length;
  if (size <= maxBodySize) {
    return { value: body, truncated: false, size };
  }
  return { value: body.slice(0, maxBodySize), truncated: true, size };
};

const safeStringify = (value: unknown) => {
  if (value == null) return null;
  if (typeof value === 'string') return value;
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
};

const normalizeArray = (value: unknown): string[] | undefined => {
  if (Array.isArray(value)) return value.map(String);
  return undefined;
};

const getLoggerConfig = async (): Promise<LoggerConfig> => {
  try {
    const config = await PicaNetworkLogger.getConfig();
    return {
      ...defaultConfig,
      ...config,
      redactHeaders: normalizeArray((config as any).redactHeaders) ?? defaultConfig.redactHeaders,
      redactJsonFields: normalizeArray((config as any).redactJsonFields) ?? defaultConfig.redactJsonFields
    };
  } catch {
    return defaultConfig;
  }
};

const generateId = () => {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const buildRequestMeta = (
  id: string,
  options: HttpOptions,
  config: LoggerConfig
): RequestMeta => {
  const headers = options.headers ? { ...options.headers } : {};
  headers[REQUEST_ID_HEADER] = id;

  const requestBody = safeStringify(options.data);
  const truncated = truncateBody(requestBody, config.maxBodySize ?? defaultConfig.maxBodySize!);

  return {
    id,
    method: (options.method ?? 'GET').toUpperCase(),
    url: options.url,
    headers: redactHeaders(headers, config.redactHeaders),
    params: options.params,
    dataType: options.dataType,
    requestBody: truncated.value,
    requestBodyTruncated: truncated.truncated,
    requestBodySize: truncated.size,
    startTs: Date.now()
  };
};

const buildResponseMeta = (
  id: string,
  response: HttpResponse | null,
  startTs: number,
  config: LoggerConfig,
  error?: string
): ResponseMeta => {
  const headers = response?.headers ? { ...(response.headers as Record<string, string>) } : {};
  const responseBody = safeStringify(response?.data);
  const truncated = truncateBody(responseBody, config.maxBodySize ?? defaultConfig.maxBodySize!);

  return {
    id,
    status: response?.status,
    headers: redactHeaders(headers, config.redactHeaders),
    responseBody: truncated.value,
    responseBodyTruncated: truncated.truncated,
    responseBodySize: truncated.size,
    durationMs: Date.now() - startTs,
    error
  };
};

const requestWithLogging = async (options: HttpOptions): Promise<HttpResponse> => {
  const config = await getLoggerConfig();
  if (!config.enabled) {
    return CapacitorHttp.request(options);
  }

  const id = generateId();
  const meta = buildRequestMeta(id, options, config);

  const headers = options.headers ? { ...options.headers } : {};
  headers[REQUEST_ID_HEADER] = id;

  try {
    await PicaNetworkLogger.startRequest(meta);
  } catch {
    // Ignore if native plugin is not available
  }

  const startTs = meta.startTs;

  try {
    const response = await CapacitorHttp.request({ ...options, headers });
    const responseMeta = buildResponseMeta(id, response, startTs, config);
    try {
      await PicaNetworkLogger.finishRequest(responseMeta);
    } catch {
      // Ignore if native plugin is not available
    }
    return response;
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Unknown error';
    const responseMeta = buildResponseMeta(id, null, startTs, config, message);
    try {
      await PicaNetworkLogger.finishRequest(responseMeta);
    } catch {
      // Ignore if native plugin is not available
    }
    throw err;
  }
};

export const HttpWithLogging = {
  request: requestWithLogging,
  get: (options: HttpOptions) => requestWithLogging({ ...options, method: 'GET' }),
  post: (options: HttpOptions) => requestWithLogging({ ...options, method: 'POST' }),
  put: (options: HttpOptions) => requestWithLogging({ ...options, method: 'PUT' }),
  patch: (options: HttpOptions) => requestWithLogging({ ...options, method: 'PATCH' }),
  delete: (options: HttpOptions) => requestWithLogging({ ...options, method: 'DELETE' })
};

export { HttpWithLogging as CapacitorHttp };
