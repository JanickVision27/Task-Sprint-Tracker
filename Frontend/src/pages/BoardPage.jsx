import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { DndContext, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import { taskApi } from '../api/endpoints';
import BoardColumn from '../components/BoardColumn';
import Modal from '../components/Modal';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../context/AuthContext';

const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'];

export default function BoardPage() {
  const { sprintId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, logout } = useAuth();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [boardError, setBoardError] = useState('');
  const [assignToMe, setAssignToMe] = useState(true);
  const [newTask, setNewTask] = useState({
    title: '',
    description: '',
    status: 'TODO',
    priority: 'MEDIUM',
  });

  useWebSocket(sprintId);

  const { data: tasks, isLoading } = useQuery({
    queryKey: ['tasks', sprintId],
    queryFn: () => taskApi.getBySprint(sprintId).then((res) => res.data),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, ...data }) => taskApi.update(id, data),
    onMutate: async (updatedTask) => {
      setBoardError('');
      await queryClient.cancelQueries({ queryKey: ['tasks', sprintId] });
      const previousTasks = queryClient.getQueryData(['tasks', sprintId]);

      queryClient.setQueryData(['tasks', sprintId], (old) =>
        old?.map((t) => (t.id === updatedTask.id ? { ...t, ...updatedTask } : t))
      );
      return { previousTasks };
    },
    onError: (err, variables, context) => {
      if (context?.previousTasks) {
        queryClient.setQueryData(['tasks', sprintId], context.previousTasks);
      }
      setBoardError(err.response?.data?.message || 'Unable to update task.');
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] });
    },
  });

  const createMutation = useMutation({
    mutationFn: (data) =>
      taskApi.create({
        ...data,
        sprintId: Number(sprintId),
        assigneeId: assignToMe ? user?.id || 1 : null,
      }),
    onSuccess: () => {
      setBoardError('');
      queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] });
      setIsCreateOpen(false);
      setNewTask({ title: '', description: '', status: 'TODO', priority: 'MEDIUM' });
      setAssignToMe(true);
    },
    onError: (err) => {
      setBoardError(err.response?.data?.message || 'Unable to create task.');
    },
  });

  function handleCreateSubmit(event) {
    event.preventDefault();
    createMutation.mutate(newTask);
  }

  const sensors = useSensors(useSensor(PointerSensor, { activationDistance: 10 }));

  function handleDragEnd(event) {
    const { active, over } = event;
    if (!over) return;

    const activeTask = tasks.find((t) => t.id === active.id);
    if (!activeTask) return;

    let newStatus;
    if (STATUSES.includes(over.id)) {
      newStatus = over.id;
    } else {
      const overTask = tasks.find((t) => t.id === over.id);
      if (overTask) {
        newStatus = overTask.status;
      }
    }

    if (newStatus && activeTask.status !== newStatus) {
      updateMutation.mutate({ id: activeTask.id, ...activeTask, status: newStatus });
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Top Navbar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/dashboard')}
              className="w-9 h-9 rounded-lg bg-indigo-600 text-white font-bold flex items-center justify-center text-sm shadow-xs cursor-pointer"
            >
              ST
            </button>
            <div className="flex items-center gap-2 text-sm">
              <button
                onClick={() => navigate(-1)}
                className="text-slate-500 hover:text-indigo-600 font-medium cursor-pointer"
              >
                &larr; Sprints
              </button>
              <span className="text-slate-300">/</span>
              <span className="font-semibold text-slate-900">Sprint #{sprintId} Board</span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <span className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              Live Sync
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

      {/* Main Board Area */}
      <main className="max-w-7xl w-full mx-auto px-6 py-8 flex-1 flex flex-col">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Sprint Kanban Board</h1>
            <p className="text-sm text-slate-500 mt-1">
              Drag and drop tasks across columns. Changes sync live with connected teammates.
            </p>
          </div>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="bg-indigo-600 text-white text-sm font-medium px-4 py-2.5 rounded-lg shadow-xs hover:bg-indigo-700 transition self-start sm:self-auto cursor-pointer"
          >
            + New Task
          </button>
        </div>

        {boardError && (
          <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm flex justify-between items-center">
            <span>{boardError}</span>
            <button
              onClick={() => setBoardError('')}
              className="text-red-500 hover:text-red-700 font-bold ml-4 text-lg leading-none"
            >
              &times;
            </button>
          </div>
        )}

        {isLoading ? (
          <div className="text-sm text-slate-500 py-12 text-center">Loading board...</div>
        ) : (
          <DndContext sensors={sensors} onDragEnd={handleDragEnd}>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 flex-1 items-start">
              {STATUSES.map((status) => (
                <BoardColumn
                  key={status}
                  status={status}
                  tasks={tasks?.filter((t) => t.status === status) || []}
                  onError={(msg) => setBoardError(msg)}
                />
              ))}
            </div>
          </DndContext>
        )}
      </main>

      {/* Create Task Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create New Task">
        <form onSubmit={handleCreateSubmit}>
          <div className="mb-4">
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Task Title</label>
            <input
              placeholder="e.g. Implement JWT authentication filter"
              value={newTask.title}
              onChange={(event) => setNewTask({ ...newTask, title: event.target.value })}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              required
            />
          </div>

          <div className="mb-4">
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              Description <span className="text-slate-400 font-normal">(optional)</span>
            </label>
            <textarea
              rows={3}
              placeholder="Add task details or acceptance criteria..."
              value={newTask.description}
              onChange={(event) => setNewTask({ ...newTask, description: event.target.value })}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          <div className="mb-4">
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Priority</label>
            <select
              value={newTask.priority}
              onChange={(event) => setNewTask({ ...newTask, priority: event.target.value })}
              className="w-full px-3.5 py-2.5 border border-slate-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="LOW">Low Priority</option>
              <option value="MEDIUM">Medium Priority</option>
              <option value="HIGH">High Priority</option>
            </select>
          </div>

          <label className="flex items-center gap-2.5 text-sm text-slate-700 mb-6 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={assignToMe}
              onChange={(event) => setAssignToMe(event.target.checked)}
              className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
            />
            <span>Assign task to me (required before moving to DONE)</span>
          </label>

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
              {createMutation.isPending ? 'Creating...' : 'Create Task'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
