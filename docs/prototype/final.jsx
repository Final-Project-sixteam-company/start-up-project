// 10. Final deduction submit + 11. Result / verdict screen

// =============================================================
// 10. FINAL DEDUCTION
// =============================================================
function FinalDeductionScreen({ go, state, dispatch }) {
  const c = window.CASE;
  const [culprit, setCulprit] = useState(null);
  const [motive, setMotive] = useState('');
  const [method, setMethod] = useState('');
  const [cover, setCover] = useState('');
  const [chosen, setChosen] = useState([]);

  const toggleEv = (id) => {
    if (chosen.includes(id)) setChosen(chosen.filter(x => x !== id));
    else if (chosen.length < 3) setChosen([...chosen, id]);
  };

  const ready = culprit && motive.length > 10 && method.length > 10 && chosen.length === 3;

  const submit = () => {
    const score = computeScore({ culprit, motive, method, cover, chosen });
    dispatch({ type: 'SUBMIT_DEDUCTION', payload: { culprit, motive, method, cover, chosen, score } });
    go('result');
  };

  const visibleEv = c.evidence.filter(e => e.isPublic || state.unlockedEvidence.includes(e.id));

  return (
    <div className="route" style={{ padding: '32px 48px 96px', maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <button onClick={() => go('dashboard')} className="btn btn-ghost" style={{ padding: '6px 10px' }}>← 대시보드</button>
        <span className="eyebrow-red">FINAL DEDUCTION · 최종 추리 제출</span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: 32 }}>
        {/* Left — form */}
        <div>
          <h1 className="h-1" style={{ margin: '0 0 8px' }}>당신의 결론을 제출하세요</h1>
          <p style={{ color: 'var(--text-dim)', fontSize: 14, marginBottom: 28, maxWidth: 540 }}>
            범인, 동기, 범행 방법, 결정적 증거 3개를 제출하면 AI 판정이 진행됩니다.
            제출 후에는 수정할 수 없습니다.
          </p>

          {/* Culprit selector */}
          <FormBlock label="범인 · CULPRIT" required>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 8 }}>
              {c.suspects.map(s => (
                <button
                  key={s.id}
                  onClick={() => setCulprit(s.id)}
                  className="card"
                  style={{
                    padding: 12,
                    cursor: 'pointer',
                    borderColor: culprit === s.id ? 'var(--red)' : 'var(--border)',
                    background: culprit === s.id ? 'var(--red-dim)' : 'var(--surface)',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: 6,
                    textAlign: 'center',
                  }}
                >
                  <Avatar name={s.name} tag={s.tag} size={44} suspicion={culprit === s.id ? 1 : (state.suspicion[s.id] ?? s.suspicion)} />
                  <div style={{ fontSize: 12, fontWeight: 600 }}>{s.name}</div>
                  <div className="mono" style={{ fontSize: 9, color: 'var(--text-muted)', letterSpacing: '0.08em' }}>{s.tag}</div>
                </button>
              ))}
            </div>
          </FormBlock>

          <FormBlock label="범행 동기 · MOTIVE" required>
            <textarea
              value={motive}
              onChange={e => setMotive(e.target.value)}
              placeholder="왜 범행을 저질렀는가? 어떤 압박/이익이 있었는가?"
              rows={3}
              style={inputStyle}
            />
          </FormBlock>

          <FormBlock label="범행 방법 · METHOD" required>
            <textarea
              value={method}
              onChange={e => setMethod(e.target.value)}
              placeholder="어떤 방법으로 범행했는가? 사용된 도구, 트릭은?"
              rows={3}
              style={inputStyle}
            />
          </FormBlock>

          <FormBlock label="은폐 방법 · COVER-UP">
            <textarea
              value={cover}
              onChange={e => setCover(e.target.value)}
              placeholder="범행을 어떻게 숨기려 했는가?"
              rows={2}
              style={inputStyle}
            />
          </FormBlock>
        </div>

        {/* Right — evidence selection + submit */}
        <div>
          <div className="card" style={{ padding: 20, marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <span className="eyebrow-gold">결정적 증거 · 3개 선택</span>
              <span className="mono" style={{ fontSize: 11, color: chosen.length === 3 ? 'var(--gold)' : 'var(--text-muted)' }}>
                {chosen.length} / 3
              </span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 380, overflowY: 'auto', paddingRight: 4 }}>
              {visibleEv.map(ev => (
                <label
                  key={ev.id}
                  style={{
                    display: 'flex',
                    gap: 10,
                    padding: 10,
                    border: '1px solid',
                    borderColor: chosen.includes(ev.id) ? 'var(--gold)' : 'var(--border)',
                    background: chosen.includes(ev.id) ? 'var(--gold-dim)' : 'transparent',
                    borderRadius: 2,
                    cursor: !chosen.includes(ev.id) && chosen.length >= 3 ? 'not-allowed' : 'pointer',
                    opacity: !chosen.includes(ev.id) && chosen.length >= 3 ? 0.5 : 1,
                  }}
                >
                  <input
                    type="checkbox"
                    checked={chosen.includes(ev.id)}
                    onChange={() => toggleEv(ev.id)}
                    disabled={!chosen.includes(ev.id) && chosen.length >= 3}
                    style={{ accentColor: 'var(--gold)' }}
                  />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 12, fontWeight: 500 }}>{ev.name}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2, overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 1, WebkitBoxOrient: 'vertical' }}>
                      {ev.desc}
                    </div>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <div className="card" style={{ padding: 18, marginBottom: 16, background: 'var(--surface-2)' }}>
            <div className="eyebrow" style={{ marginBottom: 10 }}>SUBMISSION CHECKLIST</div>
            <ChecklistRow ok={!!culprit} label="범인 선택" />
            <ChecklistRow ok={motive.length > 10} label="범행 동기 (10자 이상)" />
            <ChecklistRow ok={method.length > 10} label="범행 방법 (10자 이상)" />
            <ChecklistRow ok={chosen.length === 3} label="결정적 증거 3개" />
          </div>

          <button
            className="btn btn-danger"
            disabled={!ready}
            style={{ width: '100%', padding: '16px 18px', opacity: ready ? 1 : 0.4, cursor: ready ? 'pointer' : 'not-allowed' }}
            onClick={submit}
          >
            ▸ 최종 추리 제출
          </button>
          <div style={{ textAlign: 'center', marginTop: 10, fontSize: 11, color: 'var(--text-muted)' }}>
            제출 후에는 수정할 수 없습니다
          </div>
        </div>
      </div>
    </div>
  );
}

