import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { taskApi } from '../api/endpoints';

export default function TaskCard({ task }) {
  const { sprintId } = useParams();
  const queryClient = useQueryClient();

  const deleteMutation = useMutation({
    mutationFn: () => taskApi.delete(task.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks', sprintId] }),
  });

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const priorityColors = {
    HIGH: 'border-red-500',
    MEDIUM: 'border-orange-500',
    LOW: 'border-blue-300',
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes} 
         className={`bg-white p-4 rounded-lg shadow border-l-4 ${priorityColors[task.priority] || 'border-gray-300'} relative group`}>
      
      {/* DELETE BUTTON - Appears on hover */}
      <button 
        onClick={(e) => {
          e.stopPropagation(); // Prevent drag
          if(window.confirm("Delete this task?")) deleteMutation.mutate();
        }}
        className="absolute top-2 right-2 text-gray-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition font-bold"
      >
        ✕
      </button>

      {/* DRAGGABLE AREA */}
      <div {...listeners} className="cursor-grab active:cursor-grabbing">
        <h3 className="font-semibold text-gray-800">{task.title}</h3>
        {task.description && <p className="text-sm text-gray-500 mt-1 line-clamp-2">{task.description}</p>}
        <div className="mt-3 flex justify-between items-center text-xs">
          <span className="bg-gray-100 px-2 py-1 rounded">{task.priority}</span>
        </div>
      </div>
    </div>
  );
}