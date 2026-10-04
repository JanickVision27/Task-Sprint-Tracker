import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { taskApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';

const PRIORITY_STYLES = {
  HIGH: {
    border: 'border-l-red-500',
    badge: 'bg-red-50 text-red-700 border-red-200',
  },
  MEDIUM: {
    border: 'border-l-amber-500',
    badge: 'bg-amber-50 text-amber-700 border-amber-200',
  },
  LOW: {
    border: 'border-l-blue-500',
    badge: 'bg-blue-50 text-blue-700 border-blue-200',
  },
};

export default function TaskCard({ task, users = [], onError }) {
  const { sprintId } = useParams();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const assignedUser = users.find((candidate) => candidate.id === task.assigneeId);

  const deleteMutation = useMutation({
    mutationFn: () => taskApi.delete(task.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] }),
    onError: (err) => onError?.(err.response?.data?.message || 'Unable to delete task.'),
  });

  const assignMutation = useMutation({
    mutationFn: () => taskApi.update(task.id, { ...task, assigneeId: user?.id || 1 }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] }),
    onError: (err) => onError?.(err.response?.data?.message || 'Unable to assign task.'),
  });

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const priorityStyle = PRIORITY_STYLES[task.priority] || {
    border: 'border-l-slate-400',
    badge: 'bg-slate-100 text-slate-700 border-slate-200',
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`bg-white p-4 rounded-xl border border-slate-200 border-l-4 ${priorityStyle.border} shadow-2xs hover:shadow-md transition relative group`}
    >
      <button
        type="button"
        onClick={(event) => {
          event.stopPropagation();
          if (window.confirm('Delete this task?')) deleteMutation.mutate();
        }}
        aria-label={`Delete ${task.title}`}
        className="absolute top-3 right-3 text-slate-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition text-lg leading-none p-1"
      >
        &times;
      </button>

      <div {...attributes} {...listeners} className="cursor-grab active:cursor-grabbing">
        <h3 className="font-semibold text-sm text-slate-800 pr-5">{task.title}</h3>
        {task.description && (
          <p className="text-xs text-slate-500 mt-1.5 line-clamp-2 leading-relaxed">
            {task.description}
          </p>
        )}

        <div className="mt-3.5 pt-3 border-t border-slate-100 flex justify-between items-center text-xs">
          <span className={`font-medium px-2 py-0.5 rounded-md border ${priorityStyle.badge}`}>
            {task.priority}
          </span>

          {task.assigneeId ? (
            <span className="text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-md font-medium">
              {assignedUser ? `Assigned: ${assignedUser.name}` : `Assigned (#${task.assigneeId})`}
            </span>
          ) : (
            <button
              type="button"
              onPointerDown={(e) => e.stopPropagation()}
              onClick={(e) => {
                e.stopPropagation();
                assignMutation.mutate();
              }}
              className="text-indigo-600 hover:text-indigo-800 font-medium hover:underline cursor-pointer"
            >
              + Assign to me
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
