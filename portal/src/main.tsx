import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import './index.css';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';
import LoginPage from './auth/LoginPage';
import DashboardPage from './pages/DashboardPage';
import CasesPage from './pages/CasesPage';
import CaseDetailPage from './pages/CaseDetailPage';
import PatientsPage from './pages/PatientsPage';
import PatientDetailPage from './pages/PatientDetailPage';
import PatientWizardPage from './pages/PatientWizardPage';
import RequestsPage from './pages/RequestsPage';
import RequestDetailPage from './pages/RequestDetailPage';
import PricingPage from './pages/PricingPage';
import AgentsPage from './pages/AgentsPage';
import HospitalsPage from './pages/HospitalsPage';
import AmbulancesPage from './pages/AmbulancesPage';
import CaretakersPage from './pages/CaretakersPage';
import NotificationsPage from './pages/NotificationsPage';
import ComplaintsPage from './pages/ComplaintsPage';
import IncidentsPage from './pages/IncidentsPage';
import SettlementsPage from './pages/SettlementsPage';
import LiveOperationsPage from './pages/LiveOperationsPage';
import InventoryPage from './pages/InventoryPage';
import AccommodationsPage from './pages/AccommodationsPage';
import CashReconciliationPage from './pages/CashReconciliationPage';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<DashboardPage />} />
            <Route path="/live" element={<LiveOperationsPage />} />
            <Route path="/cases" element={<CasesPage />} />
            <Route path="/cases/:id" element={<CaseDetailPage />} />
            <Route path="/patients" element={<PatientsPage />} />
            <Route path="/patients/new" element={<PatientWizardPage />} />
            <Route path="/patients/:id" element={<PatientDetailPage />} />
            <Route path="/requests" element={<RequestsPage />} />
            <Route path="/requests/:id" element={<RequestDetailPage />} />
            <Route path="/pricing" element={<PricingPage />} />
            <Route path="/agents" element={<AgentsPage />} />
            <Route path="/hospitals" element={<HospitalsPage />} />
            <Route path="/ambulances" element={<AmbulancesPage />} />
            <Route path="/caretakers" element={<CaretakersPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/complaints" element={<ComplaintsPage />} />
            <Route path="/incidents" element={<IncidentsPage />} />
            <Route path="/settlements" element={<SettlementsPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/accommodations" element={<AccommodationsPage />} />
            <Route path="/cash" element={<CashReconciliationPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </StrictMode>
);
