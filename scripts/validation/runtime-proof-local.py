"""Normal-user HTTP/SSE proof against local ingress; credentials stay in ignored artifacts."""
import argparse, base64, json, secrets, subprocess, threading, time, urllib.request, urllib.parse
from datetime import datetime, timezone
from pathlib import Path

OUT = Path('.artifacts/runtime-proof'); OUT.mkdir(parents=True, exist_ok=True)
SESSION = OUT/'accounts.json'
KC = 'http://127.0.0.1:9080'
REPORT = {'scope':'normal-user HTTP and SSE via Kubernetes ingress; not browser UI proof','checks':{},'streams':{}}
LOCK=threading.Lock()

def request(path, token=None, body=None, method=None, keycloak=False, raw=None, content_type=None):
    headers={'Host':'auth.fakebook.test:9080' if keycloak else 'api.fakebook.test'}
    if token: headers['Authorization']='Bearer '+token
    data=raw
    if body is not None:
        data=json.dumps(body).encode(); headers['Content-Type']='application/json'
    if content_type: headers['Content-Type']=content_type
    req=urllib.request.Request((KC if keycloak else 'http://127.0.0.1')+path,data=data,headers=headers,method=method)
    r=urllib.request.urlopen(req,timeout=90)
    if r.status==204: return None
    payload=r.read()
    return json.loads(payload) if payload else None

def token(client, username, password, realm='jhipster'):
    form=urllib.parse.urlencode(dict(grant_type='password',client_id=client,username=username,password=password)).encode()
    return request('/realms/'+realm+'/protocol/openid-connect/token',keycloak=True,raw=form,content_type='application/x-www-form-urlencoded')['access_token']

def prepare():
    if SESSION.exists(): return json.loads(SESSION.read_text())
    def env(name):
        return subprocess.check_output(['docker','exec','fakebook-keycloak','printenv',name],text=True).strip()
    admin=token('admin-cli',env('KC_BOOTSTRAP_ADMIN_USERNAME'),env('KC_BOOTSTRAP_ADMIN_PASSWORD'),'master')
    suffix=datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S')
    client='gitops-proof-'+suffix
    request('/admin/realms/jhipster/clients',admin,dict(clientId=client,enabled=True,publicClient=True,
        standardFlowEnabled=False,directAccessGrantsEnabled=True,defaultClientScopes=['basic','profile','email'],
        protocol='openid-connect',protocolMappers=[
        dict(name='proof-subject',protocol='openid-connect',protocolMapper='oidc-usermodel-property-mapper',config={'user.attribute':'id','claim.name':'sub','jsonType.label':'String','access.token.claim':'true','id.token.claim':'true'}),
        dict(name='proof-audience',protocol='openid-connect',protocolMapper='oidc-audience-mapper',config={'included.custom.audience':'account','access.token.claim':'true'}),
        dict(name='proof-roles',protocol='openid-connect',protocolMapper='oidc-usermodel-realm-role-mapper',config={'claim.name':'roles','jsonType.label':'String','multivalued':'true','access.token.claim':'true'})]),keycloak=True)
    role=request('/admin/realms/jhipster/roles/ROLE_USER',admin,keycloak=True)
    accounts=[]
    for label in ['a','b']:
        username='gitops-proof-'+suffix+'-'+label; password=secrets.token_urlsafe(24)
        request('/admin/realms/jhipster/users',admin,dict(username=username,enabled=True,emailVerified=True,email=username+'@example.invalid',firstName='GitOps',lastName=label,credentials=[dict(type='password',value=password,temporary=False)]),keycloak=True)
        user=request('/admin/realms/jhipster/users?username='+username,admin,keycloak=True)[0]
        request('/admin/realms/jhipster/users/'+user['id']+'/role-mappings/realm',admin,[role],keycloak=True)
        accounts.append(dict(username=username,password=password,id=user['id']))
    session=dict(client=client,accounts=accounts)
    SESSION.write_text(json.dumps(session),encoding='utf-8')
    return session

def save():
    with LOCK: (OUT/'report.json').write_text(json.dumps(REPORT,indent=2),encoding='utf-8')

