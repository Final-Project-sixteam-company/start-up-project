// CaseLab AI — Builder screens: Custom Scenario, AI Gen, AI Validation

// ─── Custom Scenario Builder ──────────────────────────
function BuilderScreen({ go }) {
  const [step, setStep] = React.useState(1);
  const steps = [
    { n:1, l:'사건 정보', sub:'BASIC INFO' },
    { n:2, l:'피해자·개요', sub:'VICTIM' },
    { n:3, l:'용의자', sub:'SUSPECTS' },
    { n:4, l:'증거', sub:'EVIDENCE' },
    { n:5, l:'힌트', sub:'HINTS' },
    { n:6, l:'정답', sub:'SOLUTION' },
    { n:7, l:'AI 검증', sub:'VALIDATION' },
    { n:8, l:'공개 설정', sub:'PUBLISH' },
  ];
  return (
    <div className="scr">
      <TopBar onBack={()=>step===1?go('home'):setStep(step-1)} title="시나리오 제작" sub={`STEP ${step} / 8`}
        right={<button className="tap" style={{color:'var(--gold)',fontSize:12,padding:'6px 10px',fontWeight:600}}>임시저장</button>}/>

      {/* Stepper */}
      <div style={{padding:'14px 16px 12px',background:'var(--stage)',borderBottom:'1px solid var(--border)'}}>
        <div style={{display:'flex',gap:0,alignItems:'center'}}>
          {steps.map((s,i)=>(
            <React.Fragment key={s.n}>
              <button className="tap" onClick={()=>setStep(s.n)} style={{
                width:26,height:26,borderRadius:'50%',
                background: s.n<step?'var(--gold)':s.n===step?'transparent':'var(--surface-2)',
                border: s.n===step?'1.5px solid var(--gold)':'none',
                color: s.n<step?'#1a1306':s.n===step?'var(--gold)':'var(--text-mute)',
                display:'flex',alignItems:'center',justifyContent:'center',
                fontFamily:'var(--mono)',fontSize:11,fontWeight:600,flexShrink:0
              }}>{s.n<step? <Icon d="M20 6L9 17l-5-5" size={11} stroke={2.8}/>:s.n}</button>
              {i<steps.length-1 && (
                <div style={{flex:1,height:1.5,background: s.n<step?'var(--gold)':'var(--surface-3)',margin:'0 -1px'}}/>
              )}
            </React.Fragment>
          ))}
        </div>
        <div style={{marginTop:10,display:'flex',justifyContent:'space-between',alignItems:'flex-end'}}>
          <div>
            <div className="mono" style={{fontSize:9.5,letterSpacing:'.18em',color:'var(--text-mute)'}}>{steps[step-1].sub}</div>
            <div className="serif" style={{fontSize:17,fontWeight:600,marginTop:2}}>{steps[step-1].l}</div>
          </div>
          <span className="mono" style={{fontSize:11,color:'var(--text-mute)',letterSpacing:'.08em'}}>{step}/{steps.length}</span>
        </div>
      </div>

      <div className="scr-body" style={{padding:'14px 18px 100px'}}>
        {step===1 && <Step1/>}
        {step===2 && <Step2/>}
        {step===3 && <Step3/>}
        {step===4 && <Step4/>}
        {step===5 && <Step5/>}
        {step===6 && <Step6/>}
        {step===7 && <Step7 go={go}/>}
        {step===8 && <Step8/>}
      </div>

      <div style={{position:'absolute',bottom:0,left:0,right:0,padding:'12px 18px',
        background:'linear-gradient(180deg,transparent,var(--stage) 30%)',display:'flex',gap:8}}>
        {step>1 && <Btn variant="ghost" onClick={()=>setStep(step-1)}>이전</Btn>}
        <Btn full size="lg" variant="solid" icon={step<8?I.arrow:I.check} onClick={()=>step<8?setStep(step+1):go('home')}>
          {step<8?'다음':'시나리오 발행'}
        </Btn>
      </div>
    </div>
  );
}

