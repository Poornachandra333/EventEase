export interface User {
  id: number;
  name: string;
  email: string;
  roles?: string[];
  role?: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Venue {
  id: number;
  name: string;
  address: string;
  city: string;
  capacity: number;
}

export interface Event {
  id: number;
  title: string;
  description: string;
  category: string;
  eventDate: string; // LocalDate
  startTime: string; // LocalTime
  endTime: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';
  venue: Venue;
  createdAt: string;
  updatedAt: string;
}

export interface EventSummary {
  id: number;
  title: string;
  category: string;
  eventDate: string;
  status: string;
  venueName: string;
  city: string;
}

export interface TicketType {
  id: number;
  name: string;
  price: number;
  totalQuantity: number;
  availableQuantity: number;
  eventId: number;
}

export interface BookingItemRequest {
  ticketTypeId: number;
  quantity: number;
}

export interface CreateBookingRequest {
  eventId: number;
  items: BookingItemRequest[];
}

export interface BookingItemResponse {
  id: number;
  ticketTypeName: string;
  quantity: number;
  unitPrice: number;
  subTotal: number;
}

export interface BookingResponse {
  id: number;
  bookingReference: string;
  userId: number;
  userName: string;
  eventId: number;
  eventTitle: string;
  totalAmount: number;
  status: 'PENDING_PAYMENT' | 'CONFIRMED' | 'CANCELLED';
  bookedAt: string;
  cancelledAt: string | null;
  items: BookingItemResponse[];
  paymentOrderId: string | null;
  paymentStatus: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED' | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
