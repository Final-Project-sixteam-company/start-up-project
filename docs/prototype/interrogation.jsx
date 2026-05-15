// 7. Interrogation chat — uses window.claude.complete with persona system prompt

const STARTER_QUESTIONS = [
  "사건 당시 어디에 있었습니까?",
  "피해자와 마지막으로 대화한 것은 언제입니까?",
  "데모데이 전날 데모룸 근처에 간 적 있습니까?",
  "왜 처음 진술과 다르게 말했습니까?",
];

function buildSystemPrompt(suspect) {
  const c = window.CASE;
  const publicFacts = `
[사건 개요]
${c.summary}
피해자: ${c.victim.name} (${c.victim.role}). ${c.victim.notes}
사건 장소: ${c.location}
사건 시각: ${c.date}

[당신의 캐릭터]
${suspect.persona}

[공개 진술 (당신이 이미 진술한 내용)]
${suspect.statement}

[알리바이]
${suspect.alibi}

[중요 행동 규칙]
- 당신은 추리게임 NPC입니다. 사용자는 탐정 역할입니다.
- 한국어로 답변하세요. 답변은 보통 2~4문장.
- 캐릭터에 맞는 어투를 유지하세요.
- 설정에 없는 사실은 절대 만들지 마세요. 모르면 "기억나지 않습니다" 또는 "잘 모르겠습니다".
- 플레이어가 묻지 않은 비밀을 먼저 말하지 마세요.
- 결정적인 증거가 제시되기 전까지는 진실을 부인할 수 있습니다.
- 강한 증거(이름이 명시된 증거 ID 또는 구체적인 정황)가 제시되면 일부 인정하고 흔들리는 모습을 보이세요.
- 자신의 범행을 자백하지 마세요(범인일 경우에도) — 단, 결정적 증거 2개 이상이 동시에 제시되면 마지못해 부분적으로 인정 가능.
`.trim();
  return publicFacts;
}