function Step1() {
  return (
    <>
      <Field label="사건명">
        <input className="ipt" placeholder="예: 데모데이 전야 살인사건" defaultValue="동아리방 락커룸의 비밀"/>
      </Field>
      <Field label="짧은 설명">
        <textarea className="ipt" placeholder="2~3줄로 사건을 소개해주세요"
          defaultValue="동아리방 락커룸에서 발견된 의문의 노트와 사라진 회비. 평범한 동아리 모임이었던 그날 밤, 무슨 일이 있었나?"/>
      </Field>
      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:10}}>
        <Field label="난이도">
          <Select options={['쉬움','보통','어려움']} value="보통"/>
        </Field>
        <Field label="플레이 시간">
          <Select options={['15분','25분','40분','60분']} value="25분"/>
        </Field>
      </div>
      <Field label="태그 (콤마로 구분)">
        <input className="ipt" defaultValue="동아리, 절도, 회비"/>
      </Field>
      <Field label="플레이 인원">
        <div style={{display:'flex',gap:6}}>
          {['1인용','2-4인 협력','5인+'].map((o,i)=>(
            <button key={o} className="tap" style={{
              flex:1,padding:'10px',background:i===0?'rgba(212,164,55,.10)':'var(--surface-2)',
              border: i===0?'1px solid var(--gold-dim)':'1px solid var(--border)',color:i===0?'var(--gold)':'var(--text-dim)',
              borderRadius:7,fontSize:12,fontWeight: i===0?600:400
            }}>{o}</button>
          ))}
        </div>
      </Field>
      <AISparkButton label="AI에게 사건 초안 받기"/>
    </>
  );
}

function Step2() {
  return (
    <>
      <Field label="피해자 이름 / 나이">
        <div style={{display:'flex',gap:8}}>
          <input className="ipt" placeholder="이름" defaultValue="정민호" style={{flex:2}}/>
          <input className="ipt" placeholder="나이" defaultValue="22" style={{flex:1}}/>
        </div>
      </Field>
      <Field label="피해자 직책 / 관계">
        <input className="ipt" defaultValue="동아리 회장 / 회계 담당"/>
      </Field>
      <Field label="발견 장소">
        <input className="ipt" defaultValue="대학교 학생회관 3층 동아리실"/>
      </Field>
      <Field label="사건 개요">
        <textarea className="ipt" rows="5" style={{minHeight:120}}
          defaultValue="동아리 정기 모임이 끝난 후 30분, 회장 정민호가 동아리실 락커룸에서 의식 불명 상태로 발견되었다. 현장에는 부서진 머그컵과 락커에서 사라진 회비 봉투가 남아 있었다."/>
      </Field>
      <Field label="피해자 특이사항 (선택)">
        <input className="ipt" placeholder="알레르기, 지병, 비밀 등" defaultValue=""/>
      </Field>
    </>
  );
}

function Step3() {
  const items = [
    { n:'서지원', r:'부회장', alibi:'2층 자판기에서 음료 구입 (CCTV 확인)', col:'#d4a437' },
    { n:'김태훈', r:'총무 / 회계 보조', alibi:'동아리실 옆 빈 강의실에서 발표 자료 정리', col:'#5294ef' },
    { n:'박나래', r:'홍보부장', alibi:'1층 로비에서 신입생 면담', col:'#d44a4a' },
    { n:'이서영', r:'1학년 부원', alibi:'화장실 다녀옴 (정확한 시간 모호)', col:'#9c7ad4' },
  ];
  return (
    <>
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:10}}>
        <span className="mono" style={{fontSize:11,color:'var(--text-mute)',letterSpacing:'.08em'}}>{items.length}명 등록됨</span>
        <button className="tap" style={{color:'var(--gold)',fontSize:12,display:'flex',alignItems:'center',gap:4}}>{I.plus} 추가</button>
      </div>
      {items.map((s,i)=>(
        <div key={i} className="card" style={{padding:13,marginBottom:8}}>
          <div style={{display:'flex',gap:10,alignItems:'flex-start'}}>
            <div style={{
              width:38,height:38,borderRadius:'50%',
              background:`${s.col}22`,border:`1px solid ${s.col}66`,color:s.col,
              display:'flex',alignItems:'center',justifyContent:'center',fontFamily:'var(--serif)',fontSize:15,fontWeight:600
            }}>{s.n[0]}</div>
            <div style={{flex:1}}>
              <div style={{display:'flex',alignItems:'center',gap:8}}>
                <div className="serif" style={{fontSize:14,fontWeight:600}}>{s.n}</div>
                <Badge>{s.r}</Badge>
              </div>
              <div style={{fontSize:11.5,color:'var(--text-dim)',marginTop:5,lineHeight:1.45}}>{s.alibi}</div>
            </div>
            <button className="icon tap" style={{color:'var(--text-mute)'}}>{I.more}</button>
          </div>
        </div>
      ))}
      <AISparkButton label="AI에게 용의자 페르소나 추천 받기"/>
    </>
  );
}

