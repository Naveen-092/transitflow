export default function Select({ label, error, id, className = '', children, ...props }) {
  const selectId = id || props.name;
  return (
    <div className={className}>
      {label && <label htmlFor={selectId} className="mb-1 block text-sm font-medium text-slate-700">{label}</label>}
      <select
        id={selectId}
        className={`w-full rounded-lg border bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
          error ? 'border-red-400' : 'border-slate-300'
        }`}
        {...props}
      >
        {children}
      </select>
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}