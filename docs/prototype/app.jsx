// CaseLab AI — Main app & router

function App() {
  const [screen, setScreen] = React.useState({ name: 'splash' });
  const [navOpen, setNavOpen] = React.useState(true);

  // go(): pass a string (screen name) or object { type, ... }
  const go = (target) => {
    if (typeof target === 'string') {
      setScreen({ name: target });
      return;
    }
    if (target.type === 'evidence') return setScreen({ name: 'evidenceDetail', eid: target.id });
    if (target.type === 'suspect') return setScreen({ name: 'suspectDetail', sid: target.id });
    if (target.type === 'interrogate') return setScreen({ name: 'interrogation', sid: target.id });
    if (target.type === 'pickInterrogateForEvid') return setScreen({ name: 'interrogation', sid: 's1', evid: target.eid });
  };

  const screens = [
    { id:'splash', l:'스플래시', sec:'시작' },
    { id:'onboarding', l:'온보딩', sec:'시작' },
    { id:'home', l:'홈', sec:'메인 탭' },
    { id:'library', l:'시나리오 라이브러리', sec:'메인 탭' },
    { id:'records', l:'내 기록', sec:'메인 탭' },
    { id:'me', l:'마이페이지', sec:'메인 탭' },
    { id:'detail', l:'시나리오 상세', sec:'시나리오' },
    { id:'briefing', l:'사건 브리핑', sec:'시나리오' },
    { id:'dashboard', l:'탐정 대시보드 ★', sec:'플레이' },
    { id:'evidenceDetail', l:'└ 증거 상세', sec:'플레이', arg:'e10' },
    { id:'suspectDetail', l:'└ 용의자 상세', sec:'플레이', arg:'s1' },
    { id:'interrogation', l:'└ 심문 채팅 ★', sec:'플레이', arg:'s1' },
    { id:'submit', l:'최종 추리 제출', sec:'플레이' },
    { id:'result', l:'결과·해설', sec:'플레이' },
    { id:'builder', l:'시나리오 빌더 ★', sec:'제작' },
    { id:'aigen', l:'AI 시나리오 생성', sec:'제작' },
  ];

  const jump = (id, arg) => {
    if (id==='evidenceDetail') return setScreen({ name:'evidenceDetail', eid: arg||'e10' });
    if (id==='suspectDetail') return setScreen({ name:'suspectDetail', sid: arg||'s1' });
    if (id==='interrogation') return setScreen({ name:'interrogation', sid: arg||'s1' });
    setScreen({ name: id });
  };

  const sections = [...new Set(screens.map(s=>s.sec))];

  const renderScreen = () => {
    switch (screen.name) {
      case 'splash': return <SplashScreen go={go}/>;
      case 'onboarding': return <OnboardingScreen go={go}/>;
      case 'home': return <HomeScreen go={go}/>;
      case 'library': return <LibraryScreen go={go}/>;
      case 'detail': return <ScenarioDetailScreen go={go}/>;
      case 'briefing': return <BriefingScreen go={go}/>;
      case 'dashboard': return <DashboardScreen go={go}/>;
      case 'evidenceDetail': return <EvidenceDetailScreen go={go} eid={screen.eid||'e10'}/>;
      case 'suspectDetail': return <SuspectDetailScreen go={go} sid={screen.sid||'s1'}/>;
      case 'interrogation': return <InterrogationScreen go={go} sid={screen.sid||'s1'} presetEvid={screen.evid}/>;
      case 'submit': return <SubmitScreen go={go}/>;
      case 'result': return <ResultScreen go={go}/>;
      case 'builder': return <BuilderScreen go={go}/>;
      case 'aigen': return <AIGenScreen go={go}/>;
      case 'records': return <RecordsScreen go={go}/>;
      case 'me': return <MeScreen go={go}/>;
      default: return <HomeScreen go={go}/>;
    }
  };

  const curLabel = screens.find(s => s.id === screen.name)?.l || '홈';

  return (
    <div className="stage" data-screen-label={curLabel}>
      {/* File header on left */}
      <div className="stage-meta">
        <div className="title">CaseLab AI</div>
        <div className="sub">Android 추리게임 — 인터랙티브 프로토타입</div>
        <div style={{marginTop:18,fontFamily:'var(--mono)'}}>
          <div>STATUS · IN REVIEW</div>
          <div>BUILD · v0.9.2 — 2024.11.07</div>
          <div>PLATFORM · ANDROID · 412×892</div>
        </div>
      </div>

      {/* Android device */}
      <div data-screen-label={curLabel}>
        <AndroidDevice dark={true} width={412} height={892}>
          {renderScreen()}
        </AndroidDevice>
      </div>

      {/* Screen picker */}
      <div className="nav-panel">
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:10}}>
          <div style={{fontFamily:'var(--serif)',fontSize:14,fontWeight:600}}>화면 목록</div>
          <span className="mono" style={{fontSize:9.5,color:'var(--text-mute)',letterSpacing:'.14em'}}>{screens.length} SCREENS</span>
        </div>
        {sections.map(sec => (
          <div key={sec}>
            <h4>{sec}</h4>
            {screens.filter(s => s.sec === sec).map(s => (
              <button key={s.id} className={`nav-btn ${screen.name===s.id?'active':''}`} onClick={()=>jump(s.id, s.arg)}>
                <span style={{flex:1}}>{s.l}</span>
              </button>
            ))}
          </div>
        ))}
        <div style={{marginTop:14, padding:'10px 0 0',borderTop:'1px solid var(--border)',fontSize:10.5,color:'var(--text-mute)',lineHeight:1.5}}>
          ★ 표시는 가장 중요한 화면입니다. 좌측 안드로이드 프레임에서 탭하여 흐름을 따라가보세요.
        </div>
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
