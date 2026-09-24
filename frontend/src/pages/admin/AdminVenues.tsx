import React, { useState, useEffect } from 'react';
import { getVenues, createVenue, deleteVenue } from '../../api/venueApi';
import { Venue } from '../../types';
import { Plus, Trash2, X } from 'lucide-react';

const AdminVenues: React.FC = () => {
  const [venues, setVenues] = useState<Venue[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ name: '', address: '', city: '', capacity: 100 });

  const loadVenues = async () => {
    try {
      const data = await getVenues();
      setVenues(data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => { loadVenues(); }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createVenue(formData);
      setIsModalOpen(false);
      setFormData({ name: '', address: '', city: '', capacity: 100 });
      loadVenues();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create venue');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Delete venue?')) return;
    try {
      await deleteVenue(id);
      loadVenues();
    } catch {
      alert('Failed to delete venue');
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Venues</h1>
        </div>
        <button onClick={() => setIsModalOpen(true)} className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-md font-medium flex items-center shadow-sm">
          <Plus className="h-5 w-5 mr-1" /> Add Venue
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">City</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Capacity</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {venues.map(v => (
              <tr key={v.id}>
                <td className="px-6 py-4 text-sm font-medium text-gray-900">{v.name}<br/><span className="text-gray-500 text-xs font-normal">{v.address}</span></td>
                <td className="px-6 py-4 text-sm text-gray-500">{v.city}</td>
                <td className="px-6 py-4 text-sm text-gray-500">{v.capacity}</td>
                <td className="px-6 py-4 text-right text-sm">
                  <button onClick={() => handleDelete(v.id)} className="text-red-600 hover:text-red-900"><Trash2 className="h-4 w-4" /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isModalOpen && (
        <div className="fixed z-50 inset-0 overflow-y-auto bg-gray-500 bg-opacity-75 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6 text-left">
            <div className="flex justify-between mb-4">
              <h3 className="text-lg font-medium text-gray-900">Add Venue</h3>
              <button onClick={() => setIsModalOpen(false)}><X className="h-6 w-6 text-gray-400" /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div><label className="block text-sm font-medium">Name</label><input type="text" required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} className="mt-1 p-2 w-full border rounded-md" /></div>
              <div><label className="block text-sm font-medium">Address</label><input type="text" required value={formData.address} onChange={e => setFormData({...formData, address: e.target.value})} className="mt-1 p-2 w-full border rounded-md" /></div>
              <div><label className="block text-sm font-medium">City</label><input type="text" required value={formData.city} onChange={e => setFormData({...formData, city: e.target.value})} className="mt-1 p-2 w-full border rounded-md" /></div>
              <div><label className="block text-sm font-medium">Capacity</label><input type="number" required value={formData.capacity} onChange={e => setFormData({...formData, capacity: Number(e.target.value)})} className="mt-1 p-2 w-full border rounded-md" /></div>
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

export default AdminVenues;
