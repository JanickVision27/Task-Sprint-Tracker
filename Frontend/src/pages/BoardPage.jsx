import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { DndContext, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import { taskApi } from '../api/endpoints';
import BoardColumn from '../components/BoardColumn';
import Modal from '../components/Modal';
import { useWebSocket } from '../hooks/useWebSocket';

const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'];

export default function BoardPage() {
  const { sprintId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [activeId, setActiveId] = useState(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
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
      await queryClient.cancelQueries({ queryKey: ['tasks', sprintId] });
      const previousTasks = queryClient.getQueryData(['tasks', sprintId]);
      
      queryClient.setQueryData(['tasks', sprintId], (old) =>
        old?.map(t => t.id === updatedTask.id ? { ...t, ...updatedTask } : t)
      );
      return { previousTasks };
    },
    onError: (err, variables, context) => {
      queryClient.setQueryData(['tasks', sprintId], context.previousTasks);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] });
    },
  });

  const createMutation = useMutation({
    mutationFn: (data) => taskApi.create({ ...data, sprintId: Number(sprintId) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] });
      setIsCreateOpen(false);
      setNewTask({ title: '', description: '', status: 'TODO', priority: 'MEDIUM' });
    },
  });

  function handleCreateSubmit(event) {
    event.preventDefault();
    createMutation.mutate(newTask);
  }

  // Setup drag-and-drop sensors (requires moving the mouse a bit to start drag)
  const sensors = useSensors(useSensor(PointerSensor, { activationDistance: 10 }));

  // Handle dragging over a new column
  function handleDragOver(event) {
    // Logic for moving between columns can go here if needed
  }

  // Handle dropping a task
  function handleDragEnd(event) {
    const { active, over } = event;
    setActiveId(null);

    if (!over) return; // Dropped outside a droppable area

    const taskId = active.id;
    const newStatus = over.id; // The column ID is the status (TODO, IN_PROGRESS, DONE)

    const task = tasks.find(t => t.id === taskId);
    if (task && task.status !== newStatus) {
      // Send update to backend (which will broadcast via WebSocket to other users!)
      updateMutation.mutate({ id: taskId, ...task, status: newStatus });
    }
  }

  if (isLoading) return <div className="p-8">Loading board...</div>;

  return (
    <div className="min-h-screen bg-gray-100 p-8 flex flex-col">
      <button onClick={() => navigate(-1)} className="mb-6 text-blue-600 hover:underline self-start">
        ← Back to Sprints
      </button>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Sprint Board</h1>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          + New Task
        </button>
      </div>

      <DndContext 
        sensors={sensors} 
        onDragStart={(e) => setActiveId(e.active.id)} 
        onDragOver={handleDragOver} 
        onDragEnd={handleDragEnd}
      >
        <div className="flex gap-6 flex-1 overflow-x-auto">
          {STATUSES.map(status => (
            <BoardColumn 
              key={status} 
              status={status} 
              tasks={tasks?.filter(t => t.status === status) || []} 
              activeId={activeId}
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
          <button type="submit" disabled={createMutation.isPending} className="w-full bg-green-600 text-white p-2 rounded hover:bg-green-700 disabled:opacity-50">
            {createMutation.isPending ? 'Creating…' : 'Create Task'}
          </button>
        </form>
      </Modal>
    </div>
  );
}
