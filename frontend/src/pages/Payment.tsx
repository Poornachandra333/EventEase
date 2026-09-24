import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getBookingById } from '../api/bookingApi';
import { initiatePayment } from '../api/paymentApi';
import { BookingResponse } from '../types';
import { CreditCard, CheckCircle, XCircle, Loader2, ArrowRight } from 'lucide-react';
import { format } from 'date-fns';

const Payment: React.FC = () => {
  const { bookingId } = useParams<{ bookingId: string }>();
  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentProcessing, setPaymentProcessing] = useState(false);
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchBooking = async () => {
      try {
        const data = await getBookingById(Number(bookingId));
        setBooking(data);
        if (data.status === 'CONFIRMED' || data.paymentStatus === 'SUCCESS') {
          setPaymentSuccess(true);
        }
      } catch {
        setError('Failed to load booking details.');
      } finally {
        setLoading(false);
      }
    };
    
    if (bookingId) fetchBooking();
  }, [bookingId]);

  const handlePayment = async () => {
    setPaymentProcessing(true);
    setError('');
    
    try {
      await initiatePayment(Number(bookingId));
      setPaymentSuccess(true);
      // Wait a moment then maybe redirect or just show success state
    } catch (err: any) {
      setError(err.response?.data?.message || 'Payment processing failed. Please try again.');
    } finally {
      setPaymentProcessing(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <Loader2 className="h-12 w-12 text-primary-500 animate-spin" />
      </div>
    );
  }

  if (error && !booking) {
    return (
      <div className="max-w-2xl mx-auto mt-12 p-6 bg-red-50 text-red-700 rounded-lg text-center">
        <XCircle className="h-12 w-12 mx-auto mb-4" />
        <h2 className="text-2xl font-bold mb-2">Error</h2>
        <p>{error}</p>
        <Link to="/events" className="mt-6 inline-block bg-white text-red-700 px-6 py-2 border border-red-200 rounded hover:bg-red-100">
          Return to Events
        </Link>
      </div>
    );
  }

  if (!booking) return null;

  if (paymentSuccess) {
    return (
      <div className="max-w-3xl mx-auto py-16 px-4">
        <div className="bg-white rounded-2xl shadow-xl border border-gray-100 p-8 text-center">
          <div className="mx-auto flex items-center justify-center h-24 w-24 rounded-full bg-green-100 mb-6">
            <CheckCircle className="h-12 w-12 text-green-600" />
          </div>
          <h2 className="text-3xl font-extrabold text-gray-900 mb-2">Payment Successful!</h2>
          <p className="text-lg text-gray-600 mb-8">Your booking for <strong>{booking.eventTitle}</strong> is confirmed.</p>
          
          <div className="bg-gray-50 rounded-xl p-6 text-left max-w-md mx-auto mb-8 border border-gray-200">
            <div className="flex justify-between items-center border-b border-gray-200 pb-4 mb-4">
              <span className="text-gray-500">Booking Reference</span>
              <span className="font-mono font-bold text-gray-900">{booking.bookingReference}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Total Paid</span>
              <span className="font-bold text-gray-900">₹{booking.totalAmount.toFixed(2)}</span>
            </div>
          </div>
          
          <div className="flex flex-col sm:flex-row justify-center gap-4">
            <Link to="/my-bookings" className="inline-flex justify-center items-center px-6 py-3 border border-transparent rounded-md shadow-sm text-base font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500">
              View My Bookings
            </Link>
            <Link to="/events" className="inline-flex justify-center items-center px-6 py-3 border border-gray-300 shadow-sm text-base font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500">
              Discover More Events
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto py-12 px-4">
      <div className="bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden">
        <div className="bg-gray-900 px-6 py-8 sm:px-10 text-white text-center">
          <h1 className="text-3xl font-extrabold tracking-tight mb-2">Complete Your Payment</h1>
          <p className="text-gray-300">Almost there! Complete your payment to secure your tickets.</p>
        </div>
        
        <div className="px-6 py-8 sm:px-10">
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-lg font-bold text-gray-900">Order Summary</h3>
            <span className="font-mono text-sm text-gray-500">Ref: {booking.bookingReference}</span>
          </div>

          <div className="border border-gray-200 rounded-lg overflow-hidden mb-8">
            <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
              <h4 className="font-semibold text-gray-900">{booking.eventTitle}</h4>
              <p className="text-sm text-gray-500">{format(new Date(booking.bookedAt), 'MMM d, yyyy')}</p>
            </div>
            <ul className="divide-y divide-gray-200">
              {booking.items.map((item) => (
                <li key={item.id} className="px-4 py-4 flex justify-between items-center">
                  <div>
                    <p className="text-sm font-medium text-gray-900">{item.ticketTypeName}</p>
                    <p className="text-sm text-gray-500">{item.quantity} x ₹{item.unitPrice.toFixed(2)}</p>
                  </div>
                  <span className="font-semibold text-gray-900">₹{item.subTotal.toFixed(2)}</span>
                </li>
              ))}
            </ul>
            <div className="bg-gray-50 px-4 py-4 border-t border-gray-200 flex justify-between items-center">
              <span className="text-base font-bold text-gray-900">Total Amount</span>
              <span className="text-2xl font-extrabold text-primary-600">₹{booking.totalAmount.toFixed(2)}</span>
            </div>
          </div>

          {error && (
            <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6">
              <div className="flex">
                <div className="flex-shrink-0">
                  <XCircle className="h-5 w-5 text-red-400" />
                </div>
                <div className="ml-3">
                  <p className="text-sm text-red-700">{error}</p>
                </div>
              </div>
            </div>
          )}

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 flex items-start space-x-4 mb-8">
            <div className="flex-shrink-0 mt-1">
              <CreditCard className="h-8 w-8 text-blue-600" />
            </div>
            <div>
              <h4 className="text-base font-bold text-blue-900">Mock Payment System</h4>
              <p className="text-sm text-blue-700 mt-1">
                This is a simulated payment gateway. No real money will be charged.
                Clicking the button below will process a successful payment via the backend API.
              </p>
            </div>
          </div>

          <button
            onClick={handlePayment}
            disabled={paymentProcessing}
            className="w-full flex justify-center items-center py-4 px-4 border border-transparent rounded-md shadow-sm text-lg font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {paymentProcessing ? (
              <>
                <Loader2 className="animate-spin -ml-1 mr-3 h-6 w-6 text-white" />
                Processing Payment...
              </>
            ) : (
              <>
                Pay ₹{booking.totalAmount.toFixed(2)} Now
                <ArrowRight className="ml-2 h-5 w-5" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default Payment;
