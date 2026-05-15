// CaseLab AI — UI primitives (shared)

// ─── Icons (minimal inline SVG) ──────────────────────────
const Icon = ({ d, size = 18, stroke = 1.6, fill = 'none' }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={fill} stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
    {Array.isArray(d) ? d.map((p, i) => <path key={i} d={p} />) : <path d={d} />}
  </svg>
);
const I = {
  back: <Icon d="M15 18l-6-6 6-6" />,
  search: <Icon d={["M11 19a8 8 0 100-16 8 8 0 000 16z", "M21 21l-4.3-4.3"]} />,
  filter: <Icon d={["M3 6h18", "M7 12h10", "M10 18h4"]} />,
  bookmark: <Icon d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" />,
  bookmarkOn: <Icon d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" fill="currentColor" />,
  bell: <Icon d={["M18 8a6 6 0 00-12 0c0 7-3 9-3 9h18s-3-2-3-9", "M13.7 21a2 2 0 01-3.4 0"]} />,
  user: <Icon d={["M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2", "M12 11a4 4 0 100-8 4 4 0 000 8z"]} />,
  plus: <Icon d={["M12 5v14", "M5 12h14"]} />,
  more: <Icon d={["M12 13a1 1 0 100-2 1 1 0 000 2z", "M19 13a1 1 0 100-2 1 1 0 000 2z", "M5 13a1 1 0 100-2 1 1 0 000 2z"]} fill="currentColor" />,
  send: <Icon d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />,
  doc: <Icon d={["M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z", "M14 2v6h6", "M9 13h6", "M9 17h4"]} />,
  clock: <Icon d={["M12 22a10 10 0 100-20 10 10 0 000 20z", "M12 6v6l4 2"]} />,
  warn: <Icon d={["M10.3 3.86L1.82 18a2 2 0 001.7 3h16.96a2 2 0 001.7-3L13.7 3.86a2 2 0 00-3.4 0z", "M12 9v4", "M12 17h.01"]} />,
  lock: <Icon d={["M5 11h14v10H5z", "M8 11V7a4 4 0 018 0v4"]} />,
  unlock: <Icon d={["M5 11h14v10H5z", "M8 11V7a4 4 0 017.92-1"]} />,
  flame: <Icon d="M8.5 14.5A2.5 2.5 0 0011 12c0-1.38-.5-2-1-3-1.07-2.14.6-4 2-4 0 0 .65 4 3 4 1.16 0 2 1.62 2 3a4 4 0 11-8 0" />,
  check: <Icon d="M20 6L9 17l-5-5" />,
  x: <Icon d={["M18 6L6 18","M6 6l12 12"]} />,
  arrow: <Icon d="M5 12h14M13 6l6 6-6 6" />,
  home: <Icon d={["M3 12L12 3l9 9","M5 10v10h14V10"]} />,
  library: <Icon d={["M4 19.5A2.5 2.5 0 016.5 17H20","M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"]} />,
  build: <Icon d={["M12 20h9","M16.5 3.5a2.12 2.12 0 113 3L7 19l-4 1 1-4L16.5 3.5z"]} />,
  trophy: <Icon d={["M8 21h8","M12 17v4","M7 4h10v6a5 5 0 11-10 0V4z","M22 6h-5","M2 6h5"]} />,
  person: <Icon d={["M16 21v-2a4 4 0 00-8 0v2","M12 11a4 4 0 100-8 4 4 0 000 8z"]} />,
  map: <Icon d={["M1 6v16l7-4 8 4 7-4V2l-7 4-8-4-7 4z","M8 2v16","M16 6v16"]} />,
  badge: <Icon d="M12 2l3 6 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1 3-6z" />,
  bulb: <Icon d={["M9 18h6","M10 22h4","M12 2a7 7 0 00-4 12.7c.5.4 1 1 1 2v.3h6v-.3c0-1 .5-1.6 1-2A7 7 0 0012 2z"]} />,
  spark: <Icon d={["M12 2v6m0 8v6m-10-10h6m8 0h6","M4.9 4.9l4.2 4.2m5.8 5.8l4.2 4.2M4.9 19.1l4.2-4.2m5.8-5.8l4.2-4.2"]} />,
  chat: <Icon d="M21 11.5a8.4 8.4 0 01-9 8.5 8.5 8.5 0 01-4.5-1.3L3 20l1.3-4.5A8.5 8.5 0 1121 11.5z" />,
  eye: <Icon d={["M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12z","M12 15a3 3 0 100-6 3 3 0 000 6z"]} />,
  star: <Icon d="M12 2l3 6 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1 3-6z" fill="currentColor" />,
};

