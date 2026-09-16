import api from './axios';
import { BookingResponse } from '../types';

export const initiatePayment = async (bookingId: number): Promise<BookingResponse> => {
  const response = await api.post<BookingResponse>(`/payments/bookings/${bookingId}/pay`);
  return response.data;
};

export const processRefund = async (bookingId: number): Promise<any> => {
  const response = await api.post(`/payments/bookings/${bookingId}/refund`);
  return response.data;
};
