import axiosInstance from './axios';

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface ChatRequest {
  messages: ChatMessage[];
}

export interface ChatResponse {
  response: string;
}

export const sendChatMessage = async (messages: ChatMessage[]): Promise<string> => {
  const response = await axiosInstance.post<ChatResponse>('/api/v1/ai/chat', { messages });
  return response.data.response;
};
