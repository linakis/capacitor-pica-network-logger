import { CapacitorHttp, PicaNetworkLogger } from 'capacitor-pica-network-logger';
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
    await PicaNetworkLogger.requestNotificationPermission();
    let response;

    switch (kind) {
      case 'get-resource':
        response = await CapacitorHttp.get({
          url: 'https://jsonplaceholder.typicode.com/posts/1'
        });
        break;
      case 'list-resources':
        response = await CapacitorHttp.get({
          url: 'https://jsonplaceholder.typicode.com/posts'
        });
        break;
      case 'create-resource':
        response = await CapacitorHttp.post({
          url: 'https://jsonplaceholder.typicode.com/posts',
          headers: {
            'Content-type': 'application/json; charset=UTF-8'
          },
          data: {
            title: 'foo',
            body: 'bar',
            userId: 1
          }
        });
        break;
      case 'update-resource':
        response = await CapacitorHttp.put({
          url: 'https://jsonplaceholder.typicode.com/posts/1',
          headers: {
            'Content-type': 'application/json; charset=UTF-8'
          },
          data: {
            id: 1,
            title: 'foo',
            body: 'bar',
            userId: 1
          }
        });
        break;
      case 'patch-resource':
        response = await CapacitorHttp.patch({
          url: 'https://jsonplaceholder.typicode.com/posts/1',
          headers: {
            'Content-type': 'application/json; charset=UTF-8'
          },
          data: {
            title: 'foo'
          }
        });
        break;
      case 'delete-resource':
        response = await CapacitorHttp.delete({
          url: 'https://jsonplaceholder.typicode.com/posts/1'
        });
        break;
      case 'filter-resources':
        response = await CapacitorHttp.get({
          url: 'https://jsonplaceholder.typicode.com/posts',
          params: {
            userId: '1'
          }
        });
        break;
      case 'list-nested':
        response = await CapacitorHttp.get({
          url: 'https://jsonplaceholder.typicode.com/posts/1/comments'
        });
        break;
      default:
        throw new Error(`Unknown request kind: ${kind}`);
    }

    const output = response?.data ?? response;
    updateOutput(JSON.stringify(output, null, 2));
  } catch (err) {
    updateOutput(String(err));
  }
};

const openInspector = async () => {
  await PicaNetworkLogger.openInspector();
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
document.getElementById('inspector')?.addEventListener('click', openInspector);
