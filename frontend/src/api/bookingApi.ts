import api from './axios';
import { BookingResponse, CreateBookingRequest, Page } from '../types';

export const createBooking = async (data: CreateBookingRequest): Promise<BookingResponse> => {
  const response = await api.post<BookingResponse>('/bookings', data);
  return response.data;
};

export const getMyBookings = async (page = 0, size = 10): Promise<Page<BookingResponse>> => {
  const response = await api.get<Page<BookingResponse>>('/bookings/my', {
    params: { page, size }
  });
  return response.data;
};

export const getBookingById = async (id: number): Promise<BookingResponse> => {
  const response = await api.get<BookingResponse>(`/bookings/${id}`);
  return response.data;
};

export const cancelBooking = async (id: number): Promise<BookingResponse> => {
  const response = await api.put<BookingResponse>(`/bookings/${id}/cancel`);
  return response.data;
};
