import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage             from './pages/LoginPage'
import RegisterPage          from './pages/RegisterPage'
import ForgotPasswordPage    from './pages/ForgotPasswordPage'
import DashboardPage         from './pages/DashboardPage'
import EligibilityPage       from './pages/EligibilityPage'
import AuthorizationPage     from './pages/AuthorizationPage'
import ClaimPage             from './pages/ClaimPage'
import AddBeneficiaryPage    from './pages/AddBeneficiaryPage'
import PractitionersPage     from './pages/PractitionersPage'
import OrganizationsPage     from './pages/OrganizationsPage'
import ClaimDetailPage       from './pages/ClaimDetailPage'
import BeneficiaryDetailPage from './pages/BeneficiaryDetailPage'
import InviteUserPage        from './pages/InviteUserPage'
import RegisterHospitalPage  from './pages/RegisterHospitalPage'
import ProtectedRoute        from './components/ProtectedRoute'

function PrivateRoute({ children }) {
  return <ProtectedRoute>{children}</ProtectedRoute>
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login"           element={<LoginPage />} />
        <Route path="/register"        element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />

        {/* Protected */}
        <Route path="/dashboard"       element={<PrivateRoute><DashboardPage /></PrivateRoute>} />
        <Route path="/eligibility"     element={<PrivateRoute><EligibilityPage /></PrivateRoute>} />
        <Route path="/authorization"   element={<PrivateRoute><AuthorizationPage /></PrivateRoute>} />
        <Route path="/claim"           element={<PrivateRoute><ClaimPage /></PrivateRoute>} />
        <Route path="/add-beneficiary" element={<PrivateRoute><AddBeneficiaryPage /></PrivateRoute>} />
        <Route path="/practitioners"   element={<PrivateRoute><PractitionersPage /></PrivateRoute>} />
        <Route path="/organizations"   element={<PrivateRoute><OrganizationsPage /></PrivateRoute>} />
        <Route path="/claims/:claimId" element={<PrivateRoute><ClaimDetailPage /></PrivateRoute>} />
        <Route path="/beneficiaries/:id" element={<PrivateRoute><BeneficiaryDetailPage /></PrivateRoute>} />
        <Route path="/invite-user"        element={<PrivateRoute><InviteUserPage /></PrivateRoute>} />
        <Route path="/register-hospital" element={<PrivateRoute><RegisterHospitalPage /></PrivateRoute>} />

        {/* Default */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