function Step4() {
  const items = [
    { ic:'☕', n:'부서진 머그컵', loc:'락커룸', init:true, imp:'HIGH' },
    { ic:'💰', n:'사라진 회비 봉투', loc:'정민호 락커', init:true, imp:'CRITICAL' },
    { ic:'📓', n:'정민호의 회계 노트', loc:'책상', init:true, imp:'HIGH' },
    { ic:'🔑', n:'복제된 락커 열쇠', loc:'동아리실 책장', init:false, imp:'HIGH', unlock:10 },
    { ic:'📹', n:'2층 자판기 CCTV', loc:'학생회관', init:false, imp:'MID', unlock:15 },
  ];
  return (
    <>
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:10}}>
        <span className="mono" style={{fontSize:11,color:'var(--text-mute)',letterSpacing:'.08em'}}>{items.length}개 증거 · 초기공개 {items.filter(i=>i.init).length}개</span>
        <button className="tap" style={{color:'var(--gold)',fontSize:12,display:'flex',alignItems:'center',gap:4}}>{I.plus} 추가</button>
      </div>
      {items.map((e,i)=>(
        <div key={i} className="card evidence" style={{display:'flex',gap:10,alignItems:'center',padding:11,marginBottom:6}}>
          <div style={{
            width:36,height:36,borderRadius:6,background:'var(--surface-2)',border:'1px solid var(--border)',
            display:'flex',alignItems:'center',justifyContent:'center',fontSize:17
          }}>{e.ic}</div>
          <div style={{flex:1}}>
            <div style={{display:'flex',gap:5,marginBottom:2,alignItems:'center'}}>
              <span style={{fontSize:12.5,fontWeight:500}}>{e.n}</span>
              {e.init ? <Badge color="var(--green)" bg="rgba(95,179,124,.10)">초기</Badge>
                      : <Badge color="var(--gold)" bg="rgba(212,164,55,.10)">+{e.unlock}분</Badge>}
            </div>
            <div className="mono" style={{fontSize:10,color:'var(--text-mute)',letterSpacing:'.08em'}}>{e.loc}</div>
          </div>
          <ImpPill level={e.imp}/>
        </div>
      ))}
      <AISparkButton label="AI에게 추가 증거 아이디어 받기"/>
    </>
  );
}

function Step5() {
  const items = [
    { l:1, t:'락커에서 사라진 것이 정말 회비뿐인지 다시 확인해보세요.', cost:5 },
    { l:2, t:'머그컵이 부서진 위치와 정민호가 쓰러진 위치가 어떻게 다른지 비교해보세요.', cost:15 },
    { l:3, t:'복제 열쇠를 만들 기회가 있던 사람은 한 명뿐입니다.', cost:30 },
  ];
  return (
    <>
      <div style={{padding:'10px 12px',background:'rgba(82,148,239,.06)',border:'1px solid rgba(82,148,239,.2)',borderRadius:7,marginBottom:14,fontSize:11.5,color:'var(--text-dim)',lineHeight:1.5}}>
        힌트는 3단계로 구성됩니다. 단계가 올라갈수록 정답에 가까운 강한 힌트가 됩니다.
      </div>
      {items.map(h => (
        <div key={h.l} className="card" style={{padding:13,marginBottom:8}}>
          <div style={{display:'flex',justifyContent:'space-between',marginBottom:8}}>
            <div className="serif" style={{fontSize:13.5,fontWeight:600}}>힌트 {h.l}단계</div>
            <span className="mono" style={{fontSize:10.5,color:'var(--crimson-soft)',letterSpacing:'.08em'}}>-{h.cost}점</span>
          </div>
          <textarea className="ipt" defaultValue={h.t} style={{minHeight:60,fontSize:12.5}}/>
        </div>
      ))}
      <AISparkButton label="AI에게 힌트 자동 생성 요청"/>
    </>
  );
}

