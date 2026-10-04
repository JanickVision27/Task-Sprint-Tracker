import { useDroppable } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import TaskCard from './TaskCard';

const COLUMN_META = {
  TODO: {
    label: 'To Do',
    dot: 'bg-blue-500',
    badge: 'bg-blue-50 text-blue-700 border-blue-200',
  },
  IN_PROGRESS: {
    label: 'In Progress',
    dot: 'bg-amber-500',
    badge: 'bg-amber-50 text-amber-700 border-amber-200',
  },
  DONE: {
    label: 'Done',
    dot: 'bg-emerald-500',
    badge: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  },
};

export default function BoardColumn({ status, tasks, users, onError }) {
  const { setNodeRef, isOver } = useDroppable({ id: status });
  const meta = COLUMN_META[status] || {
    label: status,
    dot: 'bg-slate-400',
    badge: 'bg-slate-100 text-slate-700 border-slate-200',
  };

  return (
    <div
      ref={setNodeRef}
      className={`bg-slate-100/90 border border-slate-200/80 rounded-xl p-4 min-h-[460px] flex flex-col transition-all ${
        isOver ? 'ring-2 ring-indigo-500 bg-indigo-50/40' : ''
      }`}
    >
      {/* Column Header */}
      <div className="flex items-center justify-between mb-4 px-1">
        <div className="flex items-center gap-2">
          <span className={`w-2.5 h-2.5 rounded-full ${meta.dot}`} />
          <h2 className="font-semibold text-sm text-slate-800">{meta.label}</h2>
        </div>
        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${meta.badge}`}>
          {tasks.length}
        </span>
      </div>

      {/* Draggable Cards */}
      <SortableContext items={tasks.map((t) => t.id)} strategy={verticalListSortingStrategy}>
        <div className="flex flex-col gap-3 flex-1">
          {tasks.map((task) => (
            <TaskCard key={task.id} task={task} users={users} onError={onError} />
          ))}

          {tasks.length === 0 && (
            <div className="flex-1 flex items-center justify-center border border-dashed border-slate-300/80 rounded-lg p-6 text-xs text-slate-400 text-center">
              Drop tasks here
            </div>
          )}
        </div>
      </SortableContext>
    </div>
  );
}
