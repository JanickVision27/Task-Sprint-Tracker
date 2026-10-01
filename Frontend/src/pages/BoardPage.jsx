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
  const { user } = useAuth();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [boardError, setBoardError] = useState('');
  const [assignToMe, setAssignToMe] = useState(true);
  const [newTask, setNewTask] = useState({
    title: '', description: '', status: 'TODO', priority: 'MEDIUM',
  });

  // The subscription exists only while this specific sprint board is displayed.
  useWebSocket(sprintId);

  // Fetch tasks for this sprint
  const { data: tasks, isLoading } = useQuery({
    queryKey: ['tasks', sprintId],
    queryFn: () => taskApi.getBySprint(sprintId).then(res => res.data),
  });

  // Mutation to update a task's status when dragged
  const updateMutation = useMutation({
    mutationFn: ({ id, ...data }) => taskApi.update(id, data),
    // Optimistic update: UI moves instantly, then syncs with DB
    onMutate: async (updatedTask) => {
      setBoardError('');
      await queryClient.cancelQueries({ queryKey: ['tasks', sprintId] });
      const previousTasks = queryClient.getQueryData(['tasks', sprintId]);
      
      queryClient.setQueryData(['tasks', sprintId], (old) =>
        old?.map(t => t.id === updatedTask.id ? { ...t, ...updatedTask } : t)
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
    mutationFn: (data) => taskApi.create({
      ...data,
      sprintId: Number(sprintId),
      assigneeId: assignToMe ? (user?.id || 1) : null,
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

  // Setup drag-and-drop sensors (requires moving the mouse a bit to start drag)
  const sensors = useSensors(useSensor(PointerSensor, { activationDistance: 10 }));

  // Handle dropping a task
  function handleDragEnd(event) {
    const { active, over } = event;

    if (!over) return; // Dropped outside

    const activeTask = tasks.find(t => t.id === active.id);
    if (!activeTask) return;

    // Determine the target status
    let newStatus;
    
    // If we dropped it on a column (empty space)
    if (STATUSES.includes(over.id)) {
      newStatus = over.id;
    } else {
      // If we dropped it on another task, find that task's status
      const overTask = tasks.find(t => t.id === over.id);
      if (overTask) {
        newStatus = overTask.status;
      }
    }

    // If we successfully found a new status, and it's different from the old one, update it!
    if (newStatus && activeTask.status !== newStatus) {
      updateMutation.mutate({ id: activeTask.id, ...activeTask, status: newStatus });
    }
  }

  if (isLoading) return <div className="p-8">Loading board...</div>;

  return (
    <div className="min-h-screen bg-gray-100 p-8 flex flex-col">
      <button onClick={() => navigate(-1)} className="mb-6 text-blue-600 hover:underline self-start">
        ← Back to Sprints
      </button>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold">Sprint Board</h1>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          + New Task
        </button>
      </div>

      {boardError && (
        <div className="mb-6 bg-red-50 border border-red-300 text-red-700 px-4 py-3 rounded-lg flex justify-between items-center">
          <span>{boardError}</span>
          <button onClick={() => setBoardError('')} className="text-red-500 font-bold ml-4">×</button>
        </div>
      )}

      <DndContext 
        sensors={sensors} 
        onDragEnd={handleDragEnd}
      >
        <div className="flex gap-6 flex-1 overflow-x-auto">
          {STATUSES.map(status => (
            <BoardColumn 
              key={status} 
              status={status} 
              tasks={tasks?.filter(t => t.status === status) || []} 
            />
          ))}
        </div>
      </DndContext>

      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create Task">
        <form onSubmit={handleCreateSubmit}>
          <input
            placeholder="Task title"
            value={newTask.title}
            onChange={(event) => setNewTask({ ...newTask, title: event.target.value })}
            className="w-full p-2 mb-4 border rounded"
            required
          />
          <textarea
            placeholder="Description (optional)"
            value={newTask.description}
            onChange={(event) => setNewTask({ ...newTask, description: event.target.value })}
            className="w-full p-2 mb-4 border rounded"
          />
          <label className="block text-sm text-gray-600 mb-1">Priority</label>
          <select
            value={newTask.priority}
            onChange={(event) => setNewTask({ ...newTask, priority: event.target.value })}
            className="w-full p-2 mb-4 border rounded"
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
          <label className="flex items-center gap-2 text-sm text-gray-700 mb-4 cursor-pointer">
            <input
              type="checkbox"
              checked={assignToMe}
              onChange={(event) => setAssignToMe(event.target.checked)}
            />
            Assign task to me (required before moving to DONE)
          </label>
          <button type="submit" disabled={createMutation.isPending} className="w-full bg-green-600 text-white p-2 rounded hover:bg-green-700 disabled:opacity-50">
            {createMutation.isPending ? 'Creating…' : 'Create Task'}
          </button>
        </form>
      </Modal>
    </div>
  );
}
