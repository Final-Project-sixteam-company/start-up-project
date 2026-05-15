// CaseLab AI — Main screens: Splash, Onboarding, Home, Library, Detail, Briefing, Records, Me

// ─── Splash ──────────────────────────
function SplashScreen({ go }) {
  React.useEffect(() => {
    const t = setTimeout(() => go('onboarding'), 2200);
    return () => clearTimeout(t);
  }, []);
  return (
    <div className="scr" style={{
      background:`
        radial-gradient(ellipse 70% 50% at 50% 35%, rgba(212,164,55,.10), transparent 60%),
        radial-gradient(ellipse 50% 40% at 50% 70%, rgba(212,74,74,.06), transparent 60%),
        var(--stage)`,
      alignItems:'center',justifyContent:'center'
    }}>
      <div style={{flex:1, display:'flex',flexDirection:'column',alignItems:'center',justifyContent:'center',gap:24}}>
        {/* Logo mark */}
        <div style={{position:'relative',width:120,height:120}}>
          <svg viewBox="0 0 120 120" width="120" height="120">
            <circle cx="60" cy="60" r="46" fill="none" stroke="var(--gold)" strokeWidth="1.5" opacity="0.4"/>
            <circle cx="60" cy="60" r="38" fill="none" stroke="var(--gold)" strokeWidth="1" opacity="0.2"/>
            {/* Crosshair */}
            <line x1="60" y1="6" x2="60" y2="18" stroke="var(--gold)" strokeWidth="1.5"/>
            <line x1="60" y1="102" x2="60" y2="114" stroke="var(--gold)" strokeWidth="1.5"/>
            <line x1="6" y1="60" x2="18" y2="60" stroke="var(--gold)" strokeWidth="1.5"/>
            <line x1="102" y1="60" x2="114" y2="60" stroke="var(--gold)" strokeWidth="1.5"/>
            {/* Inner monogram */}
            <text x="60" y="72" textAnchor="middle" fontFamily="Noto Serif KR" fontSize="38" fontWeight="700" fill="var(--gold)">CL</text>
          </svg>
        </div>
        <div style={{textAlign:'center'}}>
          <div className="serif" style={{fontSize:32, fontWeight:600, letterSpacing:'.02em'}}>
            CaseLab <span style={{color:'var(--gold)'}}>AI</span>
          </div>
          <div className="mono pulse" style={{fontSize:10.5, letterSpacing:'.26em', color:'var(--text-mute)', marginTop:8}}>
            INVESTIGATING · 조사 준비 중
          </div>
        </div>
      </div>
      <div style={{padding:'0 0 36px', textAlign:'center'}}>
        <div className="mono" style={{fontSize:9.5, letterSpacing:'.22em', color:'var(--text-mute)'}}>
          A DETECTIVE GAME · v0.9.2
        </div>
      </div>
    </div>
  );
}

