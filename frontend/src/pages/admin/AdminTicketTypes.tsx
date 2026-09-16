import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getTicketTypesByEvent, createTicketType, deleteTicketType } from '../../api/ticketApi';
import { getEventById } from '../../api/eventApi';
import { TicketType, Event } from '../../types';
import { Plus, Trash2, X, ArrowLeft } from 'lucide-react';

const AdminTicketTypes: React.FC = () => {
  const { eventId } = useParams<{ eventId: string }>();
  const [event, setEvent] = useState<Event | null>(null);
  const [tickets, setTickets] = useState<TicketType[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ name: '', price: 0, totalQuantity: 100 });

  const loadData = async () => {
    try {
      if (!eventId) return;
      const [eventData, ticketData] = await Promise.all([
        getEventById(Number(eventId)),
        getTicketTypesByEvent(Number(eventId))
      ]);
      setEvent(eventData);
      setTickets(ticketData);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => { loadData(); }, [eventId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createTicketType(Number(eventId), formData);
      setIsModalOpen(false);
      setFormData({ name: '', price: 0, totalQuantity: 100 });
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create ticket type');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Delete ticket type?')) return;
    try {
      await deleteTicketType(id);
      loadData();
    } catch (err) {
      alert('Failed to delete ticket type');
    }
  };

  return (
    <div>
      <div className="mb-4">
        <Link to="/admin/events" className="text-primary-600 hover:underline flex items-center text-sm font-medium">
          <ArrowLeft className="h-4 w-4 mr-1" /> Back to Events
        </Link>
      </div>
      
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Tickets: {event?.title}</h1>
        </div>
        <button onClick={() => setIsModalOpen(true)} className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-md font-medium flex items-center shadow-sm">
          <Plus className="h-5 w-5 mr-1" /> Add Ticket Type
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Price</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Total Qty</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Available Qty</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {tickets.map(t => (
              <tr key={t.id}>
                <td className="px-6 py-4 text-sm font-medium text-gray-900">{t.name}</td>
                <td className="px-6 py-4 text-sm text-gray-500">₹{t.price}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{t.totalQuantity}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{t.availableQuantity}</td>
                <td className="px-6 py-4 text-right text-sm">
                  <button onClick={() => handleDelete(t.id)} className="text-red-600 hover:text-red-900"><Trash2 className="h-4 w-4" /></button>
                </td>
              </tr>
            ))}
            {tickets.length === 0 && (
              <tr><td colSpan={5} className="px-6 py-4 text-center text-gray-500">No ticket types found.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {isModalOpen && (
        <div className="fixed z-50 inset-0 overflow-y-auto bg-gray-500 bg-opacity-75 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6 text-left">
            <div className="flex justify-between mb-4">
              <h3 className="text-lg font-medium text-gray-900">Add Ticket Type</h3>
              <button onClick={() => setIsModalOpen(false)}><X className="h-6 w-6 text-gray-400" /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div><label className="block text-sm font-medium">Name</label><input type="text" required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} className="mt-1 p-2 w-full border rounded-md" placeholder="e.g. VIP, Regular" /></div>
              <div><label className="block text-sm font-medium">Price (₹)</label><input type="number" step="0.01" required value={formData.price} onChange={e => setFormData({...formData, price: Number(e.target.value)})} className="mt-1 p-2 w-full border rounded-md" /></div>
              <div><label className="block text-sm font-medium">Total Quantity</label><input type="number" required value={formData.totalQuantity} onChange={e => setFormData({...formData, totalQuantity: Number(e.target.value)})} className="mt-1 p-2 w-full border rounded-md" /></div>
              <div className="mt-4 flex justify-end gap-2">
                <button type="button" onClick={() => setIsModalOpen(false)} className="px-4 py-2 border rounded-md">Cancel</button>
                <button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-md">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminTicketTypes;