function Step6() {
  return (
    <>
      <Field label="범인">
        <Select options={['서지원 (부회장)','김태훈 (총무)','박나래 (홍보)','이서영 (1학년)']} value="김태훈 (총무)"/>
      </Field>
      <Field label="범행 동기">
        <textarea className="ipt" defaultValue="개인 채무를 갚기 위해 동아리 회비에 손을 댔다가, 정민호가 회계 노트로 비위를 잡아낸 것을 알고 발각을 막으려 했다." style={{minHeight:80}}/>
      </Field>
      <Field label="범행 방법">
        <textarea className="ipt" defaultValue="복제한 락커 열쇠로 회비 봉투를 빼낸 후, 머그컵에 강한 수면제를 탔다. 정민호가 컵을 든 채로 의식을 잃으며 컵이 깨졌다." style={{minHeight:80}}/>
      </Field>
      <Field label="결정적 증거 (최소 3개)">
        <div style={{display:'flex',flexDirection:'column',gap:6}}>
          {['복제된 락커 열쇠','정민호의 회계 노트','부서진 머그컵 (잔류 성분)'].map(e=>(
            <div key={e} style={{
              display:'flex',gap:8,alignItems:'center',padding:'9px 11px',
              background:'rgba(212,164,55,.06)',border:'1px solid var(--gold-dim)',borderRadius:7
            }}>
              <div style={{color:'var(--gold)'}}>{I.check}</div>
              <span style={{fontSize:12.5,flex:1}}>{e}</span>
            </div>
          ))}
        </div>
      </Field>
    </>
  );
}

