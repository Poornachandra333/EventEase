import api from './axios';
import { TicketType } from '../types';

export const getTicketTypesByEvent = async (eventId: number): Promise<TicketType[]> => {
  const response = await api.get<TicketType[]>(`/events/${eventId}/ticket-types`);
  return response.data;
};

export const createTicketType = async (eventId: number, data: any): Promise<TicketType> => {
  const response = await api.post<TicketType>(`/events/${eventId}/ticket-types`, data);
  return response.data;
};

export const updateTicketType = async (id: number, data: any): Promise<TicketType> => {
  const response = await api.put<TicketType>(`/ticket-types/${id}`, data);
  return response.data;
};

export const deleteTicketType = async (id: number): Promise<void> => {
  await api.delete(`/ticket-types/${id}`);
};