// ─── Difficulty ──────────────────────────
const Difficulty = ({ level }) => {
  const map = { EASY: 1, NORMAL: 2, HARD: 3, '쉬움': 1, '보통': 2, '어려움': 3 };
  const n = map[level] || 1;
  return (
    <span className="diff">
      {[1,2,3].map(i => <span key={i} className={`diff-bar ${i<=n?'on':''} ${n===3?'hard':''}`}></span>)}
    </span>
  );
};

// ─── Button ──────────────────────────
const Btn = ({ children, variant='solid', size='md', icon, onClick, full, style, disabled }) => {
  const base = {
    display:'inline-flex',alignItems:'center',justifyContent:'center',gap:8,
    borderRadius: size==='lg'?12:9,
    padding: size==='lg'?'14px 18px':size==='sm'?'7px 12px':'10px 14px',
    fontSize: size==='lg'?14.5:size==='sm'?12:13.5,
    fontWeight: 600, letterSpacing:'.01em',
    width: full?'100%':'auto',
    transition:'all .15s', cursor: disabled?'not-allowed':'pointer',
    opacity: disabled?.5:1,
    ...style,
  };
  const variants = {
    solid:{background:'var(--gold)',color:'#1a1306'},
    crimson:{background:'var(--crimson)',color:'#fff'},
    ghost:{background:'var(--surface-2)',color:'var(--text)',border:'1px solid var(--border)'},
    outline:{background:'transparent',color:'var(--gold)',border:'1px solid var(--gold-dim)'},
    danger:{background:'transparent',color:'var(--crimson)',border:'1px solid var(--crimson-dim)'},
  };
  return (
    <button className="tap" style={{...base, ...variants[variant]}} onClick={onClick} disabled={disabled}>
      {icon}{children}
    </button>
  );
};

// ─── Badge ──────────────────────────
const Badge = ({ children, color='var(--text-dim)', bg, mono=true, style }) => (
  <span style={{
    display:'inline-flex',alignItems:'center',gap:4,
    padding:'3px 7px',borderRadius:4,
    fontFamily: mono?'var(--mono)':'var(--kr)',
    fontSize:10,letterSpacing:mono?'.14em':0,fontWeight:600,
    background: bg || 'rgba(255,255,255,.04)',
    color, textTransform: mono?'uppercase':'none',
    border: bg ? 'none' : '1px solid var(--border)',
    ...style
  }}>{children}</span>
);

// ─── Stamp ──────────────────────────
const Stamp = ({ children, color='var(--crimson)', tilt=-3 }) => (
  <span className="stamp" style={{color, transform:`rotate(${tilt}deg)`}}>{children}</span>
);

// ─── Suspect avatar ──────────────────────────
const Avatar = ({ s, size = 40 }) => (
  <div style={{
    width:size,height:size,borderRadius:'50%',
    background:`linear-gradient(135deg, ${s.accent}40, ${s.accent}12)`,
    border:`1px solid ${s.accent}66`,
    color:s.accent,display:'flex',alignItems:'center',justifyContent:'center',
    fontFamily:'var(--serif)',fontSize: size*0.42, fontWeight:600,flexShrink:0,
  }}>{s.initial}</div>
);

// ─── Section title ──────────────────────────
const SectionTitle = ({ kicker, title, action }) => (
  <div style={{display:'flex',alignItems:'flex-end',justifyContent:'space-between',padding:'0 18px 10px',marginTop:18}}>
    <div>
      {kicker && <div className="label-kicker" style={{marginBottom:4}}>{kicker}</div>}
      <div style={{fontFamily:'var(--serif)',fontSize:17,fontWeight:600,letterSpacing:'.01em'}}>{title}</div>
    </div>
    {action}
  </div>
);

