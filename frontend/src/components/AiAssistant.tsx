import React, { useState, useRef, useEffect } from 'react';
import { sendChatMessage, ChatMessage } from '../api/aiApi';
import { MessageSquare, X, Send, Minimize2, Maximize2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const AiAssistant: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);
  
  const [messages, setMessages] = useState<ChatMessage[]>([
    { role: 'assistant', content: 'Hi there! I am your EventEase Assistant. I can help you find events, check ticket availability, or answer questions about our platform. How can I help you today?' }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const toggleOpen = () => {
    setIsOpen(!isOpen);
    setIsMinimized(false);
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading, isOpen]);

  const handleSend = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!input.trim() || loading) return;

    const userMessage = input.trim();
    setInput('');
    
    const newMessages: ChatMessage[] = [...messages, { role: 'user', content: userMessage }];
    setMessages(newMessages);
    setLoading(true);

    try {
      // Send conversation history (last 5 messages to save tokens, plus system handles it)
      const recentMessages = newMessages.slice(-5);
      const reply = await sendChatMessage(recentMessages);
      setMessages([...newMessages, { role: 'assistant', content: reply }]);
    } catch (err: any) {
      console.error(err);
      setMessages([...newMessages, { 
        role: 'assistant', 
        content: err.response?.status === 401 || err.response?.status === 403 
          ? "Please log in to chat with me!"
          : "Sorry, I'm having trouble connecting right now. Please try again later."
      }]);
    } finally {
      setLoading(false);
    }
  };

  const handleQuickPrompt = (prompt: string) => {
    setInput(prompt);
  };

  if (!isAuthenticated) return null; // Only show for logged in users

  return (
    <div className="fixed bottom-6 right-6 z-50">
      {!isOpen && (
        <button
          onClick={toggleOpen}
          className="bg-primary-600 hover:bg-primary-700 text-white rounded-full p-4 shadow-xl transition-all transform hover:scale-105 flex items-center justify-center focus:outline-none focus:ring-4 focus:ring-primary-300"
          aria-label="Open AI Assistant"
          title="Open AI Assistant"
        >
          <MessageSquare className="h-6 w-6" />
        </button>
      )}

      {isOpen && (
        <div className={`bg-white border border-gray-200 shadow-2xl rounded-2xl flex flex-col transition-all overflow-hidden ${isMinimized ? 'w-80 h-16' : 'w-80 sm:w-96 h-[500px]'}`}>
          {/* Header */}
          <div className="bg-primary-600 px-4 py-3 text-white flex justify-between items-center cursor-pointer" onClick={() => setIsMinimized(!isMinimized)}>
            <div className="flex items-center space-x-2">
              <MessageSquare className="h-5 w-5" />
              <h3 className="font-semibold text-sm">EventEase Assistant</h3>
            </div>
            <div className="flex items-center space-x-2">
              <button 
                onClick={(e) => { e.stopPropagation(); setIsMinimized(!isMinimized); }} 
                className="text-primary-100 hover:text-white focus:outline-none"
                aria-label={isMinimized ? "Maximize" : "Minimize"}
                title={isMinimized ? "Maximize" : "Minimize"}
              >
                {isMinimized ? <Maximize2 className="h-4 w-4" /> : <Minimize2 className="h-4 w-4" />}
              </button>
              <button 
                onClick={(e) => { e.stopPropagation(); setIsOpen(false); }} 
                className="text-primary-100 hover:text-white focus:outline-none"
                aria-label="Close"
                title="Close"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>

          {!isMinimized && (
            <>
              {/* Messages Area */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
                {messages.map((msg, idx) => (
                  <div key={idx} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[85%] rounded-2xl px-4 py-2 ${
                      msg.role === 'user' 
                        ? 'bg-primary-600 text-white rounded-tr-sm' 
                        : 'bg-white text-gray-800 border border-gray-200 rounded-tl-sm shadow-sm'
                    }`}>
                      <p className="text-sm leading-relaxed whitespace-pre-wrap">{msg.content}</p>
                    </div>
                  </div>
                ))}
                
                {loading && (
                  <div className="flex justify-start">
                    <div className="bg-white border border-gray-200 rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm">
                      <div className="flex space-x-1.5 items-center">
                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                      </div>
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Suggestions (only when input is empty and not loading) */}
              {messages.length < 3 && !loading && (
                <div className="px-4 pb-2 bg-gray-50 flex flex-wrap gap-2">
                  <button onClick={() => handleQuickPrompt("Find events in New York")} className="text-xs bg-white border border-gray-200 text-gray-600 px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors">
                    Find events in New York
                  </button>
                  <button onClick={() => handleQuickPrompt("Show music events")} className="text-xs bg-white border border-gray-200 text-gray-600 px-3 py-1.5 rounded-full hover:bg-gray-100 transition-colors">
                    Show music events
                  </button>
                </div>
              )}

              {/* Input Area */}
              <div className="p-3 border-t border-gray-200 bg-white">
                <form onSubmit={handleSend} className="flex items-end space-x-2">
                  <textarea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault();
                        handleSend();
                      }
                    }}
                    placeholder="Ask about events..."
                    className="flex-1 max-h-32 min-h-[44px] bg-gray-50 border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none overflow-hidden"
                    rows={1}
                  />
                  <button
                    type="submit"
                    disabled={!input.trim() || loading}
                    className="flex-shrink-0 bg-primary-600 text-white rounded-full py-2 px-4 flex items-center hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm font-medium"
                  >
                    Send
                    <Send className="h-4 w-4 ml-1.5" />
                  </button>
                </form>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default AiAssistant;
