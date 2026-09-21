import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getMyBookings, cancelBooking } from '../api/bookingApi';
import { BookingResponse, Page } from '../types';
import { format } from 'date-fns';
import { Loader2, Ticket, CheckCircle2, XCircle, AlertCircle, Clock } from 'lucide-react';

const MyBookings: React.FC = () => {
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [pageData, setPageData] = useState<Page<BookingResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  const fetchBookings = async (page = 0) => {
    setLoading(true);
    try {
      const data = await getMyBookings(page, 10);
      setBookings(data.content);
      setPageData(data);
    } catch (err: any) {
      console.error(err);
      setError('Failed to fetch your bookings.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings(0);
  }, []);

  const handleCancel = async (id: number) => {
    if (!window.confirm("Are you sure you want to cancel this booking?")) return;
    
    setCancellingId(id);
    try {
      await cancelBooking(id);
      // Refresh list
      fetchBookings(pageData ? pageData.number : 0);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel booking.');
    } finally {
      setCancellingId(null);
    }
  };

  const getStatusBadge = (status: string) => {
    switch(status) {
      case 'CONFIRMED': return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800"><CheckCircle2 className="w-3 h-3 mr-1"/> Confirmed</span>;
      case 'PENDING_PAYMENT': return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800"><Clock className="w-3 h-3 mr-1"/> Pending</span>;
      case 'CANCELLED': return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800"><XCircle className="w-3 h-3 mr-1"/> Cancelled</span>;
      default: return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">{status}</span>;
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <Loader2 className="h-12 w-12 text-primary-500 animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">My Bookings</h1>
          <p className="mt-2 text-gray-600">View and manage your event tickets.</p>
        </div>
        <Ticket className="h-10 w-10 text-primary-200" />
      </div>

      {error && (
        <div className="bg-red-50 p-4 rounded-md mb-8 flex items-start">
          <AlertCircle className="h-5 w-5 text-red-400 mr-3 mt-0.5" />
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {bookings.length === 0 ? (
        <div className="text-center py-24 bg-white rounded-2xl border border-dashed border-gray-300 shadow-sm">
          <Ticket className="mx-auto h-16 w-16 text-gray-300 mb-4" />
          <h3 className="text-xl font-medium text-gray-900 mb-2">No bookings yet</h3>
          <p className="text-gray-500 mb-6">Looks like you haven't booked any events.</p>
          <Link to="/events" className="inline-flex items-center px-6 py-3 border border-transparent shadow-sm text-base font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 transition-colors">
            Explore Events
          </Link>
        </div>
      ) : (
        <div className="space-y-6">
          {bookings.map((booking) => (
            <div key={booking.id} className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden flex flex-col md:flex-row transition-shadow hover:shadow-md">
              <div className="md:w-1/4 bg-gray-50 p-6 border-b md:border-b-0 md:border-r border-gray-200 flex flex-col justify-center items-center text-center">
                <span className="text-sm text-gray-500 font-medium uppercase tracking-wider mb-1">Ref No.</span>
                <span className="font-mono text-lg font-bold text-gray-900">{booking.bookingReference}</span>
                <div className="mt-4">
                  {getStatusBadge(booking.status)}
                </div>
              </div>
              
              <div className="p-6 md:w-3/4 flex flex-col justify-between">
                <div>
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="text-xl font-bold text-gray-900 leading-tight">
                      <Link to={`/events/${booking.eventId}`} className="hover:text-primary-600 transition-colors">
                        {booking.eventTitle}
                      </Link>
                    </h3>
                    <span className="text-xl font-extrabold text-primary-600 whitespace-nowrap ml-4">
                      ₹{booking.totalAmount.toFixed(2)}
                    </span>
                  </div>
                  
                  <div className="text-sm text-gray-500 mb-4">
                    Booked on {format(new Date(booking.bookedAt), 'MMM d, yyyy h:mm a')}
                  </div>

                  <div className="bg-gray-50 rounded-lg p-4 border border-gray-100">
                    <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Tickets</h4>
                    <ul className="space-y-1">
                      {booking.items.map(item => (
                        <li key={item.id} className="text-sm text-gray-800 flex justify-between">
                          <span>{item.quantity}x {item.ticketTypeName}</span>
                          <span className="text-gray-500">₹{item.subTotal.toFixed(2)}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>

                <div className="mt-6 flex justify-end space-x-4 items-center border-t border-gray-100 pt-4">
                  {booking.status === 'PENDING_PAYMENT' && (
                    <Link to={`/payment/${booking.id}`} className="text-sm font-medium text-white bg-green-600 hover:bg-green-700 px-4 py-2 rounded-md transition-colors shadow-sm">
                      Pay Now
                    </Link>
                  )}
                  {booking.status === 'CONFIRMED' && (
                    <button 
                      onClick={() => handleCancel(booking.id)}
                      disabled={cancellingId === booking.id}
                      className="text-sm font-medium text-red-600 bg-white border border-red-200 hover:bg-red-50 px-4 py-2 rounded-md transition-colors"
                    >
                      {cancellingId === booking.id ? 'Cancelling...' : 'Cancel Booking'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}

          {/* Pagination */}
          {pageData && pageData.totalPages > 1 && (
            <div className="mt-8 flex justify-center">
              <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                <button
                  onClick={() => fetchBookings(pageData.number - 1)}
                  disabled={pageData.number === 0}
                  className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <button
                  onClick={() => fetchBookings(pageData.number + 1)}
                  disabled={pageData.number === pageData.totalPages - 1}
                  className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </nav>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default MyBookings;
