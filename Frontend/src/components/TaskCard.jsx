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

export default function TaskCard({ task, users = [], onError, onSuccess }) {
  const { sprintId } = useParams();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  // Only show MEMBER accounts in the assignment dropdown
  const memberUsers = users.filter((candidate) => candidate.role === 'MEMBER');
  const assignedUser = users.find((candidate) => candidate.id === task.assigneeId);
  const isManagerOrAdmin = user?.role === 'MANAGER' || user?.role === 'ADMIN';

  const deleteMutation = useMutation({
    mutationFn: () => taskApi.delete(task.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] }),
    onError: (err) => onError?.(err.response?.data?.message || 'Unable to delete task.'),
  });

  // Manager / Admin approves a finished task in DONE -> confirms completion and removes it from the board
  const approveMutation = useMutation({
    mutationFn: () => taskApi.delete(task.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] });
      onSuccess?.(
        `Task "${task.title}" approved as completed${
          assignedUser ? ` by ${assignedUser.name}` : ''
        } and removed from the board!`
      );
    },
    onError: (err) => onError?.(err.response?.data?.message || 'Unable to approve task.'),
  });

  // Assign to self (for Member) or to a specific Member (for Manager/Admin)
  const assignMutation = useMutation({
    mutationFn: (newAssigneeId) =>
      taskApi.update(task.id, {
        ...task,
        assigneeId: newAssigneeId ? Number(newAssigneeId) : null,
      }),
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

        <div className="mt-3.5 pt-3 border-t border-slate-100 flex justify-between items-center gap-2 text-xs">
          <span className={`font-medium px-2 py-0.5 rounded-md border shrink-0 ${priorityStyle.badge}`}>
            {task.priority}
          </span>

          {/* Manager / Admin can assign or reassign a task directly to any MEMBER */}
          {isManagerOrAdmin ? (
            <select
              value={task.assigneeId || ''}
              onPointerDown={(e) => e.stopPropagation()}
              onClick={(e) => e.stopPropagation()}
              onChange={(e) => assignMutation.mutate(e.target.value)}
              className="text-xs border border-slate-200 rounded-md px-2 py-1 bg-slate-50 text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500 max-w-[160px] truncate cursor-pointer"
              title="Assign task to a Member"
            >
              <option value="">Unassigned</option>
              {memberUsers.map((member) => (
                <option key={member.id} value={member.id}>
                  {member.name}
                </option>
              ))}
            </select>
          ) : task.assigneeId ? (
            <span className="text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-md font-medium truncate">
              {assignedUser ? `Assigned: ${assignedUser.name}` : `Assigned (#${task.assigneeId})`}
            </span>
          ) : (
            <button
              type="button"
              onPointerDown={(e) => e.stopPropagation()}
              onClick={(e) => {
                e.stopPropagation();
                assignMutation.mutate(user?.id || 1);
              }}
              className="text-indigo-600 hover:text-indigo-800 font-medium hover:underline cursor-pointer"
            >
              + Assign to me
            </button>
          )}
        </div>

        {/* DONE Column Workflow: Member sees Waiting badge; Manager/Admin sees Approve & Finish button */}
        {task.status === 'DONE' && (
          <div className="mt-3 pt-2.5 border-t border-slate-100">
            {isManagerOrAdmin ? (
              <button
                type="button"
                disabled={approveMutation.isPending}
                onPointerDown={(e) => e.stopPropagation()}
                onClick={(e) => {
                  e.stopPropagation();
                  approveMutation.mutate();
                }}
                className="w-full py-1.5 px-3 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded-lg shadow-2xs transition flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-60"
              >
                <span>✓</span>
                <span>
                  {approveMutation.isPending
                    ? 'Approving...'
                    : 'Approve & Finish Task'}
                </span>
              </button>
            ) : (
              <div className="w-full py-1 px-2.5 bg-amber-50 border border-amber-200 text-amber-800 text-[11px] font-medium rounded-lg text-center">
                ⏳ Waiting for Manager Approval
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
