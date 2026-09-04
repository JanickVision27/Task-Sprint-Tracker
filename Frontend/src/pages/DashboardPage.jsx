import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { projectApi } from '../api/endpoints';
import Modal from '../components/Modal'; // Import the Modal

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  // State for the Modal
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newProject, setNewProject] = useState({ name: '', description: '' });

  // Fetch projects
  const { data: projects, isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.getAll().then(res => res.data),
  });

  // Mutation to create a project
  const createMutation = useMutation({
    mutationFn: (data) => projectApi.create(data),
    onSuccess: () => {
      // Refresh the project list and close the modal
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setIsCreateOpen(false);
      setNewProject({ name: '', description: '' });
    },
  });

  function handleCreateSubmit(e) {
    e.preventDefault();
    createMutation.mutate({
      name: newProject.name.trim(),
      description: newProject.description.trim(),
    });
  }

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">My Projects</h1>
        <div className="flex items-center gap-4">
          <span className="text-gray-600">Hello, {user?.name || user?.email}</span>
          <button onClick={logout} className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600">
            Logout
          </button>
        </div>
      </div>

      {/* CREATE PROJECT BUTTON */}
      <button 
        onClick={() => setIsCreateOpen(true)} 
        className="bg-blue-600 text-white px-6 py-2 rounded-lg shadow hover:bg-blue-700 mb-8 transition"
      >
        + New Project
      </button>

      {isLoading && <p>Loading projects...</p>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {projects?.map(project => (
          <div key={project.id} className="bg-white p-6 rounded-lg shadow cursor-pointer hover:shadow-lg transition"
            onClick={() => navigate(`/project/${project.id}`)}>
            <h2 className="text-xl font-semibold">{project.name}</h2>
            <p className="text-gray-500 mt-2">{project.description}</p>
          </div>
        ))}
      </div>

      {/* CREATE PROJECT MODAL */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create New Project">
        <form onSubmit={handleCreateSubmit}>
          <input 
            placeholder="Project Name" 
            value={newProject.name} 
            onChange={(e) => setNewProject({...newProject, name: e.target.value})}
            className="w-full p-2 mb-4 border rounded" 
            required 
          />
          <textarea 
            placeholder="Description" 
            value={newProject.description} 
            onChange={(e) => setNewProject({...newProject, description: e.target.value})}
            className="w-full p-2 mb-4 border rounded" 
          />
          <button type="submit" className="w-full bg-green-600 text-white p-2 rounded hover:bg-green-700">
            {createMutation.isPending ? 'Creating…' : 'Create Project'}
          </button>
          {createMutation.isError && <p className="mt-3 text-sm text-red-600">{createMutation.error.response?.data?.message || 'Unable to create the project.'}</p>}
        </form>
      </Modal>
    </div>
  );
}
