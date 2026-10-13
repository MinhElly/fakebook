"""Delete one application Pod at a time and verify replacement, digest and readiness."""
import json
import subprocess
import time
import urllib.request
from pathlib import Path

NAMESPACE = 'fakebook-local'
SERVICES = ('frontend', 'userservice', 'postservice', 'mediaservice',
            'commentservice', 'feedservice', 'gateway')


def kubectl(*args):
    return subprocess.check_output(['kubectl', '--context', 'docker-desktop',
                                    '-n', NAMESPACE, *args], text=True)


def main():
    results = []
    output = Path('.artifacts/pod-recovery.json')
    output.parent.mkdir(exist_ok=True)
    for service in SERVICES:
        deployment = json.loads(kubectl('get', 'deployment', service, '-o', 'json'))
        if deployment['status'].get('readyReplicas', 0) != deployment['spec']['replicas']:
            raise RuntimeError(f'{service} is not ready before recovery')
        image = deployment['spec']['template']['spec']['containers'][0]['image']
        pods = json.loads(kubectl('get', 'pods', '-l', f'app={service}', '-o', 'json'))['items']
        pods = [p for p in pods if not p['metadata'].get('deletionTimestamp')]
        if len(pods) != 1:
            raise RuntimeError(f'Expected exactly one active local Pod for {service}')
        old = pods[0]
        start = time.monotonic()
        kubectl('delete', 'pod', old['metadata']['name'], '--wait=false')
        for attempt in range(240):
            pods = json.loads(kubectl('get', 'pods', '-l', f'app={service}', '-o', 'json'))['items']
            ready = [p for p in pods if p['metadata']['uid'] != old['metadata']['uid']
                     and not p['metadata'].get('deletionTimestamp')
                     and all(c.get('ready') for c in p['status'].get('containerStatuses', []))
                     and p['status'].get('containerStatuses')]
            if ready:
                new = ready[0]
                if new['spec']['containers'][0]['image'] != image:
                    raise RuntimeError(f'Image changed during {service} recovery')
                break
            time.sleep(1)
        else:
            raise RuntimeError(f'{service} replacement did not become ready')
        path = '/config/app-config.json' if service == 'frontend' else (
            '/management/health/readiness' if service == 'gateway' else
            f'/services/{service}/management/health/readiness')
        host = 'app.fakebook.test' if service == 'frontend' else 'api.fakebook.test'
        req = urllib.request.Request('http://127.0.0.1' + path, headers={'Host': host})
        with urllib.request.urlopen(req, timeout=30) as response:
            assert response.status == 200
        result = {'service': service, 'status': 'PASS', 'old_uid': old['metadata']['uid'],
                  'new_uid': new['metadata']['uid'], 'image': image,
                  'seconds': round(time.monotonic() - start, 1), 'http_readiness': 200}
        results.append(result)
        output.write_text(json.dumps(results, indent=2), encoding='utf-8')
        print(f'{service}: PASS; replaced Pod, preserved digest, HTTP 200 ({result["seconds"]}s)', flush=True)


if __name__ == '__main__':
    main()
