// Static screens — Landing, Library, Brief, Profile, Hints, Result

const C = () => window.CASE;

// =============================================================
// 1. LANDING
// =============================================================
function LandingScreen({ go }) {
  return (
    <div className="route grain" style={{ position: 'relative', overflow: 'hidden' }}>
      {/* Background plates */}
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none',
        background:
          'radial-gradient(ellipse 80% 60% at 80% 20%, rgba(200,16,46,0.10), transparent 60%), radial-gradient(ellipse 60% 50% at 10% 90%, rgba(212,162,76,0.08), transparent 60%)',
      }} />
      {/* Crosshair grid */}
      <svg style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', opacity: 0.3 }}>
        <defs>
          <pattern id="grid" x="0" y="0" width="64" height="64" patternUnits="userSpaceOnUse">
            <path d="M 64 0 L 0 0 0 64" fill="none" stroke="rgba(255,255,255,0.025)" strokeWidth="1"/>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" />
      </svg>

      <div style={{
        maxWidth: 1280,
        margin: '0 auto',
        padding: '96px 48px',
        position: 'relative',
        display: 'grid',
        gridTemplateColumns: '1.4fr 1fr',
        gap: 64,
        alignItems: 'center',
        minHeight: 'calc(100vh - 36px)',
      }}>
        <div className="fade-in">
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 32 }}>
            <span className="eyebrow-red">FILE · 2025-1107-K</span>
            <span style={{ width: 32, height: 1, background: 'var(--border)' }} />
            <span className="eyebrow">CASE STATUS · OPEN</span>
          </div>

          <h1 className="h-display" style={{ margin: '0 0 28px', color: 'var(--text)' }}>
            AI가 만든 사건,<br />
            당신이 푸는 <span style={{ color: 'var(--gold)', fontStyle: 'italic' }}>진실</span>.
          </h1>

          <p style={{
            fontSize: 17,
            lineHeight: 1.6,
            color: 'var(--text-dim)',
            maxWidth: 560,
            margin: '0 0 40px',
            textWrap: 'pretty',
          }}>
            용의자를 심문하고, 증거를 조합해 범인을 찾아내세요.
            <br />
            CaseLab AI는 즉흥 생성이 아닌, <span style={{ color: 'var(--text)' }}>구조화된 사건 그래프</span>를
            기반으로 작동하는 추리 시뮬레이션입니다.
          </p>

          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <button className="btn btn-primary" onClick={() => go('brief')}>
              ▸ 새 사건 시작하기
            </button>
            <button className="btn" onClick={() => go('library')}>
              사건 라이브러리
            </button>
            <button className="btn btn-ghost" onClick={() => go('library')}>
              + 커스텀 사건 생성
            </button>
          </div>

          {/* Spec row */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: 24,
            marginTop: 64,
            paddingTop: 32,
            borderTop: '1px solid var(--border)',
          }}>
            {[
              { k: '01', t: 'Case Graph', d: '구조화된 사건' },
              { k: '02', t: 'AI Suspect', d: '페르소나 기반 NPC' },
              { k: '03', t: 'Evidence', d: '단계별 해금 카드' },
              { k: '04', t: 'Deduction', d: '추리 채점 시스템' },
            ].map(s => (
              <div key={s.k}>
                <div className="mono" style={{ fontSize: 11, color: 'var(--gold)', letterSpacing: '0.18em', marginBottom: 6 }}>
                  / {s.k}
                </div>
                <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 2 }}>{s.t}</div>
                <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{s.d}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Case file mockup */}
        <FilePreview go={go} />
      </div>
    </div>
  );
}

