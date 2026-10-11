export const ROLE_HOME = {
  ADMIN: '/admin',
  DRIVER: '/driver',
  PASSENGER: '/passenger',
};

export const roleHome = (role) => ROLE_HOME[role] || '/login';

export const NAV = {
  PASSENGER: [
    { to: '/passenger', label: 'Dashboard', end: true },
    { to: '/passenger/search', label: 'Search Trips' },
    { to: '/passenger/bookings', label: 'My Bookings' },
    { to: '/passenger/profile', label: 'Profile' },
  ],
  DRIVER: [
    { to: '/driver', label: 'Dashboard', end: true },
    { to: '/driver/trips', label: 'My Trips' },
    { to: '/driver/verify', label: 'Verify Ticket' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Dashboard', end: true },
    { to: '/admin/buses', label: 'Buses' },
    { to: '/admin/stops', label: 'Stops' },
    { to: '/admin/routes', label: 'Routes' },
    { to: '/admin/drivers', label: 'Drivers' },
    { to: '/admin/trips', label: 'Trips' },
    { to: '/admin/bookings', label: 'Bookings' },
  ],
};