function InterrogationChat({ suspect, dispatch, state }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showEvidenceSheet, setShowEvidenceSheet] = useState(false);
  const [pendingEvidence, setPendingEvidence] = useState(null);
  const scrollRef = useRef(null);
  const inputRef = useRef(null);

  // Per-suspect message store
  const storeKey = `chat:${suspect.id}`;
  useEffect(() => {
    const stored = state.chats[suspect.id] || [];
    setMessages(stored);
    setPendingEvidence(null);
    setInput('');
    setTimeout(() => inputRef.current?.focus(), 50);
  }, [suspect.id]);

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, isLoading]);

  const persist = (newMessages) => {
    setMessages(newMessages);
    dispatch({ type: 'SET_CHAT', payload: { suspectId: suspect.id, messages: newMessages } });
  };

  const ask = async (question, evidence) => {
    if (!question.trim() && !evidence) return;
    setIsLoading(true);

    const userText = evidence
      ? `[증거 제시: ${evidence.name}] ${evidence.desc}\n\n${question || '이 증거에 대해 설명해주세요.'}`
      : question;

    const newMsgs = [...messages, { role: 'user', text: question, evidence, time: now() }];
    persist(newMsgs);
    setInput('');
    setPendingEvidence(null);

    // Build conversation for claude
    const convo = newMsgs.map(m => ({
      role: m.role === 'user' ? 'user' : 'assistant',
      content: m.role === 'user'
        ? (m.evidence ? `[증거: ${m.evidence.name}] ${m.evidence.desc}\n질문: ${m.text}` : m.text)
        : m.text,
    }));

    try {
      const systemPrompt = buildSystemPrompt(suspect);
      // window.claude.complete only accepts messages
      const fullMessages = [
        { role: 'user', content: `당신이 따라야 할 시스템 지침입니다:\n\n${systemPrompt}\n\n이제 사용자의 첫 질문에 응답합니다. 지침을 따르세요.` },
        { role: 'assistant', content: '알겠습니다. 캐릭터에 맞게 답변하겠습니다.' },
        ...convo,
      ];

      let reply;
      if (window.claude?.complete) {
        reply = await window.claude.complete({ messages: fullMessages });
      } else {
        reply = mockReply(suspect, question, evidence);
      }

      // Update suspicion if evidence was presented to the suspect
      if (evidence && evidence.related?.includes(suspect.id)) {
        dispatch({ type: 'BUMP_SUSPICION', payload: { id: suspect.id, delta: 0.06 } });
      }

      // Unlock follow-up evidence after first 2 questions
      if (newMsgs.filter(m => m.role === 'user').length >= 2 && suspect.id === 'park') {
        dispatch({ type: 'UNLOCK_EVIDENCE', payload: 'access-log' });
      }
      if (evidence?.id === 'coffee-receipt' && suspect.id === 'park') {
        dispatch({ type: 'UNLOCK_EVIDENCE', payload: 'lobby-cctv' });
      }
      if (evidence?.id === 'access-log' && suspect.id === 'park') {
        dispatch({ type: 'UNLOCK_EVIDENCE', payload: 'epipen-missing' });
      }
      if (evidence?.id === 'slack-message') {
        dispatch({ type: 'UNLOCK_EVIDENCE', payload: 'phone-location' });
      }

      persist([...newMsgs, { role: 'npc', text: reply, time: now() }]);
    } catch (e) {
      console.error(e);
      persist([...newMsgs, {
        role: 'npc',
        text: mockReply(suspect, question, evidence),
        time: now(),
      }]);
    } finally {
      setIsLoading(false);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  };

  const c = window.CASE;
  const visibleEv = c.evidence.filter(e => e.isPublic || state.unlockedEvidence.includes(e.id));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
      {/* Suspect header strip */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        padding: '14px 24px',
        borderBottom: '1px solid var(--border)',
        background: 'var(--bg-elevated)',
      }}>
        <Avatar name={suspect.name} tag={suspect.tag} size={42} suspicion={state.suspicion[suspect.id] ?? suspect.suspicion} />
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 15, fontWeight: 600 }}>{suspect.name}</span>
            <span className="mono" style={{ fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em' }}>· {suspect.tag}</span>
            <span style={{ width: 1, height: 12, background: 'var(--border)' }} />
            <span style={{ fontSize: 12, color: 'var(--text-dim)' }}>{suspect.role}</span>
          </div>
          <div className="mono" style={{ fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.08em', marginTop: 2 }}>
            ▸ INTERROGATION ROOM A · 녹취 진행 중
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className="eyebrow" style={{ fontSize: 9 }}>의심도</span>
          <div style={{ width: 80 }}>
            <SuspicionMeter value={state.suspicion[suspect.id] ?? suspect.suspicion} label={false} />
          </div>
          <span className="mono" style={{ fontSize: 11, color: 'var(--red-soft)' }}>
            {Math.round((state.suspicion[suspect.id] ?? suspect.suspicion) * 100)}%
          </span>
        </div>
      </div>

      {/* Chat scroll */}
      <div ref={scrollRef} style={{
        flex: 1,
        overflowY: 'auto',
        padding: '24px 24px 8px',
        display: 'flex',
        flexDirection: 'column',
        gap: 14,
      }}>
        {messages.length === 0 && (
          <div className="fade-in" style={{
            padding: '20px 24px',
            background: 'var(--surface)',
            border: '1px dashed var(--border)',
            borderRadius: 3,
            color: 'var(--text-dim)',
            fontSize: 13,
            lineHeight: 1.6,
          }}>
            <div className="eyebrow-gold" style={{ marginBottom: 8 }}>INTERROGATION STARTED</div>
            <strong style={{ color: 'var(--text)' }}>{suspect.name}</strong> 씨가 자리에 앉았습니다.
            질문을 시작하거나, 우측 증거 보드에서 증거를 제시하세요.
            <div style={{ marginTop: 12, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {STARTER_QUESTIONS.map(q => (
                <button key={q} className="btn" style={{ padding: '6px 10px', fontSize: 11, textTransform: 'none', letterSpacing: 0 }}
                  onClick={() => ask(q)}>
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m, i) => (
          <ChatBubble key={i} m={m} suspect={suspect} />
        ))}

        {isLoading && (
          <div className="fade-in" style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
            <Avatar name={suspect.name} tag={suspect.tag} size={28} />
            <div style={{
              padding: '10px 14px',
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: '0 8px 8px 8px',
              color: 'var(--text-muted)',
              fontSize: 13,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}>
              <Typing />
            </div>
          </div>
        )}
      </div>

      {/* Composer */}
      <div style={{
        padding: '12px 20px 18px',
        borderTop: '1px solid var(--border)',
        background: 'var(--bg-elevated)',
      }}>
        {pendingEvidence && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '8px 12px',
            marginBottom: 10,
            background: 'var(--gold-dim)',
            border: '1px solid rgba(212,162,76,0.4)',
            borderRadius: 3,
          }}>
            <span className="mono" style={{ fontSize: 10, color: 'var(--gold)', letterSpacing: '0.1em' }}>▸ EVIDENCE</span>
            <span style={{ fontSize: 12, fontWeight: 500, flex: 1 }}>{pendingEvidence.name}</span>
            <button onClick={() => setPendingEvidence(null)} style={{ color: 'var(--text-muted)', fontSize: 16 }}>×</button>
          </div>
        )}

        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className="btn"
            style={{ padding: '0 12px', fontSize: 11 }}
            onClick={() => setShowEvidenceSheet(true)}
          >
            ◈ 증거 제시
          </button>
          <input
            ref={inputRef}
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); ask(input, pendingEvidence); }
            }}
            placeholder={pendingEvidence ? "증거를 가지고 어떻게 추궁하시겠습니까?" : "용의자에게 질문을 입력하세요..."}
            style={{
              flex: 1,
              padding: '12px 14px',
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 3,
              color: 'var(--text)',
              fontSize: 13,
              outline: 'none',
            }}
            onFocus={(e) => e.target.style.borderColor = 'var(--gold)'}
            onBlur={(e) => e.target.style.borderColor = 'var(--border)'}
          />
          <button
            className="btn btn-primary"
            disabled={(!input.trim() && !pendingEvidence) || isLoading}
            onClick={() => ask(input, pendingEvidence)}
            style={{ opacity: (!input.trim() && !pendingEvidence) || isLoading ? 0.5 : 1 }}
          >
            전송 ▸
          </button>
        </div>

        {/* Recommended questions row */}
        <div style={{ display: 'flex', gap: 6, marginTop: 10, flexWrap: 'wrap' }}>
          {STARTER_QUESTIONS.map(q => (
            <button
              key={q}
              onClick={() => setInput(q)}
              style={{
                padding: '4px 10px',
                fontSize: 11,
                color: 'var(--text-muted)',
                border: '1px solid var(--border)',
                borderRadius: 999,
                background: 'transparent',
                transition: 'all 0.15s',
              }}
              onMouseEnter={e => { e.target.style.color = 'var(--gold)'; e.target.style.borderColor = 'var(--gold)'; }}
              onMouseLeave={e => { e.target.style.color = 'var(--text-muted)'; e.target.style.borderColor = 'var(--border)'; }}
            >
              {q}
            </button>
          ))}
        </div>
      </div>

      {/* Evidence sheet modal */}
      {showEvidenceSheet && (
        <EvidenceSheet
          evidence={visibleEv}
          onPick={(ev) => { setPendingEvidence(ev); setShowEvidenceSheet(false); inputRef.current?.focus(); }}
          onClose={() => setShowEvidenceSheet(false)}
        />
      )}
    </div>
  );
}