function FilePreview({ go }) {
  return (
    <div style={{ position: 'relative' }}>
      {/* Stacked dossiers */}
      <div style={{
        position: 'absolute',
        inset: 0,
        transform: 'translate(12px, 12px) rotate(2deg)',
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 4,
        opacity: 0.5,
      }} />
      <div style={{
        position: 'absolute',
        inset: 0,
        transform: 'translate(6px, 6px) rotate(-1deg)',
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 4,
        opacity: 0.7,
      }} />

      <div className="dossier" style={{
        position: 'relative',
        padding: '24px 24px 28px',
        borderRadius: 4,
        boxShadow: '0 30px 80px -20px rgba(0,0,0,0.8), 0 0 0 1px rgba(212,162,76,0.05)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
          <FileTab>CASE FILE · K-2025-1107</FileTab>
          <span className="stamp red" style={{ fontSize: 9 }}>CONFIDENTIAL</span>
        </div>

        <div className="eyebrow-gold" style={{ marginBottom: 8 }}>FEATURED CASE</div>
        <h3 className="h-1" style={{ margin: '0 0 12px', fontSize: 28 }}>{C().title}</h3>
        <p style={{ fontSize: 13, color: 'var(--text-dim)', lineHeight: 1.55, margin: '0 0 20px' }}>
          {C().summary.slice(0, 110)}…
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 20 }}>
          <MetaCell label="장소" value={C().location} />
          <MetaCell label="시각" value={C().date} />
          <MetaCell label="용의자" value={`${C().suspects.length} 명`} />
          <MetaCell label="증거" value={`${C().evidence.length} 점`} />
        </div>

        <button className="btn btn-primary" style={{ width: '100%' }} onClick={() => go('brief')}>
          파일 열기 ▸
        </button>
      </div>
    </div>
  );
}

function MetaCell({ label, value }) {
  return (
    <div style={{ padding: '8px 10px', background: 'rgba(255,255,255,0.02)', border: '1px solid var(--border)' }}>
      <div className="eyebrow" style={{ fontSize: 9 }}>{label}</div>
      <div style={{ fontSize: 12, fontWeight: 500, marginTop: 2 }}>{value}</div>
    </div>
  );
}

