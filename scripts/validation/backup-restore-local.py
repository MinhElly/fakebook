"""Back up local schemas and restore into an isolated disposable MariaDB container."""
import hashlib
import json
import subprocess
import time
from datetime import datetime, timezone
from pathlib import Path

SCHEMAS = ('gateway', 'user_service', 'post_service', 'media_service',
           'comment_service', 'feed_service', 'keycloak')


def run(*args, input=None):
    result = subprocess.run(args, input=input, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, check=True)
    return result.stdout


def sql(container, query):
    return run('docker', 'exec', '-i', container, 'sh', '-c',
               'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" mariadb --protocol=tcp -h127.0.0.1 -uroot --batch --skip-column-names',
               input=query.encode()).decode()


def counts(container):
    names = ','.join("'" + s + "'" for s in SCHEMAS)
    tables = sql(container, 'SELECT TABLE_SCHEMA,TABLE_NAME FROM information_schema.tables '
                 f'WHERE TABLE_SCHEMA IN ({names}) AND TABLE_TYPE="BASE TABLE" '
                 'ORDER BY TABLE_SCHEMA,TABLE_NAME;').strip().splitlines()
    queries = []
    for table in tables:
        schema, name = table.split('\t')
        queries.append(f"SELECT '{schema}.{name}',COUNT(*) FROM `{schema}`.`{name}`;")
    return dict(line.split('\t') for line in sql(container, '\n'.join(queries)).strip().splitlines())


def main():
    source = 'fakebook-mariadb'
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    output = Path('.artifacts') / ('restore-' + stamp)
    output.mkdir(parents=True, exist_ok=False)
    restore = 'fakebook-restore-' + stamp.lower()
    remote = '/tmp/fakebook-backup-' + stamp + '.sql'
    before = counts(source)
    dump = ('MYSQL_PWD="$MARIADB_ROOT_PASSWORD" mariadb-dump -uroot '
            '--single-transaction --routines --events --triggers --hex-blob '
            '--databases ' + ' '.join(SCHEMAS) + ' --result-file=' + remote)
    run('docker', 'exec', source, 'sh', '-c', dump)
    backup = output / 'backup.sql'
    run('docker', 'cp', source + ':' + remote, str(backup.resolve()))
    image = run('docker', 'inspect', source, '--format', '{{.Config.Image}}').decode().strip()
    created = False
    try:
        run('docker', 'run', '-d', '--name', restore, '--network', 'none',
            '-e', 'MARIADB_ALLOW_EMPTY_ROOT_PASSWORD=yes', image,
            '--lower_case_table_names=1', '--character-set-server=utf8mb4')
        created = True
        for attempt in range(90):
            try:
                sql(restore, 'SELECT 1;')
                break
            except subprocess.CalledProcessError:
                time.sleep(1)
        else:
            raise RuntimeError('Disposable MariaDB did not become ready')
        run('docker', 'cp', str(backup.resolve()), restore + ':/tmp/restore.sql')
        try:
            run('docker', 'exec', restore, 'sh', '-c', 'mariadb --protocol=tcp -h127.0.0.1 -uroot < /tmp/restore.sql')
        except subprocess.CalledProcessError as error:
            (output / 'restore-error.txt').write_bytes(error.stderr)
            raise RuntimeError('Restore failed; diagnostic saved to ignored evidence directory') from None
        restored = counts(restore)
        after = counts(source)
        differences = {name: {'before': before.get(name), 'restored': restored.get(name),
                              'live_after': after.get(name)}
                       for name in set(before) | set(restored)
                       if before.get(name) != restored.get(name)}
        report = {'status': 'PASS' if not differences else 'PARTIAL',
                  'scope': 'logical restore and exact table row counts; no application against restore',
                  'schemas': SCHEMAS, 'tables': len(restored),
                  'backup_sha256': hashlib.sha256(backup.read_bytes()).hexdigest(),
                  'source_changed_during_backup': before != after,
                  'differences': differences, 'counts': restored,
                  'restore_network': 'none', 'restore_ports': [], 'image': image}
        (output / 'report.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
        print(json.dumps({k: v for k, v in report.items() if k not in ('counts', 'differences')}))
        print('Evidence: ' + str(output.resolve() / 'report.json'))
        if differences:
            raise RuntimeError('Restored row counts differ; inspect report before declaring PASS')
    finally:
        if created:
            run('docker', 'rm', '-f', '-v', restore)
        run('docker', 'exec', source, 'rm', '-f', remote)


if __name__ == '__main__':
    main()
