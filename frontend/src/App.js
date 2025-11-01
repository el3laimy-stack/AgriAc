import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

// Context
import { DataProvider } from './context/DataContext';

// Components
import Navbar from './components/Navbar';

// Pages
import Dashboard from './pages/Dashboard';
import InventoryView from './pages/InventoryView';
import SaleManagement from './pages/SaleManagement';
import PurchaseManagement from './pages/PurchaseManagement';
import GeneralLedger from './pages/GeneralLedger';
import TrialBalance from './pages/TrialBalance';
import CropManagement from './pages/CropManagement';
import ContactManagement from './pages/ContactManagement';
import FinancialAccountManagement from './pages/FinancialAccountManagement';
import ExpenseManagement from './pages/ExpenseManagement';
import IncomeStatement from './pages/IncomeStatement';
import BalanceSheet from './pages/BalanceSheet';
import JournalView from './pages/JournalView';
import EquityStatement from './pages/EquityStatement';

function App() {
  return (
    <DataProvider>
      <Router>
        <div className="App">
          <Navbar />
          <main>
            <Routes>
              {/* Main Pages */}
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/inventory" element={<InventoryView />} />
              <Route path="/sales" element={<SaleManagement />} />
              <Route path="/purchases" element={<PurchaseManagement />} />
              <Route path="/expenses" element={<ExpenseManagement />} />
              <Route path="/journal" element={<JournalView />} />

              {/* Reports */}
              <Route path="/reports/general-ledger" element={<GeneralLedger />} />
              <Route path="/reports/trial-balance" element={<TrialBalance />} />
              <Route path="/reports/income-statement" element={<IncomeStatement />} />
              <Route path="/reports/balance-sheet" element={<BalanceSheet />} />
              <Route path="/reports/equity-statement" element={<EquityStatement />} />

              {/* Setup Pages */}
              <Route path="/crops" element={<CropManagement />} />
              <Route path="/contacts" element={<ContactManagement />} />
              <Route path="/financial-accounts" element={<FinancialAccountManagement />} />

              {/* Default Redirect */}
              <Route path="*" element={<Navigate to="/dashboard" />} />
            </Routes>
          </main>
        </div>
      </Router>
    </DataProvider>
  );
}

export default App;