// ─── Onboarding ──────────────────────────
function OnboardingScreen({ go }) {
  const [step, setStep] = React.useState(0);
  const slides = [
    { num:'01', t:'사건을 선택하세요', sub:'공식 시나리오와 유저 커스텀, 매일 새로운 사건이 추가됩니다.', glyph:'doc' },
    { num:'02', t:'증거를 분석하세요', sub:'현장 정보·증거 카드·타임라인을 조합해 모순을 찾아냅니다.', glyph:'eye' },
    { num:'03', t:'AI 용의자를 심문하세요', sub:'자유 질문과 증거 제시로 용의자를 추궁합니다.', glyph:'chat' },
    { num:'04', t:'최종 추리를 제출하세요', sub:'범인·동기·범행방법·결정적 증거 3개를 제출합니다.', glyph:'badge' },
    { num:'05', t:'직접 사건을 만들고 공유하세요', sub:'AI 보조로 시나리오를 제작하고 다른 탐정들에게 공유합니다.', glyph:'spark' },
  ];
  const s = slides[step];
  const last = step === slides.length-1;
  return (
    <div className="scr">
      <div style={{padding:'16px 18px',display:'flex',justifyContent:'space-between',alignItems:'center'}}>
        <div className="mono" style={{fontSize:10.5,letterSpacing:'.22em',color:'var(--text-mute)'}}>ONBOARDING</div>
        <button className="tap" style={{color:'var(--text-dim)',fontSize:12.5}} onClick={() => go('home')}>건너뛰기</button>
      </div>
      <div className="scr-body" style={{padding:'24px 26px 0',display:'flex',flexDirection:'column',gap:28}}>
        {/* big number */}
        <div style={{paddingTop:30}}>
          <div className="serif" style={{fontSize:84, lineHeight:1, color:'var(--gold)', fontWeight:600, opacity:.9}}>{s.num}</div>
          <div className="mono" style={{fontSize:10, letterSpacing:'.22em', color:'var(--text-mute)', marginTop:8}}>STEP {step+1} / {slides.length}</div>
        </div>

        {/* visual */}
        <div style={{
          height:160, borderRadius:14, border:'1px solid var(--border)',
          background:'linear-gradient(135deg,#161823,#0e1018)',
          display:'flex',alignItems:'center',justifyContent:'center', color:'var(--gold)',position:'relative',
        }}>
          <div style={{position:'absolute',inset:8,border:'1px dashed var(--border-strong)',borderRadius:10}}/>
          <div style={{transform:'scale(2.2)'}}>{I[s.glyph]}</div>
        </div>

        <div>
          <div className="serif" style={{fontSize:24, fontWeight:600, lineHeight:1.3, letterSpacing:'.005em'}}>{s.t}</div>
          <div style={{fontSize:14, color:'var(--text-dim)', marginTop:10, lineHeight:1.6}}>{s.sub}</div>
        </div>
      </div>
      <div style={{padding:'18px 22px 22px'}}>
        <div style={{display:'flex',gap:5,marginBottom:18,justifyContent:'center'}}>
          {slides.map((_,i)=>(
            <div key={i} style={{
              height:3, width: i===step?28:8, borderRadius:2,
              background: i===step?'var(--gold)':'var(--surface-3)', transition:'all .3s'
            }}/>
          ))}
        </div>
        <Btn full size="lg" variant="solid" onClick={() => last ? go('home') : setStep(step+1)}>
          {last ? '탐정 등록 완료' : '다음'}
        </Btn>
      </div>
    </div>
  );
}

