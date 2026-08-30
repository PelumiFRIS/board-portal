export function BrandMark({ size = 40 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <defs>
        <radialGradient id="brand-mark-g" cx="35%" cy="30%" r="75%">
          <stop offset="0%" stopColor="#1a5490" />
          <stop offset="100%" stopColor="#0d3057" />
        </radialGradient>
      </defs>
      <circle cx="32" cy="32" r="32" fill="url(#brand-mark-g)" />
      <text
        x="32"
        y="43"
        textAnchor="middle"
        fontFamily="Georgia, 'Times New Roman', serif"
        fontSize="28"
        fontWeight="700"
        fill="#ffb116"
      >
        FR
      </text>
    </svg>
  );
}
