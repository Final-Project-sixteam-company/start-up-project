// Shared UI primitives — CaseLab AI

const { useState, useEffect, useRef, useMemo, useCallback } = React;

// Logo / brand mark
function LogoMark({ size = "md" }) {
  const sz = size === "lg" ? 16 : 12;
  return (
    <div className="logo-mark" style={{ fontSize: sz }}>
      <span className="dot" />
      CaseLab<span style={{ color: 'var(--gold)' }}>·AI</span>
    </div>
  );
}

// Avatar — initial-based silhouette (no AI-drawn faces)
function Avatar({ name, tag, size = 44, suspicion }) {
  const initials = name ? name.charAt(0) : "?";
  const heatColor = suspicion == null
    ? "var(--border-strong)"
    : suspicion > 0.65
    ? "var(--red-soft)"
    : suspicion > 0.4
    ? "var(--gold)"
    : "var(--border-strong)";
  return (
    <div
      className="avatar"
      style={{ width: size, height: size, borderColor: heatColor }}
    >
      <svg viewBox="0 0 44 44" width={size} height={size} style={{ position: 'absolute', inset: 0 }}>
        <defs>
          <pattern id={`stripes-${tag}`} patternUnits="userSpaceOnUse" width="4" height="4" patternTransform="rotate(45)">
            <rect width="4" height="4" fill="#161c33" />
            <rect width="1" height="4" fill="#1f2742" />
          </pattern>
        </defs>
        <rect width="44" height="44" fill={`url(#stripes-${tag})`} />
      </svg>
      <span style={{
        position: 'relative',
        fontFamily: 'var(--font-serif)',
        fontSize: size * 0.42,
        fontWeight: 700,
        color: 'var(--text)',
      }}>{initials}</span>
    </div>
  );
}

// Suspicion meter — small bar with notches
function SuspicionMeter({ value, label = true }) {
  const pct = Math.round(value * 100);
  const color = value > 0.65 ? "var(--red-soft)" : value > 0.4 ? "var(--gold)" : "var(--text-muted)";
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, width: '100%' }}>
      <div style={{
        flex: 1,
        height: 6,
        background: 'var(--bg-elevated)',
        border: '1px solid var(--border)',
        borderRadius: 1,
        position: 'relative',
        overflow: 'hidden',
      }}>
        <div style={{
          height: '100%',
          width: `${pct}%`,
          background: color,
          transition: 'width 0.4s ease, background 0.4s ease',
        }} />
        {/* Notches */}
        {[25, 50, 75].map(n => (
          <div key={n} style={{
            position: 'absolute',
            left: `${n}%`,
            top: 0,
            bottom: 0,
            width: 1,
            background: 'var(--bg)',
          }} />
        ))}
      </div>
      {label && (
        <span className="mono" style={{ fontSize: 10, color, minWidth: 28, textAlign: 'right' }}>
          {pct}%
        </span>
      )}
    </div>
  );
}