function FormBlock({ label, required, children }) {
  return (
    <div style={{ marginBottom: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
        <span className="eyebrow-gold">{label}</span>
        {required && <span style={{ color: 'var(--red-soft)', fontSize: 11 }}>*</span>}
      </div>
      {children}
    </div>
  );
}

const inputStyle = {
  width: '100%',
  padding: '12px 14px',
  background: 'var(--surface)',
  border: '1px solid var(--border)',
  borderRadius: 2,
  color: 'var(--text)',
  fontSize: 13,
  fontFamily: 'var(--font-sans)',
  lineHeight: 1.55,
  outline: 'none',
  resize: 'vertical',
};

function ChecklistRow({ ok, label }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '4px 0', fontSize: 12 }}>
      <span style={{
        width: 14, height: 14,
        border: '1px solid',
        borderColor: ok ? 'var(--gold)' : 'var(--border-strong)',
        background: ok ? 'var(--gold)' : 'transparent',
        color: '#14100a',
        borderRadius: 2,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: 10,
        fontWeight: 700,
      }}>{ok && '✓'}</span>
      <span style={{ color: ok ? 'var(--text)' : 'var(--text-muted)' }}>{label}</span>
    </div>
  );
}

// =============================================================
// Scoring (deterministic mock)
// =============================================================
function computeScore({ culprit, motive, method, cover, chosen }) {
  const c = window.CASE;
  const ans = c.answer;
  let total = 0;
  const breakdown = [];

  if (culprit === ans.culprit) {
    total += 40;
    breakdown.push({ ok: true, points: 40, label: '범인 정확히 지목', note: `${c.suspects.find(s => s.id === culprit).name}을(를) 범인으로 정확히 지목했습니다.` });
  } else {
    breakdown.push({ ok: false, points: 0, label: '범인 오답', note: `진범은 ${c.suspects.find(s => s.id === ans.culprit).name}입니다.` });
  }

  // Motive — keyword match
  const motiveKeys = ['자금', '횡령', '비리', '회계', '유용', '데모데이', '입막음', '감사'];
  const motiveHits = motiveKeys.filter(k => motive.includes(k)).length;
  if (motiveHits >= 2) {
    total += 15;
    breakdown.push({ ok: true, points: 15, label: '범행 동기 적중', note: '회사 자금 유용 / 회계 감사 위협을 파악했습니다.' });
  } else if (motiveHits === 1) {
    total += 7;
    breakdown.push({ ok: true, points: 7, label: '범행 동기 부분 적중', note: '동기 단서를 일부 짚었으나 핵심 키워드가 빠져 있습니다.' });
  } else {
    breakdown.push({ ok: false, points: 0, label: '범행 동기 미흡', note: '회계 비리와 데모데이 감사 위협이 핵심 동기였습니다.' });
  }

  // Method — keyword match
  const methodKeys = ['알레르기', '아몬드', '견과', '에피펜', '바꿔', '라벨'];
  const methodHits = methodKeys.filter(k => method.includes(k)).length;
  if (methodHits >= 3) {
    total += 20;
    breakdown.push({ ok: true, points: 20, label: '범행 방법 적중', note: '알레르기 + 라벨 바꿔치기 + 에피펜 차단을 모두 파악했습니다.' });
  } else if (methodHits >= 1) {
    total += 10;
    breakdown.push({ ok: true, points: 10, label: '범행 방법 부분 적중', note: '핵심 트릭을 일부 짚었습니다.' });
  } else {
    breakdown.push({ ok: false, points: 0, label: '범행 방법 미흡', note: '아몬드라떼 라벨 바꿔치기와 에피펜 사전 제거가 핵심입니다.' });
  }

  // Evidence match
  const keyHits = chosen.filter(id => ans.keyEvidence.includes(id)).length;
  total += keyHits * 8;
  if (keyHits === 3) {
    breakdown.push({ ok: true, points: 24, label: '결정적 증거 3개 모두 적중', note: '찢긴 컵 라벨 · 사라진 에피펜 · 휴대폰 위치 기록' });
  } else {
    breakdown.push({
      ok: keyHits > 0,
      points: keyHits * 8,
      label: `결정적 증거 ${keyHits} / 3 적중`,
      note: keyHits < 3 ? `누락: ${ans.keyEvidence.filter(id => !chosen.includes(id)).map(id => c.evidence.find(e => e.id === id).name).join(', ')}` : '',
    });
  }

  return { total: Math.min(100, total), breakdown };
}