function ChatBubble({ m, suspect }) {
  const isUser = m.role === 'user';
  return (
    <div className="fade-in" style={{
      display: 'flex',
      gap: 10,
      flexDirection: isUser ? 'row-reverse' : 'row',
      alignItems: 'flex-end',
    }}>
      {!isUser && <Avatar name={suspect.name} tag={suspect.tag} size={28} />}
      {isUser && (
        <div style={{
          width: 28, height: 28,
          background: 'var(--gold)',
          color: '#14100a',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          fontWeight: 700,
          letterSpacing: '0.08em',
        }}>YOU</div>
      )}
      <div style={{
        maxWidth: '72%',
        display: 'flex',
        flexDirection: 'column',
        alignItems: isUser ? 'flex-end' : 'flex-start',
      }}>
        {m.evidence && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '6px 10px',
            background: 'var(--gold-dim)',
            border: '1px solid rgba(212,162,76,0.35)',
            borderRadius: 3,
            marginBottom: 4,
            fontSize: 12,
          }}>
            <span className="mono" style={{ fontSize: 9, color: 'var(--gold)', letterSpacing: '0.1em' }}>◈ EVIDENCE</span>
            <span style={{ fontWeight: 500 }}>{m.evidence.name}</span>
          </div>
        )}
        <div style={{
          padding: '10px 14px',
          background: isUser ? 'var(--gold-dim)' : 'var(--surface)',
          border: '1px solid',
          borderColor: isUser ? 'rgba(212,162,76,0.35)' : 'var(--border)',
          borderRadius: isUser ? '8px 0 8px 8px' : '0 8px 8px 8px',
          color: 'var(--text)',
          fontSize: 14,
          lineHeight: 1.55,
          whiteSpace: 'pre-wrap',
          textWrap: 'pretty',
        }}>
          {m.text}
        </div>
        {m.time && (
          <span className="mono" style={{ fontSize: 9, color: 'var(--text-muted)', marginTop: 4, letterSpacing: '0.08em' }}>
            {m.time}
          </span>
        )}
      </div>
    </div>
  );
}

