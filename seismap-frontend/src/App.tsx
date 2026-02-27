import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './components/MainLayout';
import AdminPage from './pages/AdminPage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />} />
      {import.meta.env.MODE === 'development' && <Route path="/admin" element={<AdminPage />} />}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
