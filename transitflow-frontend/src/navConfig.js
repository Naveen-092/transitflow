import {
  LayoutDashboard, Search, Ticket, User, Route, Bus, MapPin, Users, CalendarClock,
  ClipboardList, ScanLine, ListChecks,
} from 'lucide-react';

export const ROLE_HOME = { ADMIN: '/admin', DRIVER: '/driver', PASSENGER: '/passenger' };

export const roleHome = (role) => ROLE_HOME[role] || '/login';

export const NAV = {
  PASSENGER: [
    { to: '/passenger', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/passenger/search', label: 'Search Trips', icon: Search },
    { to: '/passenger/bookings', label: 'My Bookings', icon: Ticket },
    { to: '/passenger/profile', label: 'Profile', icon: User },
  ],
  DRIVER: [
    { to: '/driver', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/driver/trips', label: 'My Trips', icon: ListChecks },
    { to: '/driver/verify', label: 'Verify Ticket', icon: ScanLine },
  ],
  ADMIN: [
    { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/admin/buses', label: 'Buses', icon: Bus },
    { to: '/admin/stops', label: 'Stops', icon: MapPin },
    { to: '/admin/routes', label: 'Routes', icon: Route },
    { to: '/admin/drivers', label: 'Drivers', icon: Users },
    { to: '/admin/trips', label: 'Trips', icon: CalendarClock },
    { to: '/admin/bookings', label: 'Bookings', icon: ClipboardList },
  ],
};