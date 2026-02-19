import { PicaNetworkLogger } from 'capacitor-pica-network-logger';
import './main.css';

const app = document.querySelector<HTMLDivElement>('#app');

const render = (content: string) => {
  if (app) app.innerHTML = content;
};

type RequestKind =
  | 'get-resource'
  | 'list-resources'
  | 'create-resource'
  | 'update-resource'
  | 'patch-resource'
  | 'delete-resource'
  | 'filter-resources'
  | 'list-nested';

const runRequest = async (kind: RequestKind) => {
  try {
    const urlBase = 'https://jsonplaceholder.typicode.com';
    const method = (() => {
      switch (kind) {
        case 'get-resource':
        case 'list-resources':
        case 'filter-resources':
        case 'list-nested':
          return 'GET';
        case 'create-resource':
          return 'POST';
        case 'update-resource':
          return 'PUT';
        case 'patch-resource':
          return 'PATCH';
        case 'delete-resource':
          return 'DELETE';
        default:
          return 'GET';
      }
    })();

    const request = (() => {
      switch (kind) {
        case 'get-resource':
          return { url: `${urlBase}/posts/1` };
        case 'list-resources':
          return { url: `${urlBase}/posts` };
        case 'create-resource':
          return {
            url: `${urlBase}/posts`,
            headers: {
              'Content-type': 'application/json; charset=UTF-8'
            },
            body: {
              title: 'foo',
              body: 'bar',
              userId: 1
            }
          };
        case 'update-resource':
          return {
            url: `${urlBase}/posts/1`,
            headers: {
              'Content-type': 'application/json; charset=UTF-8'
            },
            body: {
              id: 1,
              title: 'foo',
              body: 'bar',
              userId: 1
            }
          };
        case 'patch-resource':
          return {
            url: `${urlBase}/posts/1`,
            headers: {
              'Content-type': 'application/json; charset=UTF-8'
            },
            body: {
              title: 'foo'
            }
          };
        case 'delete-resource':
          return { url: `${urlBase}/posts/1` };
        case 'filter-resources':
          return { url: `${urlBase}/posts?userId=1` };
        case 'list-nested':
          return { url: `${urlBase}/posts/1/comments` };
        default:
          throw new Error(`Unknown request kind: ${kind}`);
      }
    })();

    const serializeBody = (value: unknown) => {
      if (value == null) return null;
      if (typeof value === 'string') return value;
      try {
        return JSON.stringify(value);
      } catch {
        return String(value);
      }
    };

    const start = await PicaNetworkLogger.startRequest({
      method,
      url: request.url,
      headers: request.headers,
      body: serializeBody(request.body)
    });

    const response = await fetch(request.url, {
      method,
      headers: request.headers,
      body: request.body ? JSON.stringify(request.body) : undefined
    });

    const responseBody = await response.json().catch(() => null);
    const responseHeaders: Record<string, string> = {};
    response.headers.forEach((value, key) => {
      responseHeaders[key] = value;
    });
    await PicaNetworkLogger.finishRequest({
      id: start.id,
      status: response.status,
      body: serializeBody(responseBody),
      headers: responseHeaders
    });

    updateOutput(JSON.stringify(responseBody ?? response, null, 2));
  } catch (err) {
    updateOutput(String(err));
  }
};

const updateOutput = (text: string) => {
  const output = document.getElementById('output');
  if (output) output.textContent = text;
};

render(`
  <div class="header">
    <img src="/capacitor-pica-logo.png" alt="Capacitor Pica" class="logo" />
    <div>
      <div class="title">Capacitor Pica Network Logger</div>
      <div class="subtitle">Network inspector demo</div>
    </div>
  </div>
  <div class="toolbar">
    <button id="get-resource">Get Resource</button>
    <button id="list-resources">List Resources</button>
    <button id="create-resource">Create Resource</button>
    <button id="update-resource">Update Resource</button>
    <button id="patch-resource">Patch Resource</button>
    <button id="delete-resource">Delete Resource</button>
    <button id="filter-resources">Filter Resources</button>
    <button id="list-nested">List Nested</button>
    <button id="inspector">Open Inspector</button>
  </div>
  <pre id="output"></pre>
`);

document.getElementById('get-resource')?.addEventListener('click', () => runRequest('get-resource'));
document.getElementById('list-resources')?.addEventListener('click', () => runRequest('list-resources'));
document.getElementById('create-resource')?.addEventListener('click', () => runRequest('create-resource'));
document.getElementById('update-resource')?.addEventListener('click', () => runRequest('update-resource'));
document.getElementById('patch-resource')?.addEventListener('click', () => runRequest('patch-resource'));
document.getElementById('delete-resource')?.addEventListener('click', () => runRequest('delete-resource'));
document.getElementById('filter-resources')?.addEventListener('click', () => runRequest('filter-resources'));
document.getElementById('list-nested')?.addEventListener('click', () => runRequest('list-nested'));
const openInspectorAction = async () => {
  await PicaNetworkLogger.openInspector();
};

document.getElementById('inspector')?.addEventListener('click', openInspectorAction);
