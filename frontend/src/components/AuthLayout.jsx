export default function AuthLayout({ children }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
      <div
        className="w-full max-w-3xl flex rounded-2xl overflow-hidden shadow-2xl"
        style={{ minHeight: '460px' }}
      >
        {/* Left teal panel */}
        <div
          className="relative hidden md:flex flex-col justify-between p-8 w-2/5 overflow-hidden"
          style={{ background: 'linear-gradient(145deg, #2BB5A0 0%, #1a9980 100%)' }}
        >
          {/* Decorative circles */}
          <div
            className="absolute rounded-full opacity-20"
            style={{
              width: '180px',
              height: '180px',
              background: 'rgba(255,255,255,0.3)',
              top: '-50px',
              right: '-40px',
            }}
          />
          <div
            className="absolute rounded-full opacity-10"
            style={{
              width: '220px',
              height: '220px',
              background: 'rgba(255,255,255,0.3)',
              bottom: '-60px',
              left: '-60px',
            }}
          />

          {/* Brand */}
          <div className="relative z-10">
            <h1 className="text-white font-bold text-xl leading-tight">
              TraCare<br />Claim
            </h1>
          </div>

          {/* Illustration */}
          <div className="relative z-10 flex flex-col items-center gap-6">
            <HealthIllustration />
            <p className="text-white text-center text-sm font-medium opacity-90">
              Claim with Confidence.<br />Recover with Ease.
            </p>
          </div>

          {/* Spacer */}
          <div />
        </div>

        {/* Right content panel */}
        <div className="flex-1 bg-white flex items-center justify-center p-8">
          {children}
        </div>
      </div>
    </div>
  )
}

function HealthIllustration() {
  return (
    <svg
      width="140"
      height="140"
      viewBox="0 0 140 140"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* Sparkles */}
      <g stroke="white" strokeWidth="2" strokeLinecap="round">
        <line x1="22" y1="20" x2="22" y2="28" />
        <line x1="18" y1="24" x2="26" y2="24" />
        <line x1="108" y1="15" x2="108" y2="21" />
        <line x1="105" y1="18" x2="111" y2="18" />
        <line x1="30" y1="42" x2="30" y2="46" />
        <line x1="28" y1="44" x2="32" y2="44" />
        <line x1="115" y1="45" x2="115" y2="51" />
        <line x1="112" y1="48" x2="118" y2="48" />
      </g>

      {/* Heart shape */}
      <path
        d="M70 95 C70 95 35 72 35 50 C35 38 44 30 55 30 C62 30 68 34 70 38 C72 34 78 30 85 30 C96 30 105 38 105 50 C105 72 70 95 70 95Z"
        stroke="white"
        strokeWidth="2.5"
        fill="none"
        strokeLinejoin="round"
      />

      {/* ECG line inside heart */}
      <path
        d="M50 54 L57 54 L60 46 L64 62 L67 54 L73 54 L76 48 L79 60 L82 54 L90 54"
        stroke="white"
        strokeWidth="2"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* Hand */}
      <path
        d="M45 98 C45 98 40 92 40 88 C40 86 41 84 43 84 C45 84 46 85 47 87 L47 80 C47 78 48.5 77 50 77 C51.5 77 53 78 53 80 L53 78 C53 76 54.5 75 56 75 C57.5 75 59 76 59 78 L59 79 C59 77 60.5 76 62 76 C63.5 76 65 77 65 79 L65 88 C68 86 71 86 72 88 C73 90 72 92 70 94 L65 98 C62 100 58 102 55 102 C51 102 47 100 45 98Z"
        stroke="white"
        strokeWidth="2"
        fill="none"
        strokeLinejoin="round"
        strokeLinecap="round"
      />
    </svg>
  )
}
