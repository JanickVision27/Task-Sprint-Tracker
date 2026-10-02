import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { projectApi, sprintApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import Modal from '../components/Modal';

function formatSprintDate(dateString) {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  if (Number.isNaN(date.getTime())) return dateString.split('T')[0];
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, logout } = useAuth();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newSprint, setNewSprint] = useState({ name: '', startDate: '', endDate: '' });

  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId).then((res) => res.data),
  });

  const { data: sprints, isLoading } = useQuery({
    queryKey: ['sprints', projectId],
    queryFn: () => sprintApi.getAll(projectId).then((res) => res.data),
  });

  const createMutation = useMutation({
    mutationFn: (data) =>
      sprintApi.create({
        ...data,
        projectId: Number(projectId),
        startDate: `${data.startDate}T00:00:00`,
        endDate: `${data.endDate}T00:00:00`,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints', projectId] });
      setIsCreateOpen(false);
      setNewSprint({ name: '', startDate: '', endDate: '' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => sprintApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sprints', projectId] }),
  });

  function handleCreateSubmit(e) {
    e.preventDefault();
    createMutation.mutate({ ...newSprint, name: newSprint.name.trim() });
  }

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Top Navbar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/dashboard')}
              className="w-9 h-9 rounded-lg bg-indigo-600 text-white font-bold flex items-center justify-center text-sm shadow-xs cursor-pointer"
            >
              ST
            </button>
            <div className="flex items-center gap-2 text-sm">
              <button
                onClick={() => navigate('/dashboard')}
                className="text-slate-500 hover:text-indigo-600 font-medium cursor-pointer"
              >
                Projects
              </button>
              <span className="text-slate-300">/</span>
              <span className="font-semibold text-slate-900">{project?.name || 'Project'}</span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-slate-700 hidden sm:inline">
              {user?.name || user?.email}
            </span>
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
        <button
          onClick={() => navigate('/dashboard')}
          className="text-sm font-medium text-indigo-600 hover:text-indigo-700 mb-4 inline-flex items-center gap-1 cursor-pointer"
        >
          &larr; Back to Projects
        </button>

        {/* Project Summary Header */}
        <div className="bg-white border border-slate-200 rounded-xl p-6 mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-2xs">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">{project?.name}</h1>
            <p className="text-sm text-slate-500 mt-1">
              {project?.description || 'Organize sprints and track tasks on the Kanban board.'}
            </p>
          </div>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="bg-indigo-600 text-white text-sm font-medium px-4 py-2.5 rounded-lg shadow-xs hover:bg-indigo-700 transition self-start sm:self-auto shrink-0 cursor-pointer"
          >
            + New Sprint
          </button>
        </div>

        <h2 className="text-lg font-semibold text-slate-800 mb-4">Sprints</h2>

        {isLoading && (
          <div className="text-sm text-slate-500 py-12 text-center">Loading sprints...</div>
        )}

        {!isLoading && sprints?.length === 0 && (
          <div className="bg-white border border-dashed border-slate-300 rounded-xl p-12 text-center">
            <h3 className="text-base font-semibold text-slate-800">No sprints in this project yet</h3>
            <p className="text-sm text-slate-500 mt-1 mb-5">
              Create a sprint to open a Kanban board and start adding tasks.
            </p>
            <button
              onClick={() => setIsCreateOpen(true)}
              className="bg-indigo-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-indigo-700 transition cursor-pointer"
            >
              + Create First Sprint
            </button>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {sprints?.map((sprint) => (
            <div
              key={sprint.id}
              onClick={() => navigate(`/board/${sprint.id}`)}
              className="bg-white p-6 rounded-xl border border-slate-200 border-l-4 border-l-indigo-600 shadow-2xs hover:shadow-md transition cursor-pointer relative group flex flex-col justify-between"
            >
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  if (window.confirm('Delete this sprint and all its tasks?')) {
                    deleteMutation.mutate(sprint.id);
                  }
                }}
                aria-label={`Delete ${sprint.name}`}
                className="absolute top-4 right-4 text-slate-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition text-lg leading-none p-1"
              >
                &times;
              </button>

              <div>
                <h3 className="text-lg font-semibold text-slate-900 group-hover:text-indigo-600 transition pr-6">
                  {sprint.name}
                </h3>
                <p className="text-xs font-medium text-slate-500 mt-2 bg-slate-100 inline-block px-2.5 py-1 rounded-md">
                  {formatSprintDate(sprint.startDate)} &rarr; {formatSprintDate(sprint.endDate)}
                </p>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-xs font-medium text-indigo-600">
                <span>Open Kanban Board</span>
                <span className="group-hover:translate-x-0.5 transition">&rarr;</span>
              </div>
            </div>
          ))}
        </div>
      </main>

      {/* Create Sprint Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create New Sprint">
        <form onSubmit={handleCreateSubmit}>
          <div className="mb-4">
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Sprint Name</label>
            <input
              placeholder="e.g. Sprint 1 — Authentication & Setup"
              value={newSprint.name}
              onChange={(e) => setNewSprint({ ...newSprint, name: e.target.value })}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3 mb-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Start Date</label>
              <input
                type="date"
                value={newSprint.startDate}
                onChange={(e) => setNewSprint({ ...newSprint, startDate: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">End Date</label>
              <input
                type="date"
                value={newSprint.endDate}
                onChange={(e) => setNewSprint({ ...newSprint, endDate: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                required
              />
            </div>
          </div>

          {createMutation.isError && (
            <p className="mb-4 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-2.5">
              {createMutation.error.response?.data?.message || 'Unable to create the sprint.'}
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
              {createMutation.isPending ? 'Creating...' : 'Create Sprint'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