// ─── Home ──────────────────────────
function HomeScreen({ go }) {
  return (
    <div className="scr">
      <div className="topbar" style={{paddingTop:16}}>
        <div style={{flex:1}}>
          <div className="mono" style={{fontSize:9.5,letterSpacing:'.22em',color:'var(--text-mute)'}}>DETECTIVE</div>
          <div className="serif" style={{fontSize:16.5, fontWeight:600,marginTop:1}}>안녕, <span style={{color:'var(--gold)'}}>탐정 K</span></div>
        </div>
        <button className="icon tap">{I.search}</button>
        <button className="icon tap">{I.bell}</button>
      </div>

      <div className="scr-body" style={{paddingBottom:90}}>
        {/* Today's case (hero) */}
        <div style={{padding:'14px 18px 8px'}}>
          <div className="label-kicker" style={{marginBottom:8}}>오늘의 추천 사건 · TODAY'S BRIEF</div>
          <div className="card tap" style={{padding:0,overflow:'hidden'}} onClick={()=>go('detail')}>
            <CaseArt h={150} tone="gold" code={CASE.code} label={CASE.title}/>
            <div style={{padding:'14px 16px 16px'}}>
              <div style={{display:'flex',alignItems:'center',gap:8,marginBottom:6}}>
                <Stamp color="var(--crimson)">OFFICIAL</Stamp>
                <Badge color="var(--gold-soft)">{CASE.diffLabel}</Badge>
                <span className="mono" style={{fontSize:10.5,color:'var(--text-mute)',letterSpacing:'.12em'}}>{CASE.estMin}분</span>
              </div>
              <div className="serif" style={{fontSize:18,fontWeight:600,marginBottom:6}}>{CASE.title}</div>
              <div style={{fontSize:12.5,color:'var(--text-dim)',lineHeight:1.55}}>
                데모데이 전야, 스타트업 대표의 죽음. 알레르기 사고로 위장된 살인의 진실을 밝혀라.
              </div>
              <div style={{display:'flex',gap:12,marginTop:12,paddingTop:12,borderTop:'1px solid var(--border)'}}>
                <Stat n={CASE.suspectsN} l="용의자"/>
                <Stat n={CASE.evidenceN} l="증거"/>
                <Stat n={CASE.rating} l="평점" gold/>
                <Stat n={'12.4K'} l="플레이"/>
              </div>
            </div>
          </div>
        </div>

        {/* Resume */}
        <SectionTitle kicker="이어하기 · RESUME" title="진행 중인 사건" />
        <div style={{padding:'0 18px'}}>
          <div className="card tap" style={{display:'flex',alignItems:'center',gap:12}} onClick={()=>go('dashboard')}>
            <div style={{
              width:48,height:48,borderRadius:8,
              background:'linear-gradient(135deg,#1d1a0e,#0a0b10)',
              border:'1px solid var(--gold-dim)',
              display:'flex',alignItems:'center',justifyContent:'center',
              fontFamily:'var(--serif)',fontSize:20,color:'var(--gold)'
            }}>CL</div>
            <div style={{flex:1, minWidth:0}}>
              <div className="mono" style={{fontSize:10,letterSpacing:'.16em',color:'var(--text-mute)'}}>CL-001 · 진행 62%</div>
              <div style={{fontSize:14, fontWeight:500, marginTop:2}}>{CASE.title}</div>
              <div style={{height:3,background:'var(--surface-3)',borderRadius:2,marginTop:8,overflow:'hidden'}}>
                <div style={{height:'100%',width:'62%',background:'var(--gold)',borderRadius:2}}/>
              </div>
            </div>
            <div style={{color:'var(--gold)'}}>{I.arrow}</div>
          </div>
        </div>

        {/* Popular custom */}
        <SectionTitle kicker="POPULAR · CUSTOM" title="인기 커스텀 시나리오" action={
          <button className="tap" style={{color:'var(--text-dim)',fontSize:12}} onClick={()=>go('library')}>전체 보기 →</button>
        }/>
        <div className="hscroll">
          {OTHER_SCENARIOS.filter(s=>s.type==='CUSTOM').map(s => (
            <ScenarioCardCompact key={s.id} s={s} onClick={()=>go('detail')}/>
          ))}
        </div>

        {/* Official lineup */}
        <SectionTitle kicker="OFFICIAL · MAIN CASES" title="공식 시나리오 라인업"/>
        <div style={{padding:'0 18px',display:'flex',flexDirection:'column',gap:10}}>
          {OTHER_SCENARIOS.filter(s=>s.type==='OFFICIAL').map(s => (
            <ScenarioRow key={s.id} s={s} onClick={()=>go('detail')}/>
          ))}
        </div>

        {/* Create CTA */}
        <div style={{padding:'18px'}}>
          <div className="tap" style={{
            padding:'18px 18px',
            borderRadius:12,
            background:'linear-gradient(135deg, rgba(212,164,55,.10), rgba(212,164,55,.02))',
            border:'1px dashed var(--gold-dim)',
            display:'flex',alignItems:'center',gap:14
          }} onClick={()=>go('builder')}>
            <div style={{
              width:42,height:42,borderRadius:10,background:'rgba(212,164,55,.14)',
              display:'flex',alignItems:'center',justifyContent:'center',color:'var(--gold)'
            }}>{I.spark}</div>
            <div style={{flex:1}}>
              <div className="serif" style={{fontSize:14.5,fontWeight:600}}>내 사건을 만들어보세요</div>
              <div style={{fontSize:12,color:'var(--text-dim)',marginTop:2}}>AI가 시나리오 초안부터 검증까지 도와드립니다.</div>
            </div>
            <div style={{color:'var(--gold)'}}>{I.arrow}</div>
          </div>
        </div>
      </div>

      <BottomNav active="home" onGo={go}/>
    </div>
  );
}

function Stat({ n, l, gold }) {
  return (
    <div>
      <div className="mono" style={{fontSize:14,fontWeight:600, color: gold?'var(--gold)':'var(--text)'}}>
        {gold && '★ '}{n}
      </div>
      <div style={{fontSize:10.5,color:'var(--text-mute)',marginTop:1,letterSpacing:'.04em'}}>{l}</div>
    </div>
  );
}

function ScenarioCardCompact({ s, onClick }) {
  return (
    <div className="card tap" style={{width:200,padding:0,overflow:'hidden'}} onClick={onClick}>
      <CaseArt h={90} tone={s.diffEn==='HARD'?'crimson':s.diffEn==='EASY'?'blue':'gold'} code={s.code} label={s.title.slice(0,8)}/>
      <div style={{padding:'10px 12px'}}>
        <div style={{display:'flex',gap:6,marginBottom:5}}>
          <Difficulty level={s.diffEn}/>
          <span className="mono" style={{fontSize:9.5,color:'var(--text-mute)',letterSpacing:'.12em'}}>{s.min}분</span>
        </div>
        <div style={{fontSize:12.5,fontWeight:500,lineHeight:1.4,whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>{s.title}</div>
        {s.author && <div className="mono" style={{fontSize:10,color:'var(--text-mute)',marginTop:3}}>{s.author}</div>}
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginTop:6}}>
          <span style={{fontSize:11,color:'var(--gold)'}}>★ {s.rating}</span>
          <span className="mono" style={{fontSize:9.5,color:'var(--text-mute)',letterSpacing:'.1em'}}>{s.plays>=1000?(s.plays/1000).toFixed(1)+'K':s.plays} PLAYS</span>
        </div>
      </div>
    </div>
  );
}

