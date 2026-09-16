import React, { useState, useEffect } from 'react';
import { getEvents, deleteEvent, createEvent } from '../../api/eventApi';
import { getVenues } from '../../api/venueApi';
import { EventSummary, Venue } from '../../types';
import { Calendar, Plus, Edit, Trash2, X, Ticket } from 'lucide-react';
import { format } from 'date-fns';
import { Link } from 'react-router-dom';

const AdminEvents: React.FC = () => {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [venues, setVenues] = useState<Venue[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  
  // Form State
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: '',
    eventDate: '',
    startTime: '',
    endTime: '',
    venueId: '',
    status: 'DRAFT'
  });

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const data = await getEvents({ size: 100, sort: 'eventDate,desc' });
      setEvents(data.content);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchVenues = async () => {
    try {
      const data = await getVenues();
      setVenues(data);
      if (data.length > 0) setFormData(prev => ({ ...prev, venueId: data[0].id.toString() }));
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    fetchEvents();
    fetchVenues();
  }, []);

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this event?')) return;
    try {
      await deleteEvent(id);
      fetchEvents();
    } catch (err) {
      alert('Failed to delete event');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createEvent({
        ...formData,
        venueId: Number(formData.venueId)
      });
      setIsModalOpen(false);
      setFormData({
        title: '', description: '', category: '', eventDate: '', startTime: '', endTime: '', venueId: venues[0]?.id.toString() || '', status: 'DRAFT'
      });
      fetchEvents();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create event');
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Events</h1>
          <p className="text-gray-600">Create, edit, or delete events.</p>
        </div>
        <button 
          onClick={() => setIsModalOpen(true)}
          className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-md font-medium flex items-center transition-colors shadow-sm"
        >
          <Plus className="h-5 w-5 mr-1" /> Create Event
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Event Name</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Venue</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th scope="col" className="relative px-6 py-3"><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {events.map((event) => (
              <tr key={event.id}>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{event.title}</div>
                  <div className="text-sm text-gray-500">{event.category}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {format(new Date(event.eventDate), 'MMM d, yyyy')}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {event.venueName}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    event.status === 'PUBLISHED' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                  }`}>
                    {event.status}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <Link to={`/admin/events/${event.id}/tickets`} title="Manage Tickets" className="text-primary-600 hover:text-primary-900 mr-4 inline-block align-middle"><Ticket className="h-4 w-4" /></Link>
                  <button className="text-primary-600 hover:text-primary-900 mr-4 align-middle"><Edit className="h-4 w-4" /></button>
                  <button onClick={() => handleDelete(event.id)} className="text-red-600 hover:text-red-900 align-middle"><Trash2 className="h-4 w-4" /></button>
                </td>
              </tr>
            ))}
            {events.length === 0 && !loading && (
              <tr>
                <td colSpan={5} className="px-6 py-4 text-center text-gray-500">No events found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Create Modal */}
      {isModalOpen && (
        <div className="fixed z-50 inset-0 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true">
              <div className="absolute inset-0 bg-gray-500 opacity-75" onClick={() => setIsModalOpen(false)}></div>
            </div>
            <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
            <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
              <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div className="flex justify-between items-center mb-5">
                  <h3 className="text-lg leading-6 font-medium text-gray-900">Create New Event</h3>
                  <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-500"><X className="h-6 w-6" /></button>
                </div>
                <form onSubmit={handleSubmit}>
                  <div className="space-y-4">
                    <div><label className="block text-sm font-medium text-gray-700">Title</label>
                      <input type="text" required value={formData.title} onChange={e => setFormData({...formData, title: e.target.value})} className="mt-1 p-2 block w-full border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500 sm:text-sm" />
                    </div>
                    <div><label className="block text-sm font-medium text-gray-700">Description</label>
                      <textarea required value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} className="mt-1 p-2 block w-full border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500 sm:text-sm" rows={3}></textarea>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div><label className="block text-sm font-medium text-gray-700">Category</label>
                        <input type="text" required value={formData.category} onChange={e => setFormData({...formData, category: e.target.value})} className="mt-1 p-2 block w-full border border-gray-300 rounded-md shadow-sm sm:text-sm" />
                      </div>
                      <div><label className="block text-sm font-medium text-gray-700">Date (YYYY-MM-DD)</label>
                        <input type="date" required value={formData.eventDate} onChange={e => setFormData({...formData, eventDate: e.target.value})} className="mt-1 p-2 block w-full border border-gray-300 rounded-md shadow-sm sm:text-sm" />
                      </div>
                      <div><label className="block text-sm font-medium text-gray-700">Start Time (HH:MM:SS)</label>
                        <input type="time" step="1" required value={formData.startTime} onChange={e => setFormData({...formData, startTime: e.target.value})} className="mt-1 p-2 block w-full border border-gray-300 rounded-md shadow-sm sm:text-sm" />
                      </div>
                      <div><label className="block text-sm font-medium text-gray-700">End Time (HH:MM:SS)</label>
                        <input type="time" step="1" required value={formData.endTime} onChange={e => setFormData({...formData, endTime: e.target.value})} className="mt-1 p-2 block w-full border border-gray-300 rounded-md shadow-sm sm:text-sm" />
                      </div>
                    </div>
                    <div><label className="block text-sm font-medium text-gray-700">Venue</label>
                      <select required value={formData.venueId} onChange={e => setFormData({...formData, venueId: e.target.value})} className="mt-1 p-2 block w-full border border-gray-300 rounded-md shadow-sm sm:text-sm">
                        {venues.map(v => <option key={v.id} value={v.id}>{v.name}</option>)}
                      </select>
                    </div>
                    <div><label className="block text-sm font-medium text-gray-700">Status</label>
                      <select required value={formData.status} onChange={e => setFormData({...formData, status: e.target.value})} className="mt-1 p-2 block w-full border border-gray-300 rounded-md shadow-sm sm:text-sm">
                        <option value="DRAFT">DRAFT</option>
                        <option value="PUBLISHED">PUBLISHED</option>
                        <option value="CANCELLED">CANCELLED</option>
                      </select>
                    </div>
                  </div>
                  <div className="mt-5 sm:mt-6 sm:flex sm:flex-row-reverse">
                    <button type="submit" className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary-600 text-base font-medium text-white hover:bg-primary-700 focus:outline-none sm:ml-3 sm:w-auto sm:text-sm">Save</button>
                    <button type="button" onClick={() => setIsModalOpen(false)} className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm">Cancel</button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminEvents;