function Typing() {
  return (
    <span style={{ display: 'inline-flex', gap: 3 }}>
      {[0, 0.15, 0.3].map(d => (
        <span key={d} style={{
          width: 6, height: 6, borderRadius: '50%',
          background: 'var(--text-dim)',
          animation: `pulse 0.9s ${d}s infinite ease-in-out`,
        }} />
      ))}
      <style>{`@keyframes pulse { 0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); } 40% { opacity: 1; transform: scale(1); } }`}</style>
    </span>
  );
}

function EvidenceSheet({ evidence, onPick, onClose }) {
  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0,0,0,0.75)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 100,
      backdropFilter: 'blur(4px)',
    }} onClick={onClose}>
      <div className="card" onClick={e => e.stopPropagation()} style={{
        width: 560,
        maxHeight: '80vh',
        display: 'flex',
        flexDirection: 'column',
        background: 'var(--bg-elevated)',
        padding: 24,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div>
            <div className="eyebrow-gold">EVIDENCE SHEET</div>
            <h3 className="h-2" style={{ margin: '4px 0 0' }}>제시할 증거를 선택하세요</h3>
          </div>
          <button onClick={onClose} style={{ color: 'var(--text-muted)', fontSize: 20 }}>×</button>
        </div>
        <div style={{ overflowY: 'auto', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {evidence.map(ev => (
            <EvidenceCard key={ev.id} ev={ev} onClick={() => onPick(ev)} compact />
          ))}
        </div>
      </div>
    </div>
  );
}

function now() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}

// Fallback mock if window.claude is unavailable
function mockReply(suspect, q, ev) {
  if (ev?.id === 'coffee-receipt' && suspect.id === 'park') {
    return "그건… 제가 마시려고 산 겁니다. 대표에게 준 건 아니에요. 그때 정신이 없어서 깜빡한 것 같습니다.";
  }
  if (ev?.id === 'access-log' && suspect.id === 'park') {
    return "잠깐만요. 22시 14분이요? 음… 잠깐 자료 두러 갔던 것 같습니다. 거기서 대표를 본 적은 없습니다.";
  }
  if (suspect.id === 'park') {
    return "사건 당시 저는 재무팀 자리에서 투자자료를 정리하고 있었습니다. 데모룸 근처에는 간 적 없습니다.";
  }
  if (suspect.id === 'lee') {
    return "리허설 마치고 옥상에서 잠깐 담배 피웠어요. 그게 다입니다.";
  }
  if (suspect.id === 'seo') {
    return "회의실 B에서 자료 리뷰하고 있었어요. 강 대표와는 사적으로 만난 적 없습니다.";
  }
  if (suspect.id === 'kim') {
    return "22시 55분쯤 슬랙 받고 데모룸 갔는데, 대표님은 안 계셨어요. USB만 챙겨서 나왔어요.";
  }
  return "특별히 드릴 말씀은 없습니다.";
}

Object.assign(window, { InterrogationChat });
