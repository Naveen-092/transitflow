export default function LoadingSpinner({ size = 'md', center = false, label = 'Loading' }) {
  const sizes = { sm: 'h-4 w-4 border-2', md: 'h-8 w-8 border-4', lg: 'h-12 w-12 border-4' };
  const spinner = (
    <span
      role="status"
      aria-label={label}
      className={`inline-block animate-spin rounded-full border-slate-300 border-t-blue-600 ${sizes[size]}`}
    />
  );
  return center ? <div className="flex justify-center py-10">{spinner}</div> : spinner;
}