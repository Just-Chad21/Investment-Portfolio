// A simple chevron mark echoing Enviro365's own logo (two arrows in the brand's
// blue and green) — not a reproduction of their trademark asset, just a nod to it
// using the same sampled colors, sized for this app's header/login screen.
export default function BrandMark({ size = 26 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 26 26" fill="none" aria-hidden="true">
      <path d="M13 3 L5 13 L13 23" stroke="#3fa6d6" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M13 3 L21 13 L13 23" stroke="#77da3c" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
