import { BrandMark } from "./BrandMark";

const FEATURES: { label: string; path: string }[] = [
  {
    label: "Meetings",
    path: "M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 18.75zm0-7.5h18",
  },
  {
    label: "Resolutions",
    path: "M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z",
  },
  {
    label: "Compliance",
    path: "M9 12h3.75M9 15h3.75M9 18h3.75M17.25 4.5v15a.75.75 0 01-.75.75H6a.75.75 0 01-.75-.75v-15A.75.75 0 016 3.75h10.5a.75.75 0 01.75.75zM12.75 7.5a.75.75 0 01-.75.75h-2.25a.75.75 0 01-.75-.75V6.75a.75.75 0 01.75-.75H12a.75.75 0 01.75.75v.75z",
  },
  {
    label: "Documents",
    path: "M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z",
  },
];

function AuthHeroIllustration() {
  return (
    <svg className="auth-hero-illustration" viewBox="0 0 320 220" aria-hidden="true">
      <g transform="rotate(-6 140 105)">
        <rect x="40" y="20" width="200" height="150" rx="10" fill="#f7f4ec" stroke="#b9852b" strokeWidth="2" />
        <rect x="40" y="20" width="200" height="34" rx="10" fill="#12406e" opacity="0.1" />
        <rect x="60" y="36" width="70" height="8" rx="4" fill="#12406e" opacity="0.55" />
        <rect x="60" y="76" width="160" height="6" rx="3" fill="#12406e" opacity="0.22" />
        <rect x="60" y="92" width="140" height="6" rx="3" fill="#12406e" opacity="0.22" />
        <rect x="60" y="108" width="150" height="6" rx="3" fill="#12406e" opacity="0.22" />
        <rect x="60" y="124" width="100" height="6" rx="3" fill="#12406e" opacity="0.22" />
        <line x1="60" y1="150" x2="150" y2="150" stroke="#12406e" strokeOpacity="0.35" strokeWidth="1.5" />
      </g>
      <g transform="translate(226 158)">
        <polygon points="-14,10 -14,42 0,30 14,42 14,10" fill="#0d3057" />
        <circle cx="0" cy="0" r="26" fill="#ffb116" stroke="#b9852b" strokeWidth="2" />
        <circle cx="0" cy="0" r="18" fill="none" stroke="#0d3057" strokeWidth="1.5" strokeDasharray="2 3" />
        <path d="M-8 0l5 6 11-13" fill="none" stroke="#0d3057" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
      </g>
      <g transform="translate(14 140)">
        <rect x="0" y="46" width="16" height="20" rx="3" fill="#ffffff" opacity="0.55" />
        <rect x="22" y="32" width="16" height="34" rx="3" fill="#ffffff" opacity="0.75" />
        <rect x="44" y="14" width="16" height="52" rx="3" fill="#ffb116" />
      </g>
    </svg>
  );
}

export function AuthHero() {
  return (
    <div className="auth-hero">
      <svg className="auth-hero-pattern" aria-hidden="true">
        <defs>
          <pattern id="auth-hero-guilloche" width="40" height="40" patternUnits="userSpaceOnUse">
            <circle cx="20" cy="20" r="16" fill="none" stroke="#ffffff" strokeOpacity="0.07" strokeWidth="1" />
            <circle cx="0" cy="0" r="16" fill="none" stroke="#ffffff" strokeOpacity="0.07" strokeWidth="1" />
            <circle cx="40" cy="0" r="16" fill="none" stroke="#ffffff" strokeOpacity="0.07" strokeWidth="1" />
            <circle cx="0" cy="40" r="16" fill="none" stroke="#ffffff" strokeOpacity="0.07" strokeWidth="1" />
            <circle cx="40" cy="40" r="16" fill="none" stroke="#ffffff" strokeOpacity="0.07" strokeWidth="1" />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#auth-hero-guilloche)" />
      </svg>

      <div className="auth-hero-content">
        <BrandMark size={44} />
        <p className="auth-hero-eyebrow">FirstRegistrars Board Portal</p>
        <h1 className="auth-hero-headline">Board governance, done right.</h1>
        <p className="auth-hero-copy">
          Meeting management, resolutions, compliance tracking, and secure document control &mdash; built for
          FirstRegistrars &amp; Investor Services and the boards it serves.
        </p>

        <AuthHeroIllustration />

        <div className="auth-hero-pills">
          {FEATURES.map((feature) => (
            <span key={feature.label} className="auth-hero-pill">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d={feature.path} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              {feature.label}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}
