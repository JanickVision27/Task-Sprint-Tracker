import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { projectApi } from '../api/endpoints';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // React Query fetches projects and caches them automatically
  const { data: projects, isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.getAll().then(res => res.data),
  });

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">My Projects</h1>
        <div className="flex items-center gap-4">
          <span className="text-gray-600">Hello, {user?.name || user?.email}</span>
          <button onClick={logout} className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600">
            Logout
          </button>
        </div>
      </div>

      {isLoading && <p>Loading projects...</p>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {projects?.map(project => (
          <div key={project.id} className="bg-white p-6 rounded-lg shadow cursor-pointer hover:shadow-lg transition"
            onClick={() => navigate(`/project/${project.id}`)}>
            <h2 className="text-xl font-semibold">{project.name}</h2>
            <p className="text-gray-500 mt-2">{project.description}</p>
          </div>
        ))}
      </div>
    </div>
  );
}