export default function Modal({ isOpen, onClose, title, children }) {
  if (!isOpen) return null; // Don't render anything if it's closed

  return (
    // The dark overlay behind the modal
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onClick={onClose}>
      
      // The white modal box itself
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold">{title}</h2>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-800 text-2xl">&times;</button>
        </div>
        
        {/* This is where our form will go */}
        {children}
      </div>
    </div>
  );
}