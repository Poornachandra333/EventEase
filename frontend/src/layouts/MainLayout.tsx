import React from 'react';
import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Calendar, LogOut, Settings } from 'lucide-react';
import AiAssistant from '../components/AiAssistant';

const MainLayout: React.FC = () => {
  const { user, logout, isAuthenticated, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <header className="bg-white border-b border-gray-200 shadow-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <Link to="/" className="flex items-center text-primary-600">
                <Calendar className="h-8 w-8 mr-2" />
                <span className="font-bold text-xl tracking-tight">EventEase</span>
              </Link>
              <nav className="hidden md:ml-8 md:flex md:space-x-8">
                <Link to="/events" className="text-gray-600 hover:text-gray-900 px-3 py-2 text-sm font-medium">Explore Events</Link>
                {isAuthenticated && (
                  <Link to="/my-bookings" className="text-gray-600 hover:text-gray-900 px-3 py-2 text-sm font-medium">My Bookings</Link>
                )}
              </nav>
            </div>
            <div className="flex items-center space-x-4">
              {isAuthenticated ? (
                <>
                  {isAdmin && (
                    <Link to="/admin" className="text-gray-600 hover:text-primary-600 px-3 py-2 flex items-center text-sm font-medium">
                      <Settings className="h-4 w-4 mr-1" />
                      Admin
                    </Link>
                  )}
                  <span className="text-sm font-medium text-gray-700 hidden sm:block">
                    Hi, {user?.name.split(' ')[0]}
                  </span>
                  <button onClick={handleLogout} className="text-gray-500 hover:text-red-600 p-2 rounded-full flex items-center text-sm font-medium transition-colors">
                    <LogOut className="h-5 w-5 mr-1" />
                    Logout
                  </button>
                </>
              ) : (
                <>
                  <Link to="/login" className="text-gray-600 hover:text-gray-900 px-3 py-2 text-sm font-medium">Log in</Link>
                  <Link to="/register" className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors">
                    Create Account
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <AiAssistant />

      <footer className="bg-gray-900 text-gray-400 py-12 text-center text-sm">
        <p>&copy; {new Date().getFullYear()} EventEase. All rights reserved.</p>
      </footer>
    </div>
  );
};

export default MainLayout;