// ─── Top bar ──────────────────────────
const TopBar = ({ title, sub, onBack, right, transparent }) => (
  <div className="topbar" style={transparent?{background:'transparent',borderBottom:'1px solid transparent'}:{}}>
    {onBack !== false && (
      <button className="back tap" onClick={onBack}>{I.back}</button>
    )}
    <div style={{flex:1,minWidth:0,overflow:'hidden'}}>
      {sub && <div className="ttl-sub">{sub}</div>}
      <div className="ttl" style={{whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>{title}</div>
    </div>
    {right}
  </div>
);

// ─── Bottom nav ──────────────────────────
const BottomNav = ({ active, onGo }) => {
  const items = [
    { id:'home', label:'홈', icon:I.home },
    { id:'library', label:'시나리오', icon:I.library },
    { id:'builder', label:'제작', icon:I.build },
    { id:'records', label:'내 기록', icon:I.trophy },
    { id:'me', label:'마이', icon:I.person },
  ];
  return (
    <div className="bnav">
      {items.map(it => (
        <button key={it.id} className={`bnav-item tap ${active===it.id?'active':''}`} onClick={() => onGo(it.id)}>
          <span className="bnav-ico">{it.icon}</span>
          <span>{it.label}</span>
        </button>
      ))}
    </div>
  );
};

// ─── Placeholder image (case art) ──────────────────────────
const CaseArt = ({ tone='gold', h=140, code='CASE NO. 24-1107', label='데모데이 전야' }) => {
  const c = tone==='gold' ? 'var(--gold)' : tone==='crimson' ? 'var(--crimson)' : 'var(--blue)';
  return (
    <div style={{
      height:h, borderRadius:10, overflow:'hidden', position:'relative',
      background:`linear-gradient(135deg, ${tone==='gold'?'#1d1a0e':tone==='crimson'?'#1d0e0e':'#0e151d'} 0%, #0a0b10 100%)`,
      border:'1px solid var(--border)',
    }}>
      {/* corner brackets */}
      {[{top:8,left:8,b:'border-top:1.5px solid;border-left:1.5px solid'},
        {top:8,right:8,b:'border-top:1.5px solid;border-right:1.5px solid'},
        {bottom:8,left:8,b:'border-bottom:1.5px solid;border-left:1.5px solid'},
        {bottom:8,right:8,b:'border-bottom:1.5px solid;border-right:1.5px solid'}].map((p,i)=>(
        <div key={i} style={{position:'absolute',width:14,height:14,borderColor:c,...p,
          borderTop: p.b.includes('border-top')?`1.5px solid ${c}`:'none',
          borderLeft: p.b.includes('border-left')?`1.5px solid ${c}`:'none',
          borderRight: p.b.includes('border-right')?`1.5px solid ${c}`:'none',
          borderBottom: p.b.includes('border-bottom')?`1.5px solid ${c}`:'none',
        }}/>
      ))}
      {/* diagonal hash */}
      <div style={{position:'absolute',inset:0,backgroundImage:
        `repeating-linear-gradient(45deg, transparent 0 14px, ${c}08 14px 15px)`,opacity:.7}}/>
      {/* center */}
      <div style={{position:'absolute',inset:0,display:'flex',flexDirection:'column',alignItems:'center',justifyContent:'center',gap:6}}>
        <div className="mono" style={{fontSize:10,letterSpacing:'.22em',color:c,opacity:.8}}>{code}</div>
        <div className="serif" style={{fontSize:18,letterSpacing:'.02em',color:'var(--text)'}}>{label}</div>
        <div className="mono" style={{fontSize:9,letterSpacing:'.18em',color:'var(--text-mute)',marginTop:2}}>CLASSIFIED · 기밀자료</div>
      </div>
    </div>
  );
};

// ─── Importance / Severity pill ──────────────────────────
const ImpPill = ({ level }) => {
  const map = {
    CRITICAL: {label:'결정적', c:'var(--crimson-soft)', bg:'rgba(212,74,74,.13)'},
    HIGH: {label:'중요', c:'var(--gold-soft)', bg:'rgba(212,164,55,.13)'},
    MID: {label:'보통', c:'var(--blue)', bg:'rgba(82,148,239,.13)'},
    LOW: {label:'참고', c:'var(--text-dim)', bg:'rgba(255,255,255,.04)'},
  };
  const m = map[level] || map.LOW;
  return <Badge color={m.c} bg={m.bg}>{m.label}</Badge>;
};

Object.assign(window, {
  Icon, I, Difficulty, Btn, Badge, Stamp, Avatar,
  SectionTitle, TopBar, BottomNav, CaseArt, ImpPill,
});
