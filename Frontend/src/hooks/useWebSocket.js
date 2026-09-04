import { useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';

export function useWebSocket(sprintId) {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  useEffect(() => {
    if (!isAuthenticated || !sprintId) return;

    const client = new Client({
      // The Spring endpoint is registered with SockJS, so use its SockJS transport
      // instead of a native WebSocket URL.
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      
      onConnect: () => {
        console.log('✅ Connected to WebSocket');
        
        client.subscribe(`/topic/sprints/${sprintId}/tasks`, (message) => {
          console.log('Real-time task update received:', message.body);
          queryClient.invalidateQueries({ queryKey: ['tasks', String(sprintId)] });
        });
      },
      
      onDisconnect: () => {
        console.log('Disconnected from WebSocket');
      },
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, [isAuthenticated, queryClient, sprintId]);
}
