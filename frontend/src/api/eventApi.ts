import api from './axios';
import { Event, EventSummary, Page } from '../types';

export const getEvents = async (params?: any): Promise<Page<EventSummary>> => {
  const response = await api.get<Page<EventSummary>>('/events', { params });
  return response.data;
};

export const getEventById = async (id: number): Promise<Event> => {
  const response = await api.get<Event>(`/events/${id}`);
  return response.data;
};

export const createEvent = async (data: any): Promise<Event> => {
  const response = await api.post<Event>('/events', data);
  return response.data;
};

export const updateEvent = async (id: number, data: any): Promise<Event> => {
  const response = await api.put<Event>(`/events/${id}`, data);
  return response.data;
};

export const deleteEvent = async (id: number): Promise<void> => {
  await api.delete(`/events/${id}`);
};
