// CaseLab AI — Game screens: Dashboard, Scene, Evidence, Suspects, Timeline, Interrogation, Submit, Result

// ─── Game state shared via simple module ──────────────────────────
window.GameState = {
  elapsedMin: 14,   // for evidence unlock calc
  unlocked: ['e1','e2','e3','e4','e5','e12','e6','e11'], // initial + first batch
  newEvids: ['e6','e11'],
  hintsUsed: [],
  suspicions: { s1:72, s2:45, s3:38, s4:28, s5:22 },
};

function isUnlocked(e) {
  if (e.initial) return true;
  return GameState.unlocked.includes(e.id);
}

// ─── Detective Dashboard (main play screen) ──────────────────────────
function DashboardScreen({ go }) {
  const [tab, setTab] = React.useState('scene');
  const [showHint, setShowHint] = React.useState(false);
  const tabs = [
    {id:'scene', l:'현장', ic:I.map},
    {id:'evidence', l:'증거', ic:I.doc},
    {id:'suspects', l:'용의자', ic:I.person},
    {id:'timeline', l:'타임라인', ic:I.clock},
    {id:'submit', l:'추리제출', ic:I.badge},
  ];
  return (
    <div className="scr">
      {/* Custom dashboard top */}
      <div style={{
        padding:'14px 16px 12px',background:'var(--stage)',
        borderBottom:'1px solid var(--border)',position:'sticky',top:0,zIndex:10
      }}>
        <div style={{display:'flex',alignItems:'center',gap:10}}>
          <button className="back tap" style={{
            width:32,height:32,borderRadius:8,
            display:'flex',alignItems:'center',justifyContent:'center',color:'var(--text-dim)'
          }} onClick={()=>go('home')}>{I.back}</button>
          <div style={{flex:1,minWidth:0}}>
            <div style={{display:'flex',alignItems:'center',gap:8}}>
              <span className="mono" style={{fontSize:9.5,letterSpacing:'.18em',color:'var(--text-mute)'}}>{CASE.code}</span>
              <Stamp color="var(--crimson)" tilt={0}>LIVE</Stamp>
            </div>
            <div className="serif" style={{fontSize:15,fontWeight:600,marginTop:2,whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>{CASE.title}</div>
          </div>
          <button className="tap" onClick={()=>setShowHint(true)} style={{
            display:'flex',alignItems:'center',gap:5,padding:'7px 11px',borderRadius:18,
            background:'rgba(212,164,55,.10)',border:'1px solid var(--gold-dim)',color:'var(--gold)',
            fontSize:11.5,fontWeight:600,
          }}>{I.bulb} 힌트</button>
        </div>
        {/* Status row */}
        <div style={{display:'flex',gap:0,marginTop:12,background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8,overflow:'hidden'}}>
          <StatusCell label="진행 시간" value="14:22" mono color="var(--text)"/>
          <StatusCell label="해금 증거" value={`${GameState.unlocked.length}/${EVIDENCE.length}`} color="var(--gold)"/>
          <StatusCell label="다음 해금" value="3분" color="var(--crimson-soft)"/>
        </div>
      </div>

      <div className="scr-body" style={{paddingBottom:70}}>
        {tab==='scene' && <SceneTab go={go}/>}
        {tab==='evidence' && <EvidenceTab go={go}/>}
        {tab==='suspects' && <SuspectsTab go={go}/>}
        {tab==='timeline' && <TimelineTab/>}
        {tab==='submit' && <SubmitTab go={go}/>}
      </div>

      {/* Sub tab bar */}
      <div style={{
        display:'flex',background:'var(--surface)',borderTop:'1px solid var(--border)',padding:'6px 4px 8px'
      }}>
        {tabs.map(t => (
          <button key={t.id} className={`bnav-item tap`} onClick={()=>setTab(t.id)} style={{
            color: tab===t.id?'var(--gold)':'var(--text-mute)'
          }}>
            <span className="bnav-ico">{t.ic}</span>
            <span>{t.l}</span>
          </button>
        ))}
      </div>

      {showHint && <HintsModal close={()=>setShowHint(false)}/>}
    </div>
  );
}

function StatusCell({ label, value, color='var(--text)', mono }) {
  return (
    <div style={{flex:1,padding:'8px 12px',borderRight:'1px solid var(--border)'}}>
      <div className="mono" style={{fontSize:9,letterSpacing:'.14em',color:'var(--text-mute)'}}>{label}</div>
      <div className={mono?'mono':'serif'} style={{fontSize:15,fontWeight:600,color,marginTop:2,fontVariantNumeric:'tabular-nums'}}>{value}</div>
    </div>
  );
}

// ─── Scene tab ──────────────────────────
function SceneTab({ go }) {
  const [sel, setSel] = React.useState('l1');
  const loc = LOCATIONS.find(l=>l.id===sel);
  return (
    <div style={{padding:'16px 18px'}}>
      <div className="label-kicker">현장 단면도 · CRIME SCENE</div>

      {/* Building cutaway */}
      <div style={{
        marginTop:10,background:'var(--surface)',border:'1px solid var(--border)',
        borderRadius:10,padding:14,position:'relative'
      }}>
        <svg viewBox="0 0 320 280" width="100%" height="auto" style={{display:'block'}}>
          {/* Floor labels */}
          <text x="6" y="38" fill="#5a5e70" fontFamily="JetBrains Mono" fontSize="9" letterSpacing="1.5">6F</text>
          <text x="6" y="118" fill="#5a5e70" fontFamily="JetBrains Mono" fontSize="9" letterSpacing="1.5">4F</text>
          <text x="6" y="218" fill="#5a5e70" fontFamily="JetBrains Mono" fontSize="9" letterSpacing="1.5">1F</text>
          {/* 6F */}
          <g>
            <rect x="22" y="20" width="296" height="60" fill="#181b27" stroke="#2a2d3a"/>
            <line x1="160" y1="20" x2="160" y2="80" stroke="#2a2d3a" strokeDasharray="3,3"/>
            <line x1="240" y1="20" x2="240" y2="80" stroke="#2a2d3a" strokeDasharray="3,3"/>
            <text x="86" y="46" fill="#9095a8" fontFamily="Pretendard" fontSize="10" textAnchor="middle">데모룸</text>
            <text x="86" y="60" fill="#5a5e70" fontFamily="JetBrains Mono" fontSize="8" textAnchor="middle">6F-A</text>
            <text x="200" y="46" fill="#9095a8" fontFamily="Pretendard" fontSize="10" textAnchor="middle">복도</text>
            <text x="280" y="46" fill="#9095a8" fontFamily="Pretendard" fontSize="10" textAnchor="middle">회의실</text>
            <text x="280" y="60" fill="#5a5e70" fontFamily="JetBrains Mono" fontSize="8" textAnchor="middle">6F-B</text>
            {/* Victim marker */}
            <circle cx="86" cy="50" r="12" fill="none" stroke="#d44a4a" strokeWidth="1.5" className={sel==='l1'?'new-glow':''}/>
            <text x="86" y="54" fill="#d44a4a" fontFamily="Noto Serif KR" fontSize="14" textAnchor="middle" fontWeight="600">×</text>
            {/* CCTV blind */}
            <rect x="178" y="48" width="44" height="14" fill="none" stroke="#9c7ad4" strokeDasharray="2,2"/>
            <text x="200" y="74" fill="#9c7ad4" fontFamily="JetBrains Mono" fontSize="7" textAnchor="middle" letterSpacing="1">CCTV BLIND</text>
          </g>
          {/* 4F */}
          <g>
            <rect x="22" y="100" width="296" height="60" fill="#181b27" stroke="#2a2d3a"/>
            <text x="170" y="126" fill="#9095a8" fontFamily="Pretendard" fontSize="10" textAnchor="middle">재무팀 사무실</text>
            <text x="170" y="140" fill="#5a5e70" fontFamily="JetBrains Mono" fontSize="8" textAnchor="middle">4F</text>
            {/* Epipen marker */}
            <circle cx="240" cy="130" r="8" fill="#d4a437" opacity="0.2"/>
            <text x="240" y="134" fill="#d4a437" fontFamily="Noto Serif KR" fontSize="11" textAnchor="middle" fontWeight="600">💉</text>
          </g>
          {/* 1F */}
          <g>
            <rect x="22" y="200" width="296" height="60" fill="#181b27" stroke="#2a2d3a"/>
            <text x="170" y="226" fill="#9095a8" fontFamily="Pretendard" fontSize="10" textAnchor="middle">리프트커피 강남2호점</text>
            <text x="170" y="240" fill="#5a5e70" fontFamily="JetBrains Mono" fontSize="8" textAnchor="middle">1F · 카페</text>
            <circle cx="70" cy="230" r="6" fill="#5294ef" opacity="0.4"/>
            <text x="70" y="234" fill="#5294ef" fontFamily="Pretendard" fontSize="9" textAnchor="middle">☕</text>
          </g>
          {/* Stairs lines */}
          <line x1="160" y1="80" x2="160" y2="100" stroke="#3a3e54" strokeDasharray="2,2"/>
          <line x1="160" y1="160" x2="160" y2="200" stroke="#3a3e54" strokeDasharray="2,2"/>
        </svg>
        <div style={{display:'flex',justifyContent:'space-between',marginTop:8,paddingTop:8,borderTop:'1px solid var(--border)'}}>
          <Legend dot="#d44a4a" l="피해자 발견"/>
          <Legend dot="#d4a437" l="중요 증거"/>
          <Legend dot="#9c7ad4" l="CCTV 사각"/>
        </div>
      </div>

      {/* Locations list */}
      <div className="label-kicker" style={{marginTop:18}}>장소 · LOCATIONS</div>
      <div style={{marginTop:8, display:'flex',flexDirection:'column',gap:6}}>
        {LOCATIONS.map(l => (
          <button key={l.id} className="tap" onClick={()=>setSel(l.id)} style={{
            display:'flex',alignItems:'center',gap:10,
            padding:'10px 12px',background: sel===l.id?'var(--surface-3)':'var(--surface)',
            border:`1px solid ${sel===l.id?'var(--border-strong)':'var(--border)'}`,
            borderRadius:9,width:'100%',textAlign:'left'
          }}>
            <span style={{width:24,height:24,borderRadius:6,background:'var(--surface-3)',
              display:'flex',alignItems:'center',justifyContent:'center',color:'var(--gold)',fontSize:11,fontFamily:'var(--mono)'}}>
              {l.mark}
            </span>
            <div style={{flex:1}}>
              <div style={{fontSize:13,fontWeight:500}}>{l.name}</div>
              <div className="mono" style={{fontSize:9.5,color:'var(--text-mute)',marginTop:2,letterSpacing:'.1em'}}>{l.floor} · {l.tag}</div>
            </div>
            <span className="mono" style={{fontSize:10,color:'var(--text-dim)'}}>{l.evids.length} EVID</span>
          </button>
        ))}
      </div>

      {/* Selected location detail */}
      {loc && loc.evids.length > 0 && (
        <>
          <div className="label-kicker" style={{marginTop:18}}>{loc.name}에서 발견된 증거</div>
          <div style={{marginTop:8,display:'flex',flexDirection:'column',gap:6}}>
            {loc.evids.map(eid => {
              const e = EVIDENCE.find(x=>x.id===eid);
              const unlocked = isUnlocked(e);
              return (
                <div key={eid} className="card tap" style={{display:'flex',gap:10,alignItems:'center',padding:11,opacity:unlocked?1:.5}}
                  onClick={()=>unlocked && go({type:'evidence',id:eid})}>
                  <div style={{fontSize:20}}>{unlocked?e.icon:'🔒'}</div>
                  <div style={{flex:1}}>
                    <div style={{fontSize:12.5,fontWeight:500}}>{unlocked?e.name:'??? (잠금)'}</div>
                    <div className="mono" style={{fontSize:9.5,color:'var(--text-mute)',marginTop:2,letterSpacing:'.08em'}}>{e.loc}</div>
                  </div>
                  {unlocked && <ImpPill level={e.importance}/>}
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}

function Legend({ dot, l }) {
  return (
    <div style={{display:'flex',alignItems:'center',gap:6}}>
      <span style={{width:8,height:8,borderRadius:'50%',background:dot}}/>
      <span className="mono" style={{fontSize:9.5,color:'var(--text-mute)',letterSpacing:'.1em'}}>{l}</span>
    </div>
  );
}

// ─── Evidence tab ──────────────────────────
function EvidenceTab({ go }) {
  const [filter, setFilter] = React.useState('all');
  const filters = [
    { id:'all', l:'전체' }, { id:'new', l:'NEW' }, { id:'crit', l:'결정적' }, { id:'locked', l:'잠금' }
  ];
  const list = EVIDENCE.filter(e => {
    if (filter==='all') return true;
    if (filter==='new') return GameState.newEvids.includes(e.id);
    if (filter==='crit') return e.importance==='CRITICAL' || e.importance==='HIGH';
    if (filter==='locked') return !isUnlocked(e);
    return true;
  });
  return (
    <div style={{padding:'16px 18px'}}>
      <div style={{display:'flex',alignItems:'flex-end',justifyContent:'space-between'}}>
        <div>
          <div className="label-kicker">증거 보드 · EVIDENCE BOARD</div>
          <div className="serif" style={{fontSize:17,fontWeight:600,marginTop:4}}>{GameState.unlocked.length}<span style={{color:'var(--text-mute)',fontSize:13}}>/{EVIDENCE.length}</span> 수집</div>
        </div>
        <button className="tap" style={{color:'var(--text-dim)',fontSize:12,padding:'6px 10px',background:'var(--surface-2)',borderRadius:6,border:'1px solid var(--border)'}}>
          정렬 ▾
        </button>
      </div>

      <div style={{display:'flex',gap:6,marginTop:12,overflowX:'auto'}}>
        {filters.map(f => (
          <button key={f.id} className="tap" onClick={()=>setFilter(f.id)} style={{
            padding:'5px 11px',borderRadius:14,fontSize:11.5,whiteSpace:'nowrap',
            background: filter===f.id?'var(--gold)':'transparent',
            color: filter===f.id?'#1a1306':'var(--text-dim)',
            border: filter===f.id?'none':'1px solid var(--border)',
            fontFamily:'var(--mono)',letterSpacing:'.06em',fontWeight:600,
          }}>{f.l}</button>
        ))}
      </div>

      <div style={{marginTop:14,display:'flex',flexDirection:'column',gap:8}}>
        {list.map(e => {
          const unlocked = isUnlocked(e);
          const isNew = GameState.newEvids.includes(e.id);
          return (
            <div key={e.id} className={`card evidence tap ${isNew?'new-glow':''}`}
              style={{display:'flex',gap:12,alignItems:'flex-start',padding:13,opacity:unlocked?1:.5}}
              onClick={()=>unlocked && go({type:'evidence',id:e.id})}>
              <div style={{
                width:42,height:42,borderRadius:6,flexShrink:0,
                background:unlocked?'var(--surface-2)':'var(--surface-3)',
                border:'1px solid var(--border)',
                display:'flex',alignItems:'center',justifyContent:'center',
                fontSize:20,color:'var(--gold)'
              }}>{unlocked?e.icon:'🔒'}</div>
              <div style={{flex:1,minWidth:0}}>
                <div style={{display:'flex',gap:6,alignItems:'center',marginBottom:3,flexWrap:'wrap'}}>
                  <span className="mono" style={{fontSize:9.5,color:'var(--text-mute)',letterSpacing:'.1em'}}>{e.id.toUpperCase()}</span>
                  {isNew && <Badge color="var(--gold)" bg="rgba(212,164,55,.16)">NEW</Badge>}
                  {e.fake && unlocked && <Badge color="var(--text-mute)">참고</Badge>}
                </div>
                <div style={{fontSize:13.5,fontWeight:500}}>{unlocked?e.name:'??? (해금 대기)'}</div>
                {unlocked ? (
                  <div className="mono" style={{fontSize:10,color:'var(--text-mute)',marginTop:3,letterSpacing:'.08em'}}>{e.loc}</div>
                ) : (
                  <div className="mono" style={{fontSize:10,color:'var(--gold)',marginTop:3,letterSpacing:'.08em'}}>+{e.unlockMin}분 후 해금</div>
                )}
              </div>
              {unlocked && <ImpPill level={e.importance}/>}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── Suspects tab ──────────────────────────
function SuspectsTab({ go }) {
  return (
    <div style={{padding:'16px 18px'}}>
      <div className="label-kicker">용의자 명단 · PERSONS OF INTEREST</div>
      <div style={{marginTop:10, display:'flex',flexDirection:'column',gap:10}}>
        {SUSPECTS.map(s => (
          <div key={s.id} className="card tap" style={{padding:14}} onClick={()=>go({type:'suspect',id:s.id})}>
            <div style={{display:'flex',gap:12,alignItems:'flex-start'}}>
              <Avatar s={s} size={48}/>
              <div style={{flex:1,minWidth:0}}>
                <div style={{display:'flex',alignItems:'center',gap:6}}>
                  <span className="serif" style={{fontSize:15.5,fontWeight:600}}>{s.name}</span>
                  <span style={{fontSize:11,color:'var(--text-mute)'}}>{s.age}세</span>
                </div>
                <div style={{fontSize:11.5,color:'var(--text-dim)',marginTop:2}}>{s.role}</div>
                <div className="mono" style={{fontSize:9.5,color:'var(--text-mute)',marginTop:4,letterSpacing:'.08em'}}>↳ {s.relations}</div>
              </div>
              <div style={{textAlign:'right'}}>
                <div className="mono" style={{fontSize:9,letterSpacing:'.14em',color:'var(--text-mute)'}}>의심도</div>
                <div className="serif" style={{fontSize:18,fontWeight:600,color: s.suspicion>=60?'var(--crimson-soft)':s.suspicion>=40?'var(--gold)':'var(--text)'}}>{s.suspicion}</div>
              </div>
            </div>
            <div style={{marginTop:10}}>
              <div className="sus-meter">
                <div className="sus-meter-fill" style={{width:`${s.suspicion}%`}}/>
              </div>
            </div>
            <div style={{display:'flex',gap:6,marginTop:11}}>
              <button className="tap" style={{
                flex:1,padding:'8px',borderRadius:7,background:'var(--surface-2)',border:'1px solid var(--border)',
                fontSize:12,color:'var(--text-dim)'
              }} onClick={(e)=>{e.stopPropagation();go({type:'suspect',id:s.id})}}>프로필 보기</button>
              <button className="tap" style={{
                flex:1,padding:'8px',borderRadius:7,background:'rgba(212,164,55,.10)',border:'1px solid var(--gold-dim)',
                fontSize:12,color:'var(--gold)',fontWeight:600,display:'flex',alignItems:'center',justifyContent:'center',gap:5,
              }} onClick={(e)=>{e.stopPropagation();go({type:'interrogate',id:s.id})}}>
                {I.chat} 심문하기
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Timeline tab ──────────────────────────
function TimelineTab() {
  return (
    <div style={{padding:'16px 18px'}}>
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'flex-end'}}>
        <div>
          <div className="label-kicker">사건 타임라인 · CASE TIMELINE</div>
          <div className="serif" style={{fontSize:17,fontWeight:600,marginTop:4}}>22:00 ~ 23:37</div>
        </div>
        <div style={{display:'flex',gap:5}}>
          <Legend dot="var(--crimson)" l="모순"/>
        </div>
      </div>

      {/* Timeline */}
      <div style={{marginTop:18,paddingLeft:14,position:'relative'}}>
        <div style={{position:'absolute',left:6,top:8,bottom:8,width:1,background:'var(--border-strong)'}}/>
        {TIMELINE.map((t,i) => (
          <div key={i} style={{display:'flex',gap:12,padding:'10px 0',position:'relative'}}>
            <div style={{
              position:'absolute',left:-14,top:14,width:11,height:11,borderRadius:'50%',
              background: t.conflict?'var(--crimson)':'var(--gold)',
              border:'2px solid var(--stage)'
            }}/>
            <div style={{flex:1}}>
              <div style={{display:'flex',gap:8,alignItems:'center'}}>
                <span className="mono" style={{fontSize:12.5,fontWeight:600,color: t.conflict?'var(--crimson-soft)':'var(--gold)',letterSpacing:'.04em'}}>{t.t}</span>
                {t.conflict && <Badge color="var(--crimson-soft)" bg="rgba(212,74,74,.13)">모순 발견</Badge>}
              </div>
              <div style={{fontSize:13,color:'var(--text)',marginTop:3,lineHeight:1.45}}>{t.label}</div>
              <div className="mono" style={{fontSize:10,color:'var(--text-mute)',marginTop:3,letterSpacing:'.06em'}}>source: {t.source}</div>
              {t.conflict && t.conflictNote && (
                <div style={{
                  marginTop:6,padding:'8px 10px',background:'rgba(212,74,74,.08)',
                  border:'1px solid var(--crimson-dim)',borderRadius:6,
                  fontSize:11.5,color:'var(--crimson-soft)',lineHeight:1.45
                }}>⚠ {t.conflictNote}</div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Submit tab (entry to final deduction) ──────────────────────────
function SubmitTab({ go }) {
  return (
    <div style={{padding:'16px 18px'}}>
      <div style={{
        textAlign:'center',padding:'24px 18px',
        border:'1px dashed var(--gold-dim)',borderRadius:12,
        background:'linear-gradient(180deg, rgba(212,164,55,.06), transparent)',
      }}>
        <div style={{fontSize:38,color:'var(--gold)',marginBottom:8}}>{I.badge}</div>
        <div className="serif" style={{fontSize:18,fontWeight:600}}>최종 추리 제출</div>
        <div style={{fontSize:12.5,color:'var(--text-dim)',marginTop:6,lineHeight:1.55}}>
          준비가 되었다면 범인·동기·범행방법과<br/>결정적 증거 3개를 제시하세요.
        </div>
      </div>

      <div style={{marginTop:18}}>
        <div className="label-kicker">제출 전 체크리스트</div>
        <div style={{marginTop:8,display:'flex',flexDirection:'column',gap:8}}>
          {[
            { ok:true, l:'증거 8/12개 수집' },
            { ok:true, l:'타임라인 모순 3건 확인' },
            { ok:false, l:'결정적 증거(에피펜 위치) 미확인' },
            { ok:false, l:'회계 파일 상세 미확인' },
          ].map((c,i) => (
            <div key={i} style={{
              display:'flex',gap:10,alignItems:'center',padding:'10px 12px',
              background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8
            }}>
              <div style={{
                width:18,height:18,borderRadius:4,
                background: c.ok?'rgba(95,179,124,.18)':'rgba(255,255,255,.04)',
                color: c.ok?'var(--green)':'var(--text-mute)',
                display:'flex',alignItems:'center',justifyContent:'center'
              }}>{c.ok?<Icon d="M20 6L9 17l-5-5" size={12} stroke={2.4}/>:<Icon d="M5 12h14" size={12} stroke={2.4}/>}</div>
              <div style={{fontSize:12.5,color:c.ok?'var(--text)':'var(--text-dim)',flex:1}}>{c.l}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{marginTop:18,padding:'12px 14px',background:'rgba(212,74,74,.05)',border:'1px solid var(--crimson-dim)',borderRadius:8}}>
        <div style={{display:'flex',gap:8}}>
          <div style={{color:'var(--crimson-soft)',flexShrink:0}}>{I.warn}</div>
          <div style={{fontSize:11.5,color:'var(--text-dim)',lineHeight:1.5}}>
            제출 후엔 수정할 수 없습니다. 핵심 증거를 모두 확인했는지 다시 한번 살펴보세요.
          </div>
        </div>
      </div>

      <div style={{marginTop:20}}>
        <Btn full size="lg" variant="crimson" onClick={()=>go('submit')} icon={I.arrow}>
          추리 제출하러 가기
        </Btn>
      </div>
    </div>
  );
}

// ─── Evidence detail screen ──────────────────────────
function EvidenceDetailScreen({ go, eid }) {
  const e = EVIDENCE.find(x=>x.id===eid) || EVIDENCE[0];
  return (
    <div className="scr">
      <TopBar onBack={()=>go('dashboard')} title="증거 상세" sub={`EVID · ${e.id.toUpperCase()}`}/>
      <div className="scr-body" style={{padding:'14px 18px 100px'}}>
        {/* hero */}
        <div className="card evidence" style={{padding:0,overflow:'hidden'}}>
          <div style={{
            height:160,background:'linear-gradient(135deg,#1d1a0e 0%,#0a0b10 100%)',
            display:'flex',alignItems:'center',justifyContent:'center',position:'relative'
          }}>
            <div style={{position:'absolute',top:10,left:10,display:'flex',gap:6}}>
              <Stamp color="var(--gold)" tilt={-3}>EVIDENCE</Stamp>
            </div>
            <div style={{position:'absolute',top:10,right:10}}>
              <ImpPill level={e.importance}/>
            </div>
            <div style={{fontSize:80,opacity:.95}}>{e.icon}</div>
            <div style={{position:'absolute',bottom:8,left:12,right:12,display:'flex',justifyContent:'space-between'}}>
              <span className="mono" style={{fontSize:9,letterSpacing:'.2em',color:'var(--text-mute)'}}>{e.id.toUpperCase()}</span>
              <span className="mono" style={{fontSize:9,letterSpacing:'.16em',color:'var(--text-mute)'}}>{CASE.code}</span>
            </div>
          </div>
        </div>

        <div style={{marginTop:16}}>
          <div className="serif" style={{fontSize:22,fontWeight:600,lineHeight:1.3}}>{e.name}</div>
          <div className="mono" style={{fontSize:11,color:'var(--text-mute)',marginTop:4,letterSpacing:'.08em'}}>발견 위치: {e.loc}</div>
        </div>

        <div style={{marginTop:16}}>
          <div className="label-kicker">상세 설명</div>
          <p style={{fontSize:13.5,lineHeight:1.65,color:'var(--text-dim)',marginTop:8}}>{e.desc}</p>
        </div>

        {e.related && e.related.length>0 && (
          <div style={{marginTop:18}}>
            <div className="label-kicker">관련 용의자</div>
            <div style={{display:'flex',gap:8,marginTop:8}}>
              {e.related.map(sid => {
                const s = SUSPECTS.find(x=>x.id===sid);
                return (
                  <button key={sid} className="tap" onClick={()=>go({type:'suspect',id:sid})} style={{
                    display:'flex',gap:8,alignItems:'center',
                    padding:'8px 12px',background:'var(--surface-2)',border:'1px solid var(--border)',
                    borderRadius:20
                  }}>
                    <Avatar s={s} size={24}/>
                    <span style={{fontSize:12.5}}>{s.name}</span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        <div style={{marginTop:18}}>
          <div className="label-kicker">관련 타임라인</div>
          <div style={{marginTop:8}}>
            {TIMELINE.filter(t=>t.evidId===e.id).map((t,i)=>(
              <div key={i} style={{display:'flex',gap:10,padding:'8px 12px',background:'var(--surface)',border:'1px solid var(--border)',borderRadius:7,marginBottom:6}}>
                <span className="mono" style={{fontSize:12,color:t.conflict?'var(--crimson-soft)':'var(--gold)',fontWeight:600}}>{t.t}</span>
                <span style={{fontSize:12.5,color:'var(--text-dim)',flex:1}}>{t.label}</span>
              </div>
            ))}
            {TIMELINE.filter(t=>t.evidId===e.id).length===0 && (
              <div className="mono" style={{fontSize:11,color:'var(--text-mute)',letterSpacing:'.08em'}}>— 연관 타임라인 없음 —</div>
            )}
          </div>
        </div>
      </div>

      <div style={{position:'absolute',bottom:0,left:0,right:0,padding:'14px 18px',background:'linear-gradient(180deg,transparent,var(--stage) 30%)'}}>
        <Btn full size="lg" variant="solid" icon={I.chat} onClick={()=>go({type:'pickInterrogateForEvid', eid})}>
          이 증거로 심문하기
        </Btn>
      </div>
    </div>
  );
}

// ─── Suspect detail screen ──────────────────────────
function SuspectDetailScreen({ go, sid }) {
  const s = SUSPECTS.find(x=>x.id===sid) || SUSPECTS[0];
  return (
    <div className="scr">
      <TopBar onBack={()=>go('dashboard')} title="용의자 프로필" sub={`SUSPECT · ${s.id.toUpperCase()}`}
        right={<button className="icon tap">{I.more}</button>}/>
      <div className="scr-body" style={{padding:'14px 0 100px'}}>
        <div style={{padding:'0 18px'}}>
          <div className="card" style={{padding:18,position:'relative',overflow:'hidden'}}>
            {/* tinted bg */}
            <div style={{
              position:'absolute',top:0,right:0,width:140,height:140,
              background:`radial-gradient(circle, ${s.accent}22, transparent 70%)`,pointerEvents:'none'
            }}/>
            <div style={{display:'flex',gap:14,alignItems:'flex-start',position:'relative'}}>
              <Avatar s={s} size={72}/>
              <div style={{flex:1}}>
                <div className="mono" style={{fontSize:9.5,letterSpacing:'.18em',color:'var(--text-mute)'}}>{s.id.toUpperCase()}</div>
                <div className="serif" style={{fontSize:22,fontWeight:600,marginTop:2}}>{s.name}</div>
                <div style={{fontSize:12,color:'var(--text-dim)',marginTop:2}}>{s.age}세 · {s.role}</div>
                <div style={{marginTop:10,display:'flex',gap:8,alignItems:'center'}}>
                  <span className="mono" style={{fontSize:10,color:'var(--text-mute)',letterSpacing:'.12em'}}>SUSPICION</span>
                  <div style={{width:80}}>
                    <div className="sus-meter"><div className="sus-meter-fill" style={{width:`${s.suspicion}%`}}/></div>
                  </div>
                  <span className="mono" style={{fontSize:13,fontWeight:600,color:s.suspicion>=60?'var(--crimson-soft)':'var(--gold)'}}>{s.suspicion}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div style={{padding:'18px 18px 0'}}>
          <div className="label-kicker">관계 · RELATION</div>
          <p style={{fontSize:13,color:'var(--text-dim)',marginTop:6,lineHeight:1.55}}>{s.rel}</p>
        </div>

        <div style={{padding:'18px 18px 0'}}>
          <div className="label-kicker">공개 진술 · STATEMENT</div>
          <div className="card" style={{marginTop:8,padding:14,position:'relative'}}>
            <div style={{position:'absolute',top:-1,left:14,width:18,height:2,background:s.accent}}/>
            <div style={{fontSize:13,color:'var(--text)',lineHeight:1.6,fontStyle:'italic'}}>“{s.statement}”</div>
          </div>
        </div>

        <div style={{padding:'18px 18px 0'}}>
          <div className="label-kicker">알리바이 · ALIBI</div>
          <div className="mono" style={{fontSize:12.5,color:'var(--text)',marginTop:6,letterSpacing:'.02em',lineHeight:1.5}}>{s.alibi}</div>
        </div>

        <div style={{padding:'18px 18px 0'}}>
          <div className="label-kicker">관련 증거</div>
          <div style={{marginTop:8,display:'flex',flexDirection:'column',gap:6}}>
            {EVIDENCE.filter(e => e.related?.includes(s.id) && isUnlocked(e)).map(e => (
              <div key={e.id} className="card tap" style={{display:'flex',gap:10,alignItems:'center',padding:11}}
                onClick={()=>go({type:'evidence',id:e.id})}>
                <div style={{fontSize:18}}>{e.icon}</div>
                <div style={{flex:1}}>
                  <div style={{fontSize:12.5,fontWeight:500}}>{e.name}</div>
                  <div className="mono" style={{fontSize:9.5,color:'var(--text-mute)',marginTop:2,letterSpacing:'.08em'}}>{e.loc}</div>
                </div>
                <ImpPill level={e.importance}/>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div style={{position:'absolute',bottom:0,left:0,right:0,padding:'14px 18px',background:'linear-gradient(180deg,transparent,var(--stage) 30%)'}}>
        <Btn full size="lg" variant="solid" icon={I.chat} onClick={()=>go({type:'interrogate',id:s.id})}>심문 시작</Btn>
      </div>
    </div>
  );
}

// ─── Interrogation chat ──────────────────────────
function InterrogationScreen({ go, sid, presetEvid }) {
  const s = SUSPECTS.find(x=>x.id===sid) || SUSPECTS[0];
  const data = INTERROGATION[sid] || INTERROGATION.s1;
  const [log, setLog] = React.useState(() => {
    const init = [
      { type:'sys', text:'심문이 시작되었습니다. AI 용의자는 공개된 정보 안에서만 답변합니다.' },
      { type:'sus', text:`(자리에 앉으며) 무엇이 궁금하신가요, 조사관님.` },
    ];
    if (presetEvid) {
      const e = EVIDENCE.find(x=>x.id===presetEvid);
      init.push({ type:'user', text:`${e.name}을(를) 보여드리면서 묻겠습니다.`, evid:presetEvid });
      const resp = data.onEvidence?.[presetEvid] || data.deny;
      init.push({ type:'sus', text:resp });
    }
    return init;
  });
  const [input, setInput] = React.useState('');
  const [showEvid, setShowEvid] = React.useState(false);
  const logRef = React.useRef(null);
  React.useEffect(()=>{ if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight; },[log]);

  const ask = (q) => {
    setLog(l => [...l, { type:'user', text:q }]);
    const a = data.base?.[q] || data.deny;
    setTimeout(() => setLog(l => [...l, { type:'sus', text:a, typing:true }]), 350);
    setInput('');
  };

  const present = (eid) => {
    const e = EVIDENCE.find(x=>x.id===eid);
    setShowEvid(false);
    setLog(l => [...l, { type:'user', text:`${e.name}에 대해 설명해주시겠습니까?`, evid:eid }]);
    setTimeout(() => {
      const resp = data.onEvidence?.[eid] || data.deny;
      setLog(l => [...l, { type:'sus', text:resp }]);
    }, 400);
  };

  return (
    <div className="scr">
      {/* Custom top */}
      <div style={{padding:'12px 16px',background:'var(--stage)',borderBottom:'1px solid var(--border)',display:'flex',gap:10,alignItems:'center'}}>
        <button className="back tap" onClick={()=>go({type:'suspect',id:sid})} style={{width:32,height:32,borderRadius:8,display:'flex',alignItems:'center',justifyContent:'center',color:'var(--text-dim)'}}>{I.back}</button>
        <Avatar s={s} size={38}/>
        <div style={{flex:1,minWidth:0}}>
          <div className="serif" style={{fontSize:14.5,fontWeight:600}}>{s.name}</div>
          <div className="mono" style={{fontSize:10,color:'var(--text-mute)',marginTop:1,letterSpacing:'.08em'}}>{s.role} · 의심도 {s.suspicion}</div>
        </div>
        <button className="icon tap">{I.more}</button>
      </div>

      {/* Chat log */}
      <div ref={logRef} className="scr-body" style={{padding:'12px 14px',display:'flex',flexDirection:'column',gap:8}}>
        {log.map((m,i)=>{
          if (m.type==='sys') return <div key={i} className="bub bub-sys">{m.text}</div>;
          if (m.type==='user') return (
            <div key={i} style={{display:'flex',flexDirection:'column',gap:4,alignItems:'flex-end'}}>
              {m.evid && (
                <div style={{
                  fontSize:10.5,color:'var(--gold-soft)',fontFamily:'var(--mono)',letterSpacing:'.08em',
                  padding:'4px 8px',background:'rgba(212,164,55,.10)',borderRadius:5,border:'1px solid var(--gold-dim)'
                }}>📎 증거 제시: {EVIDENCE.find(e=>e.id===m.evid)?.name}</div>
              )}
              <div className="bub bub-user">{m.text}</div>
            </div>
          );
          return (
            <div key={i} style={{display:'flex',gap:8,alignItems:'flex-end'}}>
              <Avatar s={s} size={26}/>
              <div className="bub bub-sus">{m.text}</div>
            </div>
          );
        })}
        <div style={{height:8}}/>
      </div>

      {/* Suggested */}
      <div style={{padding:'8px 12px',borderTop:'1px solid var(--border)',background:'var(--surface)'}}>
        <div style={{display:'flex',gap:5,overflowX:'auto',paddingBottom:6}}>
          {data.suggested.map((q,i)=>(
            <button key={i} className="tap" onClick={()=>ask(q)} style={{
              padding:'7px 11px',background:'var(--surface-2)',border:'1px solid var(--border)',
              borderRadius:14,fontSize:11.5,color:'var(--text-dim)',whiteSpace:'nowrap',flexShrink:0
            }}>{q}</button>
          ))}
        </div>
        <div style={{display:'flex',gap:8,alignItems:'center',paddingTop:4}}>
          <button className="tap" onClick={()=>setShowEvid(true)} style={{
            width:36,height:36,borderRadius:8,background:'rgba(212,164,55,.10)',border:'1px solid var(--gold-dim)',
            display:'flex',alignItems:'center',justifyContent:'center',color:'var(--gold)',flexShrink:0
          }}>{I.doc}</button>
          <input className="ipt" placeholder="질문을 입력하세요…" value={input} onChange={e=>setInput(e.target.value)}
            onKeyDown={e=>{if(e.key==='Enter'&&input.trim())ask(input.trim())}} style={{flex:1}}/>
          <button className="tap" onClick={()=>input.trim()&&ask(input.trim())} style={{
            width:36,height:36,borderRadius:8,background:'var(--gold)',
            display:'flex',alignItems:'center',justifyContent:'center',color:'#1a1306',flexShrink:0
          }}>{I.send}</button>
        </div>
      </div>

      {showEvid && <EvidencePickerModal close={()=>setShowEvid(false)} onPick={present}/>}
    </div>
  );
}

function EvidencePickerModal({ close, onPick }) {
  const [q, setQ] = React.useState('');
  const list = EVIDENCE.filter(e => isUnlocked(e) && e.name.includes(q));
  return (
    <div style={{position:'absolute',inset:0,background:'rgba(0,0,0,.6)',zIndex:100,display:'flex',alignItems:'flex-end'}} onClick={close}>
      <div style={{
        background:'var(--surface)',width:'100%',borderRadius:'14px 14px 0 0',
        maxHeight:'80%',display:'flex',flexDirection:'column',
        boxShadow:'0 -10px 40px rgba(0,0,0,.4)'
      }} onClick={e=>e.stopPropagation()}>
        <div style={{padding:'14px 16px',borderBottom:'1px solid var(--border)',display:'flex',alignItems:'center',gap:10}}>
          <div style={{flex:1}}>
            <div className="label-kicker">증거 제시</div>
            <div className="serif" style={{fontSize:16,fontWeight:600,marginTop:2}}>어떤 증거로 추궁할까요?</div>
          </div>
          <button className="icon tap" onClick={close} style={{color:'var(--text-dim)'}}>{I.x}</button>
        </div>
        <div style={{padding:'10px 16px'}}>
          <input className="ipt" placeholder="증거 검색…" value={q} onChange={e=>setQ(e.target.value)}/>
        </div>
        <div style={{flex:1,overflowY:'auto',padding:'4px 12px 14px'}}>
          {list.map(e => (
            <div key={e.id} className="tap" style={{
              display:'flex',gap:10,alignItems:'center',padding:'10px 12px',
              borderRadius:8
            }} onClick={()=>onPick(e.id)}>
              <div style={{
                width:36,height:36,borderRadius:6,background:'var(--surface-2)',border:'1px solid var(--border)',
                display:'flex',alignItems:'center',justifyContent:'center',fontSize:17
              }}>{e.icon}</div>
              <div style={{flex:1,minWidth:0}}>
                <div style={{fontSize:13,fontWeight:500}}>{e.name}</div>
                <div className="mono" style={{fontSize:10,color:'var(--text-mute)',marginTop:2,letterSpacing:'.08em'}}>{e.loc}</div>
              </div>
              <ImpPill level={e.importance}/>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Hints modal ──────────────────────────
function HintsModal({ close }) {
  const [used, setUsed] = React.useState(GameState.hintsUsed);
  const useHint = (level) => {
    if (!used.includes(level)) {
      GameState.hintsUsed.push(level);
      setUsed([...GameState.hintsUsed]);
    }
  };
  return (
    <div style={{position:'absolute',inset:0,background:'rgba(0,0,0,.7)',zIndex:200,display:'flex',alignItems:'center',justifyContent:'center',padding:20}} onClick={close}>
      <div style={{
        background:'var(--surface)',width:'100%',borderRadius:14,
        border:'1px solid var(--border)',
        display:'flex',flexDirection:'column',maxHeight:'85%',overflow:'hidden'
      }} onClick={e=>e.stopPropagation()}>
        <div style={{padding:'16px',borderBottom:'1px solid var(--border)',display:'flex',alignItems:'center',gap:10}}>
          <div style={{color:'var(--gold)'}}>{I.bulb}</div>
          <div style={{flex:1}}>
            <div className="label-kicker">힌트 · HINTS</div>
            <div className="serif" style={{fontSize:15,fontWeight:600,marginTop:2}}>막혔다면 도움을 받으세요</div>
          </div>
          <button className="icon tap" onClick={close} style={{color:'var(--text-dim)'}}>{I.x}</button>
        </div>
        <div style={{flex:1,overflowY:'auto',padding:'14px 16px'}}>
          <div style={{padding:'10px 12px',background:'rgba(212,164,55,.06)',border:'1px solid var(--gold-dim)',borderRadius:7,marginBottom:14,fontSize:11.5,color:'var(--text-dim)',lineHeight:1.5}}>
            ⚠ 힌트 사용 시 최종 점수에서 차감됩니다.
          </div>
          {HINTS.map((h,i) => {
            const open = used.includes(h.level);
            const locked = i>0 && !used.includes(h.level-1);
            return (
              <div key={h.level} className="card" style={{padding:14,marginBottom:8,opacity:locked?.5:1}}>
                <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:8}}>
                  <div className="serif" style={{fontSize:14,fontWeight:600}}>힌트 {h.level}단계</div>
                  <span className="mono" style={{fontSize:10,color:'var(--crimson-soft)',letterSpacing:'.08em'}}>-{h.cost}점</span>
                </div>
                {open ? (
                  <div style={{fontSize:13,color:'var(--text)',lineHeight:1.55}}>{h.content}</div>
                ) : (
                  <Btn full size="sm" variant="ghost" disabled={locked} onClick={()=>useHint(h.level)}>
                    {locked?`${h.level-1}단계 사용 후 해금`:`힌트 ${h.level}단계 사용`}
                  </Btn>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ─── Final deduction submit ──────────────────────────
function SubmitScreen({ go }) {
  const [culprit, setCulprit] = React.useState('s1');
  const [motive, setMotive] = React.useState('');
  const [method, setMethod] = React.useState('');
  const [cover, setCover] = React.useState('');
  const [keyEv, setKeyEv] = React.useState([]);
  const toggleEv = (id) => setKeyEv(prev => prev.includes(id) ? prev.filter(x=>x!==id) : prev.length<3 ? [...prev,id] : prev);
  return (
    <div className="scr">
      <TopBar onBack={()=>go('dashboard')} title="최종 추리 제출" sub="FINAL DEDUCTION"/>
      <div className="scr-body" style={{padding:'14px 18px 110px'}}>
        <div style={{padding:'12px 14px',background:'rgba(212,74,74,.06)',border:'1px solid var(--crimson-dim)',borderRadius:8,marginBottom:18}}>
          <div className="mono" style={{fontSize:10.5,color:'var(--crimson-soft)',letterSpacing:'.12em',fontWeight:600}}>⚠ 단 한 번만 제출 가능</div>
          <div style={{fontSize:11.5,color:'var(--text-dim)',marginTop:4,lineHeight:1.5}}>제출 이후 수정할 수 없습니다. 신중하게 작성하세요.</div>
        </div>

        {/* Culprit picker */}
        <div className="label-kicker">01 · 범인 선택</div>
        <div style={{marginTop:8,display:'grid',gridTemplateColumns:'1fr 1fr',gap:6}}>
          {SUSPECTS.map(s => (
            <button key={s.id} className="tap" onClick={()=>setCulprit(s.id)} style={{
              display:'flex',gap:8,alignItems:'center',padding:'10px 11px',
              background: culprit===s.id?'var(--surface-3)':'var(--surface)',
              border: culprit===s.id?`1px solid ${s.accent}`:'1px solid var(--border)',
              borderRadius:8,textAlign:'left',
            }}>
              <Avatar s={s} size={28}/>
              <div style={{minWidth:0,flex:1}}>
                <div style={{fontSize:12,fontWeight:500,whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>{s.name}</div>
                <div className="mono" style={{fontSize:9,color:'var(--text-mute)',letterSpacing:'.06em'}}>{s.role.split(' /')[0]}</div>
              </div>
            </button>
          ))}
        </div>

        <div className="divider"/>

        <div className="label-kicker">02 · 범행 동기</div>
        <textarea className="ipt" placeholder="왜 범인이 이 사건을 저질렀는지 설명해주세요."
          value={motive} onChange={e=>setMotive(e.target.value)} style={{marginTop:8}}/>

        <div className="label-kicker" style={{marginTop:18}}>03 · 범행 방법</div>
        <textarea className="ipt" placeholder="어떻게 범행이 이루어졌는지 추론해주세요."
          value={method} onChange={e=>setMethod(e.target.value)} style={{marginTop:8}}/>

        <div className="label-kicker" style={{marginTop:18}}>04 · 은폐 방법</div>
        <textarea className="ipt" placeholder="범인이 어떻게 사실을 은폐하려 했는지 작성해주세요."
          value={cover} onChange={e=>setCover(e.target.value)} style={{marginTop:8}}/>

        <div className="divider"/>

        <div style={{display:'flex',alignItems:'flex-end',justifyContent:'space-between'}}>
          <div className="label-kicker">05 · 결정적 증거 3개</div>
          <span className="mono" style={{fontSize:11,color: keyEv.length===3?'var(--green)':'var(--gold)',letterSpacing:'.08em'}}>{keyEv.length}/3 선택</span>
        </div>
        <div style={{marginTop:8,display:'flex',flexDirection:'column',gap:6}}>
          {EVIDENCE.filter(e=>isUnlocked(e)).map(e => {
            const on = keyEv.includes(e.id);
            return (
              <button key={e.id} className="tap" onClick={()=>toggleEv(e.id)} style={{
                display:'flex',gap:10,alignItems:'center',padding:'9px 11px',width:'100%',textAlign:'left',
                background: on?'rgba(212,164,55,.10)':'var(--surface)',
                border: on?'1px solid var(--gold-dim)':'1px solid var(--border)',
                borderRadius:7,
              }}>
                <div style={{
                  width:18,height:18,borderRadius:4,
                  background: on?'var(--gold)':'transparent',
                  border: on?'none':'1px solid var(--border-strong)',
                  color:'#1a1306',display:'flex',alignItems:'center',justifyContent:'center'
                }}>{on && <Icon d="M20 6L9 17l-5-5" size={11} stroke={3}/>}</div>
                <div style={{fontSize:16}}>{e.icon}</div>
                <div style={{flex:1,minWidth:0}}>
                  <div style={{fontSize:12.5,fontWeight:500,whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>{e.name}</div>
                  <div className="mono" style={{fontSize:9.5,color:'var(--text-mute)',letterSpacing:'.06em'}}>{e.loc}</div>
                </div>
                <ImpPill level={e.importance}/>
              </button>
            );
          })}
        </div>
      </div>

      <div style={{position:'absolute',bottom:0,left:0,right:0,padding:'14px 18px',background:'linear-gradient(180deg,transparent,var(--stage) 30%)'}}>
        <Btn full size="lg" variant="crimson" icon={I.badge} onClick={()=>go('result')}
          disabled={keyEv.length<3 || !motive.trim() || !method.trim()}>
          추리 최종 제출
        </Btn>
      </div>
    </div>
  );
}

// ─── Result screen ──────────────────────────
function ResultScreen({ go }) {
  const culprit = SUSPECTS.find(s=>s.id===SOLUTION.culprit);
  return (
    <div className="scr">
      <TopBar onBack={()=>go('home')} title="사건 종결" sub="CASE CLOSED"/>
      <div className="scr-body" style={{padding:'0 0 30px'}}>
        {/* Grade banner */}
        <div style={{padding:'24px 22px 18px',textAlign:'center',
          background:`
            radial-gradient(ellipse 80% 60% at 50% 0%, rgba(212,164,55,.18), transparent 60%),
            transparent`
        }}>
          <div className="mono" style={{fontSize:10.5,letterSpacing:'.26em',color:'var(--text-mute)'}}>DETECTIVE GRADE</div>
          <div style={{position:'relative',display:'inline-block',marginTop:6}}>
            <div className="serif" style={{
              fontSize:96,fontWeight:700,color:'var(--gold)',lineHeight:1,
              textShadow:'0 0 40px rgba(212,164,55,.4)',
            }}>S</div>
            <div style={{position:'absolute',top:0,right:-26,top:-4}}>
              <Stamp color="var(--crimson)" tilt={8}>CLOSED</Stamp>
            </div>
          </div>
          <div className="serif" style={{fontSize:18,fontWeight:600,marginTop:8}}>특별 수사관 · INSPECTOR</div>
          <div style={{fontSize:12,color:'var(--text-dim)',marginTop:4}}>당신은 이 사건을 완전히 꿰뚫었습니다.</div>
        </div>

        {/* Score breakdown */}
        <div style={{padding:'0 18px'}}>
          <div className="card" style={{padding:0}}>
            <div style={{padding:'14px 16px',borderBottom:'1px solid var(--border)',display:'flex',justifyContent:'space-between',alignItems:'center'}}>
              <div className="label-kicker">채점 결과</div>
              <span className="serif" style={{fontSize:22,fontWeight:700,color:'var(--gold)'}}>92<span style={{fontSize:12,color:'var(--text-mute)'}}>/100</span></span>
            </div>
            <div style={{padding:'10px 16px'}}>
              {[
                ['범인 식별', 30, 30, true],
                ['범행 동기', 20, 20, true],
                ['범행 방법', 20, 18, true],
                ['결정적 증거 (3/3)', 30, 24, true],
              ].map(([l,max,got,ok],i)=>(
                <div key={i} style={{display:'flex',gap:10,alignItems:'center',padding:'7px 0',borderBottom:i<3?'1px solid var(--border)':'none'}}>
                  <div style={{color: ok?'var(--green)':'var(--crimson)',flexShrink:0}}>
                    {ok ? <Icon d="M20 6L9 17l-5-5" size={14} stroke={2.4}/> : <Icon d={["M18 6L6 18","M6 6l12 12"]} size={14} stroke={2.4}/>}
                  </div>
                  <div style={{fontSize:12.5,flex:1}}>{l}</div>
                  <div className="mono" style={{fontSize:12,color:'var(--text-dim)'}}>{got}/{max}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Culprit reveal */}
        <div style={{padding:'18px 18px 0'}}>
          <div className="label-kicker">진범 공개 · CULPRIT REVEALED</div>
          <div className="card" style={{marginTop:8,padding:14,background:'linear-gradient(135deg, rgba(212,74,74,.08), var(--surface))',border:'1px solid var(--crimson-dim)'}}>
            <div style={{display:'flex',gap:12,alignItems:'center'}}>
              <Avatar s={culprit} size={48}/>
              <div style={{flex:1}}>
                <Stamp color="var(--crimson)" tilt={-2}>CULPRIT</Stamp>
                <div className="serif" style={{fontSize:18,fontWeight:600,marginTop:5}}>{culprit.name}</div>
                <div style={{fontSize:11.5,color:'var(--text-dim)',marginTop:1}}>{culprit.role}</div>
              </div>
            </div>
            <div style={{marginTop:12,paddingTop:12,borderTop:'1px solid var(--crimson-dim)'}}>
              <div className="label-kicker" style={{color:'var(--crimson-soft)'}}>MOTIVE · 동기</div>
              <p style={{fontSize:13,lineHeight:1.6,color:'var(--text)',marginTop:6}}>{SOLUTION.motive}</p>
            </div>
          </div>
        </div>

        {/* Reconstruction */}
        <div style={{padding:'18px 18px 0'}}>
          <div className="label-kicker">범행 재구성 · RECONSTRUCTION</div>
          <div style={{marginTop:8,display:'flex',flexDirection:'column',gap:8}}>
            <ReconStep t="22:05" l="박재민 — 인턴(김나은)을 시켜 카페에서 아몬드라떼를 구매. 본인은 ‘오트라떼’와 ‘아몬드라떼’를 혼동하지 않도록 미리 라벨을 뜯어 표시."/>
            <ReconStep t="22:15" l="박재민 6층 데모룸에 입실. 피해자에게 ‘오트라떼’라 속이고 아몬드라떼를 건넴."/>
            <ReconStep t="22:27" l="피해자 알레르기 발작. 박재민은 미리 빼돌린 에피펜을 사용하지 않고 시간을 끌어 사망에 이르게 함."/>
            <ReconStep t="22:33–55" l="CCTV 사각지대를 통해 재무팀 사무실로 이동, 에피펜을 캐비닛에 숨김. 회계 파일을 ‘기타 지출’ 항목으로 USB 수정."/>
            <ReconStep t="23:12" t2="alibi" l="피해자 휴대폰으로 단톡방에 두 번째 메시지 발송 — 사망 시각 알리바이 조작."/>
          </div>
        </div>

        {/* Key evidence */}
        <div style={{padding:'18px 18px 0'}}>
          <div className="label-kicker">결정적 증거 해설</div>
          <div style={{marginTop:8,display:'flex',flexDirection:'column',gap:6}}>
            {SOLUTION.keyEvids.map(eid => {
              const e = EVIDENCE.find(x=>x.id===eid);
              return (
                <div key={eid} className="card evidence" style={{padding:12}}>
                  <div style={{display:'flex',gap:10,alignItems:'center'}}>
                    <span style={{fontSize:22}}>{e.icon}</span>
                    <div style={{flex:1}}>
                      <div style={{fontSize:13,fontWeight:500}}>{e.name}</div>
                      <div className="mono" style={{fontSize:10,color:'var(--text-mute)',letterSpacing:'.08em'}}>{e.loc}</div>
                    </div>
                    <ImpPill level={e.importance}/>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div style={{padding:'22px 18px 0'}}>
          <div className="label-kicker">놓친 부분</div>
          <div style={{marginTop:8,padding:12,background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8}}>
            <div style={{fontSize:12.5,color:'var(--text-dim)',lineHeight:1.6}}>
              범행 방법 설명에서 ‘오트라떼-아몬드라떼’ 라벨 트릭을 명시하지 않았습니다. 다음 사건에서는 음료를 바꿔치기한 방식까지 명확히 짚어보세요.
            </div>
          </div>
        </div>

        <div style={{padding:'22px 18px 0',display:'flex',gap:8}}>
          <Btn variant="ghost" full onClick={()=>go('home')}>홈으로</Btn>
          <Btn variant="solid" full icon={I.arrow} onClick={()=>go('library')}>다음 사건 보기</Btn>
        </div>
        <div style={{padding:'10px 18px 0'}}>
          <Btn variant="outline" full icon={I.star}>이 사건에 리뷰 남기기</Btn>
        </div>
      </div>
    </div>
  );
}

function ReconStep({ t, t2, l }) {
  return (
    <div style={{display:'flex',gap:10,alignItems:'flex-start',padding:'10px 12px',background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8}}>
      <span className="mono" style={{fontSize:12,fontWeight:600,color: t2==='alibi'?'var(--crimson-soft)':'var(--gold)',flexShrink:0,paddingTop:1,letterSpacing:'.04em'}}>{t}</span>
      <div style={{fontSize:12.5,color:'var(--text)',lineHeight:1.55,flex:1}}>{l}</div>
    </div>
  );
}

Object.assign(window, {
  DashboardScreen, EvidenceDetailScreen, SuspectDetailScreen,
  InterrogationScreen, SubmitScreen, ResultScreen, HintsModal,
});
