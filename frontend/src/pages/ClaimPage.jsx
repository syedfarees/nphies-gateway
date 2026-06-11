import DashboardLayout from '../components/DashboardLayout'
import ClaimWizard from '../components/ClaimWizard'

const TEAL = '#2BB5A0'

export default function ClaimPage() {
  return (
    <DashboardLayout>
      <div className="px-8 pt-7 pb-2">
        <h1 className="text-base font-semibold" style={{ color: TEAL }}>Claims</h1>
        <p className="text-xs text-gray-400 mt-0.5">Submit and manage NPHIES claim bundles</p>
      </div>
      <div className="px-8 pb-8">
        <ClaimWizard mode="claim" />
      </div>
    </DashboardLayout>
  )
}