def stream(label, session, end):
    account=session['accounts'][0 if label=='a' else 1]
    stats={'connections':0,'heartbeats':0,'events':[],'errors':[],'started_at':time.time()}
    REPORT['streams'][label]=stats
    while time.time()<end:
        try:
            access=token(session['client'],account['username'],account['password'])
            req=urllib.request.Request('http://127.0.0.1/api/realtime/events',headers={'Host':'api.fakebook.test','Authorization':'Bearer '+access,'Accept':'text/event-stream'})
            with urllib.request.urlopen(req,timeout=45) as r:
                stats['connections']+=1; stats['last_connected_at']=time.time()
                data=[]; connected=time.time()
                while time.time()<end:
                    raw=r.readline()
                    if not raw: break
                    line=raw.decode().strip()
                    if line.startswith(':'): stats['heartbeats']+=1; stats['last_heartbeat_at']=time.time()
                    elif line.startswith('data:'): data.append(line[5:].strip())
                    elif not line and data:
                        event=json.loads(''.join(data)); stats['events'].append(event); data=[]
                    # Reconnect periodically to verify refreshed user authentication.
                    if time.time()-connected>300: break
        except Exception as error:
            stats['errors'].append(dict(type=type(error).__name__,at=time.time()))
            time.sleep(2)
        save()
    stats['duration_seconds']=round(time.time()-stats['started_at']); save()

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--duration',type=int,default=1800); args=parser.parse_args()
    session=prepare(); accounts=session['accounts']
    tokens=[token(session['client'],a['username'],a['password']) for a in accounts]
    profiles=[request('/services/userservice/api/user-profiles/me',t) for t in tokens]
    assert [p['id'] for p in profiles]==[a['id'] for a in accounts]
    REPORT['checks']['normal_user_login_profile']='PASS'
    REPORT['accounts']=[dict(username=a['username'],id=a['id']) for a in accounts]
    end=time.time()+args.duration
    threads=[threading.Thread(target=stream,args=(label,session,end),daemon=True) for label in ['a','b']]
    for thread in threads: thread.start()
    for attempt in range(45):
        if all(REPORT['streams'].get(label,{}).get('connections',0)>0 for label in ['a','b']): break
        time.sleep(1)
    else: raise RuntimeError('SSE did not connect for both users')
    # Reset only the relationship between these two disposable proof accounts.
    relationship='/services/userservice/api/friendships/check?userId1='+accounts[0]['id']+'&userId2='+accounts[1]['id']
    if request(relationship,tokens[0]):
        request('/services/userservice/api/friendships/user/'+accounts[1]['id'],tokens[0],method='DELETE')
    for pending in request('/services/userservice/api/friend-requests/sent',tokens[0]):
        if pending.get('status')=='PENDING' and pending.get('receiver',{}).get('id')==accounts[1]['id']:
            request('/services/userservice/api/friend-requests/'+pending['id']+'/cancel',tokens[0],method='POST')
    friend=request('/services/userservice/api/friend-requests/user/'+accounts[1]['id'],tokens[0],method='POST')
    REPORT['friend_request_id']=friend['id']
    request('/services/userservice/api/friend-requests/'+friend['id']+'/accept',tokens[1],method='POST')
    REPORT['checks']['friendship_accept']='PASS'
    # A valid tiny PNG, uploaded as real multipart bytes.
    png=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a5WQAAAAASUVORK5CYII=')
    boundary='proof'+secrets.token_hex(12)
    multipart=('--'+boundary+'\r\nContent-Disposition: form-data; name="purpose"\r\n\r\nPOST\r\n--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="gitops-proof.png"\r\nContent-Type: image/png\r\n\r\n').encode()+png+('\r\n--'+boundary+'--\r\n').encode()
    media=request('/services/mediaservice/api/media/upload',tokens[0],raw=multipart,content_type='multipart/form-data; boundary='+boundary)
    REPORT['checks']['upload']='PASS'; REPORT['media_id']=media['id']
    file_req=urllib.request.Request('http://127.0.0.1/services/mediaservice/api/media/'+media['id']+'/file',headers={'Host':'api.fakebook.test','Authorization':'Bearer '+tokens[0]})
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None
    try:
        response=urllib.request.build_opener(NoRedirect).open(file_req,timeout=60)
    except urllib.error.HTTPError as error:
        if error.code not in (301,302,303,307,308): raise
        # Never forward the local Host header or bearer token to Cloudinary.
        response=urllib.request.urlopen(error.headers['Location'],timeout=60)
    with response:
        assert response.status==200 and len(response.read())>0
    REPORT['checks']['uploaded_file_read']='PASS'
    post=request('/services/postservice/api/posts/create',tokens[0],dict(content='GitOps recovery proof '+str(time.time()),visibility='PUBLIC',mediaIds=[media['id']],taggedUserIds=[]))
    REPORT['post_id']=post['id']; REPORT['checks']['post_create']='PASS'
    request('/services/commentservice/api/comments/create',tokens[1],dict(postId=post['id'],content='Realtime proof from account B'))
    request('/services/postservice/api/post-reactions/posts/'+post['id'],tokens[1],dict(reactionType='LIKE'),method='PUT')
    for attempt in range(45):
        feed=request('/services/feedservice/api/feed/me?size=100',tokens[1])
        if any(item.get('postId')==post['id'] or item.get('id')==post['id'] for item in feed):
            REPORT['checks']['feed_contains_created_post']='PASS'; break
        time.sleep(1)
    else: REPORT['checks']['feed_contains_created_post']='FAIL'
    for attempt in range(45):
        events=[e.get('eventType') for e in REPORT['streams']['b']['events']]
        if {'COMMENT_CHANGED','POST_REACTION_CHANGED','FRIEND_REQUEST_CREATED'}.issubset(events):
            REPORT['checks']['two_account_realtime']='PASS'; break
        time.sleep(1)
    else: REPORT['checks']['two_account_realtime']='FAIL'
    save(); print('Initial authenticated HTTP/SSE checks: '+json.dumps(REPORT['checks']),flush=True)
    while time.time()<end:
        time.sleep(min(30,max(0,end-time.time()))); save()
        print('SSE progress: '+json.dumps({k:{'connections':v['connections'],'heartbeats':v['heartbeats'],'events':len(v['events']),'errors':len(v['errors'])} for k,v in REPORT['streams'].items()}),flush=True)
    for thread in threads: thread.join(timeout=25)
    REPORT['checks']['long_connection']='PASS' if all(v['heartbeats']>=args.duration//30 and not v['errors'] for v in REPORT['streams'].values()) else 'PARTIAL'
    save(); print('Runtime evidence: '+str((OUT/'report.json').resolve()),flush=True)

if __name__=='__main__':
    try: main()
    except Exception as error:
        REPORT['failure']={'type':type(error).__name__,'message':str(error)}; save(); raise
