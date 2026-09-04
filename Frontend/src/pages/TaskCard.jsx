import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

export default function TaskCard({ task }) {
  // Make this card "Sortable" (draggable)
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1, // Fade out while dragging
  };

  const priorityColors = {
    HIGH: 'border-red-500',
    MEDIUM: 'border-orange-500',
    LOW: 'border-blue-300',
  };

  return (
    <div 
      ref={setNodeRef} 
      style={style} 
      {...attributes} 
      {...listeners}
      className={`bg-white p-4 rounded-lg shadow border-l-4 ${priorityColors[task.priority] || 'border-gray-300'} cursor-grab active:cursor-grabbing hover:shadow-md transition`}
    >
      <h3 className="font-semibold text-gray-800">{task.title}</h3>
      {task.description && <p className="text-sm text-gray-500 mt-1 line-clamp-2">{task.description}</p>}
      <div className="mt-3 flex justify-between items-center text-xs">
        <span className="bg-gray-100 px-2 py-1 rounded">{task.priority}</span>
        {task.assigneeId && <span className="text-gray-400">Assigned</span>}
      </div>
    </div>
  );
}