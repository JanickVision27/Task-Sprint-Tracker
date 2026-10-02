import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { projectApi } from '../api/endpoints';
import Modal from '../components/Modal';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newProject, setNewProject] = useState({ name: '', description: '' });

  const { data: projects, isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.getAll().then((res) => res.data),
  });

  const createMutation = useMutation({
    mutationFn: (data) => projectApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setIsCreateOpen(false);
      setNewProject({ name: '', description: '' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => projectApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  });

  function handleCreateSubmit(e) {
    e.preventDefault();
    createMutation.mutate({
      name: newProject.name.trim(),
      description: newProject.description.trim(),
    });
  }

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Top Navbar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-indigo-600 text-white font-bold flex items-center justify-center text-sm shadow-xs">
              ST
            </div>
            <span className="font-semibold text-slate-900 text-lg">Task & Sprint Tracker</span>
          </div>

          <div className="flex items-center gap-4">
            <div className="text-sm text-slate-600 flex items-center gap-2">
              <span className="font-medium text-slate-800">{user?.name || user?.email}</span>
              {user?.role && (
                <span className="text-xs bg-indigo-50 text-indigo-700 border border-indigo-200 px-2.5 py-0.5 rounded-full font-semibold">
                  {user.role}
                </span>
              )}
            </div>
            <button
              onClick={logout}
              className="text-sm font-medium text-slate-600 hover:text-red-600 border border-slate-200 hover:border-red-200 px-3.5 py-1.5 rounded-lg transition cursor-pointer"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-6xl mx-auto px-6 py-8">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Projects</h1>
            <p className="text-sm text-slate-500 mt-1">
              Select a project to manage its sprints and real-time Kanban board.
            </p>
          </div>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="bg-indigo-600 text-white text-sm font-medium px-4 py-2.5 rounded-lg shadow-xs hover:bg-indigo-700 transition self-start sm:self-auto cursor-pointer"
          >
            + New Project
          </button>
        </div>

        {isLoading && (
          <div className="text-sm text-slate-500 py-12 text-center">Loading projects...</div>
        )}

        {!isLoading && projects?.length === 0 && (
          <div className="bg-white border border-dashed border-slate-300 rounded-xl p-12 text-center">
            <h3 className="text-base font-semibold text-slate-800">No projects yet</h3>
            <p className="text-sm text-slate-500 mt-1 mb-5">
              Create your first project to start organizing sprints and tasks.
            </p>
            <button
              onClick={() => setIsCreateOpen(true)}
              className="bg-indigo-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-indigo-700 transition cursor-pointer"
            >
              + Create First Project
            </button>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {projects?.map((project) => (
            <div
              key={project.id}
              onClick={() => navigate(`/project/${project.id}`)}
              className="bg-white p-6 rounded-xl border border-slate-200 shadow-2xs hover:shadow-md hover:border-indigo-300 transition cursor-pointer relative group flex flex-col justify-between"
            >
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  if (window.confirm('Delete this project and all its sprints/tasks?')) {
                    deleteMutation.mutate(project.id);
                  }
                }}
                aria-label={`Delete ${project.name}`}
                className="absolute top-4 right-4 text-slate-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition text-lg leading-none p-1"
              >
                &times;
              </button>

              <div>
                <h2 className="text-lg font-semibold text-slate-900 group-hover:text-indigo-600 transition pr-6">
                  {project.name}
                </h2>
                <p className="text-sm text-slate-500 mt-2 line-clamp-3">
                  {project.description || 'No description provided.'}
                </p>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-xs font-medium text-indigo-600">
                <span>View Sprints</span>
                <span className="group-hover:translate-x-0.5 transition">&rarr;</span>
              </div>
            </div>
          ))}
        </div>
      </main>

      {/* Create Project Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create New Project">
        <form onSubmit={handleCreateSubmit}>
          <div className="mb-4">
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Project Name</label>
            <input
              placeholder="e.g. Mobile App Redesign"
              value={newProject.name}
              onChange={(e) => setNewProject({ ...newProject, name: e.target.value })}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              required
            />
          </div>

          <div className="mb-5">
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Description</label>
            <textarea
              rows={3}
              placeholder="Brief overview of what this project is about..."
              value={newProject.description}
              onChange={(e) => setNewProject({ ...newProject, description: e.target.value })}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          {createMutation.isError && (
            <p className="mb-4 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-2.5">
              {createMutation.error.response?.data?.message || 'Unable to create the project.'}
            </p>
          )}

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsCreateOpen(false)}
              className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-lg transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-4 py-2 text-sm font-medium bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-60 cursor-pointer"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Project'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
