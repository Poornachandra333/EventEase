import React, { useState, useEffect } from 'react';
import { getEvents } from '../../api/eventApi';
import { getVenues } from '../../api/venueApi';
import { Calendar, MapPin, Ticket, TrendingUp } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

const AdminDashboard: React.FC = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState({
    events: 0,
    venues: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [eventsRes, venuesRes] = await Promise.all([
          getEvents({ size: 1 }), // just to get totalElements
          getVenues()
        ]);
        
        setStats({
          events: eventsRes.totalElements,
          venues: venuesRes.length,
        });
      } catch (err) {
        console.error("Failed to load dashboard stats", err);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) {
    return <div className="p-8">Loading dashboard...</div>;
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Welcome back, {user?.name}</h1>
        <p className="text-gray-600">Here's what's happening with your events today.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center">
          <div className="p-3 rounded-lg bg-blue-100 text-blue-600 mr-4">
            <Calendar className="h-6 w-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Total Events</p>
            <h3 className="text-2xl font-bold text-gray-900">{stats.events}</h3>
          </div>
        </div>
        
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center">
          <div className="p-3 rounded-lg bg-purple-100 text-purple-600 mr-4">
            <MapPin className="h-6 w-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Total Venues</p>
            <h3 className="text-2xl font-bold text-gray-900">{stats.venues}</h3>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center">
          <div className="p-3 rounded-lg bg-green-100 text-green-600 mr-4">
            <Ticket className="h-6 w-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Active Bookings</p>
            <h3 className="text-2xl font-bold text-gray-900">--</h3>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center">
          <div className="p-3 rounded-lg bg-amber-100 text-amber-600 mr-4">
            <TrendingUp className="h-6 w-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Revenue</p>
            <h3 className="text-2xl font-bold text-gray-900">₹--</h3>
          </div>
        </div>
      </div>
      
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Quick Actions</h2>
        <p className="text-gray-500 mb-6">Use the sidebar to manage your platform resources.</p>
      </div>
    </div>
  );
};

export default AdminDashboard;