// =============================================================
// 11. RESULT / VERDICT
// =============================================================
function ResultScreen({ go, state }) {
  const c = window.CASE;
  const ded = state.deduction;
  if (!ded) {
    return (
      <div className="route" style={{ padding: 48, textAlign: 'center' }}>
        <div className="eyebrow-gold">NO SUBMISSION</div>
        <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => go('final')}>최종 추리로</button>
      </div>
    );
  }
  const ans = c.answer;
  const culpritOk = ded.culprit === ans.culprit;
  const grade = ded.score.total >= 90 ? 'A+' : ded.score.total >= 80 ? 'A' : ded.score.total >= 70 ? 'B+' : ded.score.total >= 60 ? 'B' : ded.score.total >= 50 ? 'C' : 'D';
  const gradeColor = ded.score.total >= 80 ? 'var(--gold)' : ded.score.total >= 60 ? 'var(--text)' : 'var(--red-soft)';

  return (
    <div className="route" style={{ padding: '32px 48px 96px', maxWidth: 1200, margin: '0 auto' }}>
      <div className="eyebrow-red" style={{ marginBottom: 12 }}>VERDICT · 사건 해설</div>

      {/* Verdict header */}
      <div className="dossier" style={{
        padding: 40,
        marginBottom: 24,
        display: 'grid',
        gridTemplateColumns: '1fr 280px',
        gap: 32,
        alignItems: 'center',
      }}>
        <div>
          <div className="mono" style={{ fontSize: 11, letterSpacing: '0.18em', color: 'var(--gold)', marginBottom: 12 }}>
            CASE CLOSED · K-2025-1107
          </div>
          <h1 className="h-display" style={{ fontSize: 48, margin: '0 0 12px' }}>
            {culpritOk ? '사건 해결.' : '범인을 놓쳤습니다.'}
          </h1>
          <p style={{ color: 'var(--text-dim)', fontSize: 15, maxWidth: 540, lineHeight: 1.6, margin: 0 }}>
            진범은 <strong style={{ color: 'var(--gold)' }}>{c.suspects.find(s => s.id === ans.culprit).name} ({c.suspects.find(s => s.id === ans.culprit).role})</strong> 입니다.
            {culpritOk
              ? ' 정확히 지목하셨습니다. 아래에서 각 항목별 채점 결과를 확인하세요.'
              : ' 당신은 다른 인물을 지목했습니다. 아래에서 단서가 어디서 어긋났는지 확인하세요.'}
          </p>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div className="eyebrow" style={{ marginBottom: 4 }}>정답률 · SCORE</div>
          <div style={{
            fontFamily: 'var(--font-serif)',
            fontWeight: 700,
            fontSize: 84,
            color: gradeColor,
            lineHeight: 1,
            letterSpacing: '-0.02em',
          }}>{ded.score.total}<span style={{ fontSize: 32, color: 'var(--text-muted)' }}>%</span></div>
          <div style={{ marginTop: 8 }}>
            <span className="stamp gold" style={{ fontSize: 12, padding: '4px 14px', color: gradeColor, borderColor: gradeColor }}>
              GRADE · {grade}
            </span>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 24 }}>
        {/* Left — breakdown + reconstruction */}
        <div>
          <div className="card" style={{ padding: 24, marginBottom: 16 }}>
            <div className="eyebrow-gold" style={{ marginBottom: 14 }}>SCORE BREAKDOWN · 채점 결과</div>
            {ded.score.breakdown.map((b, i) => (
              <div key={i} style={{
                display: 'flex',
                gap: 14,
                padding: '14px 0',
                borderBottom: i < ded.score.breakdown.length - 1 ? '1px solid var(--border)' : 'none',
              }}>
                <span style={{
                  width: 22, height: 22,
                  display: 'inline-flex',
                  alignItems: 'center', justifyContent: 'center',
                  borderRadius: 2,
                  background: b.ok ? 'var(--gold)' : 'var(--red)',
                  color: b.ok ? '#14100a' : '#fff',
                  fontSize: 12,
                  fontWeight: 700,
                  flexShrink: 0,
                  marginTop: 2,
                }}>{b.ok ? '✓' : '×'}</span>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span style={{ fontSize: 14, fontWeight: 500 }}>{b.label}</span>
                    <span className="mono" style={{ fontSize: 11, color: b.ok ? 'var(--gold)' : 'var(--text-muted)' }}>
                      +{b.points}
                    </span>
                  </div>
                  {b.note && <div style={{ fontSize: 12, color: 'var(--text-dim)', lineHeight: 1.5 }}>{b.note}</div>}
                </div>
              </div>
            ))}
          </div>

          <div className="card" style={{ padding: 24 }}>
            <div className="eyebrow-gold" style={{ marginBottom: 14 }}>RECONSTRUCTION · 범행 재구성</div>
            <ReconstructionTimeline />
          </div>
        </div>

        {/* Right — truth column */}
        <div>
          <div className="dossier" style={{ padding: 20, marginBottom: 16 }}>
            <div className="eyebrow-red" style={{ marginBottom: 12 }}>TRUE CULPRIT · 진범</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
              <Avatar name={c.suspects.find(s => s.id === ans.culprit).name} tag="PJM" size={64} suspicion={1} />
              <div>
                <div className="h-2" style={{ margin: 0 }}>{c.suspects.find(s => s.id === ans.culprit).name}</div>
                <div style={{ color: 'var(--text-dim)', fontSize: 13 }}>{c.suspects.find(s => s.id === ans.culprit).role}</div>
              </div>
            </div>
            <div style={{ marginTop: 16 }}>
              <div className="eyebrow" style={{ marginBottom: 6 }}>MOTIVE</div>
              <div style={{ fontSize: 13, color: 'var(--text-dim)', lineHeight: 1.55 }}>{ans.motive}</div>
            </div>
            <div style={{ marginTop: 14 }}>
              <div className="eyebrow" style={{ marginBottom: 6 }}>METHOD</div>
              <div style={{ fontSize: 13, color: 'var(--text-dim)', lineHeight: 1.55 }}>{ans.method}</div>
            </div>
            <div style={{ marginTop: 14 }}>
              <div className="eyebrow" style={{ marginBottom: 6 }}>COVER-UP</div>
              <div style={{ fontSize: 13, color: 'var(--text-dim)', lineHeight: 1.55 }}>{ans.cover}</div>
            </div>
          </div>

          <div className="card" style={{ padding: 20, marginBottom: 16 }}>
            <div className="eyebrow-gold" style={{ marginBottom: 12 }}>KEY EVIDENCE · 결정적 증거</div>
            {ans.keyEvidence.map(id => {
              const ev = c.evidence.find(e => e.id === id);
              return (
                <div key={id} style={{
                  padding: '10px 0',
                  borderBottom: '1px dashed var(--border)',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                    <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--gold)' }} />
                    <span style={{ fontSize: 13, fontWeight: 500 }}>{ev.name}</span>
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)', marginLeft: 12, lineHeight: 1.5 }}>
                    {ev.inference}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="card" style={{ padding: 18 }}>
            <div className="eyebrow-gold" style={{ marginBottom: 10 }}>AI DETECTIVE REVIEW</div>
            <p className="serif" style={{ fontSize: 14, lineHeight: 1.65, color: 'var(--text-dim)', margin: 0, fontStyle: 'italic' }}>
              {generateReview(ded, ans)}
            </p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 16 }}>
            <button className="btn btn-primary" style={{ width: '100%' }} onClick={() => go('library')}>
              다음 사건 플레이 ▸
            </button>
            <button className="btn" style={{ width: '100%' }} onClick={() => go('landing')}>
              홈으로
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function ReconstructionTimeline() {
  const c = window.CASE;
  return (
    <div style={{ position: 'relative', paddingLeft: 24 }}>
      <div style={{ position: 'absolute', left: 6, top: 0, bottom: 0, width: 1, background: 'var(--gold)', opacity: 0.4 }} />
      {c.timeline.map((t, i) => (
        <div key={i} style={{ position: 'relative', paddingBottom: 14 }}>
          <div style={{
            position: 'absolute',
            left: -24,
            top: 4,
            width: 13, height: 13,
            border: '2px solid var(--gold)',
            background: t.conflict ? 'var(--red)' : 'var(--bg)',
            borderRadius: '50%',
          }} />
          <div className="mono" style={{ fontSize: 12, color: 'var(--gold)', letterSpacing: '0.04em', marginBottom: 2 }}>
            {t.time}
          </div>
          <div style={{ fontSize: 13, color: 'var(--text)', lineHeight: 1.5 }}>{t.event}</div>
        </div>
      ))}
    </div>
  );
}

function generateReview(ded, ans) {
  if (ded.culprit !== ans.culprit) {
    return "당신은 표면적 단서에 휘둘렸습니다. 진범의 알리바이가 무너지는 지점(출입 로그와 본인 진술의 모순)을 발견했다면 결과가 달랐을 것입니다. 다음 사건에서는 진술과 기록을 항상 교차 검증하세요.";
  }
  if (ded.score.total >= 90) {
    return "사건의 핵심 구조를 전부 파악했습니다. 동기·방법·은폐 세 축이 어떻게 맞물리는지 정확히 추론했습니다. 탁월한 탐정입니다.";
  }
  if (ded.score.total >= 70) {
    return "범인과 큰 줄기는 맞혔지만 일부 디테일을 놓쳤습니다. 특히 에피펜이 사라진 사실은 단순한 실수가 아니라 계획범행을 입증하는 핵심 물증입니다.";
  }
  return "범인은 맞혔지만 추리의 디테일이 부족합니다. 증거 카드를 더 꼼꼼히 조합하면 다음엔 더 높은 점수를 받을 수 있습니다.";
}

Object.assign(window, { FinalDeductionScreen, ResultScreen });
