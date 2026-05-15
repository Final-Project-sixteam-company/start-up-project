// Detective Dashboard — main hub + Evidence board + Timeline + Profile

// =============================================================
// 4. DASHBOARD (hub)
// =============================================================
function Dashboard({ go, state, dispatch }) {
  const c = window.CASE;
  const { selectedSuspect, selectedEvidence, unlockedEvidence, revealedHints, suspicion, elapsedSec } = state;
  const [centerTab, setCenterTab] = useState('interrogate'); // interrogate | evidence | profile | timeline

  // Right panel tab
  const [rightTab, setRightTab] = useState('evidence'); // evidence | timeline | hints

  const elapsedFmt = useMemo(() => {
    const m = Math.floor(elapsedSec / 60);
    const s = elapsedSec % 60;
    return `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
  }, [elapsedSec]);

  const unlockedCount = unlockedEvidence.length;
  const totalEv = c.evidence.length;
  const progress = Math.min(100, Math.round((unlockedCount / totalEv) * 100));

  return (
    <div className="route" style={{ display: 'flex', flexDirection: 'column' }}>
      {/* Top strip */}
      <ProgressStrip
        elapsed={elapsedFmt}
        unlocked={unlockedCount}
        total={totalEv}
        progress={progress}
      />

      {/* 3-col layout */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '280px 1fr 360px',
        gap: 0,
        flex: 1,
        minHeight: 'calc(100vh - 36px - 49px)',
      }}>
        {/* LEFT — suspect list */}
        <aside style={{
          borderRight: '1px solid var(--border)',
          background: 'var(--bg-elevated)',
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
        }}>
          <div style={{ padding: '16px 16px 8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div className="eyebrow-gold">SUSPECTS · 용의자</div>
            <span className="mono" style={{ fontSize: 10, color: 'var(--text-muted)' }}>{c.suspects.length}명</span>
          </div>
          <div style={{ padding: '8px 12px', flex: 1, overflowY: 'auto' }}>
            {c.suspects.map(s => (
              <SuspectRow
                key={s.id}
                s={s}
                active={selectedSuspect?.id === s.id}
                suspicion={suspicion[s.id] ?? s.suspicion}
                onClick={() => {
                  dispatch({ type: 'SELECT_SUSPECT', payload: s });
                  setCenterTab('interrogate');
                }}
              />
            ))}
          </div>

          <div style={{ padding: 16, borderTop: '1px solid var(--border)' }}>
            <button className="btn btn-danger" style={{ width: '100%' }} onClick={() => go('final')}>
              ▸ 최종 추리 제출
            </button>
          </div>
        </aside>

        {/* CENTER — context-sensitive workspace */}
        <main style={{
          background: 'var(--bg)',
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
        }}>
          <CenterTabs
            tab={centerTab}
            setTab={setCenterTab}
            selectedSuspect={selectedSuspect}
            selectedEvidence={selectedEvidence}
          />
          <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            {centerTab === 'interrogate' && selectedSuspect && (
              <InterrogationChat suspect={selectedSuspect} dispatch={dispatch} state={state} />
            )}
            {centerTab === 'profile' && selectedSuspect && (
              <SuspectProfile s={selectedSuspect} suspicion={suspicion[selectedSuspect.id] ?? selectedSuspect.suspicion} onInterrogate={() => setCenterTab('interrogate')} />
            )}
            {centerTab === 'evidence' && (
              <EvidenceBoard
                unlockedIds={unlockedEvidence}
                onSelect={(ev) => dispatch({ type: 'SELECT_EVIDENCE', payload: ev })}
                selected={selectedEvidence}
              />
            )}
            {centerTab === 'timeline' && (
              <TimelinePanel />
            )}
            {!selectedSuspect && centerTab === 'interrogate' && (
              <EmptyState
                title="용의자를 선택하세요"
                desc="좌측에서 심문할 용의자를 선택하면 채팅이 시작됩니다."
              />
            )}
            {!selectedSuspect && centerTab === 'profile' && (
              <EmptyState
                title="용의자 프로필 비어 있음"
                desc="좌측 목록에서 용의자를 선택하세요."
              />
            )}
          </div>
        </main>

        {/* RIGHT — evidence / timeline / hints */}
        <aside style={{
          borderLeft: '1px solid var(--border)',
          background: 'var(--bg-elevated)',
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
        }}>
          <div style={{
            display: 'flex',
            borderBottom: '1px solid var(--border)',
          }}>
            {[
              { id: 'evidence', label: '증거', n: unlockedCount },
              { id: 'timeline', label: '타임라인', n: c.timeline.length },
              { id: 'hints', label: '힌트', n: revealedHints.length },
            ].map(t => (
              <button
                key={t.id}
                onClick={() => setRightTab(t.id)}
                style={{
                  flex: 1,
                  padding: '14px 8px',
                  fontFamily: 'var(--font-mono)',
                  fontSize: 11,
                  letterSpacing: '0.12em',
                  textTransform: 'uppercase',
                  color: rightTab === t.id ? 'var(--gold)' : 'var(--text-muted)',
                  borderBottom: rightTab === t.id ? '2px solid var(--gold)' : '2px solid transparent',
                  background: rightTab === t.id ? 'rgba(212,162,76,0.05)' : 'transparent',
                }}
              >
                {t.label} <span style={{ opacity: 0.6 }}>{t.n}</span>
              </button>
            ))}
          </div>

          <div style={{ flex: 1, overflowY: 'auto' }}>
            {rightTab === 'evidence' && (
              <RightEvidenceList
                unlockedIds={unlockedEvidence}
                onSelect={(ev) => { dispatch({ type: 'SELECT_EVIDENCE', payload: ev }); setCenterTab('evidence'); }}
                selectedId={selectedEvidence?.id}
              />
            )}
            {rightTab === 'timeline' && (
              <RightTimeline />
            )}
            {rightTab === 'hints' && (
              <HintsPanel
                revealed={revealedHints}
                onReveal={(i) => dispatch({ type: 'REVEAL_HINT', payload: i })}
              />
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}

// ---------- Suspect row in left panel ----------
function SuspectRow({ s, active, suspicion, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        width: '100%',
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '10px 12px',
        marginBottom: 4,
        border: '1px solid',
        borderColor: active ? 'var(--gold)' : 'transparent',
        borderRadius: 3,
        background: active ? 'var(--gold-dim)' : 'transparent',
        textAlign: 'left',
        transition: 'all 0.15s',
      }}
      onMouseEnter={(e) => { if (!active) e.currentTarget.style.background = 'rgba(255,255,255,0.025)'; }}
      onMouseLeave={(e) => { if (!active) e.currentTarget.style.background = 'transparent'; }}
    >
      <Avatar name={s.name} tag={s.tag} size={40} suspicion={suspicion} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
          <span style={{ fontSize: 13, fontWeight: 600 }}>{s.name}</span>
          <span className="mono" style={{ fontSize: 9, color: 'var(--text-muted)', letterSpacing: '0.08em' }}>{s.tag}</span>
        </div>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {s.role}
        </div>
        <SuspicionMeter value={suspicion} />
      </div>
    </button>
  );
}

// ---------- Center tabs bar ----------
function CenterTabs({ tab, setTab, selectedSuspect, selectedEvidence }) {
  const tabs = [
    { id: 'interrogate', label: '심문', icon: '▸' },
    { id: 'profile', label: '프로필', icon: '◆' },
    { id: 'evidence', label: '증거 상세', icon: '◈' },
    { id: 'timeline', label: '타임라인', icon: '─' },
  ];
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      borderBottom: '1px solid var(--border)',
      background: 'var(--bg-elevated)',
      paddingRight: 16,
    }}>
      {tabs.map(t => (
        <button
          key={t.id}
          onClick={() => setTab(t.id)}
          style={{
            padding: '14px 18px',
            fontFamily: 'var(--font-mono)',
            fontSize: 11,
            letterSpacing: '0.12em',
            textTransform: 'uppercase',
            color: tab === t.id ? 'var(--text)' : 'var(--text-muted)',
            background: tab === t.id ? 'var(--bg)' : 'transparent',
            borderRight: '1px solid var(--border)',
            borderBottom: tab === t.id ? '2px solid var(--gold)' : '2px solid transparent',
            marginBottom: -1,
          }}
        >
          <span style={{ marginRight: 6, color: tab === t.id ? 'var(--gold)' : 'inherit' }}>{t.icon}</span>
          {t.label}
        </button>
      ))}
      <div style={{ flex: 1 }} />
      {tab === 'interrogate' && selectedSuspect && (
        <span className="mono" style={{ fontSize: 11, color: 'var(--text-muted)', letterSpacing: '0.08em' }}>
          ▸ {selectedSuspect.name} 심문 중
        </span>
      )}
      {tab === 'evidence' && selectedEvidence && (
        <span className="mono" style={{ fontSize: 11, color: 'var(--gold)', letterSpacing: '0.08em' }}>
          ▸ {selectedEvidence.name}
        </span>
      )}
    </div>
  );
}

// ---------- Empty state ----------
function EmptyState({ title, desc }) {
  return (
    <div style={{
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      color: 'var(--text-muted)',
      padding: 40,
      textAlign: 'center',
    }}>
      <div style={{
        fontFamily: 'var(--font-mono)',
        fontSize: 11,
        letterSpacing: '0.2em',
        color: 'var(--gold)',
        marginBottom: 16,
      }}>NO DATA</div>
      <div style={{ fontSize: 18, color: 'var(--text-dim)', marginBottom: 8, fontFamily: 'var(--font-serif)' }}>{title}</div>
      <div style={{ fontSize: 13, maxWidth: 340 }}>{desc}</div>
    </div>
  );
}

// =============================================================
// 6. SUSPECT PROFILE
// =============================================================
function SuspectProfile({ s, suspicion, onInterrogate }) {
  const c = window.CASE;
  const evidenceList = c.evidence.filter(e => s.relatedEvidence?.includes(e.id));
  return (
    <div style={{ padding: 32, overflowY: 'auto' }}>
      <div style={{ display: 'flex', gap: 24, marginBottom: 28, alignItems: 'flex-start' }}>
        <Avatar name={s.name} tag={s.tag} size={120} suspicion={suspicion} />
        <div style={{ flex: 1 }}>
          <div className="eyebrow-gold" style={{ marginBottom: 4 }}>SUSPECT · {s.tag}</div>
          <h1 className="h-1" style={{ margin: '0 0 4px' }}>{s.name}</h1>
          <div style={{ color: 'var(--text-dim)', fontSize: 15, marginBottom: 12 }}>{s.role}</div>
          <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{s.relation}</div>

          <div style={{ marginTop: 20, maxWidth: 400 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
              <span className="eyebrow">SUSPICION · 의심도</span>
              <span className="mono" style={{ fontSize: 11, color: suspicion > 0.65 ? 'var(--red-soft)' : 'var(--gold)' }}>
                {Math.round(suspicion * 100)}%
              </span>
            </div>
            <SuspicionMeter value={suspicion} label={false} />
          </div>
        </div>
        <button className="btn btn-primary" onClick={onInterrogate}>심문하기 ▸</button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
        <ProfileSection title="공개 진술 · STATEMENT">
          <p className="serif" style={{ fontSize: 15, lineHeight: 1.6, margin: 0, fontStyle: 'italic', color: 'var(--text)' }}>
            "{s.statement}"
          </p>
        </ProfileSection>
        <ProfileSection title="알리바이 · ALIBI">
          <div style={{ fontSize: 14, color: 'var(--text)' }}>{s.alibi}</div>
        </ProfileSection>
      </div>

      <ProfileSection title="관련 증거 · RELATED EVIDENCE">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 10 }}>
          {evidenceList.map(ev => (
            <div key={ev.id} className="card" style={{ padding: 12 }}>
              <div className="mono" style={{ fontSize: 10, color: 'var(--gold)', letterSpacing: '0.1em', marginBottom: 4 }}>
                EV·{ev.id.toUpperCase().slice(0,6)}
              </div>
              <div style={{ fontSize: 13, fontWeight: 500 }}>{ev.name}</div>
            </div>
          ))}
        </div>
      </ProfileSection>
    </div>
  );
}

function ProfileSection({ title, children }) {
  return (
    <div className="card" style={{ padding: 18 }}>
      <div className="eyebrow-gold" style={{ marginBottom: 10 }}>{title}</div>
      {children}
    </div>
  );
}

// =============================================================
// 5. EVIDENCE BOARD (center detail view)
// =============================================================
function EvidenceBoard({ unlockedIds, onSelect, selected }) {
  const c = window.CASE;
  const groups = [
    { id: 'key', label: '핵심 증거', filter: ev => ev.isKey && unlockedIds.includes(ev.id) },
    { id: 'public', label: '공개 증거', filter: ev => ev.isPublic && !ev.isKey },
    { id: 'unlocked', label: '추가 해금 증거', filter: ev => !ev.isPublic && !ev.isKey && unlockedIds.includes(ev.id) },
    { id: 'locked', label: '잠긴 증거', filter: ev => !ev.isPublic && !unlockedIds.includes(ev.id) },
  ];

  if (selected) {
    return <EvidenceDetail ev={selected} onBack={() => onSelect(null)} />;
  }

  return (
    <div style={{ padding: 28, overflowY: 'auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <div>
          <div className="eyebrow-gold" style={{ marginBottom: 4 }}>EVIDENCE BOARD · 증거 보드</div>
          <h2 className="h-2" style={{ margin: 0 }}>수집된 증거 카드</h2>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Pill>해금 {unlockedIds.length} / {c.evidence.length}</Pill>
          <Pill>핵심 {c.evidence.filter(e => e.isKey && unlockedIds.includes(e.id)).length} / {c.evidence.filter(e => e.isKey).length}</Pill>
        </div>
      </div>

      {groups.map(g => {
        const items = c.evidence.filter(g.filter);
        if (!items.length) return null;
        return (
          <div key={g.id} style={{ marginBottom: 28 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
              <span className="eyebrow" style={{ color: g.id === 'key' ? 'var(--gold)' : g.id === 'locked' ? 'var(--text-muted)' : 'var(--text-dim)' }}>
                {g.label}
              </span>
              <div style={{ flex: 1, height: 1, background: 'var(--border)' }} />
              <span className="mono" style={{ fontSize: 10, color: 'var(--text-muted)' }}>{items.length}</span>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
              {items.map(ev => (
                <EvidenceCard
                  key={ev.id}
                  ev={ev}
                  onClick={() => g.id !== 'locked' && onSelect(ev)}
                  locked={g.id === 'locked'}
                />
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function EvidenceDetail({ ev, onBack }) {
  const c = window.CASE;
  const related = c.suspects.filter(s => ev.related?.includes(s.id));
  return (
    <div style={{ padding: 28, overflowY: 'auto' }}>
      <button className="btn btn-ghost" onClick={onBack} style={{ marginBottom: 16, padding: '6px 10px' }}>
        ← 증거 보드로
      </button>

      <div className="dossier" style={{ padding: 28, position: 'relative' }}>
        {ev.isKey && (
          <span className="stamp gold" style={{ position: 'absolute', top: 16, right: 16, fontSize: 10 }}>KEY EVIDENCE</span>
        )}
        <div className="mono" style={{ fontSize: 11, color: 'var(--gold)', letterSpacing: '0.18em', marginBottom: 8 }}>
          EV·{ev.id.toUpperCase()}
        </div>
        <h2 className="h-1" style={{ margin: '0 0 16px' }}>{ev.name}</h2>
        <p style={{ fontSize: 15, color: 'var(--text)', lineHeight: 1.65, margin: '0 0 24px', maxWidth: 720 }}>
          {ev.desc}
        </p>

        <hr className="hair" style={{ marginBottom: 20 }} />

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
          <DetailField label="발견 위치" value={ev.location} />
          <DetailField label="중요도" value={'★'.repeat(ev.importance) + '☆'.repeat(3 - ev.importance)} />
          <DetailField label="공개 여부" value={ev.isPublic ? '공개' : '해금됨'} />
        </div>

        <div style={{ marginBottom: 16 }}>
          <div className="eyebrow-gold" style={{ marginBottom: 8 }}>관련 추론 · INFERENCE</div>
          <p className="serif" style={{
            fontSize: 15,
            lineHeight: 1.65,
            color: 'var(--text-dim)',
            margin: 0,
            padding: 16,
            background: 'rgba(212,162,76,0.04)',
            borderLeft: '2px solid var(--gold)',
            fontStyle: 'italic',
          }}>
            "{ev.inference}"
          </p>
        </div>

        {related.length > 0 && (
          <div>
            <div className="eyebrow-gold" style={{ marginBottom: 10 }}>관련 인물 · RELATED PERSONS</div>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              {related.map(s => (
                <div key={s.id} style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  padding: '8px 12px',
                  border: '1px solid var(--border)',
                  borderRadius: 3,
                }}>
                  <Avatar name={s.name} tag={s.tag} size={28} />
                  <div>
                    <div style={{ fontSize: 12, fontWeight: 500 }}>{s.name}</div>
                    <div className="mono" style={{ fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.08em' }}>{s.tag}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function DetailField({ label, value }) {
  return (
    <div>
      <div className="eyebrow" style={{ marginBottom: 4 }}>{label}</div>
      <div style={{ fontSize: 13, color: 'var(--text)' }}>{value}</div>
    </div>
  );
}

// =============================================================
// 8. TIMELINE (center detail view)
// =============================================================
function TimelinePanel() {
  const c = window.CASE;
  const [filter, setFilter] = useState('all'); // all | confirmed | suspect | conflicts
  const items = c.timeline.filter(t => {
    if (filter === 'confirmed') return t.confirmed && !t.conflict;
    if (filter === 'conflicts') return t.conflict;
    if (filter === 'suspect') return !t.confirmed;
    return true;
  });

  return (
    <div style={{ padding: 28, overflowY: 'auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <div>
          <div className="eyebrow-gold" style={{ marginBottom: 4 }}>TIMELINE · 사건 타임라인</div>
          <h2 className="h-2" style={{ margin: 0 }}>2025.11.07 21:00 ~ 23:59</h2>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          {[
            { id: 'all', label: '전체' },
            { id: 'confirmed', label: '확인됨' },
            { id: 'suspect', label: '주장' },
            { id: 'conflicts', label: '모순' },
          ].map(f => (
            <button key={f.id}
              className="btn"
              onClick={() => setFilter(f.id)}
              style={{
                padding: '6px 10px',
                fontSize: 10,
                color: filter === f.id ? 'var(--gold)' : 'var(--text-muted)',
                borderColor: filter === f.id ? 'var(--gold)' : 'var(--border)',
                background: filter === f.id ? 'var(--gold-dim)' : 'transparent',
              }}
            >{f.label}</button>
          ))}
        </div>
      </div>

      <div style={{ position: 'relative', paddingLeft: 28 }}>
        {/* Vertical line */}
        <div style={{ position: 'absolute', left: 8, top: 0, bottom: 0, width: 1, background: 'var(--border)' }} />

        {items.map((t, i) => (
          <div key={i} style={{
            position: 'relative',
            paddingBottom: 16,
          }}>
            {/* Node */}
            <div style={{
              position: 'absolute',
              left: -28,
              top: 4,
              width: 17,
              height: 17,
              border: '2px solid',
              borderColor: t.conflict ? 'var(--red-soft)' : t.confirmed ? 'var(--gold)' : 'var(--text-muted)',
              background: 'var(--bg)',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              {t.conflict && <span style={{ width: 7, height: 7, background: 'var(--red-soft)', borderRadius: '50%' }} />}
            </div>

            <div className="card" style={{
              padding: 14,
              borderColor: t.conflict ? 'rgba(230,57,79,0.35)' : 'var(--border)',
              background: t.conflict ? 'rgba(200,16,46,0.05)' : 'var(--surface)',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
                <span className="mono" style={{
                  fontSize: 14,
                  fontWeight: 600,
                  color: t.conflict ? 'var(--red-soft)' : 'var(--gold)',
                  letterSpacing: '0.04em',
                }}>{t.time}</span>
                {t.conflict && <span className="tag red" style={{ fontSize: 9, padding: '1px 6px' }}>CONFLICT</span>}
                {!t.confirmed && <span className="tag" style={{ fontSize: 9, padding: '1px 6px' }}>CLAIM</span>}
              </div>
              <div style={{ fontSize: 13, color: 'var(--text)', lineHeight: 1.5 }}>{t.event}</div>
              {t.related && t.related.length > 0 && (
                <div className="mono" style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 8, letterSpacing: '0.08em' }}>
                  ▸ {t.related.map(r => window.CASE.suspects.find(s => s.id === r)?.tag).filter(Boolean).join(' · ')}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// =============================================================
// Right panel — compact evidence list
// =============================================================
function RightEvidenceList({ unlockedIds, onSelect, selectedId }) {
  const c = window.CASE;
  const visibleEv = c.evidence.filter(e => e.isPublic || unlockedIds.includes(e.id));
  const lockedEv = c.evidence.filter(e => !e.isPublic && !unlockedIds.includes(e.id));

  return (
    <div style={{ padding: 12 }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {visibleEv.map(ev => (
          <button
            key={ev.id}
            onClick={() => onSelect(ev)}
            className="card"
            style={{
              padding: 10,
              textAlign: 'left',
              borderColor: selectedId === ev.id ? 'var(--gold)' : ev.isKey ? 'rgba(212,162,76,0.3)' : 'var(--border)',
              background: selectedId === ev.id ? 'var(--gold-dim)' : 'var(--surface)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{
                width: 6, height: 6,
                borderRadius: '50%',
                background: ev.isKey ? 'var(--gold)' : 'var(--text-muted)',
                flexShrink: 0,
              }} />
              <span style={{ fontSize: 12, fontWeight: 500, flex: 1 }}>{ev.name}</span>
              {ev.isKey && <span className="mono" style={{ fontSize: 9, color: 'var(--gold)', letterSpacing: '0.1em' }}>KEY</span>}
            </div>
          </button>
        ))}
      </div>

      {lockedEv.length > 0 && (
        <>
          <div className="eyebrow" style={{ marginTop: 16, marginBottom: 8, fontSize: 10 }}>잠긴 증거 · {lockedEv.length}</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {lockedEv.map(ev => (
              <div key={ev.id} className="card" style={{
                padding: 10,
                opacity: 0.5,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}>
                <span style={{ fontSize: 10 }}>🔒</span>
                <span style={{ fontSize: 11, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', letterSpacing: '0.1em' }}>
                  ▒▒▒▒▒▒▒▒
                </span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

// ---------- Right panel — compact timeline ----------
function RightTimeline() {
  const c = window.CASE;
  return (
    <div style={{ padding: 16 }}>
      <div className="eyebrow-gold" style={{ marginBottom: 12 }}>실제 타임라인</div>
      <div style={{ position: 'relative', paddingLeft: 16 }}>
        <div style={{ position: 'absolute', left: 4, top: 6, bottom: 6, width: 1, background: 'var(--border)' }} />
        {c.timeline.map((t, i) => (
          <div key={i} style={{ position: 'relative', paddingBottom: 10 }}>
            <div style={{
              position: 'absolute',
              left: -16,
              top: 5,
              width: 9, height: 9,
              borderRadius: '50%',
              background: t.conflict ? 'var(--red-soft)' : t.confirmed ? 'var(--gold)' : 'var(--text-muted)',
            }} />
            <div className="mono" style={{ fontSize: 11, color: t.conflict ? 'var(--red-soft)' : 'var(--gold)', letterSpacing: '0.04em', marginBottom: 2 }}>
              {t.time}
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-dim)', lineHeight: 1.45 }}>{t.event}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, {
  Dashboard, SuspectProfile, EvidenceBoard, TimelinePanel,
  RightEvidenceList, RightTimeline,
});
