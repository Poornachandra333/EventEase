import api from './axios';
import { Venue } from '../types';

export const getVenues = async (): Promise<Venue[]> => {
  const response = await api.get<Venue[]>('/venues');
  return response.data;
};

export const getVenueById = async (id: number): Promise<Venue> => {
  const response = await api.get<Venue>(`/venues/${id}`);
  return response.data;
};

export const createVenue = async (data: any): Promise<Venue> => {
  const response = await api.post<Venue>('/venues', data);
  return response.data;
};

export const updateVenue = async (id: number, data: any): Promise<Venue> => {
  const response = await api.put<Venue>(`/venues/${id}`, data);
  return response.data;
};

export const deleteVenue = async (id: number): Promise<void> => {
  await api.delete(`/venues/${id}`);
};
