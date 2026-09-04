import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { projectApi, sprintApi } from '../api/endpoints';
import Modal from '../components/Modal'; // Import the Modal

export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // State for the Modal
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newSprint, setNewSprint] = useState({ name: '', startDate: '', endDate: '' });

  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId).then(res => res.data),
  });

  const { data: sprints, isLoading } = useQuery({
    queryKey: ['sprints', projectId],
    queryFn: () => sprintApi.getAll(projectId).then(res => res.data),
  });

  // Mutation to create a sprint
  const createMutation = useMutation({
    mutationFn: (data) => sprintApi.create({
      ...data,
      projectId: Number(projectId),
      // The backend DTO uses LocalDateTime; an HTML date input only supplies a date.
      startDate: `${data.startDate}T00:00:00`,
      endDate: `${data.endDate}T00:00:00`,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints', projectId] });
      setIsCreateOpen(false);
      setNewSprint({ name: '', startDate: '', endDate: '' });
    },
  });

  function handleCreateSubmit(e) {
    e.preventDefault();
    createMutation.mutate({ ...newSprint, name: newSprint.name.trim() });
  }

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <button onClick={() => navigate('/dashboard')} className="mb-6 text-blue-600 hover:underline">
        ← Back to Projects
      </button>
      <h1 className="text-3xl font-bold mb-2">{project?.name}</h1>
      <p className="text-gray-500 mb-8">{project?.description}</p>

      {/* CREATE SPRINT BUTTON */}
      <button 
        onClick={() => setIsCreateOpen(true)} 
        className="bg-blue-600 text-white px-6 py-2 rounded-lg shadow hover:bg-blue-700 mb-6 transition"
      >
        + New Sprint
      </button>

      <h2 className="text-2xl font-semibold mb-4">Sprints</h2>
      {isLoading && <p>Loading sprints...</p>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {sprints?.map(sprint => (
          <div key={sprint.id} className="bg-white p-6 rounded-lg shadow cursor-pointer hover:shadow-lg transition border-l-4 border-blue-500"
            onClick={() => navigate(`/board/${sprint.id}`)}>
            <h3 className="text-xl font-semibold">{sprint.name}</h3>
            <p className="text-sm text-gray-400 mt-2">
              {sprint.startDate} → {sprint.endDate}
            </p>
          </div>
        ))}
      </div>

      {/* CREATE SPRINT MODAL */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create New Sprint">
        <form onSubmit={handleCreateSubmit}>
          <input 
            placeholder="Sprint Name (e.g. Sprint 1)" 
            value={newSprint.name} 
            onChange={(e) => setNewSprint({...newSprint, name: e.target.value})}
            className="w-full p-2 mb-4 border rounded" 
            required 
          />
          <label className="block text-sm text-gray-600 mb-1">Start Date</label>
          <input 
            type="date" 
            value={newSprint.startDate} 
            onChange={(e) => setNewSprint({...newSprint, startDate: e.target.value})}
            className="w-full p-2 mb-4 border rounded" 
            required 
          />
          <label className="block text-sm text-gray-600 mb-1">End Date</label>
          <input 
            type="date" 
            value={newSprint.endDate} 
            onChange={(e) => setNewSprint({...newSprint, endDate: e.target.value})}
            className="w-full p-2 mb-6 border rounded" 
            required 
          />
          <button type="submit" className="w-full bg-green-600 text-white p-2 rounded hover:bg-green-700">
            {createMutation.isPending ? 'Creating…' : 'Create Sprint'}
          </button>
          {createMutation.isError && <p className="mt-3 text-sm text-red-600">{createMutation.error.response?.data?.message || 'Unable to create the sprint.'}</p>}
        </form>
      </Modal>
    </div>
  );
}