function ScenarioRow({ s, onClick }) {
  return (
    <div className="card tap" style={{display:'flex',gap:12,alignItems:'center',padding:12}} onClick={onClick}>
      <div style={{
        width:48,height:60,borderRadius:6,flexShrink:0,
        background:`linear-gradient(135deg, ${s.diffEn==='HARD'?'#1d0e0e':s.diffEn==='EASY'?'#0e151d':'#1d1a0e'} 0%, #0a0b10 100%)`,
        border:'1px solid var(--border)',
        display:'flex',alignItems:'center',justifyContent:'center',
        fontFamily:'var(--mono)',fontSize:9,color:'var(--gold)',letterSpacing:'.1em'
      }}>{s.code.split('-')[0]}<br/>{s.code.split('-')[1]}</div>
      <div style={{flex:1,minWidth:0}}>
        <div style={{display:'flex',gap:6,alignItems:'center',marginBottom:3}}>
          <Difficulty level={s.diffEn}/>
          <span className="mono" style={{fontSize:10,color:'var(--text-mute)',letterSpacing:'.1em'}}>{s.min}분 · 용의자 {s.sus}</span>
        </div>
        <div style={{fontSize:13.5,fontWeight:500,whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>{s.title}</div>
        <div style={{display:'flex',gap:5,marginTop:5}}>
          {s.tags.slice(0,2).map(t => <Badge key={t} mono={false}>{t}</Badge>)}
        </div>
      </div>
      <div style={{textAlign:'right'}}>
        <div style={{fontSize:12,color:'var(--gold)'}}>★ {s.rating}</div>
        <div className="mono" style={{fontSize:9.5,color:'var(--text-mute)',marginTop:3,letterSpacing:'.1em'}}>{s.plays>=1000?(s.plays/1000).toFixed(1)+'K':s.plays}</div>
      </div>
    </div>
  );
}

// ─── Library ──────────────────────────
function LibraryScreen({ go }) {
  const [filter, setFilter] = React.useState('전체');
  const filters = ['전체','공식','커스텀','인기','최신','쉬움','보통','어려움','1인용','협력용'];
  const all = [{...CASE, type:'OFFICIAL', code:CASE.code, diff:CASE.diffLabel, diffEn:'HARD', min:CASE.estMin, sus:CASE.suspectsN, ev:CASE.evidenceN, rating:CASE.rating, plays:CASE.plays, tags:CASE.tags}, ...OTHER_SCENARIOS];
  const list = filter==='전체' ? all :
               filter==='공식' ? all.filter(s=>s.type==='OFFICIAL') :
               filter==='커스텀' ? all.filter(s=>s.type==='CUSTOM') :
               ['쉬움','보통','어려움'].includes(filter) ? all.filter(s=>s.diff===filter) :
               all;
  return (
    <div className="scr">
      <TopBar onBack={()=>go('home')} title="시나리오 라이브러리" sub="MYSTERY LIBRARY"
        right={<button className="icon tap">{I.filter}</button>}/>

      <div style={{padding:'12px 18px 8px'}}>
        <div style={{position:'relative'}}>
          <div style={{position:'absolute',left:12,top:'50%',transform:'translateY(-50%)',color:'var(--text-mute)'}}>{I.search}</div>
          <input className="ipt" style={{paddingLeft:38}} placeholder="사건명, 태그, 제작자 검색…"/>
        </div>
      </div>

      <div style={{display:'flex',gap:7,overflowX:'auto',padding:'4px 18px 12px'}}>
        {filters.map(f => (
          <button key={f} className="tap" onClick={()=>setFilter(f)} style={{
            padding:'6px 12px',borderRadius:20,whiteSpace:'nowrap',fontSize:12,
            background: filter===f?'var(--gold)':'var(--surface-2)',
            color: filter===f?'#1a1306':'var(--text-dim)',
            border: filter===f?'none':'1px solid var(--border)',
            fontWeight: filter===f?600:400,
          }}>{f}</button>
        ))}
      </div>

      <div className="scr-body" style={{padding:'0 18px 90px',display:'flex',flexDirection:'column',gap:10}}>
        {list.map((s,i)=>(
          <ScenarioRow key={s.id||i} s={s} onClick={()=>go('detail')}/>
        ))}
      </div>

      <BottomNav active="library" onGo={go}/>
    </div>
  );
}

// ─── Scenario Detail ──────────────────────────
function ScenarioDetailScreen({ go }) {
  const [book, setBook] = React.useState(false);
  return (
    <div className="scr">
      <TopBar onBack={()=>go('home')} title=" " sub={CASE.code} transparent
        right={
          <>
            <button className="icon tap" onClick={()=>setBook(!book)} style={{color:book?'var(--gold)':'var(--text-dim)'}}>
              {book?I.bookmarkOn:I.bookmark}
            </button>
            <button className="icon tap">{I.more}</button>
          </>
        }/>
      <div className="scr-body">
        {/* Hero */}
        <div style={{padding:'0 18px 16px'}}>
          <CaseArt h={200} tone="gold" code={CASE.code} label={CASE.title}/>
        </div>

        <div style={{padding:'0 18px'}}>
          <div style={{display:'flex',gap:6,marginBottom:8,alignItems:'center'}}>
            <Stamp color="var(--crimson)">OFFICIAL</Stamp>
            <Stamp color="var(--gold)" tilt={2}>HARD</Stamp>
          </div>
          <h1 className="serif" style={{fontSize:24,fontWeight:700,letterSpacing:'.01em',lineHeight:1.25,margin:'0 0 6px'}}>
            {CASE.title}
          </h1>
          <div style={{color:'var(--text-dim)',fontSize:13}}>{CASE.subtitle} · {CASE.date}</div>

          {/* meta grid */}
          <div style={{
            display:'grid',gridTemplateColumns:'1fr 1fr 1fr 1fr',gap:1,marginTop:16,
            background:'var(--border)',border:'1px solid var(--border)',borderRadius:10,overflow:'hidden'
          }}>
            {[
              { l:'난이도', v:CASE.diffLabel, c:'var(--gold)' },
              { l:'플레이', v:`${CASE.estMin}분`, c:'var(--text)' },
              { l:'용의자', v:CASE.suspectsN, c:'var(--text)' },
              { l:'증거', v:CASE.evidenceN, c:'var(--text)' },
            ].map((m,i)=>(
              <div key={i} style={{background:'var(--surface)',padding:'12px 8px',textAlign:'center'}}>
                <div className="mono" style={{fontSize:9.5,letterSpacing:'.12em',color:'var(--text-mute)'}}>{m.l}</div>
                <div className="serif" style={{fontSize:17,fontWeight:600,color:m.c,marginTop:4}}>{m.v}</div>
              </div>
            ))}
          </div>

          {/* synopsis */}
          <div style={{marginTop:22}}>
            <div className="label-kicker">시놉시스 · SYNOPSIS</div>
            <p style={{fontSize:13.5,lineHeight:1.65,color:'var(--text-dim)',marginTop:8,whiteSpace:'pre-line'}}>
              {CASE.briefing}
            </p>
          </div>

          {/* tags */}
          <div style={{marginTop:18, display:'flex',flexWrap:'wrap',gap:6}}>
            {CASE.tags.map(t => <Badge key={t} mono={false}>#{t}</Badge>)}
          </div>

          {/* Rating */}
          <div style={{marginTop:22}}>
            <div style={{display:'flex',alignItems:'flex-end',justifyContent:'space-between',marginBottom:10}}>
              <div className="label-kicker">평점 · REVIEWS</div>
              <div className="mono" style={{fontSize:11,color:'var(--text-mute)'}}>{CASE.plays.toLocaleString()} 플레이</div>
            </div>
            <div style={{display:'flex',gap:14,alignItems:'center'}}>
              <div className="serif" style={{fontSize:36,fontWeight:600,color:'var(--gold)'}}>{CASE.rating}</div>
              <div style={{flex:1}}>
                {[5,4,3,2,1].map(s => (
                  <div key={s} style={{display:'flex',alignItems:'center',gap:8,fontSize:10}}>
                    <span className="mono" style={{color:'var(--text-mute)',width:14}}>{s}★</span>
                    <div style={{flex:1,height:3,background:'var(--surface-3)',borderRadius:2,overflow:'hidden'}}>
                      <div style={{height:'100%',width:`${s===5?78:s===4?15:s===3?5:s===2?1:1}%`,background:'var(--gold)'}}/>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div style={{marginTop:14,display:'flex',flexDirection:'column',gap:10}}>
              <Review n="@hawk_eye" r={5} t="3일 전" body="시간 조작 트릭이 진짜 통쾌함. 에피펜 위치에서 소름 돋았어요."/>
              <Review n="@noir_42" r={4} t="1주 전" body="용의자 5명 다 매력있고, 심문할 때 박재민이 진짜 거짓말쟁이 같음 ㅋㅋ" spoiler/>
            </div>
          </div>
        </div>

        <div style={{height:120}}/>
      </div>

      {/* Sticky CTA */}
      <div style={{
        padding:'14px 18px',background:'linear-gradient(180deg,transparent,var(--stage) 30%)',
        position:'absolute',bottom:0,left:0,right:0,
        display:'flex',gap:10
      }}>
        <Btn variant="ghost" onClick={()=>setBook(!book)} icon={book?I.bookmarkOn:I.bookmark} style={{flexShrink:0}}>
          {book?'저장됨':'저장'}
        </Btn>
        <Btn full size="lg" onClick={()=>go('briefing')} icon={I.arrow}>
          조사 시작
        </Btn>
      </div>
    </div>
  );
}

function Review({ n, r, t, body, spoiler }) {
  const [open, setOpen] = React.useState(false);
  return (
    <div className="card" style={{padding:12}}>
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:5}}>
        <div style={{display:'flex',gap:8,alignItems:'center'}}>
          <span className="mono" style={{fontSize:11,color:'var(--text)',letterSpacing:'.04em'}}>{n}</span>
          <span style={{color:'var(--gold)',fontSize:11}}>{'★'.repeat(r)}{'☆'.repeat(5-r)}</span>
        </div>
        <span className="mono" style={{fontSize:9.5,color:'var(--text-mute)',letterSpacing:'.08em'}}>{t}</span>
      </div>
      {spoiler && !open ? (
        <button className="tap" onClick={()=>setOpen(true)} style={{
          width:'100%',background:'var(--surface-2)',padding:'10px',borderRadius:6,border:'1px dashed var(--border-strong)',
          color:'var(--text-mute)',fontSize:11.5,fontFamily:'var(--mono)',letterSpacing:'.04em'
        }}>⚠ SPOILER · 탭하여 펼치기</button>
      ) : (
        <div style={{fontSize:12.5,color:'var(--text-dim)',lineHeight:1.5}}>{body}</div>
      )}
    </div>
  );
}

// ─── Briefing ──────────────────────────
function BriefingScreen({ go }) {
  return (
    <div className="scr">
      <TopBar onBack={()=>go('detail')} title="사건 브리핑" sub="CASE BRIEFING"/>
      <div className="scr-body" style={{padding:'0 0 100px'}}>
        {/* Header art */}
        <div style={{padding:'14px 18px 0'}}>
          <div style={{
            border:'1px solid var(--border)',borderRadius:10,padding:'18px',
            background:'linear-gradient(180deg, rgba(212,74,74,.06), transparent)',
            position:'relative'
          }}>
            <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:14}}>
              <Stamp color="var(--crimson)">CONFIDENTIAL</Stamp>
              <span className="mono" style={{fontSize:10,color:'var(--text-mute)',letterSpacing:'.12em'}}>{CASE.code}</span>
            </div>
            <div className="mono" style={{fontSize:10,letterSpacing:'.22em',color:'var(--text-mute)'}}>CASE FILE</div>
            <div className="serif" style={{fontSize:22,fontWeight:600,marginTop:4,lineHeight:1.3}}>{CASE.title}</div>
            <div className="mono" style={{fontSize:10,letterSpacing:'.12em',color:'var(--text-mute)',marginTop:8}}>
              {CASE.date} · {CASE.victim.foundAt}
            </div>
          </div>
        </div>

        {/* Briefing text */}
        <div style={{padding:'20px 18px 0'}}>
          <div className="label-kicker">개요 · OVERVIEW</div>
          <p style={{fontSize:13.5,lineHeight:1.7,color:'var(--text)',marginTop:8,whiteSpace:'pre-line'}}>
            {CASE.briefing}
          </p>
        </div>

        {/* Victim */}
        <div style={{padding:'20px 18px 0'}}>
          <div className="label-kicker">피해자 · VICTIM</div>
          <div className="card" style={{marginTop:8,display:'flex',gap:12,padding:14}}>
            <div style={{
              width:56,height:56,borderRadius:6,flexShrink:0,
              background:'linear-gradient(135deg,#2a0f0f,#0a0b10)',
              border:'1px solid var(--crimson-dim)',
              display:'flex',alignItems:'center',justifyContent:'center',
              fontFamily:'var(--serif)',fontSize:22,color:'var(--crimson-soft)'
            }}>강</div>
            <div style={{flex:1}}>
              <div className="serif" style={{fontSize:16,fontWeight:600}}>{CASE.victim.name} <span style={{color:'var(--text-mute)',fontSize:12,fontWeight:400}}>· {CASE.victim.age}세</span></div>
              <div style={{fontSize:12,color:'var(--text-dim)',marginTop:2}}>{CASE.victim.role}</div>
              <div style={{marginTop:8,fontSize:11.5,color:'var(--text-mute)',display:'flex',flexDirection:'column',gap:3}}>
                <div><span style={{color:'var(--text-dim)'}}>사인:</span> {CASE.victim.cause}</div>
                <div><span style={{color:'var(--text-dim)'}}>발견:</span> {CASE.victim.foundAt}</div>
                <div><span style={{color:'var(--crimson-soft)'}}>※</span> {CASE.victim.allergy}</div>
              </div>
            </div>
          </div>
        </div>

        {/* Goals */}
        <div style={{padding:'20px 18px 0'}}>
          <div className="label-kicker">탐정 목표 · OBJECTIVES</div>
          <div style={{marginTop:8}}>
            {CASE.goals.map((g,i) => (
              <div key={i} style={{display:'flex',gap:10,padding:'10px 0',borderBottom:'1px solid var(--border)'}}>
                <div className="mono" style={{
                  width:20,height:20,borderRadius:'50%',border:'1px solid var(--gold-dim)',
                  display:'flex',alignItems:'center',justifyContent:'center',
                  fontSize:9.5,color:'var(--gold)',flexShrink:0,letterSpacing:'.06em'
                }}>0{i+1}</div>
                <div style={{fontSize:13,color:'var(--text)',lineHeight:1.5}}>{g}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Initial evidence preview */}
        <div style={{padding:'20px 18px 0'}}>
          <div className="label-kicker">초기 공개 증거 · INITIAL EVIDENCE</div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8,marginTop:8}}>
            {EVIDENCE.filter(e=>e.initial).slice(0,4).map(e => (
              <div key={e.id} className="card evidence" style={{padding:10}}>
                <div style={{fontSize:20,marginBottom:6}}>{e.icon}</div>
                <div style={{fontSize:11.5,fontWeight:500,lineHeight:1.3}}>{e.name}</div>
                <div className="mono" style={{fontSize:9.5,color:'var(--text-mute)',marginTop:3,letterSpacing:'.08em'}}>{e.loc}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div style={{position:'absolute',bottom:0,left:0,right:0,padding:'14px 18px',
        background:'linear-gradient(180deg,transparent,var(--stage) 30%)'}}>
        <Btn full size="lg" onClick={()=>go('dashboard')} icon={I.eye}>조사 시작</Btn>
      </div>
    </div>
  );
}

// ─── Records ──────────────────────────
function RecordsScreen({ go }) {
  const [tab, setTab] = React.useState('all');
  const tabs = [{id:'all',l:'전체'},{id:'done',l:'완료'},{id:'wip',l:'진행 중'},{id:'mine',l:'내 시나리오'}];
  return (
    <div className="scr">
      <TopBar onBack={()=>go('home')} title="내 기록" sub="DETECTIVE FILE"/>
      <div className="scr-body" style={{paddingBottom:90}}>
        {/* Detective rank */}
        <div style={{padding:'16px 18px 0'}}>
          <div className="card" style={{padding:18,background:'linear-gradient(135deg, rgba(212,164,55,.10), var(--surface) 70%)'}}>
            <div style={{display:'flex',alignItems:'center',gap:14}}>
              <div style={{
                width:64,height:64,borderRadius:12,
                background:'rgba(212,164,55,.12)',border:'1px solid var(--gold-dim)',
                display:'flex',alignItems:'center',justifyContent:'center',
                fontFamily:'var(--serif)',fontSize:36,fontWeight:700,color:'var(--gold)'
              }}>A</div>
              <div style={{flex:1}}>
                <div className="mono" style={{fontSize:10,letterSpacing:'.18em',color:'var(--text-mute)'}}>CURRENT RANK</div>
                <div className="serif" style={{fontSize:18,fontWeight:600,marginTop:2}}>주임 탐정 · ASSOCIATE</div>
                <div style={{fontSize:11.5,color:'var(--text-dim)',marginTop:4}}>다음 등급까지 사건 4건</div>
              </div>
            </div>
            <div style={{display:'flex',gap:16,marginTop:14,paddingTop:14,borderTop:'1px solid var(--border)'}}>
              <Stat n={14} l="해결 사건"/>
              <Stat n={'72%'} l="평균 정답률"/>
              <Stat n={3} l="제작 시나리오"/>
              <Stat n={'4.6'} l="제작 평점" gold/>
            </div>
          </div>
        </div>

        <div style={{display:'flex',gap:5,padding:'18px 18px 10px'}}>
          {tabs.map(t => (
            <button key={t.id} className="tap" onClick={()=>setTab(t.id)} style={{
              padding:'7px 14px',borderRadius:18,fontSize:12,
              background: tab===t.id?'var(--surface-3)':'transparent',
              color: tab===t.id?'var(--text)':'var(--text-dim)',
              border: tab===t.id?'1px solid var(--border-strong)':'1px solid var(--border)',
              fontWeight: tab===t.id?600:400,
            }}>{t.l}</button>
          ))}
        </div>

        <div style={{padding:'0 18px',display:'flex',flexDirection:'column',gap:8}}>
          {MY_RECORDS.map((r,i) => (
            <div key={i} className="card tap" style={{display:'flex',gap:12,alignItems:'center'}}
              onClick={()=>r.state==='IN_PROGRESS'?go('dashboard'):go('detail')}>
              <div style={{
                width:42,height:42,borderRadius:8,
                background: r.state==='DONE'?'rgba(95,179,124,.12)':r.state==='IN_PROGRESS'?'rgba(212,164,55,.12)':'var(--surface-2)',
                border:'1px solid var(--border)',
                display:'flex',alignItems:'center',justifyContent:'center',color:'var(--gold)'
              }}>{r.state==='DONE'?I.check:r.state==='IN_PROGRESS'?I.clock:I.x}</div>
              <div style={{flex:1,minWidth:0}}>
                <div className="mono" style={{fontSize:10,color:'var(--text-mute)',letterSpacing:'.12em'}}>{r.case} · {r.t}</div>
                <div style={{fontSize:13.5,fontWeight:500,marginTop:2,whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>{r.title}</div>
                <div style={{height:3,background:'var(--surface-3)',borderRadius:2,marginTop:6,overflow:'hidden'}}>
                  <div style={{
                    height:'100%',width:`${r.progress}%`,
                    background: r.state==='DONE'?'var(--green)':r.state==='IN_PROGRESS'?'var(--gold)':'var(--text-mute)'
                  }}/>
                </div>
              </div>
              {r.grade !== '-' && (
                <div className="serif" style={{fontSize:24,fontWeight:700,color:r.grade==='S'?'var(--gold)':'var(--text-dim)'}}>{r.grade}</div>
              )}
            </div>
          ))}
        </div>
      </div>
      <BottomNav active="records" onGo={go}/>
    </div>
  );
}

// ─── My page (simple) ──────────────────────────
function MeScreen({ go }) {
  return (
    <div className="scr">
      <TopBar onBack={()=>go('home')} title="마이페이지" sub="PROFILE"/>
      <div className="scr-body" style={{padding:'16px 18px 90px'}}>
        <div className="card" style={{padding:18,display:'flex',alignItems:'center',gap:14}}>
          <div style={{width:56,height:56,borderRadius:'50%',background:'var(--gold)',color:'#1a1306',
            display:'flex',alignItems:'center',justifyContent:'center',fontFamily:'var(--serif)',fontSize:24,fontWeight:700}}>K</div>
          <div style={{flex:1}}>
            <div className="serif" style={{fontSize:17,fontWeight:600}}>탐정 K</div>
            <div className="mono" style={{fontSize:10.5,color:'var(--text-mute)',marginTop:2,letterSpacing:'.1em'}}>k.detective@case.lab</div>
          </div>
          <button className="icon tap">{I.arrow}</button>
        </div>
        {[
          ['알림 설정', I.bell], ['도움말 / 튜토리얼', I.bulb], ['이용약관', I.doc], ['로그아웃', I.x]
        ].map(([l,ic],i)=>(
          <div key={i} className="card tap" style={{display:'flex',alignItems:'center',gap:12,marginTop:8,padding:'14px 16px'}}>
            <div style={{color:'var(--text-dim)'}}>{ic}</div>
            <div style={{flex:1,fontSize:13.5}}>{l}</div>
            <div style={{color:'var(--text-mute)'}}>{I.arrow}</div>
          </div>
        ))}
      </div>
      <BottomNav active="me" onGo={go}/>
    </div>
  );
}

Object.assign(window, {
  SplashScreen, OnboardingScreen, HomeScreen, LibraryScreen, ScenarioDetailScreen,
  BriefingScreen, RecordsScreen, MeScreen,
});
