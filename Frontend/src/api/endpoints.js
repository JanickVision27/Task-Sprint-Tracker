import api from './client';

// Health check (wakes up Render container when user visits Login/Register)
export const healthApi = {
  check: () => api.get('/health'),
};

// Auth
export const authApi = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (data) => api.post('/auth/register', data),
};

// Projects
export const projectApi = {
  getAll: () => api.get('/projects'),
  create: (data) => api.post('/projects', data),
  getById: (id) => api.get(`/projects/${id}`),
  delete: (id) => api.delete(`/projects/${id}`),
};

// Sprints
export const sprintApi = {
  getAll: (projectId) => api.get(`/sprints/project/${projectId}`),
  create: (data) => api.post('/sprints', data),
  delete: (id) => api.delete(`/sprints/${id}`),
};

// Tasks
export const taskApi = {
  getBySprint: (sprintId) => api.get(`/tasks/sprint/${sprintId}`),
  create: (data) => api.post('/tasks', data),
  update: (taskId, data) => api.put(`/tasks/${taskId}`, data),
  delete: (taskId) => api.delete(`/tasks/${taskId}`),
};
