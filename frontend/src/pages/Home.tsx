import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getEvents } from '../api/eventApi';
import { EventSummary } from '../types';
import { Calendar, MapPin, ArrowRight } from 'lucide-react';
import { format } from 'date-fns';

const Home: React.FC = () => {
  const [featuredEvents, setFeaturedEvents] = useState<EventSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchFeatured = async () => {
      try {
        const data = await getEvents({ size: 3, sort: 'eventDate,asc' });
        setFeaturedEvents(data.content);
      } catch (error) {
        console.error("Failed to load featured events", error);
      } finally {
        setLoading(false);
      }
    };
    fetchFeatured();
  }, []);

  return (
    <div className="flex flex-col min-h-screen">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-gray-900 to-gray-800 text-white py-24 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto text-center">
          <h1 className="text-4xl sm:text-5xl md:text-6xl font-extrabold tracking-tight mb-6">
            Discover Your Next <span className="text-primary-500">Unforgettable</span> Experience
          </h1>
          <p className="mt-4 text-xl sm:text-2xl text-gray-300 max-w-3xl mx-auto mb-10">
            Book tickets for concerts, workshops, conferences, and more. All in one place.
          </p>
          <div className="flex justify-center">
            <Link
              to="/events"
              className="inline-flex items-center justify-center px-8 py-3 border border-transparent text-base font-medium rounded-md text-gray-900 bg-primary-500 hover:bg-primary-400 transition-colors md:py-4 md:text-lg md:px-10"
            >
              Explore Events <ArrowRight className="ml-2 h-5 w-5" />
            </Link>
          </div>
        </div>
      </section>

      {/* Featured Events Section */}
      <section className="py-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h2 className="text-3xl font-bold text-gray-900 tracking-tight">Upcoming Events</h2>
            <p className="mt-2 text-lg text-gray-600">Don't miss out on these popular upcoming experiences.</p>
          </div>
          <Link to="/events" className="hidden sm:flex items-center text-primary-600 hover:text-primary-700 font-medium">
            View all <ArrowRight className="ml-1 h-4 w-4" />
          </Link>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white rounded-xl shadow-sm border border-gray-100 h-80 animate-pulse">
                <div className="bg-gray-200 h-48 rounded-t-xl"></div>
                <div className="p-5">
                  <div className="h-6 bg-gray-200 rounded w-3/4 mb-4"></div>
                  <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
                  <div className="h-4 bg-gray-200 rounded w-1/3"></div>
                </div>
              </div>
            ))}
          </div>
        ) : featuredEvents.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {featuredEvents.map((event) => (
              <Link key={event.id} to={`/events/${event.id}`} className="group bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow">
                <div className="h-48 bg-gray-100 flex items-center justify-center relative">
                  <Calendar className="h-16 w-16 text-gray-300" />
                  <div className="absolute top-4 right-4 bg-white/90 backdrop-blur-sm px-3 py-1 rounded-full text-xs font-semibold text-primary-700">
                    {event.category}
                  </div>
                </div>
                <div className="p-5">
                  <h3 className="text-xl font-bold text-gray-900 mb-2 group-hover:text-primary-600 transition-colors">
                    {event.title}
                  </h3>
                  <div className="flex items-center text-gray-600 mb-2 text-sm">
                    <Calendar className="h-4 w-4 mr-2" />
                    {format(new Date(event.eventDate), 'MMMM d, yyyy')}
                  </div>
                  <div className="flex items-center text-gray-600 text-sm mb-4">
                    <MapPin className="h-4 w-4 mr-2" />
                    {event.venueName}, {event.city}
                  </div>
                  <div className="pt-4 border-t border-gray-100 flex justify-between items-center">
                    <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      event.status === 'PUBLISHED' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                    }`}>
                      {event.status}
                    </span>
                    <span className="text-primary-600 font-medium text-sm group-hover:underline">
                      View Details
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 bg-gray-50 rounded-xl border border-dashed border-gray-300">
            <h3 className="text-lg font-medium text-gray-900">No events found</h3>
            <p className="mt-1 text-gray-500">Check back later for exciting new events.</p>
          </div>
        )}
        
        <div className="mt-8 sm:hidden flex justify-center">
          <Link to="/events" className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
            View all events
          </Link>
        </div>
      </section>
    </div>
  );
};

export default Home;
