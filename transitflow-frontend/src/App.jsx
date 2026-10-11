import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import LoadingSpinner from './components/LoadingSpinner';
import ProtectedRoute from './components/ProtectedRoute';
import { useAuth } from './hooks/useAuth';
import { roleHome } from './navConfig';
import Login from './pages/Login';
import NotFound from './pages/NotFound';
import Register from './pages/Register';

import MyBookings from './pages/MyBookings';
import PassengerDashboard from './pages/PassengerDashboard';
import Profile from './pages/Profile';
import SearchTrips from './pages/SearchTrips';
import SeatSelection from './pages/SeatSelection';
import Ticket from './pages/Ticket';
import TrackBus from './pages/TrackBus';
import TripDetails from './pages/TripDetails';

import DriverDashboard from './pages/DriverDashboard';
import DriverPassengers from './pages/DriverPassengers';
import DriverTripDetails from './pages/DriverTripDetails';
import DriverTrips from './pages/DriverTrips';
import UpdateLocation from './pages/UpdateLocation';
import VerifyTicket from './pages/VerifyTicket';

import AdminBookings from './pages/AdminBookings';
import AdminDashboard from './pages/AdminDashboard';
import ManageBuses from './pages/ManageBuses';
import ManageDrivers from './pages/ManageDrivers';
import ManageRoutes from './pages/ManageRoutes';
import ManageSeats from './pages/ManageSeats';
import ManageStops from './pages/ManageStops';
import ManageTrips from './pages/ManageTrips';

function RootRedirect() {
  const { user, loading } = useAuth();
  if (loading) return <LoadingSpinner center size="lg" />;
  return <Navigate to={user ? roleHome(user.role) : '/login'} replace />;
}


export default function App() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

            {/* PASSENGER */}
      <Route element={<ProtectedRoute roles={['PASSENGER']} />}>
        <Route path="/passenger" element={<AppLayout />}>
          <Route index element={<PassengerDashboard />} />
          <Route path="search" element={<SearchTrips />} />
          <Route path="trips/:id" element={<TripDetails />} />
          <Route path="trips/:id/seats" element={<SeatSelection />} />
          <Route path="trips/:id/track" element={<TrackBus />} />
          <Route path="bookings" element={<MyBookings />} />
          <Route path="bookings/:id" element={<Ticket />} />
          <Route path="profile" element={<Profile />} />
        </Route>
      </Route>

           {/* DRIVER */}
      <Route element={<ProtectedRoute roles={['DRIVER']} />}>
        <Route path="/driver" element={<AppLayout />}>
          <Route index element={<DriverDashboard />} />
          <Route path="trips" element={<DriverTrips />} />
          <Route path="trips/:id" element={<DriverTripDetails />} />
          <Route path="trips/:id/passengers" element={<DriverPassengers />} />
          <Route path="trips/:id/location" element={<UpdateLocation />} />
          <Route path="verify" element={<VerifyTicket />} />
        </Route>
      </Route>

      {/* ADMIN */}
      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route path="/admin" element={<AppLayout />}>
          <Route index element={<AdminDashboard />} />
          <Route path="buses" element={<ManageBuses />} />
          <Route path="buses/:id/seats" element={<ManageSeats />} />
          <Route path="stops" element={<ManageStops />} />
          <Route path="routes" element={<ManageRoutes />} />
          <Route path="drivers" element={<ManageDrivers />} />
          <Route path="trips" element={<ManageTrips />} />
          <Route path="bookings" element={<AdminBookings />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}