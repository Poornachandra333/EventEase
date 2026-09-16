import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getEvents } from '../api/eventApi';
import { EventSummary, Page } from '../types';
import { Calendar, MapPin, Search, Filter, Loader2, X } from 'lucide-react';
import { format } from 'date-fns';

const Events: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [pageData, setPageData] = useState<Page<EventSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filter States
  const [searchTerm, setSearchTerm] = useState(searchParams.get('title') || '');
  const [category, setCategory] = useState(searchParams.get('category') || '');
  const [city, setCity] = useState(searchParams.get('city') || '');
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  const fetchEvents = async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      const params: any = { page, size: 9, sort: 'eventDate,asc' };
      if (searchParams.get('title')) params.title = searchParams.get('title');
      if (searchParams.get('category')) params.category = searchParams.get('category');
      if (searchParams.get('city')) params.city = searchParams.get('city');
      if (searchParams.get('status')) params.status = searchParams.get('status');

      const data = await getEvents(params);
      setEvents(data.content);
      setPageData(data);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch events. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents(0);
  }, [searchParams]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const newParams = new URLSearchParams(searchParams);
    
    if (searchTerm) newParams.set('title', searchTerm);
    else newParams.delete('title');
    
    if (category) newParams.set('category', category);
    else newParams.delete('category');

    if (city) newParams.set('city', city);
    else newParams.delete('city');

    if (status) newParams.set('status', status);
    else newParams.delete('status');

    newParams.set('page', '0'); // reset to page 0 on search
    setSearchParams(newParams);
    setIsFilterOpen(false);
  };

  const clearFilters = () => {
    setSearchTerm('');
    setCategory('');
    setCity('');
    setStatus('');
    setSearchParams(new URLSearchParams());
    setIsFilterOpen(false);
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && pageData && newPage < pageData.totalPages) {
      fetchEvents(newPage);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="flex flex-col md:flex-row md:items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Explore Events</h1>
          <p className="mt-2 text-gray-600">Find and book the best experiences near you.</p>
        </div>
        
        <div className="mt-4 md:mt-0 flex items-center space-x-2">
          <button 
            onClick={() => setIsFilterOpen(!isFilterOpen)}
            className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            <Filter className="h-4 w-4 mr-2" />
            Filters
          </button>
        </div>
      </div>

      {isFilterOpen && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 mb-8 transition-all">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-medium text-gray-900">Search Filters</h3>
            <button onClick={() => setIsFilterOpen(false)} className="text-gray-400 hover:text-gray-500">
              <X className="h-5 w-5" />
            </button>
          </div>
          <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Search Title</label>
              <div className="relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search className="h-4 w-4 text-gray-400" />
                </div>
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="focus:ring-primary-500 focus:border-primary-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md p-2 border"
                  placeholder="Event name..."
                />
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm rounded-md border"
              >
                <option value="">All Categories</option>
                <option value="Concert">Concert</option>
                <option value="Conference">Conference</option>
                <option value="Workshop">Workshop</option>
                <option value="Sports">Sports</option>
                <option value="Theater">Theater</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                className="focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md p-2 border"
                placeholder="E.g. New York"
              />
            </div>

            <div className="flex items-end space-x-2">
              <button
                type="button"
                onClick={clearFilters}
                className="w-full bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              >
                Clear
              </button>
              <button
                type="submit"
                className="w-full bg-primary-600 border border-transparent rounded-md shadow-sm py-2 px-4 text-sm font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              >
                Apply
              </button>
            </div>
          </form>
        </div>
      )}

      {error && (
        <div className="bg-red-50 border-l-4 border-red-400 p-4 mb-8">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center items-center py-24">
          <Loader2 className="h-12 w-12 text-primary-500 animate-spin" />
        </div>
      ) : events.length > 0 ? (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {events.map((event) => (
              <Link key={event.id} to={`/events/${event.id}`} className="group bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow flex flex-col h-full">
                <div className="h-48 bg-gray-100 flex items-center justify-center relative flex-shrink-0">
                  <Calendar className="h-16 w-16 text-gray-300" />
                  <div className="absolute top-4 right-4 bg-white/90 backdrop-blur-sm px-3 py-1 rounded-full text-xs font-semibold text-primary-700 shadow-sm">
                    {event.category}
                  </div>
                </div>
                <div className="p-6 flex flex-col flex-1">
                  <h3 className="text-xl font-bold text-gray-900 mb-2 group-hover:text-primary-600 transition-colors line-clamp-2">
                    {event.title}
                  </h3>
                  <div className="flex items-center text-gray-600 mb-2 text-sm mt-auto">
                    <Calendar className="h-4 w-4 mr-2 text-gray-400" />
                    {format(new Date(event.eventDate), 'MMMM d, yyyy')}
                  </div>
                  <div className="flex items-center text-gray-600 text-sm mb-4">
                    <MapPin className="h-4 w-4 mr-2 text-gray-400" />
                    {event.venueName}, {event.city}
                  </div>
                  <div className="pt-4 border-t border-gray-100 flex justify-between items-center">
                    <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      event.status === 'PUBLISHED' ? 'bg-green-100 text-green-800' : 
                      event.status === 'CANCELLED' ? 'bg-red-100 text-red-800' : 'bg-gray-100 text-gray-800'
                    }`}>
                      {event.status}
                    </span>
                    <span className="text-primary-600 font-medium text-sm group-hover:underline">
                      Book Now &rarr;
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>

          {/* Pagination */}
          {pageData && pageData.totalPages > 1 && (
            <div className="mt-12 flex justify-center">
              <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                <button
                  onClick={() => handlePageChange(pageData.number - 1)}
                  disabled={pageData.number === 0}
                  className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span className="sr-only">Previous</span>
                  &larr; Prev
                </button>
                <span className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">
                  Page {pageData.number + 1} of {pageData.totalPages}
                </span>
                <button
                  onClick={() => handlePageChange(pageData.number + 1)}
                  disabled={pageData.number === pageData.totalPages - 1}
                  className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span className="sr-only">Next</span>
                  Next &rarr;
                </button>
              </nav>
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-24 bg-white rounded-xl border border-dashed border-gray-300 shadow-sm">
          <Calendar className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-lg font-medium text-gray-900">No events found</h3>
          <p className="mt-1 text-gray-500">We couldn't find any events matching your criteria.</p>
          <div className="mt-6">
            <button
              onClick={clearFilters}
              className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              Clear all filters
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Events;
