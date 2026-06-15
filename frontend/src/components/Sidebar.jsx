import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export const SIDEBAR_COLLAPSED = 72
export const SIDEBAR_EXPANDED  = 220

const NAV_ITEMS = [
  { to: '/dashboard',        label: 'Dashboard',        icon: HomeIcon },
  { to: '/eligibility',      label: 'Eligibility',      icon: EligibilityIcon },
  { to: '/authorization',    label: 'Authorization',    icon: AuthIcon },
  { to: '/claim',            label: 'Claim',            icon: ClaimIcon },
  { to: '/practitioners',    label: 'Practitioners',    icon: PractitionersIcon },
  { to: '/organizations',    label: 'Insurers',         icon: OrganizationsIcon },
  { to: '/invite-user',      label: 'Invite User',      icon: InviteIcon,   adminOnly: true },
  { to: '/register-hospital',label: 'Register Hospital',icon: HospitalIcon, adminOnly: true },
]

export default function Sidebar() {
  const { user, logout } = useAuth()
  const navigate   = useNavigate()
  const [expanded, setExpanded] = useState(false)
  const navItems = NAV_ITEMS.filter((item) => !item.adminOnly || user?.role === 'ADMIN')

  return (
    <aside
      onMouseEnter={() => setExpanded(true)}
      onMouseLeave={() => setExpanded(false)}
      style={{
        width:         expanded ? SIDEBAR_EXPANDED : SIDEBAR_COLLAPSED,
        minHeight:     '100vh',
        background:    'linear-gradient(180deg, #2BB5A0 0%, #22a090 60%, #1d9e88 100%)',
        position:      'fixed',
        top:           0,
        left:          0,
        zIndex:        50,
        display:       'flex',
        flexDirection: 'column',
        paddingTop:    '22px',
        paddingBottom: '0',
        overflow:      'hidden',
        transition:    'width 220ms ease',
      }}
    >
      {/* ── Brand ── */}
      <div
        style={{
          padding:        '0 0 28px 0',
          flexShrink:     0,
          display:        'flex',
          alignItems:     'center',
          justifyContent: expanded ? 'flex-start' : 'center',
          paddingLeft:    expanded ? '16px' : '0',
          transition:     'padding-left 220ms ease, justify-content 220ms ease',
        }}
      >
        <span
          className="text-white font-bold"
          style={{ fontSize: '13px', whiteSpace: 'nowrap' }}
        >
          {expanded ? 'TraCare Claim' : 'TC'}
        </span>
      </div>

      {/* ── Nav ── */}
      <nav
        style={{
          flex:          1,
          display:       'flex',
          flexDirection: 'column',
          alignItems:    'stretch',
          gap:           '4px',
          padding:       '0 10px',
        }}
      >
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            title={!expanded ? label : undefined}
            style={{ width: '100%', display: 'flex', alignItems: 'center', textDecoration: 'none' }}
          >
            {({ isActive }) => (
              <div
                style={{
                  display:         'flex',
                  alignItems:      'center',
                  justifyContent:  expanded ? 'flex-start' : 'center',
                  gap:             '10px',
                  width:           '100%',
                  padding:         '6px',
                  borderRadius:    '12px',
                  backgroundColor: isActive ? 'rgba(255,255,255,0.2)' : 'transparent',
                  transition:      'background 150ms',
                }}
              >
                {/* Circle icon */}
                <div
                  style={{
                    width:           '40px',
                    height:          '40px',
                    borderRadius:    '50%',
                    backgroundColor: isActive ? '#fff' : 'rgba(255,255,255,0.18)',
                    display:         'flex',
                    alignItems:      'center',
                    justifyContent:  'center',
                    flexShrink:      0,
                    color:           isActive ? '#2BB5A0' : '#fff',
                    transition:      'background 150ms, color 150ms',
                  }}
                >
                  <Icon />
                </div>

                {/* Label — always in DOM, fades with width */}
                <span
                  style={{
                    color:      isActive ? '#fff' : 'rgba(255,255,255,0.85)',
                    fontWeight: isActive ? 600 : 400,
                    fontSize:   '13px',
                    whiteSpace: 'nowrap',
                    overflow:   'hidden',
                    maxWidth:   expanded ? '160px' : '0',
                    opacity:    expanded ? 1 : 0,
                    transition: 'max-width 220ms ease, opacity 180ms ease',
                  }}
                >
                  {label}
                </span>
              </div>
            )}
          </NavLink>
        ))}
      </nav>

      {/* ── Decorative bubble bottom-right ── */}
      <div style={{ position: 'relative', height: '130px', flexShrink: 0 }}>
        <div
          style={{
            position:        'absolute',
            bottom:          '-40px',
            right:           '-40px',
            width:           '120px',
            height:          '120px',
            borderRadius:    '50%',
            backgroundColor: 'rgba(255,255,255,0.12)',
          }}
        />
        <div
          style={{
            position:        'absolute',
            bottom:          '-70px',
            right:           '-60px',
            width:           '160px',
            height:          '160px',
            borderRadius:    '50%',
            backgroundColor: 'rgba(255,255,255,0.07)',
          }}
        />

        {/* Logout */}
        <button
          onClick={() => { logout(); navigate('/login') }}
          title="Logout"
          style={{
            position:        'absolute',
            bottom:          '18px',
            left:            '50%',
            transform:       'translateX(-50%)',
            width:           '36px',
            height:          '36px',
            borderRadius:    '50%',
            backgroundColor: 'rgba(255,255,255,0.15)',
            border:          'none',
            cursor:          'pointer',
            display:         'flex',
            alignItems:      'center',
            justifyContent:  'center',
            color:           'rgba(255,255,255,0.75)',
            transition:      'background 150ms',
          }}
          onMouseEnter={e => e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.28)'}
          onMouseLeave={e => e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.15)'}
        >
          <LogoutIcon />
        </button>
      </div>
    </aside>
  )
}

/* ── Icons ─────────────────────────────────────────────────────── */

function HomeIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
      <polyline points="9 22 9 12 15 12 15 22" />
    </svg>
  )
}

function EligibilityIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
      <circle cx="12" cy="7" r="4" />
      <polyline points="16 11 18 13 22 9" />
    </svg>
  )
}

function AuthIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 00-3-3.87" />
      <path d="M16 3.13a4 4 0 010 7.75" />
    </svg>
  )
}

function ClaimIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
    </svg>
  )
}

function LogoutIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  )
}

function PractitionersIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
      <line x1="12" y1="8" x2="12" y2="16" />
      <line x1="8" y1="12" x2="16" y2="12" />
    </svg>
  )
}

function InviteIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
      <circle cx="8.5" cy="7" r="4" />
      <line x1="20" y1="8" x2="20" y2="14" />
      <line x1="23" y1="11" x2="17" y2="11" />
    </svg>
  )
}

function OrganizationsIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
      <rect x="9" y="12" width="6" height="10" />
      <line x1="9" y1="12" x2="9" y2="22" />
      <line x1="15" y1="12" x2="15" y2="22" />
    </svg>
  )
}

function HospitalIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <line x1="12" y1="8" x2="12" y2="16" />
      <line x1="8"  y1="12" x2="16" y2="12" />
    </svg>
  )
}
