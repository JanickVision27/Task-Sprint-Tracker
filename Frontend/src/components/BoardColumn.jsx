import { useDroppable } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import TaskCard from './TaskCard';

export default function BoardColumn({ status, tasks, activeId }) {
  // Set up this column as a "Droppable" area. Its ID is the status (e.g., "TODO")
  const { setNodeRef, isOver } = useDroppable({ id: status });

  // Color mapping for column headers
  const colors = {
    TODO: 'bg-blue-500',
    IN_PROGRESS: 'bg-yellow-500',
    DONE: 'bg-green-500',
  };

  return (
    <div 
      ref={setNodeRef} 
      className={`flex-1 bg-gray-200 rounded-xl p-4 flex flex-col transition-colors ${isOver ? 'bg-gray-300 ring-2 ring-blue-400' : ''}`}
    >
      <div className={`${colors[status]} text-white px-4 py-2 rounded-lg mb-4 font-bold text-center`}>
        {status.replace('_', ' ')} ({tasks.length})
      </div>
      
      <SortableContext items={tasks.map(t => t.id)} strategy={verticalListSortingStrategy}>
        <div className="flex flex-col gap-3 flex-1">
          {tasks.map(task => (
            <TaskCard key={task.id} task={task} />
          ))}
        </div>
      </SortableContext>
    </div>
  );
}
