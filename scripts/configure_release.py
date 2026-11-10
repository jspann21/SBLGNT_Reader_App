"""Upload local signing settings as encrypted GitHub Actions repository secrets.

Requires Python 3 and PyNaCl (python -m pip install PyNaCl).
Uses GH_TOKEN/GITHUB_TOKEN or Git's existing credential helper without prompting.
Never logs credentials, signing passwords, or the key contents.
"""
import argparse
import base64
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from nacl.public import PublicKey, SealedBox


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', default='jspann21/SBLGNT_Reader_App')
    args = parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', args.repo):
        parser.error('Expected owner/repository')
    root = Path(__file__).resolve().parent.parent
    properties = dict(line.split('=', 1) for line in
                      (root / 'keystore.properties').read_text(encoding='utf-8-sig').splitlines()
                      if '=' in line and not line.lstrip().startswith('#'))
    values = {
        'ANDROID_KEYSTORE_BASE64': base64.b64encode(
            (root / properties['storeFile']).read_bytes()).decode('ascii'),
        'ANDROID_KEYSTORE_PASSWORD': properties['storePassword'],
        'ANDROID_KEY_ALIAS': properties['keyAlias'],
        'ANDROID_KEY_PASSWORD': properties['keyPassword'],
    }
    google_services = root / 'app/google-services.json'
    if google_services.exists():
        config = google_services.read_text(encoding='utf-8-sig')
        json.loads(config)
        values['GOOGLE_SERVICES_JSON'] = config

    token = os.environ.get('GH_TOKEN') or os.environ.get('GITHUB_TOKEN')
    if not token:
        environment = dict(os.environ, GIT_TERMINAL_PROMPT='0', GCM_INTERACTIVE='never')
        result = subprocess.run(['git', 'credential', 'fill'],
                                input='protocol=https\nhost=github.com\n\n',
                                capture_output=True, text=True, env=environment, check=False)
        if result.returncode != 0:
            raise RuntimeError('No GitHub authentication available. Set GH_TOKEN or sign in to Git.')
        credentials = dict(line.split('=', 1) for line in result.stdout.splitlines() if '=' in line)
        token = credentials.get('password')
    if not token:
        raise RuntimeError('GitHub token is missing.')

    def api(path, payload=None):
        request = Request('https://api.github.com/' + path,
                          data=json.dumps(payload).encode() if payload is not None else None,
                          method='PUT' if payload is not None else 'GET',
                          headers={'Authorization': 'Bearer ' + token,
                                   'Accept': 'application/vnd.github+json',
                                   'Content-Type': 'application/json',
                                   'User-Agent': 'GNT-Reader-release-setup',
                                   'X-GitHub-Api-Version': '2022-11-28'})
        try:
            with urlopen(request, timeout=30) as response:
                body = response.read()
                return json.loads(body) if body else None
        except HTTPError as error:
            raise RuntimeError(f'GitHub API returned HTTP {error.code} for {path}') from None

    repo = api('repos/' + args.repo)
    if not repo.get('permissions', {}).get('admin'):
        raise RuntimeError('Repository administration permission is required to configure secrets.')
    key = api(f'repos/{args.repo}/actions/secrets/public-key')
    box = SealedBox(PublicKey(base64.b64decode(key['key'])))
    for name, value in values.items():
        if not value:
            raise RuntimeError(f'{name} is empty.')
        encrypted = base64.b64encode(box.encrypt(value.encode())).decode('ascii')
        api(f'repos/{args.repo}/actions/secrets/{name}',
            {'encrypted_value': encrypted, 'key_id': key['key_id']})
        print(f'Configured encrypted secret: {name}')
    configured = api(f'repos/{args.repo}/actions/secrets')
    names = {item['name'] for item in configured['secrets']}
    if not names.issuperset(values):
        raise RuntimeError('Some secrets are missing from the repository after upload.')
    print(f'Secrets verified. Repository visibility: {repo["visibility"]} (unchanged).')


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        raise SystemExit(str(error)) from None
