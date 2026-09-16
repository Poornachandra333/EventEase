import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getEventById } from '../api/eventApi';
import { getTicketTypesByEvent } from '../api/ticketApi';
import { createBooking } from '../api/bookingApi';
import { Event, TicketType } from '../types';
import { useAuth } from '../context/AuthContext';
import { Calendar, MapPin, Clock, Info, Loader2, AlertCircle } from 'lucide-react';
import { format } from 'date-fns';

const EventDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  
  const [event, setEvent] = useState<Event | null>(null);
  const [tickets, setTickets] = useState<TicketType[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [selectedTicketId, setSelectedTicketId] = useState<number | ''>('');
  const [quantity, setQuantity] = useState<number>(1);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingError, setBookingError] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [eventData, ticketsData] = await Promise.all([
          getEventById(Number(id)),
          getTicketTypesByEvent(Number(id))
        ]);
        setEvent(eventData);
        setTickets(ticketsData);
      } catch (err) {
        console.error(err);
        setError('Failed to load event details.');
      } finally {
        setLoading(false);
      }
    };
    
    if (id) fetchData();
  }, [id]);

  const handleBook = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    
    if (!selectedTicketId || quantity < 1) {
      setBookingError('Please select a ticket type and valid quantity.');
      return;
    }

    setBookingLoading(true);
    setBookingError('');
    
    try {
      const response = await createBooking({
        eventId: Number(id),
        items: [{ ticketTypeId: Number(selectedTicketId), quantity }]
      });
      navigate(`/payment/${response.id}`);
    } catch (err: any) {
      console.error(err);
      if (err.response?.status === 409) {
        setBookingError("Sorry, these tickets are no longer available in the requested quantity.");
      } else if (err.response?.status === 429) {
        setBookingError("Too many requests. Please wait a moment and try again.");
      } else {
        setBookingError(err.response?.data?.message || 'Failed to create booking.');
      }
    } finally {
      setBookingLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <Loader2 className="h-12 w-12 text-primary-500 animate-spin" />
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="max-w-3xl mx-auto mt-12 p-6 bg-red-50 text-red-700 rounded-lg border border-red-200">
        <div className="flex items-center">
          <AlertCircle className="h-6 w-6 mr-2" />
          <p>{error || 'Event not found.'}</p>
        </div>
        <Link to="/events" className="mt-4 inline-block text-primary-600 hover:underline">
          &larr; Back to Events
        </Link>
      </div>
    );
  }

  const selectedTicket = tickets.find(t => t.id === Number(selectedTicketId));
  const totalPrice = selectedTicket ? selectedTicket.price * quantity : 0;

  return (
    <div className="bg-white">
      {/* Event Header */}
      <div className="bg-gray-900 text-white py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="inline-block px-3 py-1 bg-primary-900/50 text-primary-200 rounded-full text-sm font-semibold mb-4 border border-primary-700">
            {event.category}
          </div>
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mb-4">
            {event.title}
          </h1>
          <div className="flex flex-wrap items-center gap-6 text-gray-300">
            <div className="flex items-center">
              <Calendar className="h-5 w-5 mr-2 text-primary-400" />
              {format(new Date(event.eventDate), 'EEEE, MMMM d, yyyy')}
            </div>
            <div className="flex items-center">
              <Clock className="h-5 w-5 mr-2 text-primary-400" />
              {event.startTime} - {event.endTime}
            </div>
            <div className="flex items-center">
              <MapPin className="h-5 w-5 mr-2 text-primary-400" />
              {event.venue.name}, {event.venue.city}
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-8">
            <section>
              <h2 className="text-2xl font-bold text-gray-900 mb-4">About this event</h2>
              <div className="prose prose-primary max-w-none text-gray-600 whitespace-pre-wrap">
                {event.description}
              </div>
            </section>

            <section>
              <h2 className="text-2xl font-bold text-gray-900 mb-4">Venue Information</h2>
              <div className="bg-gray-50 rounded-xl p-6 border border-gray-200">
                <h3 className="text-lg font-bold text-gray-900 mb-2">{event.venue.name}</h3>
                <p className="text-gray-600 mb-1 flex items-start">
                  <MapPin className="h-5 w-5 mr-2 text-gray-400 mt-0.5" />
                  {event.venue.address}, {event.venue.city}
                </p>
                <p className="text-gray-500 text-sm ml-7 mt-4">Capacity: {event.venue.capacity} people</p>
              </div>
            </section>
          </div>

          {/* Booking Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-lg border border-gray-200 p-6 sticky top-24">
              <h3 className="text-xl font-bold text-gray-900 mb-6">Select Tickets</h3>
              
              {event.status !== 'PUBLISHED' ? (
                <div className="bg-amber-50 border border-amber-200 p-4 rounded-md">
                  <p className="text-amber-800 flex items-center">
                    <Info className="h-5 w-5 mr-2" />
                    Tickets are not currently available for this event (Status: {event.status}).
                  </p>
                </div>
              ) : tickets.length === 0 ? (
                <p className="text-gray-500 italic">No tickets available yet.</p>
              ) : (
                <div className="space-y-6">
                  {bookingError && (
                    <div className="bg-red-50 text-red-700 p-3 rounded text-sm border border-red-200">
                      {bookingError}
                    </div>
                  )}
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Ticket Type</label>
                    <select
                      value={selectedTicketId}
                      onChange={(e) => setSelectedTicketId(e.target.value === '' ? '' : Number(e.target.value))}
                      className="block w-full border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500 sm:text-sm p-2.5 border"
                    >
                      <option value="">-- Select a ticket --</option>
                      {tickets.map(t => (
                        <option key={t.id} value={t.id} disabled={t.availableQuantity === 0}>
                          {t.name} - ₹{t.price} {t.availableQuantity === 0 ? '(Sold Out)' : `(${t.availableQuantity} left)`}
                        </option>
                      ))}
                    </select>
                  </div>

                  {selectedTicketId && selectedTicket && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">Quantity</label>
                      <div className="flex items-center">
                        <input
                          type="number"
                          min="1"
                          max={Math.min(10, selectedTicket.availableQuantity)}
                          value={quantity}
                          onChange={(e) => setQuantity(Number(e.target.value))}
                          className="block w-24 border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500 sm:text-sm p-2.5 border"
                        />
                        <span className="ml-3 text-gray-500 text-sm">
                          Max: {Math.min(10, selectedTicket.availableQuantity)}
                        </span>
                      </div>
                    </div>
                  )}

                  <div className="border-t border-gray-200 pt-4 mt-6">
                    <div className="flex justify-between items-center mb-6">
                      <span className="text-gray-600 font-medium">Total:</span>
                      <span className="text-2xl font-bold text-gray-900">
                        ₹{totalPrice.toFixed(2)}
                      </span>
                    </div>

                    <button
                      onClick={handleBook}
                      disabled={!selectedTicketId || bookingLoading || quantity < 1}
                      className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-base font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
                    >
                      {bookingLoading ? <Loader2 className="animate-spin h-5 w-5" /> : isAuthenticated ? 'Book Now' : 'Sign in to Book'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EventDetails;
