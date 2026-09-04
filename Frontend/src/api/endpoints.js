import api from './client';

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
};

// Sprints
export const sprintApi = {
  getAll: (projectId) => api.get(`/projects/${projectId}/sprints`),
  create: (projectId, data) => api.post(`/projects/${projectId}/sprints`, data),
};

// Tasks
export const taskApi = {
  getBySprint: (sprintId) => api.get(`/sprints/${sprintId}/tasks`),
  create: (sprintId, data) => api.post(`/sprints/${sprintId}/tasks`, data),
  update: (taskId, data) => api.put(`/tasks/${taskId}`, data),
  delete: (taskId) => api.delete(`/tasks/${taskId}`),
};