// =============================================================
// 2. LIBRARY
// =============================================================
function LibraryScreen({ go }) {
  const [filter, setFilter] = useState('all');
  const tabs = [
    { id: 'all', label: '전체' },
    { id: 'official', label: '공식' },
    { id: 'ai', label: 'AI 생성' },
    { id: 'recent', label: '최근 플레이' },
  ];

  return (
    <div className="route" style={{ padding: '48px 48px 96px', maxWidth: 1400, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 32 }}>
        <div>
          <div className="eyebrow-gold" style={{ marginBottom: 8 }}>CASE LIBRARY · 미해결 사건 보관소</div>
          <h1 className="h-1" style={{ margin: 0 }}>어떤 사건을 맡으시겠습니까?</h1>
        </div>
        <button className="btn btn-primary">+ 커스텀 사건 생성</button>
      </div>

      {/* Filters */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
        {tabs.map(t => (
          <button
            key={t.id}
            onClick={() => setFilter(t.id)}
            className="btn"
            style={{
              padding: '8px 14px',
              borderColor: filter === t.id ? 'var(--gold)' : 'var(--border)',
              color: filter === t.id ? 'var(--gold)' : 'var(--text-dim)',
              background: filter === t.id ? 'var(--gold-dim)' : 'transparent',
            }}
          >
            {t.label}
          </button>
        ))}
        <div style={{ width: 1, height: 24, background: 'var(--border)', margin: '0 8px' }} />
        <FilterSelect label="난이도" options={['전체', '쉬움', '보통', '어려움']} />
        <FilterSelect label="플레이 시간" options={['전체', '15분', '30분', '45분']} />
        <FilterSelect label="배경" options={['전체', '스타트업', '학교', '카페', '동아리', '회사', 'MT']} />
      </div>

      {/* Featured case */}
      <FeaturedCaseCard go={go} />

      {/* Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20, marginTop: 32 }}>
        {LIBRARY_CASES.map((c, i) => (
          <LibraryCaseCard key={i} c={c} disabled={!c.live} go={go} />
        ))}
      </div>
    </div>
  );
}

const LIBRARY_CASES = [
  { id: 'demoday-eve', title: '데모데이 전야 살인사건', tag: '스타트업', diff: '보통', time: 30, suspects: 5, evidence: 12, hot: true, live: true, status: 'OPEN' },
  { id: 'lab-401', title: '401호 실험실 화재', tag: '학교', diff: '쉬움', time: 20, suspects: 4, evidence: 9, status: 'OPEN' },
  { id: 'mt-cabin', title: 'MT 별장 회비 실종', tag: '동아리', diff: '쉬움', time: 15, suspects: 6, evidence: 11, status: 'OPEN' },
  { id: 'cafe-poison', title: '카페 단골의 마지막 주문', tag: '카페', diff: '어려움', time: 45, suspects: 5, evidence: 18, status: 'OPEN' },
  { id: 'leak-q4', title: 'Q4 자료 유출 사건', tag: '회사', diff: '보통', time: 30, suspects: 5, evidence: 14, status: 'OPEN' },
  { id: 'club-betray', title: '여름 워크샵의 배신자', tag: '동아리', diff: '어려움', time: 45, suspects: 7, evidence: 20, status: 'OPEN' },
];

function FeaturedCaseCard({ go }) {
  return (
    <div className="dossier" style={{
      padding: 32,
      borderRadius: 4,
      display: 'grid',
      gridTemplateColumns: '1fr 380px',
      gap: 40,
      alignItems: 'center',
      position: 'relative',
      overflow: 'hidden',
    }}>
      <div style={{ position: 'absolute', top: 16, right: 16 }}>
        <FileTab color="red">FEATURED</FileTab>
      </div>

      <div>
        <div className="eyebrow-gold" style={{ marginBottom: 12 }}>이번 주 추천 사건</div>
        <h2 className="h-1" style={{ margin: '0 0 12px' }}>{C().title}</h2>
        <p style={{ color: 'var(--text-dim)', fontSize: 14, lineHeight: 1.6, margin: '0 0 20px', maxWidth: 560 }}>
          {C().summary}
        </p>

        <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
          <Pill>난이도 · {C().difficulty}</Pill>
          <Pill>{C().playTime}분</Pill>
          <Pill>용의자 {C().suspects.length}명</Pill>
          <Pill>증거 {C().evidence.length}점</Pill>
        </div>

        <button className="btn btn-primary" onClick={() => go('brief')}>사건 시작 ▸</button>
      </div>

      {/* Suspect lineup */}
      <div>
        <div className="eyebrow" style={{ marginBottom: 12 }}>SUSPECT LINEUP</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 8 }}>
          {C().suspects.map(s => (
            <div key={s.id} style={{ textAlign: 'center' }}>
              <Avatar name={s.name} tag={s.tag} size={48} />
              <div style={{ marginTop: 8, fontSize: 11, color: 'var(--text-dim)' }}>{s.name}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function Pill({ children }) {
  return (
    <span style={{
      fontFamily: 'var(--font-mono)',
      fontSize: 11,
      letterSpacing: '0.08em',
      color: 'var(--text-dim)',
      padding: '4px 10px',
      border: '1px solid var(--border)',
      borderRadius: 2,
    }}>{children}</span>
  );
}

function LibraryCaseCard({ c, disabled, go }) {
  return (
    <button
      onClick={() => !disabled && go('brief')}
      className="card"
      disabled={disabled}
      style={{
        padding: 20,
        textAlign: 'left',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.5 : 1,
        transition: 'all 0.15s',
        width: '100%',
      }}
      onMouseEnter={(e) => { if (!disabled) e.currentTarget.style.borderColor = 'var(--gold)'; }}
      onMouseLeave={(e) => { if (!disabled) e.currentTarget.style.borderColor = 'var(--border)'; }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <span className="tag">{c.tag}</span>
        <span className="mono" style={{ fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.12em' }}>
          {c.status}
        </span>
      </div>
      <h4 style={{ margin: '0 0 12px', fontSize: 17, fontWeight: 600, letterSpacing: '-0.005em' }}>
        {c.title}
      </h4>
      <div style={{ display: 'flex', gap: 12, fontSize: 11, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
        <span>난이도 {c.diff}</span>
        <span>·</span>
        <span>{c.time}분</span>
        <span>·</span>
        <span>용의자 {c.suspects}</span>
        <span>·</span>
        <span>증거 {c.evidence}</span>
      </div>
      {disabled && (
        <div style={{ marginTop: 12, fontSize: 11, color: 'var(--text-muted)' }}>
          🔒 데모에서는 잠겨 있습니다
        </div>
      )}
    </button>
  );
}

function FilterSelect({ label, options }) {
  const [val, setVal] = useState(options[0]);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <span className="eyebrow" style={{ fontSize: 10 }}>{label}</span>
      <select
        value={val}
        onChange={e => setVal(e.target.value)}
        style={{
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          color: 'var(--text)',
          padding: '6px 10px',
          fontFamily: 'var(--font-mono)',
          fontSize: 11,
          letterSpacing: '0.08em',
          borderRadius: 2,
        }}
      >
        {options.map(o => <option key={o}>{o}</option>)}
      </select>
    </div>
  );
}

// =============================================================
// 3. BRIEFING
// =============================================================
function BriefingScreen({ go }) {
  return (
    <div className="route" style={{ padding: '48px 48px 96px', maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <button onClick={() => go('library')} className="btn btn-ghost" style={{ padding: '6px 10px' }}>← 라이브러리</button>
        <span className="eyebrow">CASE BRIEFING · 사건 브리핑</span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: 32 }}>
        {/* Left — narrative */}
        <div className="dossier" style={{ padding: 40, position: 'relative' }}>
          <div style={{ position: 'absolute', top: 24, right: 24, display: 'flex', gap: 8 }}>
            <span className="stamp red" style={{ fontSize: 10 }}>CONFIDENTIAL</span>
          </div>

          <div className="mono" style={{ fontSize: 11, color: 'var(--gold)', letterSpacing: '0.2em', marginBottom: 16 }}>
            FILE · K-2025-1107 / 강력계 인계
          </div>

          <h1 className="h-1" style={{ margin: '0 0 8px' }}>{C().title}</h1>
          <div className="mono" style={{ color: 'var(--text-muted)', fontSize: 12, marginBottom: 32, letterSpacing: '0.08em' }}>
            {C().date} · {C().location}
          </div>

          <hr className="hair" style={{ marginBottom: 24 }} />

          <div style={{ marginBottom: 28 }}>
            <div className="eyebrow-gold" style={{ marginBottom: 10 }}>SUMMARY · 사건 개요</div>
            <p className="serif" style={{
              fontSize: 18,
              lineHeight: 1.7,
              color: 'var(--text)',
              margin: 0,
              textWrap: 'pretty',
            }}>
              {C().summary}
            </p>
          </div>

          <div style={{ marginBottom: 28 }}>
            <div className="eyebrow-gold" style={{ marginBottom: 10 }}>VICTIM · 피해자 정보</div>
            <div style={{
              padding: 16,
              background: 'rgba(255,255,255,0.02)',
              borderLeft: '3px solid var(--red)',
            }}>
              <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>
                {C().victim.name} <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: 13 }}>({C().victim.age}세, {C().victim.role})</span>
              </div>
              <div style={{ fontSize: 13, color: 'var(--text-dim)' }}>{C().victim.notes}</div>
            </div>
          </div>

          <div>
            <div className="eyebrow-gold" style={{ marginBottom: 10 }}>INITIAL CLUES · 초기 단서</div>
            <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
              {[
                '데모룸 책상 위에 마시다 만 라떼 컵 1잔',
                '쓰레기통에서 발견된 찢긴 컵 라벨 (\"...MOND LAT...\")',
                '피해자 책상 서랍의 에피펜 부재',
                '22:36 단톡방에 피해자 계정으로 전송된 메시지',
              ].map((clue, i) => (
                <li key={i} style={{
                  display: 'flex',
                  gap: 12,
                  padding: '10px 0',
                  borderTop: i === 0 ? '1px solid var(--border)' : '1px dashed var(--border)',
                  fontSize: 14,
                }}>
                  <span className="mono" style={{ color: 'var(--gold)', fontSize: 11, letterSpacing: '0.1em', minWidth: 32 }}>
                    0{i + 1}
                  </span>
                  <span style={{ color: 'var(--text-dim)' }}>{clue}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Right — goals + CTA */}
        <div>
          <div className="card" style={{ padding: 24, marginBottom: 16 }}>
            <div className="eyebrow-gold" style={{ marginBottom: 14 }}>DETECTIVE OBJECTIVES · 탐정 목표</div>
            <ol style={{ margin: 0, paddingLeft: 0, listStyle: 'none' }}>
              {C().goals.map((g, i) => (
                <li key={i} style={{
                  display: 'flex',
                  gap: 14,
                  alignItems: 'flex-start',
                  padding: '12px 0',
                  borderBottom: i < C().goals.length - 1 ? '1px solid var(--border)' : 'none',
                }}>
                  <span style={{
                    width: 24, height: 24,
                    display: 'inline-flex',
                    alignItems: 'center', justifyContent: 'center',
                    border: '1px solid var(--gold)',
                    color: 'var(--gold)',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 11,
                    fontWeight: 600,
                    flexShrink: 0,
                  }}>{i + 1}</span>
                  <span style={{ fontSize: 14 }}>{g}</span>
                </li>
              ))}
            </ol>
          </div>

          <div className="card" style={{ padding: 20, marginBottom: 16 }}>
            <div className="eyebrow" style={{ marginBottom: 10 }}>SUSPECTS · 5명</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {C().suspects.map(s => (
                <div key={s.id} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <Avatar name={s.name} tag={s.tag} size={32} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 13, fontWeight: 500 }}>{s.name}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{s.role}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <button className="btn btn-primary" style={{ width: '100%', padding: '16px 18px' }} onClick={() => go('dashboard')}>
            ▸ 조사 시작하기
          </button>
          <div style={{ textAlign: 'center', marginTop: 10, fontSize: 11, color: 'var(--text-muted)' }}>
            예상 소요 시간 {C().playTime}분
          </div>
        </div>
      </div>
    </div>
  );
}

// =============================================================
// 9. HINTS panel content (used inside dashboard)
// =============================================================
function HintsPanel({ revealed, onReveal }) {
  return (
    <div style={{ padding: 16 }}>
      <div className="eyebrow-gold" style={{ marginBottom: 4 }}>HINTS · 힌트</div>
      <p style={{ fontSize: 12, color: 'var(--text-muted)', margin: '0 0 16px' }}>
        힌트를 사용하면 최종 점수에서 일정 점수가 차감됩니다.
      </p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {C().hints.map((h, i) => {
          const isOpen = revealed.includes(i);
          return (
            <div
              key={i}
              className="card"
              style={{
                padding: 14,
                borderColor: isOpen ? 'rgba(212,162,76,0.4)' : 'var(--border)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: isOpen ? 8 : 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span className="mono" style={{
                    fontSize: 10, letterSpacing: '0.12em',
                    color: isOpen ? 'var(--gold)' : 'var(--text-muted)',
                  }}>
                    LV.{h.level}
                  </span>
                  <span style={{ fontSize: 13, fontWeight: 500, color: isOpen ? 'var(--text)' : 'var(--text-dim)' }}>
                    {isOpen ? h.title : '잠긴 힌트'}
                  </span>
                </div>
                {isOpen ? (
                  <span className="mono" style={{ fontSize: 10, color: 'var(--red-soft)' }}>{h.penalty}점</span>
                ) : (
                  <button className="btn" style={{ padding: '4px 8px', fontSize: 10 }} onClick={() => onReveal(i)}>
                    해금 ({h.penalty}점)
                  </button>
                )}
              </div>
              {isOpen && (
                <div style={{ fontSize: 12, color: 'var(--text-dim)', lineHeight: 1.55 }}>
                  {h.content}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

Object.assign(window, {
  LandingScreen, LibraryScreen, BriefingScreen, HintsPanel,
});