// Evidence card — supports public/locked/key states
function EvidenceCard({ ev, onClick, locked, isNew, compact }) {
  const stateColor = locked ? "var(--text-muted)" : ev.isKey ? "var(--gold)" : "var(--text)";
  return (
    <button
      onClick={onClick}
      className="card"
      style={{
        padding: compact ? 12 : 16,
        textAlign: 'left',
        background: locked ? 'rgba(7,9,15,0.6)' : 'var(--surface)',
        borderColor: ev.isKey && !locked ? 'rgba(212,162,76,0.4)' : 'var(--border)',
        position: 'relative',
        cursor: locked ? 'not-allowed' : 'pointer',
        transition: 'all 0.15s ease',
        opacity: locked ? 0.55 : 1,
        width: '100%',
      }}
      onMouseEnter={(e) => { if (!locked) e.currentTarget.style.borderColor = 'var(--gold)'; }}
      onMouseLeave={(e) => { if (!locked) e.currentTarget.style.borderColor = ev.isKey ? 'rgba(212,162,76,0.4)' : 'var(--border)'; }}
    >
      {isNew && !locked && (
        <span className="stamp gold" style={{ position: 'absolute', top: 10, right: 10, fontSize: 9, padding: '2px 6px' }}>NEW</span>
      )}
      {locked && (
        <span style={{
          position: 'absolute',
          top: 12,
          right: 12,
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          color: 'var(--text-muted)',
          letterSpacing: '0.12em',
        }}>🔒 LOCKED</span>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
        <span className="mono" style={{ fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.1em' }}>
          EV·{ev.id.toUpperCase().slice(0,6)}
        </span>
        {ev.isKey && !locked && <span className="tag gold" style={{ fontSize: 9, padding: '1px 6px' }}>KEY</span>}
        {!ev.isPublic && !locked && <span className="tag" style={{ fontSize: 9, padding: '1px 6px' }}>UNLOCKED</span>}
      </div>

      <h4 style={{
        margin: '0 0 6px',
        fontSize: compact ? 13 : 14,
        fontWeight: 600,
        color: stateColor,
        letterSpacing: '-0.005em',
      }}>
        {locked ? "▒▒▒▒▒▒▒▒▒▒" : ev.name}
      </h4>

      {!compact && (
        <p style={{
          margin: '0 0 10px',
          fontSize: 12,
          color: 'var(--text-dim)',
          lineHeight: 1.5,
          minHeight: 36,
        }}>
          {locked ? (ev.unlock ? `해금 조건: ${ev.unlock}` : "추가 조사 필요") : ev.desc}
        </p>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
        {/* Importance dots */}
        <div style={{ display: 'flex', gap: 3 }}>
          {[1,2,3].map(i => (
            <span key={i} style={{
              width: 6, height: 6, borderRadius: '50%',
              background: i <= ev.importance ? (ev.isKey ? 'var(--gold)' : 'var(--text-dim)') : 'var(--border)',
            }} />
          ))}
        </div>
        {!locked && ev.related && ev.related.length > 0 && (
          <span className="mono" style={{ fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.05em' }}>
            관련: {ev.related.map(r => window.CASE.suspects.find(s => s.id === r)?.tag).filter(Boolean).join(' · ')}
          </span>
        )}
      </div>
    </button>
  );
}

// Section header — like a file folder tab
function SectionHeader({ kicker, title, action, sub }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 }}>
        <div>
          {kicker && <div className="eyebrow-gold" style={{ marginBottom: 6 }}>{kicker}</div>}
          <h2 className="h-2" style={{ margin: 0 }}>{title}</h2>
          {sub && <div style={{ color: 'var(--text-dim)', fontSize: 13, marginTop: 4 }}>{sub}</div>}
        </div>
        {action}
      </div>
    </div>
  );
}

// File label sticker
function FileTab({ children, color = "gold" }) {
  return (
    <span style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      padding: '4px 10px 4px 8px',
      background: color === 'gold' ? 'var(--gold)' : 'var(--red)',
      color: color === 'gold' ? '#14100a' : '#fff',
      fontFamily: 'var(--font-mono)',
      fontSize: 10,
      letterSpacing: '0.16em',
      fontWeight: 600,
      textTransform: 'uppercase',
      clipPath: 'polygon(0 0, 100% 0, calc(100% - 8px) 50%, 100% 100%, 0 100%)',
      paddingRight: 16,
    }}>
      {children}
    </span>
  );
}

// Top progress strip used in dashboard
function ProgressStrip({ elapsed, unlocked, total, progress }) {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 24,
      padding: '12px 20px',
      background: 'var(--bg-elevated)',
      borderBottom: '1px solid var(--border)',
    }}>
      <Stat label="경과 시간" value={elapsed} mono />
      <Divider />
      <Stat label="해금 증거" value={`${unlocked} / ${total}`} mono />
      <Divider />
      <Stat label="사건 진행률" value={`${progress}%`} mono color="var(--gold)" />
      <div style={{ flex: 1 }} />
      <span className="mono blink" style={{ fontSize: 10, color: 'var(--red-soft)', letterSpacing: '0.18em' }}>● REC</span>
    </div>
  );
}

function Divider() {
  return <div style={{ width: 1, height: 22, background: 'var(--border)' }} />;
}

function Stat({ label, value, mono, color }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <span className="eyebrow" style={{ fontSize: 10 }}>{label}</span>
      <span style={{
        fontFamily: mono ? 'var(--font-mono)' : 'inherit',
        fontSize: 14,
        fontWeight: 600,
        color: color || 'var(--text)',
        letterSpacing: '0.04em',
      }}>{value}</span>
    </div>
  );
}

// Pill row
function PillRow({ children }) {
  return (
    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>{children}</div>
  );
}

// Export
Object.assign(window, {
  LogoMark, Avatar, SuspicionMeter, EvidenceCard, SectionHeader, FileTab,
  ProgressStrip, Stat, Divider, PillRow,
});