function Step7({ go }) {
  const [running, setRunning] = React.useState(false);
  const [done, setDone] = React.useState(false);
  React.useEffect(() => {
    if (running && !done) {
      const t = setTimeout(() => setDone(true), 2200);
      return () => clearTimeout(t);
    }
  }, [running, done]);

  if (!running) {
    return (
      <>
        <div style={{textAlign:'center',padding:'30px 20px'}}>
          <div style={{fontSize:36,color:'var(--gold)',marginBottom:10}}>{I.spark}</div>
          <div className="serif" style={{fontSize:18,fontWeight:600}}>AI 시나리오 검증</div>
          <p style={{fontSize:12.5,color:'var(--text-dim)',marginTop:8,lineHeight:1.6}}>
            범인·증거·힌트가 논리적으로 연결되는지<br/>AI가 시나리오를 분석합니다.
          </p>
        </div>
        <div className="card" style={{padding:14}}>
          <div className="label-kicker">검증 항목</div>
          <div style={{marginTop:8}}>
            {[
              '범인이 설정되어 있는가',
              '결정적 증거가 3개 이상 존재하는가',
              '용의자 알리바이가 모두 채워져 있는가',
              '힌트가 정답으로 연결되는가',
              '증거-동기-방법의 인과 관계가 성립하는가',
              '난이도가 적절한가',
            ].map((c,i)=>(
              <div key={i} style={{display:'flex',gap:8,padding:'6px 0',borderBottom:i<5?'1px solid var(--border)':'none',fontSize:12.5,color:'var(--text-dim)'}}>
                <span className="mono" style={{color:'var(--text-mute)',fontSize:10,minWidth:24,letterSpacing:'.08em'}}>0{i+1}</span>
                <span>{c}</span>
              </div>
            ))}
          </div>
        </div>
        <div style={{marginTop:18}}>
          <Btn full size="lg" variant="solid" icon={I.spark} onClick={()=>setRunning(true)}>
            AI 검증 시작
          </Btn>
        </div>
      </>
    );
  }
  if (running && !done) {
    return (
      <div style={{padding:'40px 20px',textAlign:'center'}}>
        <div className="pulse" style={{fontSize:42,color:'var(--gold)',marginBottom:14}}>{I.spark}</div>
        <div className="serif" style={{fontSize:17,fontWeight:600}}>분석 중…</div>
        <div className="mono pulse" style={{fontSize:10.5,color:'var(--text-mute)',marginTop:8,letterSpacing:'.18em'}}>VALIDATING SCENARIO</div>
        <div style={{maxWidth:200,margin:'30px auto 0'}}>
          {['시나리오 구조 파싱…','증거 인과 검증…','알리바이 모순 검사…','힌트-정답 연결 확인…'].map((s,i)=>(
            <div key={i} style={{display:'flex',gap:8,alignItems:'center',padding:'5px 0',fontSize:11,color:'var(--text-dim)',textAlign:'left'}}>
              <div className="pulse" style={{width:6,height:6,borderRadius:'50%',background:'var(--gold)',animationDelay:`${i*0.4}s`}}/>
              <span>{s}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }
  // done
  return (
    <>
      <div style={{padding:'20px 0 4px',textAlign:'center'}}>
        <div style={{
          display:'inline-flex',alignItems:'center',gap:8,
          padding:'8px 14px',borderRadius:20,
          background:'rgba(95,179,124,.10)',border:'1px solid rgba(95,179,124,.4)',
          color:'var(--green)',fontFamily:'var(--mono)',fontSize:11,letterSpacing:'.12em'
        }}>{I.check} PLAYABLE · 플레이 가능</div>
      </div>
      <div className="card" style={{marginTop:14, padding:0}}>
        <div style={{padding:'12px 14px',borderBottom:'1px solid var(--border)',display:'flex',justifyContent:'space-between',alignItems:'center'}}>
          <span className="label-kicker">검증 결과</span>
          <span className="serif" style={{fontSize:18,fontWeight:700,color:'var(--green)'}}>84<span style={{fontSize:10,color:'var(--text-mute)'}}>/100</span></span>
        </div>
        <div style={{padding:'12px 14px'}}>
          {[
            ['범인 식별 가능 여부', true, ''],
            ['결정적 증거 3개 이상', true, ''],
            ['알리바이 완성도', true, ''],
            ['힌트-정답 연결', false, '힌트 3단계가 범인을 직접 지목합니다. 한 단계 완곡하게 다듬어 보세요.'],
            ['난이도 적절성', true, ''],
          ].map(([l,ok,note],i)=>(
            <div key={i} style={{padding:'8px 0',borderBottom:i<4?'1px solid var(--border)':'none'}}>
              <div style={{display:'flex',gap:8,alignItems:'center'}}>
                <div style={{color: ok?'var(--green)':'var(--crimson-soft)',flexShrink:0}}>
                  {ok ? <Icon d="M20 6L9 17l-5-5" size={14} stroke={2.4}/> : <Icon d="M12 9v4M12 17h.01M10.3 3.86L1.82 18a2 2 0 001.7 3h16.96a2 2 0 001.7-3L13.7 3.86a2 2 0 00-3.4 0z" size={14}/>}
                </div>
                <div style={{fontSize:12.5,flex:1,color: ok?'var(--text)':'var(--text)'}}>{l}</div>
              </div>
              {note && <div style={{marginLeft:22,marginTop:6,padding:'8px 10px',background:'rgba(212,164,55,.06)',border:'1px solid var(--gold-dim)',borderRadius:6,fontSize:11.5,color:'var(--gold-soft)',lineHeight:1.5}}>💡 {note}</div>}
            </div>
          ))}
        </div>
      </div>

      <div className="label-kicker" style={{marginTop:16}}>AI 평가</div>
      <div className="card" style={{padding:14,marginTop:8}}>
        <p style={{fontSize:12.5,lineHeight:1.65,color:'var(--text-dim)',margin:0}}>
          전체적으로 잘 짜인 시나리오입니다. 회계 노트와 복제 열쇠 두 축이 명확해서 추리 진행이 깔끔합니다.<br/><br/>
          난이도는 <span style={{color:'var(--gold)'}}>‘보통’</span>으로 평가됩니다. 다만 힌트 3단계가 너무 직접적이라 ‘쉬움’으로 흐를 가능성이 있습니다.
        </p>
      </div>
    </>
  );
}

function Step8() {
  const [vis, setVis] = React.useState('PUBLIC');
  const opts = [
    { id:'PRIVATE', l:'비공개', sub:'나만 플레이할 수 있어요', ic:I.lock },
    { id:'UNLISTED', l:'링크 공유', sub:'링크가 있는 사람만 플레이', ic:I.eye },
    { id:'PUBLIC', l:'전체 공개', sub:'시나리오 라이브러리에 노출됩니다', ic:I.library },
  ];
  return (
    <>
      <div className="label-kicker">공개 범위</div>
      <div style={{marginTop:10,display:'flex',flexDirection:'column',gap:8}}>
        {opts.map(o => (
          <button key={o.id} className="tap" onClick={()=>setVis(o.id)} style={{
            display:'flex',gap:12,alignItems:'center',padding:'14px',
            background: vis===o.id?'rgba(212,164,55,.08)':'var(--surface)',
            border: vis===o.id?'1px solid var(--gold-dim)':'1px solid var(--border)',
            borderRadius:9,textAlign:'left',
          }}>
            <div style={{
              width:36,height:36,borderRadius:8,
              background:vis===o.id?'rgba(212,164,55,.12)':'var(--surface-2)',
              color:vis===o.id?'var(--gold)':'var(--text-dim)',
              display:'flex',alignItems:'center',justifyContent:'center',flexShrink:0
            }}>{o.ic}</div>
            <div style={{flex:1}}>
              <div className="serif" style={{fontSize:14.5,fontWeight:600}}>{o.l}</div>
              <div style={{fontSize:11.5,color:'var(--text-dim)',marginTop:2}}>{o.sub}</div>
            </div>
            <div style={{
              width:18,height:18,borderRadius:'50%',
              background:vis===o.id?'var(--gold)':'transparent',
              border: vis===o.id?'none':'1px solid var(--border-strong)',
            }}/>
          </button>
        ))}
      </div>

      <div className="label-kicker" style={{marginTop:18}}>최종 확인</div>
      <div className="card" style={{padding:14,marginTop:8}}>
        <div style={{display:'flex',gap:12}}>
          <div style={{
            width:54,height:64,borderRadius:6,flexShrink:0,
            background:'linear-gradient(135deg,#1d1a0e,#0a0b10)',border:'1px solid var(--gold-dim)',
            display:'flex',alignItems:'center',justifyContent:'center',
            fontFamily:'var(--mono)',fontSize:10,color:'var(--gold)',letterSpacing:'.1em'
          }}>NEW<br/>CASE</div>
          <div style={{flex:1}}>
            <div className="serif" style={{fontSize:15,fontWeight:600}}>동아리방 락커룸의 비밀</div>
            <div style={{fontSize:11.5,color:'var(--text-dim)',marginTop:4,lineHeight:1.5}}>
              보통 · 25분 · 용의자 4명 · 증거 5개
            </div>
            <div style={{display:'flex',gap:5,marginTop:6}}>
              {['동아리','절도','회비'].map(t=>(<Badge key={t} mono={false}>#{t}</Badge>))}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

// ─── Helpers ──────────────────────────
function Field({ label, children }) {
  return (
    <div style={{marginBottom:14}}>
      <div className="label-kicker" style={{marginBottom:8}}>{label}</div>
      {children}
    </div>
  );
}

function Select({ options, value }) {
  return (
    <div className="ipt" style={{display:'flex',alignItems:'center',justifyContent:'space-between',cursor:'pointer'}}>
      <span>{value}</span>
      <span style={{color:'var(--text-mute)',fontSize:11}}>▾</span>
    </div>
  );
}

function AISparkButton({ label }) {
  return (
    <button className="tap" style={{
      marginTop:10,width:'100%',padding:'12px 14px',
      background:'linear-gradient(135deg, rgba(212,164,55,.10), rgba(212,164,55,.02))',
      border:'1px dashed var(--gold-dim)',borderRadius:9,
      display:'flex',alignItems:'center',gap:10,color:'var(--gold)',fontSize:12.5,fontWeight:600,textAlign:'left'
    }}>
      <div style={{flexShrink:0}}>{I.spark}</div>
      <span style={{flex:1}}>{label}</span>
      <span style={{fontSize:10,opacity:.6}}>{I.arrow}</span>
    </button>
  );
}

// ─── AI Scenario Generation ──────────────────────────
function AIGenScreen({ go }) {
  return (
    <div className="scr">
      <TopBar onBack={()=>go('builder')} title="AI 시나리오 생성" sub="AI ASSIST"/>
      <div className="scr-body" style={{padding:'14px 18px 100px'}}>
        <div style={{textAlign:'center',padding:'16px 12px 6px'}}>
          <div style={{fontSize:34,color:'var(--gold)',marginBottom:8}}>{I.spark}</div>
          <div className="serif" style={{fontSize:18,fontWeight:600}}>몇 가지만 알려주시면 됩니다</div>
          <div style={{fontSize:12,color:'var(--text-dim)',marginTop:4}}>AI가 사건 초안을 만들어드립니다.</div>
        </div>

        <Field label="사건 배경">
          <div style={{display:'flex',gap:6,flexWrap:'wrap'}}>
            {['스타트업','학교','카페','회사','동아리','호텔'].map((o,i)=>(
              <button key={o} className="tap" style={{
                padding:'7px 14px',borderRadius:18,fontSize:12,
                background: i===0?'rgba(212,164,55,.10)':'var(--surface-2)',
                border: i===0?'1px solid var(--gold-dim)':'1px solid var(--border)',
                color: i===0?'var(--gold)':'var(--text-dim)',fontWeight:i===0?600:400
              }}>{o}</button>
            ))}
          </div>
        </Field>
        <Field label="사건 유형">
          <div style={{display:'flex',gap:6,flexWrap:'wrap'}}>
            {['살인','실종','유출','절도','회비 실종'].map((o,i)=>(
              <button key={o} className="tap" style={{
                padding:'7px 14px',borderRadius:18,fontSize:12,
                background: i===0?'rgba(212,74,74,.10)':'var(--surface-2)',
                border: i===0?'1px solid var(--crimson-dim)':'1px solid var(--border)',
                color: i===0?'var(--crimson-soft)':'var(--text-dim)',fontWeight:i===0?600:400
              }}>{o}</button>
            ))}
          </div>
        </Field>
        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:10}}>
          <Field label="난이도">
            <Select options={['쉬움','보통','어려움']} value="보통"/>
          </Field>
          <Field label="용의자 수">
            <Select options={['3명','5명','7명']} value="5명"/>
          </Field>
        </div>
        <Field label="플레이 시간">
          <Select options={['15분','25분','40분','60분']} value="25분"/>
        </Field>
        <Field label="추가 요구사항 (선택)">
          <textarea className="ipt" placeholder="예: 여성 캐릭터가 범인이 아니어야 함, 트릭은 알리바이 조작 위주로…" style={{minHeight:70}}/>
        </Field>
      </div>
      <div style={{position:'absolute',bottom:0,left:0,right:0,padding:'14px 18px',background:'linear-gradient(180deg,transparent,var(--stage) 30%)'}}>
        <Btn full size="lg" icon={I.spark} variant="solid" onClick={()=>go('builder')}>초안 생성 시작</Btn>
      </div>
    </div>
  );
}

Object.assign(window, { BuilderScreen, AIGenScreen });
